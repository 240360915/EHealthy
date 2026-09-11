package ehealthy.connect

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val green = Color(0xFF218B78)
    val lightGreen = Color(0xFFDFF5F1)
    val greyText = Color(0xFF4F555C)
    val navy = Color(0xFF293147)

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp)
                .padding(paddingValues)
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ---- PATIENT ILLUSTRATION ----
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(lightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.patient),
                        contentDescription = null,

                        )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                when (mode) {
                    LoginMode.LOGIN -> "Welcome Back"
                    LoginMode.FORGOT_REQUEST -> "Reset your password"
                    LoginMode.FORGOT_VERIFY -> "Check your email"
                    LoginMode.FORGOT_RESET -> "Choose a new password"
                },
                color = darkText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                when (mode) {
                    LoginMode.LOGIN -> "Enter your email and password to sign in."
                    LoginMode.FORGOT_REQUEST -> "Enter your email and we'll send you a code."
                    LoginMode.FORGOT_VERIFY -> "Enter the 6-digit code we sent to $email"
                    LoginMode.FORGOT_RESET -> "Set a new password for your account."
                },
                color = greyText, fontSize = 15.sp, lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(28.dp))

            when (mode) {

                // ---- LOGIN: email + password together ----
                LoginMode.LOGIN -> {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email address") },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = onForgotPasswordClick) {
                        Text(
                            "Forgot password?",
                            color = green,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // ---- FORGOT: step 1, request code ----
                LoginMode.FORGOT_REQUEST -> {
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        label = { Text("Email address") },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- FORGOT: step 2, enter code ----
                LoginMode.FORGOT_VERIFY -> {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6) onOtpChange(it) },
                        label = { Text("6-digit code") },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- FORGOT: step 3, set new password ----
                LoginMode.FORGOT_RESET -> {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = onNewPasswordChange,
                        label = { Text("New password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasswordStrengthChecklist(newPassword)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        enabled = !isLoading,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = Color(0xFFD64545), fontSize = 13.sp)
            }
            successMessage?.let {
                Spacer(modifier = Modifier.height(10.dp))
                Text(it, color = green, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = when (mode) {
                    LoginMode.LOGIN -> onLogin
                    LoginMode.FORGOT_REQUEST -> onSendResetCode
                    LoginMode.FORGOT_VERIFY -> onVerifyResetCode
                    LoginMode.FORGOT_RESET -> onResetPassword
                },
                enabled = !isLoading && when (mode) {
                    LoginMode.LOGIN -> email.isNotBlank() && password.isNotBlank()
                    LoginMode.FORGOT_REQUEST -> email.isNotBlank()
                    LoginMode.FORGOT_VERIFY -> otp.length == 6
                    LoginMode.FORGOT_RESET -> isPasswordStrong(newPassword) && newPassword == confirmPassword
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navy)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        when (mode) {
                            LoginMode.LOGIN -> "Login"
                            LoginMode.FORGOT_REQUEST -> "Send code"
                            LoginMode.FORGOT_VERIFY -> "Verify code"
                            LoginMode.FORGOT_RESET -> "Reset password"
                        },
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            if (mode != LoginMode.LOGIN) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBackToLogin) {
                    Text(
                        "Back to login",
                        color = greyText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (mode == LoginMode.LOGIN) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Don't have an account? ", color = greyText, fontSize = 14.sp)
                    Text(
                        "Register here",
                        color = green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onGoToRegister() }
                    )
                }
            }
        }
    }
}