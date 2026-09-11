package ehealthy.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val greyText = Color(0xFF4F555C)
    val green = Color(0xFF218B78)

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = darkText)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Privacy Policy", color = darkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "e-Health Connect",
                color = green,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(20.dp))

            PolicySection(
                "1. Information We Collect", darkText, green, greyText,
                "When you register as a patient, we collect personal details (name, date of birth, ID number, contact information, address), and medical information you choose to share (allergies, medications, chronic conditions, surgical history) to help doctors on our platform provide you with safe and informed care."
            )
            PolicySection(
                "2. How We Use Your Information", darkText, green, greyText,
                "Your information is used to create and manage your account, connect you with registered healthcare professionals, support appointment booking and consultations, and maintain accurate medical records for continuity of care. We do not sell your personal or medical information to third parties."
            )
            PolicySection(
                "3. Data Security", darkText, green, greyText,
                "We apply reasonable technical and organizational safeguards to protect your information, including encrypted storage and access controls limiting who can view your medical records to you and your treating healthcare providers."
            )
            PolicySection(
                "4. Your Rights", darkText, green, greyText,
                "You may request access to, correction of, or deletion of your personal information at any time, subject to our obligation to retain certain medical records as required by applicable healthcare regulations. You may also withdraw consent for optional data uses."
            )
            PolicySection(
                "5. Compliance", darkText, green, greyText,
                "We process personal information in accordance with South Africa's Protection of Personal Information Act (POPIA) and applicable healthcare record-keeping requirements."
            )
            PolicySection(
                "6. Contact Us", darkText, green, greyText,
                "For questions about this policy or your data, please contact our support team through the Contact Us section of the app."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PolicySection(
    title: String,
    titleColor: Color,
    accent: Color,
    bodyColor: Color,
    body: String
) {
    Text(title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(6.dp))
    Text(body, color = bodyColor, fontSize = 13.5.sp, lineHeight = 21.sp)
    Spacer(modifier = Modifier.height(18.dp))
}