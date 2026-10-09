package ehealthy.connect.ui.doctor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MedicalServices
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
import ehealthy.connect.ui.common.authTextFieldColors
import ehealthy.connect.ui.common.isValidEmail

@Composable
fun DoctorLogin(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit
) {
    val accent = AuthColors.DoctorAccent
    val textFieldColors = authTextFieldColors(accent)
    var passwordVisible by remember { mutableStateOf(false) }

    AuthPage {
        AuthHeroIcon(
            icon = Icons.Outlined.MedicalServices,
            accent = accent,
            background = AuthColors.DoctorSoft
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(
            title = "Doctor sign in",
            subtitle = "Access appointments, patient consultations, prescriptions, and your professional account."
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
                .clickable(enabled = !isLoading) { onForgotPassword() }
        )

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            AuthErrorMessage(errorMessage, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(26.dp))

        AuthPrimaryButton(
            text = "Sign in",
            onClick = onLogin,
            enabled = isValidEmail(email) && password.isNotBlank(),
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text("New to e-Health Connect? ", color = AuthColors.TextSecondary, fontSize = 14.sp)
            Text(
                text = "Apply as a doctor",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(enabled = !isLoading) { onRegister() }
            )
        }
    }
}
