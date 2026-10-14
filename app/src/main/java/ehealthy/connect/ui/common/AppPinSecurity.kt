package ehealthy.connect.ui.common

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Optional local screen lock, NOT a replacement for Supabase authentication
 * or full database encryption. The PIN is never persisted.
 * Its verifier is an Android Keystore-keyed HMAC, scoped by signed-in user.
 * App backups are disabled in AndroidManifest.
 */
object AppPinStore {
    private const val PREFS = "ehealthy_private_pin_v1"
    private const val MAX_FAILURES = 5
    private const val LOCKOUT_MS = 60_000L

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun macFor(accountId: String, pin: String): ByteArray {
        require(accountId.isNotBlank())
        val alias = "ehealthy_local_pin_" + accountId
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = (keystore.getKey(alias, null) as? SecretKey)
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
                .run {
                    init(
                        KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                            .setDigests(KeyProperties.DIGEST_SHA256)
                            .build()
                    )
                    generateKey()
                }
        return Mac.getInstance("HmacSHA256").apply { init(key) }
            .doFinal((accountId + ":" + pin).toByteArray(Charsets.UTF_8))
    }

    fun hasPin(context: Context, accountId: String): Boolean =
        accountId.isNotBlank() && prefs(context).contains("hash_" + accountId)

    fun enable(context: Context, accountId: String, pin: String) {
        require(pin.length == 6 && pin.all(Char::isDigit)) {
            "Choose exactly six digits for your app PIN."
        }
        require(!hasPin(context, accountId)) { "An app PIN is already enabled." }
        val encoded = Base64.encodeToString(macFor(accountId, pin), Base64.NO_WRAP)
        prefs(context).edit().putString("hash_" + accountId, encoded)
            .remove("fail_" + accountId).remove("blocked_" + accountId).apply()
    }

    /** Negative result includes remaining lockout seconds. */
    data class Check(val accepted: Boolean, val waitSeconds: Long = 0L)

    fun check(context: Context, accountId: String, pin: String): Check {
        if (!hasPin(context, accountId)) return Check(false)
        val p = prefs(context)
        val blockedUntil = p.getLong("blocked_" + accountId, 0L)
        val remaining = blockedUntil - System.currentTimeMillis()
        if (remaining > 0L) return Check(false, (remaining + 999L) / 1000L)
        if (pin.length != 6 || !pin.all(Char::isDigit)) return Check(false)

        val stored = p.getString("hash_" + accountId, null) ?: return Check(false)
        val storedBytes = Base64.decode(stored, Base64.NO_WRAP)
        val correct = MessageDigest.isEqual(storedBytes, macFor(accountId, pin))
        if (correct) {
            p.edit().remove("fail_" + accountId).remove("blocked_" + accountId).apply()
            return Check(true)
        }

        val failures = p.getInt("fail_" + accountId, 0) + 1
        val shouldBlock = failures >= MAX_FAILURES
        p.edit()
            .putInt("fail_" + accountId, if (shouldBlock) 0 else failures)
            .putLong(
                "blocked_" + accountId,
                if (shouldBlock) System.currentTimeMillis() + LOCKOUT_MS else 0L
            )
            .apply()
        return Check(false, if (shouldBlock) LOCKOUT_MS / 1000 else 0L)
    }

    fun disable(context: Context, accountId: String, currentPin: String): Check {
        val result = check(context, accountId, currentPin)
        if (!result.accepted) return result
        prefs(context).edit().remove("hash_" + accountId)
            .remove("fail_" + accountId).remove("blocked_" + accountId).apply()
        // Keep the Android Keystore key: it cannot be extracted and is
        // safely reused if this account enables the PIN again.
        return Check(true)
    }

    fun change(context: Context, accountId: String, oldPin: String, newPin: String): Check {
        require(newPin.length == 6 && newPin.all(Char::isDigit))
        val result = check(context, accountId, oldPin)
        if (!result.accepted) return result
        val encoded = Base64.encodeToString(macFor(accountId, newPin), Base64.NO_WRAP)
        prefs(context).edit().putString("hash_" + accountId, encoded).apply()
        return Check(true)
    }
}

