package ehealthy.connect.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import java.security.MessageDigest
import java.util.UUID

private const val WEB_CLIENT_ID =
    "937650946248-q8ej01helfr2jnoq5jjv9cnhvlhnqfcu.apps.googleusercontent.com"

data class GoogleSignInInfo(
    val email: String,
    val firstName: String,
    val lastName: String
)

private fun generateNonce(): String {
    val rawNonce = UUID.randomUUID().toString()
    val bytes = rawNonce.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}

suspend fun signInWithGoogle(context: Context): Result<GoogleSignInInfo> {
    return try {
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
            serverClientId = WEB_CLIENT_ID
        )
            .setNonce(generateNonce())
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        val credentialManager = CredentialManager.create(context)
        val result = credentialManager.getCredential(context, request)

        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return Result.failure(Exception("Unexpected credential type"))
        }

        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

        SupabaseClientProvider.client.auth.signInWith(IDToken) {
            idToken = googleIdTokenCredential.idToken
            provider = Google
        }

        val info = GoogleSignInInfo(
            email = googleIdTokenCredential.id,
            firstName = googleIdTokenCredential.givenName ?: "",
            lastName = googleIdTokenCredential.familyName ?: ""
        )

        Result.success(info)
    } catch (e: Exception) {
        Log.e("GoogleAuth", "Sign-in failed", e)
        Result.failure(e)
    }
}