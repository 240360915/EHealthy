package ehealthy.connect.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.common.PasswordRequirementsChecklist
import ehealthy.connect.ui.common.PasswordStrengthMeter
import ehealthy.connect.ui.common.isPasswordValid

@Composable
fun DoctorResetPassword(
    email: String,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (code: String, newPassword: String) -> Unit,
    onResendCode: () -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val blue = Color(0xFF385A9E)
    val greyText = Color(0xFF4F555C)
    val navyButton = Color(0xFF293147)
    val borderGrey = Color(0xFFE2E5EC)

    // Explicit colors so typed text is always visible, regardless of the
    // device's light/dark theme.
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = blue,
        unfocusedBorderColor = borderGrey,
        focusedTextColor = darkText,
        unfocusedTextColor = darkText,
        cursorColor = blue,
        focusedLabelColor = blue,
        unfocusedLabelColor = greyText,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedPlaceholderColor = greyText,
        unfocusedPlaceholderColor = greyText
    )

    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordFieldFocused by remember { mutableStateOf(false) }

    val passwordsMatch = confirmPassword.isEmpty() ||
            confirmPassword.length < newPassword.length ||
            newPassword == confirmPassword
    val formValid =
        code.length == 6 && isPasswordValid(newPassword) && newPassword == confirmPassword

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Enter your code",
                color = darkText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "We sent a 6-digit code to $email. Enter it below with your new password.",
                color = greyText,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { new ->
                    if (new.length <= 6 && new.all { it.isDigit() }) code = new
                },
                label = { Text("6-digit code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Didn't get a code? Resend",
                color = blue,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onResendCode)
            )

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = greyText
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
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
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                supportingText = {
                    if (!passwordsMatch) {
                        Text("Passwords don't match", color = Color(0xFFD64545))
                    }
                },
                isError = !passwordsMatch,
                shape = RoundedCornerShape(16.dp),
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFD64545),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onSubmit(code, newPassword) },
                enabled = formValid && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyButton)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        "Reset password",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}