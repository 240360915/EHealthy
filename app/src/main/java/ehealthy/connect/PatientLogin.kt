package ehealthy.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PatientLogin(
    isLoading: Boolean = false,
    email: String,
    onEmailChange: (String) -> Unit,
    onContinueWithGoogle: () -> Unit,
    onContinueWithEmail: () -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val green = Color(0xFF218B78)
    val lightBlue = Color(0xFFEFF1FF)
    val greyText = Color(0xFF4F555C)

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(horizontal = 28.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(24.dp))
                    .background(lightBlue),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF27A88E))
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF087A66))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Welcome, patient",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = darkText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "New here? Sign up in seconds.\nAlready registered? Sign in below.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = greyText,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ------------------------------------------------
            // CONTINUE WITH GOOGLE (new signup or existing account)
            // ------------------------------------------------

            Button(
                onClick = onContinueWithGoogle,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = darkText
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E5EC))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = green
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFE2E5EC)))
                Text(
                    "  OR SIGN IN WITH EMAIL  ",
                    color = Color(0xFF9099A6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFE2E5EC)))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------------
            // RETURNING USER — EMAIL ONLY (verified via OTP next screen)
            // ------------------------------------------------

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("your@email.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = green),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onContinueWithEmail,
                enabled = email.contains("@") && email.contains("."),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF293147))
            ) {
                Text(
                    "Continue",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By continuing, you agree to our Terms & Privacy Policy",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color(0xFF9099A6),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}