package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import androidx.compose.material.icons.outlined.SupportAgent


@Serializable
data class PatientSettings(
    val email_notifications: Boolean = true,
    val sms_notifications: Boolean = true,
    val profile_visible: Boolean = true
)


private val settingsRed =
    Color(0xFFEF4444)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    fetchSettings: suspend () -> Result<PatientSettings>,
    onUpdateSetting: (PatientSettings) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onNavigateEditProfile: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onNavigateContactUs: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {

    var settings by remember {
        mutableStateOf(
            PatientSettings()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }


    LaunchedEffect(Unit) {

        val result =
            fetchSettings()

        result.onSuccess {
            settings = it
        }

        isLoading = false
    }


    fun update(
        newSettings: PatientSettings
    ) {

        settings =
            newSettings

        onUpdateSetting(
            newSettings
        )
    }


    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text =
                                "Settings",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                18.sp
                        )


                        Text(
                            text =
                                "Manage your preferences",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        36.dp
                                    )
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
                                contentDescription =
                                    "Back",
                                tint =
                                    PatientColors.DoctorAccent,
                                modifier =
                                    Modifier.size(
                                        20.dp
                                    )
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                        )
            )
        },

        containerColor =
            MaterialTheme
                .colorScheme
                .background

    ) { paddingValues ->


        if (isLoading) {

            SettingsLoadingState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        )
            )

            return@Scaffold
        }


        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 18.dp
                    )
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            /*
             * Intro
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    RoundedCornerShape(
                        22.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.DoctorCard
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            0.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                16.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    49.dp
                                )
                                .background(
                                    PatientColors.DoctorAccent
                                        .copy(
                                            alpha = 0.11f
                                        ),
                                    RoundedCornerShape(
                                        15.dp
                                    )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Settings,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.DoctorAccent,
                            modifier =
                                Modifier.size(
                                    24.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(
                                12.dp
                            )
                    )


                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Text(
                            text =
                                "Your preferences",
                            color =
                                PatientColors.TextPrimary,
                            fontSize =
                                14.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )


                        Text(
                            text =
                                "Personalise how EHealthy looks, communicates and shares your profile.",
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
                    Modifier.height(
                        20.dp
                    )
            )


            /*
             * Account
             */
            SettingsSection(
                title =
                    "Account",
                subtitle =
                    "Manage your personal information",
                accent =
                    PatientColors.DoctorAccent,
                background =
                    PatientColors.DoctorCard
            ) {

                SettingsLinkRow(
                    icon =
                        Icons.Outlined.AccountCircle,
                    label =
                        "Edit profile",
                    description =
                        "Update your personal and contact details",
                    accent =
                        PatientColors.DoctorAccent,
                    background =
                        PatientColors.DoctorCard,
                    onClick =
                        onNavigateEditProfile
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * Appearance
             */
            SettingsSection(
                title =
                    "Appearance",
                subtitle =
                    "Choose how the app looks",
                accent =
                    PatientColors.Purple,
                background =
                    PatientColors.PurpleSoft
            ) {

                SettingsToggleRow(
                    icon =
                        Icons.Outlined.DarkMode,
                    label =
                        "Dark mode",
                    description =
                        "Switch between light and dark appearance",
                    checked =
                        isDarkMode,
                    accent =
                        PatientColors.Purple,
                    background =
                        PatientColors.PurpleSoft,
                    onToggle =
                        onToggleDarkMode
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * Notifications
             */
            SettingsSection(
                title =
                    "Notifications",
                subtitle =
                    "Control how appointment updates reach you",
                accent =
                    PatientColors.AppointmentAccent,
                background =
                    PatientColors.AppointmentCard
            ) {

                SettingsToggleRow(
                    icon =
                        Icons.Outlined.Email,
                    label =
                        "Email notifications",
                    description =
                        "Appointment confirmations and reminders",
                    checked =
                        settings.email_notifications,
                    accent =
                        PatientColors.AppointmentAccent,
                    background =
                        PatientColors.AppointmentCard
                ) { enabled ->

                    update(
                        settings.copy(
                            email_notifications =
                                enabled
                        )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                SettingsToggleRow(
                    icon =
                        Icons.Outlined.Sms,
                    label =
                        "SMS notifications",
                    description =
                        "Text message reminders for appointments",
                    checked =
                        settings.sms_notifications,
                    accent =
                        PatientColors.AppointmentAccent,
                    background =
                        PatientColors.AppointmentCard
                ) { enabled ->

                    update(
                        settings.copy(
                            sms_notifications =
                                enabled
                        )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * Privacy
             */
            SettingsSection(
                title =
                    "Privacy",
                subtitle =
                    "Manage visibility and privacy information",
                accent =
                    PatientColors.SuccessAccent,
                background =
                    PatientColors.SuccessCard
            ) {

                SettingsToggleRow(
                    icon =
                        Icons.Outlined.Visibility,
                    label =
                        "Profile visible to doctors",
                    description =
                        "Doctors you've booked with can see your profile details",
                    checked =
                        settings.profile_visible,
                    accent =
                        PatientColors.SuccessAccent,
                    background =
                        PatientColors.SuccessCard
                ) { enabled ->

                    update(
                        settings.copy(
                            profile_visible =
                                enabled
                        )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                SettingsLinkRow(
                    icon =
                        Icons.Outlined.Lock,
                    label =
                        "Privacy policy",
                    description =
                        "Learn how your information is handled",
                    accent =
                        PatientColors.SuccessAccent,
                    background =
                        PatientColors.SuccessCard,
                    onClick =
                        onViewPrivacyPolicy
                )
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            SettingsSection(
                title = "Support",
                subtitle = "Get help with EHealthy",
                accent = PatientColors.TipsAccent,
                background = PatientColors.TipsCard
            ) {

                SettingsLinkRow(
                    icon = Icons.Outlined.SupportAgent,
                    label = "Contact us",
                    description = "Get help with your account, appointments or the app",
                    accent = PatientColors.TipsAccent,
                    background = PatientColors.TipsCard,
                    onClick = onNavigateContactUs
                )
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )


            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )


            /*
             * Logout
             */
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = onLogout
                        ),

                shape =
                    RoundedCornerShape(
                        18.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.RedSoft
                    ),

                border =
                    BorderStroke(
                        width =
                            1.dp,
                        color =
                            settingsRed.copy(
                                alpha = 0.12f
                            )
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            0.dp
                    )
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                14.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    40.dp
                                )
                                .background(
                                    settingsRed.copy(
                                        alpha = 0.10f
                                    ),
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Logout,
                            contentDescription =
                                null,
                            tint =
                                settingsRed,
                            modifier =
                                Modifier.size(
                                    20.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(
                                11.dp
                            )
                    )


                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Text(
                            text =
                                "Log out",
                            color =
                                settingsRed,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                13.sp
                        )


                        Text(
                            text =
                                "Sign out of your EHealthy account",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                9.5.sp
                        )
                    }


                    Icon(
                        imageVector =
                            Icons.Outlined.ChevronRight,
                        contentDescription =
                            null,
                        tint =
                            settingsRed.copy(
                                alpha = 0.65f
                            ),
                        modifier =
                            Modifier.size(
                                19.dp
                            )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )
        }
    }
}


@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    accent: Color,
    background: Color,
    content: @Composable () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .width(
                        4.dp
                    )
                    .height(
                        30.dp
                    )
                    .background(
                        accent,
                        RoundedCornerShape(
                            3.dp
                        )
                    )
        )


        Spacer(
            modifier =
                Modifier.width(
                    9.dp
                )
        )


        Column {

            Text(
                text =
                    title,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.ExtraBold
            )


            Text(
                text =
                    subtitle,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    9.5.sp
            )
        }
    }


    Spacer(
        modifier =
            Modifier.height(
                9.dp
            )
    )


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                20.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),

        border =
            BorderStroke(
                width =
                    1.dp,
                color =
                    accent.copy(
                        alpha = 0.08f
                    )
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    10.dp
                )
        ) {

            content()
        }
    }
}


