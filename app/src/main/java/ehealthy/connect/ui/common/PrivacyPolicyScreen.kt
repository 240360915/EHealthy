package ehealthy.connect.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit
) {

    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text = "Privacy Policy",
                            color = AuthColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "How EHealthy handles your information",
                            color = AuthColors.TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(
                                        AuthColors.PatientSoft,
                                        CircleShape
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint =
                                    AuthColors.PatientAccent,
                                modifier =
                                    Modifier.size(20.dp)
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            AuthColors.Background
                    )
            )
        },

        containerColor =
            AuthColors.Background

    ) { paddingValues ->


        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 18.dp
                    )
        ) {

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            /*
             * Privacy introduction
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(22.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            AuthColors.PatientSoft
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(17.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(54.dp)
                                .background(
                                    AuthColors.PatientAccent
                                        .copy(alpha = 0.12f),
                                    RoundedCornerShape(16.dp)
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.PrivacyTip,
                            contentDescription = null,
                            tint =
                                AuthColors.PatientAccent,
                            modifier =
                                Modifier.size(27.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Your privacy matters",
                            color =
                                AuthColors.TextPrimary,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                14.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(
                            text =
                                "This policy explains how e-Health Connect collects, uses and protects your personal and health information.",
                            color =
                                AuthColors.TextSecondary,
                            fontSize =
                                10.5.sp,
                            lineHeight =
                                15.sp
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            /*
             * Information collection
             */
            PolicySection(
                number = "01",
                icon =
                    Icons.Outlined.DataUsage,
                title =
                    "Information We Collect",
                accent =
                    AuthColors.DoctorAccent,
                background =
                    AuthColors.DoctorSoft,
                body =
                    "When you register as a patient, we collect personal details (name, date of birth, ID number, contact information, address), and medical information you choose to share (allergies, medications, chronic conditions, surgical history) to help doctors on our platform provide you with safe and informed care."
            )


            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )


            /*
             * Usage
             */
            PolicySection(
                number = "02",
                icon =
                    Icons.Outlined.Policy,
                title =
                    "How We Use Your Information",
                accent =
                    AuthColors.DoctorAccent,
                background =
                    AuthColors.DoctorSoft,
                body =
                    "Your information is used to create and manage your account, connect you with registered healthcare professionals, support appointment booking and consultations, and maintain accurate medical records for continuity of care. We do not sell your personal or medical information to third parties."
            )


            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )


            /*
             * Security
             */
            PolicySection(
                number = "03",
                icon =
                    Icons.Outlined.Security,
                title =
                    "Data Security",
                accent =
                    AuthColors.DoctorAccent,
                background =
                    AuthColors.DoctorSoft,
                body =
                    "We apply technical and organizational safeguards to protect your information. Access controls are used to restrict health information to authorized users and legitimate healthcare purposes."
            )


            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )


            /*
             * Rights
             */
            PolicySection(
                number = "04",
                icon =
                    Icons.Outlined.VerifiedUser,
                title =
                    "Your Rights",
                accent =
                    AuthColors.PatientAccent,
                background =
                    AuthColors.PatientSoft,
                body =
                    "You may request access to, correction of, or deletion of your personal information at any time, subject to our obligation to retain certain medical records as required by applicable healthcare regulations. You may also withdraw consent for optional data uses."
            )


            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )


            /*
             * Compliance
             */
            PolicySection(
                number = "05",
                icon =
                    Icons.Outlined.Gavel,
                title =
                    "Compliance",
                accent =
                    AuthColors.PatientAccent,
                background =
                    AuthColors.PatientSoft,
                body =
                    "Our privacy practices are designed around South Africa's Protection of Personal Information Act (POPIA) and applicable healthcare record-keeping requirements."
            )


            Spacer(
                modifier =
                    Modifier.height(13.dp)
            )


            /*
             * Contact
             */
            PolicySection(
                number = "06",
                icon =
                    Icons.Outlined.ContactSupport,
                title =
                    "Contact Us",
                accent =
                    AuthColors.DoctorAccent,
                background =
                    AuthColors.DoctorSoft,
                body =
                    "For questions about this policy or your data, please contact our support team through the Contact Us section of the app."
            )


            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )


            /*
             * Privacy footer
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(18.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            AuthColors.SurfaceMuted
                    ),

                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            AuthColors.PatientAccent
                                .copy(alpha = 0.10f)
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(15.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .background(
                                    AuthColors.PatientAccent
                                        .copy(alpha = 0.10f),
                                    CircleShape
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Lock,
                            contentDescription = null,
                            tint =
                                AuthColors.PatientAccent,
                            modifier =
                                Modifier.size(19.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Privacy & trust",
                            color =
                                AuthColors.TextPrimary,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                11.5.sp
                        )


                        Text(
                            text =
                                "Your personal and health information should only be used for legitimate healthcare and account-related purposes.",
                            color =
                                AuthColors.TextSecondary,
                            fontSize =
                                9.5.sp,
                            lineHeight =
                                13.sp
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )
        }
    }
}


@Composable
private fun PolicySection(
    number: String,
    icon: ImageVector,
    title: String,
    accent: Color,
    background: Color,
    body: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    AuthColors.Background
            ),

        border =
            BorderStroke(
                width = 1.dp,
                color =
                    accent.copy(alpha = 0.10f)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(43.dp)
                            .background(
                                background,
                                RoundedCornerShape(13.dp)
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            icon,
                        contentDescription = null,
                        tint =
                            accent,
                        modifier =
                            Modifier.size(21.dp)
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            number,
                        color =
                            accent,
                        fontSize =
                            9.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Text(
                        text =
                            title,
                        color =
                            AuthColors.TextPrimary,
                        fontSize =
                            13.5.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            Text(
                text =
                    body,
                color =
                    AuthColors.TextSecondary,
                fontSize =
                    11.sp,
                lineHeight =
                    17.sp
            )
        }
    }
}