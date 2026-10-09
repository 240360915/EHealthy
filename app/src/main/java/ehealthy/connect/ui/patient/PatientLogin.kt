package ehealthy.connect.ui.patient

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import ehealthy.connect.ui.common.AuthSuccessMessage
import ehealthy.connect.ui.common.PasswordRequirementsChecklist
import ehealthy.connect.ui.common.PasswordStrengthMeter
import ehealthy.connect.ui.common.authTextFieldColors
import ehealthy.connect.ui.common.isPasswordValid
import ehealthy.connect.ui.common.isValidEmail

enum class LoginMode { LOGIN, FORGOT_REQUEST, FORGOT_VERIFY, FORGOT_RESET }

@Composable
fun PatientLogin(
    mode: LoginMode,
    isLoading: Boolean,
    email: String,
    password: String,
    otp: String,
    newPassword: String,
    confirmPassword: String,
    errorMessage: String?,
    successMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSendResetCode: () -> Unit,
    onVerifyResetCode: () -> Unit,
    onResetPassword: () -> Unit,
    onBackToLogin: () -> Unit,
    onGoToRegister: () -> Unit
) {
    val accent = AuthColors.PatientAccent
    val textFieldColors = authTextFieldColors(accent)

    var passwordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val emailValid = isValidEmail(email)
    val passwordsMatch = confirmPassword.isEmpty() || newPassword == confirmPassword

    val heroIcon: ImageVector = when (mode) {
        LoginMode.LOGIN -> Icons.Outlined.Person
        LoginMode.FORGOT_REQUEST -> Icons.Outlined.Security
        LoginMode.FORGOT_VERIFY -> Icons.Outlined.Email
        LoginMode.FORGOT_RESET -> Icons.Outlined.Security
    }

    val title = when (mode) {
        LoginMode.LOGIN -> "Patient sign in"
        LoginMode.FORGOT_REQUEST -> "Reset your password"
        LoginMode.FORGOT_VERIFY -> "Check your email"
        LoginMode.FORGOT_RESET -> "Choose a new password"
    }

    val subtitle = when (mode) {
        LoginMode.LOGIN -> "Access appointments, consultations, prescriptions, and your health information."
        LoginMode.FORGOT_REQUEST -> "Enter your account email and we'll send you a 6-digit reset code."
        LoginMode.FORGOT_VERIFY -> "Enter the 6-digit code sent to ${email.trim()}."
        LoginMode.FORGOT_RESET -> "Create a strong new password for your patient account."
    }

    AuthPage {
        AuthHeroIcon(
            icon = heroIcon,
            accent = accent,
            background = AuthColors.PatientSoft
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(title = title, subtitle = subtitle)

        Spacer(modifier = Modifier.height(32.dp))

        when (mode) {
            LoginMode.LOGIN -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email address") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = AuthColors.TextSecondary
                            )
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Forgot password?",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(enabled = !isLoading) { onForgotPasswordClick() }
                )
            }

            LoginMode.FORGOT_REQUEST -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text("Email address") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LoginMode.FORGOT_VERIFY -> {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { raw -> onOtpChange(raw.filter(Char::isDigit).take(6)) },
                    label = { Text("6-digit code") },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LoginMode.FORGOT_RESET -> {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
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
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                PasswordStrengthMeter(password = newPassword)
                Spacer(modifier = Modifier.height(10.dp))
                PasswordRequirementsChecklist(password = newPassword)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm new password") },
                    singleLine = true,
                    enabled = !isLoading,
                    isError = !passwordsMatch,
                    supportingText = {
                        if (!passwordsMatch) {
                            Text("Passwords don't match", color = AuthColors.Error)
                        }
                    },
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            AuthErrorMessage(errorMessage, modifier = Modifier.fillMaxWidth())
        }

        if (!successMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            AuthSuccessMessage(successMessage, accent, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(26.dp))

        AuthPrimaryButton(
            text = when (mode) {
                LoginMode.LOGIN -> "Sign in"
                LoginMode.FORGOT_REQUEST -> "Send reset code"
                LoginMode.FORGOT_VERIFY -> "Verify code"
                LoginMode.FORGOT_RESET -> "Reset password"
            },
            onClick = when (mode) {
                LoginMode.LOGIN -> onLogin
                LoginMode.FORGOT_REQUEST -> onSendResetCode
                LoginMode.FORGOT_VERIFY -> onVerifyResetCode
                LoginMode.FORGOT_RESET -> onResetPassword
            },
            enabled = when (mode) {
                LoginMode.LOGIN -> emailValid && password.isNotBlank()
                LoginMode.FORGOT_REQUEST -> emailValid
                LoginMode.FORGOT_VERIFY -> otp.length == 6 && otp.all(Char::isDigit)
                LoginMode.FORGOT_RESET -> isPasswordValid(newPassword) && newPassword == confirmPassword
            },
            isLoading = isLoading
        )

        if (mode != LoginMode.LOGIN) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Back to sign in",
                color = AuthColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = !isLoading) { onBackToLogin() }
            )
        } else {
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("New to e-Health Connect? ", color = AuthColors.TextSecondary, fontSize = 14.sp)
                Text(
                    text = "Create account",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !isLoading) { onGoToRegister() }
                )
            }
        }
    }
}
