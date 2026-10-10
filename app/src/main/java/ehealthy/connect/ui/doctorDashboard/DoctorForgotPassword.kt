package ehealthy.connect.ui.doctorDashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.common.AuthColors
import ehealthy.connect.ui.common.AuthErrorMessage
import ehealthy.connect.ui.common.AuthHeader
import ehealthy.connect.ui.common.AuthHeroIcon
import ehealthy.connect.ui.common.AuthPage
import ehealthy.connect.ui.common.AuthPrimaryButton
import ehealthy.connect.ui.common.authTextFieldColors
import ehealthy.connect.ui.common.isValidEmail

@Composable
fun DoctorForgotPassword(
    email: String,
    onEmailChange: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSendCode: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val accent = AuthColors.DoctorAccent
    val textFieldColors = authTextFieldColors(accent)

    AuthPage {
        AuthHeroIcon(
            icon = Icons.Outlined.Email,
            accent = accent,
            background = AuthColors.DoctorSoft
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(
            title = "Reset your password",
            subtitle = "Enter your doctor account email and we'll send you a 6-digit reset code."
        )

        Spacer(modifier = Modifier.height(32.dp))

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

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            AuthErrorMessage(errorMessage, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(26.dp))

        AuthPrimaryButton(
            text = "Send reset code",
            onClick = onSendCode,
            enabled = isValidEmail(email),
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
