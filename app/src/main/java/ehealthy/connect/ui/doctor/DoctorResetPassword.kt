package ehealthy.connect.ui.doctor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.common.AuthColors
import ehealthy.connect.ui.common.AuthErrorMessage
import ehealthy.connect.ui.common.AuthHeader
import ehealthy.connect.ui.common.AuthHeroIcon
import ehealthy.connect.ui.common.AuthPage
import ehealthy.connect.ui.common.AuthPrimaryButton
import ehealthy.connect.ui.common.PasswordRequirementsChecklist
import ehealthy.connect.ui.common.PasswordStrengthMeter
import ehealthy.connect.ui.common.authTextFieldColors
import ehealthy.connect.ui.common.isPasswordValid

@Composable
fun DoctorResetPassword(
    email: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (code: String, newPassword: String) -> Unit,
    onResendCode: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val accent = AuthColors.DoctorAccent
    val textFieldColors = authTextFieldColors(accent)

    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordFieldFocused by remember { mutableStateOf(false) }

    val passwordsMatch = confirmPassword.isEmpty() || newPassword == confirmPassword
    val formValid =
        code.length == 6 && code.all(Char::isDigit) &&
                isPasswordValid(newPassword) && newPassword == confirmPassword

    AuthPage {
        AuthHeroIcon(
            icon = Icons.Outlined.Security,
            accent = accent,
            background = AuthColors.DoctorSoft
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(
            title = "Choose a new password",
            subtitle = "Enter the 6-digit code sent to ${email.trim()} and create a new password."
        )

        Spacer(modifier = Modifier.height(30.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { raw -> code = raw.filter(Char::isDigit).take(6) },
            label = { Text("6-digit code") },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Didn't receive it? Resend code",
            color = accent,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.End)
                .clickable(enabled = !isLoading) { onResendCode() }
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New password") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                    Icon(
                        imageVector = if (newPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                        tint = AuthColors.TextSecondary
                    )
                }
            },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = textFieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusEvent { passwordFieldFocused = it.isFocused }
        )

        if (passwordFieldFocused || newPassword.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            PasswordStrengthMeter(password = newPassword)
            Spacer(modifier = Modifier.height(10.dp))
            PasswordRequirementsChecklist(password = newPassword)
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm new password") },
            singleLine = true,
            enabled = !isLoading,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                        tint = AuthColors.TextSecondary
                    )
                }
            },
            supportingText = {
                if (!passwordsMatch) {
                    Text("Passwords don't match", color = AuthColors.Error)
                }
            },
            isError = !passwordsMatch,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = textFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            AuthErrorMessage(errorMessage, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(26.dp))

        AuthPrimaryButton(
            text = "Reset password",
            onClick = { onSubmit(code, newPassword) },
            enabled = formValid,
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Back to sign in",
            color = AuthColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(enabled = !isLoading) { onBackToLogin() }
        )
    }
}
