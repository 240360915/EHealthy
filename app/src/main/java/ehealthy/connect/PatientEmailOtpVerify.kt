package ehealthy.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PatientEmailOtpVerify(
    email: String,
    otpValues: List<String>,
    onOtpValueChange: (index: Int, value: String) -> Unit,
    onVerify: () -> Unit,
    onResendCode: () -> Unit,
    errorMessage: String? = null
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val green = Color(0xFF218B78)
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
            Text(
                "Enter your code",
                color = darkText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "We sent a 6-digit code to $email",
                color = greyText,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(36.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                otpValues.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { new ->
                            if (new.length <= 1 && new.all { it.isDigit() }) onOtpValueChange(
                                index,
                                new
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = green,
                            unfocusedBorderColor = Color(0xFFE2E5EC)
                        ),
                        modifier = Modifier.size(48.dp, 58.dp)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage, color = Color(0xFFD64545), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onVerify,
                enabled = otpValues.all { it.isNotEmpty() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF293147))
            ) {
                Text(
                    "Verify & Continue",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Didn't get a code? ", color = greyText, fontSize = 14.sp)
                Text(
                    "Resend", color = green, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onResendCode() })
            }
        }
    }
}