@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    accent: Color,
    background: Color,
    onToggle: (Boolean) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    background,
                    RoundedCornerShape(
                        15.dp
                    )
                )
                .padding(
                    12.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        40.dp
                    )
                    .background(
                        accent.copy(
                            alpha = 0.11f
                        ),
                        RoundedCornerShape(
                            12.dp
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    accent,
                modifier =
                    Modifier.size(
                        20.dp
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.width(
                    11.dp
                )
        )


        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    label,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    12.5.sp,
                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
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


        Spacer(
            modifier =
                Modifier.width(
                    8.dp
                )
        )


        Switch(
            checked =
                checked,

            onCheckedChange =
                onToggle,

            colors =
                SwitchDefaults.colors(
                    checkedThumbColor =
                        Color.White,

                    checkedTrackColor =
                        accent,

                    uncheckedThumbColor =
                        MaterialTheme
                            .colorScheme
                            .outline,

                    uncheckedTrackColor =
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                )
        )
    }
}


@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    label: String,
    description: String,
    accent: Color,
    background: Color,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    background,
                    RoundedCornerShape(
                        15.dp
                    )
                )
                .clickable(
                    onClick = onClick
                )
                .padding(
                    12.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        40.dp
                    )
                    .background(
                        accent.copy(
                            alpha = 0.11f
                        ),
                        RoundedCornerShape(
                            12.dp
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    accent,
                modifier =
                    Modifier.size(
                        20.dp
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.width(
                    11.dp
                )
        )


        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    label,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    12.5.sp,
                fontWeight =
                    FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
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
            contentDescription =
                null,
            tint =
                accent.copy(
                    alpha = 0.65f
                ),
            modifier =
                Modifier.size(
                    19.dp
                )
        )
    }
}


@Composable
private fun SettingsLoadingState(
    modifier: Modifier
) {

    Box(
        modifier =
            modifier,
        contentAlignment =
            Alignment.Center
    ) {

        Card(
            shape =
                RoundedCornerShape(
                    24.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        PatientColors.DoctorCard
                ),

            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        0.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 32.dp,
                        vertical = 27.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                62.dp
                            )
                            .background(
                                PatientColors.DoctorAccent
                                    .copy(
                                        alpha = 0.10f
                                    ),
                                CircleShape
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                30.dp
                            ),
                        color =
                            PatientColors.DoctorAccent,
                        strokeWidth =
                            2.5.dp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            13.dp
                        )
                )


                Text(
                    text =
                        "Loading your settings",
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        "Getting your saved preferences...",
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        10.5.sp
                )
            }
        }
    }
}