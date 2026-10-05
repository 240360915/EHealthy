package ehealthy.connect.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.patientDashboard.PatientColors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactUsScreen(
    onBack: () -> Unit,
    onEmailSupport: () -> Unit,
    onCallSupport: () -> Unit,
    onReportProblem: () -> Unit
) {

    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text = "Contact Us",
                            color = PatientColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "We're here when you need help",
                            color = PatientColors.TextSecondary,
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
                                        PatientColors.DoctorCard,
                                        CircleShape
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PatientColors.DoctorAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
            )
        },

        containerColor =
            MaterialTheme.colorScheme.background

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
             * Support hero
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(24.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.DoctorCard
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(68.dp)
                                .background(
                                    PatientColors.DoctorAccent
                                        .copy(alpha = 0.12f),
                                    CircleShape
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.SupportAgent,
                            contentDescription = null,
                            tint =
                                PatientColors.DoctorAccent,
                            modifier =
                                Modifier.size(33.dp)
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )


                    Text(
                        text =
                            "How can we help?",
                        color =
                            PatientColors.TextPrimary,
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(5.dp)
                    )


                    Text(
                        text =
                            "Contact the EHealthy support team for help with your account, appointments, consultations or technical problems.",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            11.sp,
                        lineHeight =
                            16.sp,
                        textAlign =
                            TextAlign.Center
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            Text(
                text =
                    "CONTACT SUPPORT",
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    10.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                letterSpacing =
                    0.7.sp
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            /*
             * Email
             */
            SupportActionCard(
                icon =
                    Icons.Outlined.Email,

                title =
                    "Email support",

                description =
                    "Send us a message about your account or service.",

                accent =
                    PatientColors.AppointmentAccent,

                background =
                    PatientColors.AppointmentCard,

                onClick =
                    onEmailSupport
            )


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            /*
             * Phone
             */
            SupportActionCard(
                icon =
                    Icons.Outlined.Phone,

                title =
                    "Call support",

                description =
                    "Speak directly with the EHealthy support team.",

                accent =
                    PatientColors.SuccessAccent,

                background =
                    PatientColors.SuccessCard,

                onClick =
                    onCallSupport
            )


            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )


            /*
             * Report problem
             */
            SupportActionCard(
                icon =
                    Icons.Outlined.BugReport,

                title =
                    "Report a problem",

                description =
                    "Tell us if something in the app isn't working correctly.",

                accent =
                    PatientColors.Purple,

                background =
                    PatientColors.PurpleSoft,

                onClick =
                    onReportProblem
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            /*
             * Common help
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.TipsCard
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(16.dp),

                    verticalAlignment =
                        Alignment.Top
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(43.dp)
                                .background(
                                    PatientColors.TipsAccent
                                        .copy(alpha = 0.12f),
                                    RoundedCornerShape(13.dp)
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint =
                                PatientColors.TipsAccent,
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
                                "Before contacting support",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                13.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(5.dp)
                        )


                        Text(
                            text =
                                "When reporting a problem, explain what you were trying to do and what happened. Avoid including passwords or other sensitive login information.",
                            color =
                                PatientColors.TextSecondary,
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
                    Modifier.height(14.dp)
            )


            /*
             * Emergency warning
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(20.dp),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.RedSoft
                    ),

                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            PatientColors.Red
                                .copy(alpha = 0.12f)
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier.padding(16.dp),

                    verticalAlignment =
                        Alignment.Top
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(43.dp)
                                .background(
                                    PatientColors.Red
                                        .copy(alpha = 0.10f),
                                    RoundedCornerShape(13.dp)
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Warning,
                            contentDescription = null,
                            tint =
                                PatientColors.Red,
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
                                "Medical emergency?",
                            color =
                                PatientColors.Red,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                13.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )


                        Text(
                            text =
                                "EHealthy support is not an emergency service. For life-threatening situations, contact emergency services immediately.",
                            color =
                                PatientColors.TextSecondary,
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
                    Modifier.height(28.dp)
            )
        }
    }
}


@Composable
private fun SupportActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color,
    background: Color,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),

        shape =
            RoundedCornerShape(18.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
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

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .background(
                            background,
                            RoundedCornerShape(14.dp)
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
                        Modifier.size(22.dp)
                )
            }


            Spacer(
                modifier =
                    Modifier.width(11.dp)
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        title,
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        13.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )


                Text(
                    text =
                        description,
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        9.5.sp,
                    lineHeight =
                        13.sp
                )
            }


            Icon(
                imageVector =
                    Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint =
                    accent.copy(alpha = 0.65f),
                modifier =
                    Modifier.size(19.dp)
            )
        }
    }
}