@Composable
private fun PinEntry(label: String, value: String, onChanged: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChanged(it.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Always modal, never dismissed by Back. Only unlocks the local screen. */
@Composable
fun AppPinGate(
    context: Context,
    accountId: String,
    onUnlocked: () -> Unit
) {
    var pin by remember(accountId) { mutableStateOf("") }
    var message by remember(accountId) { mutableStateOf<String?>(null) }
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.widthIn(max = 380.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Outlined.Lock, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp))
                    Text("Unlock EHealthy", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text(
                        "Enter your optional six-digit app PIN to access your health information.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PinEntry("App PIN", pin) { pin = it; message = null }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pin.length == 6,
                        onClick = {
                            val result = runCatching { AppPinStore.check(context, accountId, pin) }
                            result.onSuccess {
                                if (it.accepted) onUnlocked()
                                else {
                                    pin = ""
                                    message = if (it.waitSeconds > 0)
                                        "Too many attempts. Try again in " + it.waitSeconds + " seconds."
                                    else "Incorrect PIN."
                                }
                            }.onFailure {
                                message = "Unable to unlock the PIN. You can recover by clearing the app's local storage and signing in again."
                            }
                        }
                    ) { Text("Unlock") }
                    Text(
                        "Forgot the PIN? Clearing this app's local storage will reset the device PIN and require you to sign in again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPinSettingsScreen(
    context: Context,
    accountId: String?,
    onBack: () -> Unit
) {
    var enabled by remember(accountId) {
        mutableStateOf(accountId != null && AppPinStore.hasPin(context, accountId))
    }
    var oldPin by remember(accountId) { mutableStateOf("") }
    var newPin by remember(accountId) { mutableStateOf("") }
    var confirmPin by remember(accountId) { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var changing by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("App PIN security") },
            navigationIcon = {
                TextButton(onClick = onBack) { Text("Back") }
            }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (enabled) "PIN protection is enabled" else "PIN protection is off",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Optional device-only privacy lock. Your PIN is not stored or sent to Supabase. " +
                    "This does not replace your account password.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (accountId.isNullOrBlank()) {
                Text("Sign in to manage your device PIN.")
                return@Column
            }

            if (enabled) {
                PinEntry("Current PIN", oldPin) { oldPin = it; message = null }
            }
            if (!enabled || changing) {
                PinEntry("New six-digit PIN", newPin) { newPin = it; message = null }
                PinEntry("Confirm new PIN", confirmPin) { confirmPin = it; message = null }
            }

            message?.let {
                Text(it, color = if (it.contains("successfully") || it.contains("enabled") ||
                    it.contains("disabled")) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error)
            }
            if (!enabled || changing) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = newPin.length == 6 && confirmPin.length == 6 &&
                        (!enabled || oldPin.length == 6),
                    onClick = {
                        if (newPin != confirmPin) {
                            message = "PINs do not match."
                        } else {
                            runCatching {
                                if (enabled) AppPinStore.change(context, accountId, oldPin, newPin)
                                else {
                                    AppPinStore.enable(context, accountId, newPin)
                                    AppPinStore.Check(true)
                                }
                            }.onSuccess { outcome ->
                                if (outcome.accepted) {
                                    enabled = true; changing = false
                                    newPin = ""; confirmPin = ""; oldPin = ""
                                    message = "PIN protection enabled successfully."
                                } else {
                                    message = if (outcome.waitSeconds > 0)
                                        "Too many attempts. Wait " + outcome.waitSeconds + " seconds."
                                    else "Incorrect current PIN."
                                }
                            }.onFailure { message = it.message ?: "Could not save PIN." }
                        }
                    }
                ) { Text(if (enabled) "Change PIN" else "Enable PIN") }
            }
            if (enabled) {
                if (!changing) OutlinedButton(onClick = { changing = true; message = null }) {
                    Text("Change PIN")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = oldPin.length == 6,
                    onClick = {
                        runCatching { AppPinStore.disable(context, accountId, oldPin) }
                            .onSuccess { result ->
                                if (result.accepted) {
                                    enabled = false; oldPin = ""; newPin = ""; confirmPin = ""
                                    changing = false
                                    message = "App PIN disabled successfully."
                                } else {
                                    message = if (result.waitSeconds > 0)
                                        "Try again in " + result.waitSeconds + " seconds."
                                    else "Incorrect current PIN."
                                }
                            }.onFailure { message = it.message ?: "Unable to disable PIN." }
                    }
                ) { Text("Disable PIN") }
            }
        }
    }
}
