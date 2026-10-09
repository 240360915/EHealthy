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
                            color = AuthColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "We're here when you need help",
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
                                        AuthColors.DoctorSoft,
                                        CircleShape
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = AuthColors.DoctorAccent,
                                modifier = Modifier.size(20.dp)
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
                            AuthColors.DoctorSoft
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
                                    AuthColors.DoctorAccent
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
                                AuthColors.DoctorAccent,
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
                            AuthColors.TextPrimary,
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
                            AuthColors.TextSecondary,
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
                    AuthColors.TextSecondary,
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
                    AuthColors.DoctorAccent,

                background =
                    AuthColors.DoctorSoft,

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
                    AuthColors.PatientAccent,

                background =
                    AuthColors.PatientSoft,

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
                    AuthColors.DoctorAccent,

                background =
                    AuthColors.DoctorSoft,

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
                            AuthColors.PatientSoft
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
                                    AuthColors.PatientAccent
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
                                AuthColors.PatientAccent,
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
                                AuthColors.TextPrimary,
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
                            AuthColors.ErrorSoft
                    ),

                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            AuthColors.Error
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
                                    AuthColors.Error
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
                                AuthColors.Error,
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
                                AuthColors.Error,
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
                        AuthColors.TextPrimary,
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
                        AuthColors.TextSecondary,
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

