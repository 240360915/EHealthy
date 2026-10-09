package ehealthy.connect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfUseScreen(
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = AuthColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Terms of Use",
                        color = AuthColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AuthColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AuthColors.Background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Text(
                text = "Using e-Health Connect",
                color = AuthColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "These terms describe the basic rules for using e-Health Connect.",
                color = AuthColors.TextSecondary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            TermsSection(
                title = "1. Your account",
                body = "Provide accurate information when creating your account and keep your login details secure. You are responsible for activity performed through your account."
            )

            TermsSection(
                title = "2. Healthcare services",
                body = "e-Health Connect helps patients and healthcare professionals communicate, manage appointments, and exchange health-related information. The platform is not an emergency service and should not be used for urgent or life-threatening situations."
            )

            TermsSection(
                title = "3. Appropriate use",
                body = "Do not misuse the platform, impersonate another person, submit fraudulent information, interfere with the service, or use another person's health information without authorization."
            )

            TermsSection(
                title = "4. Information and privacy",
                body = "Personal and health information is handled according to the Privacy Policy and the permissions available within the app."
            )

            TermsSection(
                title = "5. Service availability",
                body = "Features may change, be temporarily unavailable, or require maintenance. We aim to keep the service reliable but cannot guarantee uninterrupted availability."
            )

            TermsSection(
                title = "6. Professional responsibility",
                body = "Healthcare professionals remain responsible for their clinical decisions, professional obligations, and the accuracy of information they provide through the platform."
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun TermsSection(
    title: String,
    body: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = AuthColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            color = AuthColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}
