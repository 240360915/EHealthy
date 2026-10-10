package ehealthy.connect.ui.doctorDashboard

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
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable

@Serializable
data class DoctorAccountSettings(
    val email_notifications: Boolean = true,
    val sms_notifications: Boolean = true,
    val profile_visible: Boolean = true
)

// Doctor settings palette - intentionally aligned with the patient Settings screen.
private val SettingsBackground = Color(0xFFF7F9FC)
private val SettingsInk = Color(0xFF171B27)
private val SettingsMuted = Color(0xFF6C7280)
private val SettingsTeal = Color(0xFF119E95)
private val SettingsTealSoft = Color(0xFFE8F7F5)
private val SettingsBlue = Color(0xFF2F6FED)
private val SettingsBlueSoft = Color(0xFFEAF2FF)
private val SettingsPurple = Color(0xFF8B5CF6)
private val SettingsPurpleSoft = Color(0xFFF4EEFF)
private val SettingsGreen = Color(0xFF18A572)
private val SettingsGreenSoft = Color(0xFFE9FAF2)
private val SettingsAmber = Color(0xFFF59E0B)
private val SettingsAmberSoft = Color(0xFFFFF4DD)
private val SettingsRed = Color(0xFFE53935)
private val SettingsRedSoft = Color(0xFFFFEEEE)
private val SettingsBorder = Color(0xFFE3E8EF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorSettingsScreen(
    fetchSettings: suspend () -> Result<DoctorAccountSettings>,
    onUpdateSetting: (DoctorAccountSettings) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onViewPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    var settings by remember {
        mutableStateOf(DoctorAccountSettings())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        val result = fetchSettings()
        isLoading = false
        result.onSuccess {
            settings = it
        }
    }

    fun update(newSettings: DoctorAccountSettings) {
        settings = newSettings
        onUpdateSetting(newSettings)
    }

    Scaffold(
        containerColor = SettingsBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            color = SettingsInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Manage your preferences",
                            color = SettingsMuted,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SettingsTealSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = SettingsTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SettingsTeal
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 12.dp,
                    bottom = 28.dp
                )
        ) {

            // INTRO CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SettingsTealSoft
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(SettingsTeal.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WorkspacePremium,
                            contentDescription = null,
                            tint = SettingsTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column {
                        Text(
                            text = "Your preferences",
                            color = SettingsInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Personalise how eHealthy looks, communicates and presents your doctor profile.",
                            color = SettingsMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // APPEARANCE
            SettingsSectionHeader(
                title = "Appearance",
                subtitle = "Choose how the app looks",
                accent = SettingsPurple
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            SettingsGroupCard {
                DoctorSettingsToggleRow(
                    icon = Icons.Outlined.DarkMode,
                    label = "Dark mode",
                    description = "Switch between light and dark appearance",
                    checked = isDarkMode,
                    onToggle = onToggleDarkMode,
                    accent = SettingsPurple,
                    soft = SettingsPurpleSoft
                )
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // NOTIFICATIONS
            SettingsSectionHeader(
                title = "Notifications",
                subtitle = "Control how appointment updates reach you",
                accent = SettingsBlue
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            SettingsGroupCard {
                DoctorSettingsToggleRow(
                    icon = Icons.Outlined.Email,
                    label = "New appointment requests",
                    description = "Get notified when a patient books with you",
                    checked = settings.email_notifications,
                    onToggle = {
                        update(
                            settings.copy(
                                email_notifications = it
                            )
                        )
                    },
                    accent = SettingsBlue,
                    soft = SettingsBlueSoft
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DoctorSettingsToggleRow(
                    icon = Icons.Outlined.Sms,
                    label = "SMS reminders",
                    description = "Text reminders before scheduled appointments",
                    checked = settings.sms_notifications,
                    onToggle = {
                        update(
                            settings.copy(
                                sms_notifications = it
                            )
                        )
                    },
                    accent = SettingsBlue,
                    soft = SettingsBlueSoft
                )
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // PRIVACY
            SettingsSectionHeader(
                title = "Privacy",
                subtitle = "Manage visibility and privacy information",
                accent = SettingsGreen
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            SettingsGroupCard {
                DoctorSettingsToggleRow(
                    icon = Icons.Outlined.Visibility,
                    label = "Profile visible to patients",
                    description = "Patients can find and book with you when this is on",
                    checked = settings.profile_visible,
                    onToggle = {
                        update(
                            settings.copy(
                                profile_visible = it
                            )
                        )
                    },
                    accent = SettingsGreen,
                    soft = SettingsGreenSoft
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DoctorSettingsLinkRow(
                    icon = Icons.Outlined.Lock,
                    label = "Privacy policy",
                    description = "Learn how your information is handled",
                    accent = SettingsGreen,
                    soft = SettingsGreenSoft,
                    onClick = onViewPrivacyPolicy
                )
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            // ACCOUNT INFO / SECURITY
            SettingsSectionHeader(
                title = "Account",
                subtitle = "Doctor account and security information",
                accent = SettingsAmber
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            SettingsGroupCard {
                DoctorSettingsInfoRow(
                    icon = Icons.Outlined.PrivacyTip,
                    label = "Doctor account security",
                    description = "Your profile and appointment access are protected by your signed-in account.",
                    accent = SettingsAmber,
                    soft = SettingsAmberSoft
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DoctorSettingsInfoRow(
                    icon = Icons.Outlined.Notifications,
                    label = "Practice notifications",
                    description = "Keep important booking and consultation alerts enabled.",
                    accent = SettingsAmber,
                    soft = SettingsAmberSoft
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // LOG OUT
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onLogout
                    ),
                shape = RoundedCornerShape(21.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SettingsRedSoft
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = SettingsRed.copy(alpha = 0.16f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                SettingsRed.copy(
                                    alpha = 0.10f
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ExitToApp,
                            contentDescription = null,
                            tint = SettingsRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Log out",
                            color = SettingsRed,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(
                            modifier = Modifier.height(2.dp)
                        )

                        Text(
                            text = "Sign out of your eHealthy doctor account",
                            color = SettingsMuted,
                            fontSize = 9.5.sp
                        )
                    }

                    Text(
                        text = "›",
                        color = SettingsRed,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(accent)
        )

        Spacer(
            modifier = Modifier.width(9.dp)
        )

        Column {
            Text(
                text = title,
                color = SettingsInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subtitle,
                color = SettingsMuted,
                fontSize = 9.5.sp
            )
        }
    }
}

@Composable
private fun SettingsGroupCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = SettingsBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun DoctorSettingsToggleRow(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    accent: Color,
    soft: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(soft)
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    accent.copy(
                        alpha = 0.10f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(11.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                color = SettingsInk,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = description,
                color = SettingsMuted,
                fontSize = 9.sp,
                lineHeight = 12.5.sp
            )
        }

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color(0xFF9FB0C4),
                uncheckedTrackColor = Color(0xFFEAF0F5),
                uncheckedBorderColor = Color(0xFF9FB0C4)
            )
        )
    }
}

@Composable
private fun DoctorSettingsLinkRow(
    icon: ImageVector,
    label: String,
    description: String,
    accent: Color,
    soft: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(soft)
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    accent.copy(
                        alpha = 0.10f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(11.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                color = SettingsInk,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = description,
                color = SettingsMuted,
                fontSize = 9.sp
            )
        }

        Text(
            text = "›",
            color = accent,
            fontSize = 24.sp
        )
    }
}

@Composable
private fun DoctorSettingsInfoRow(
    icon: ImageVector,
    label: String,
    description: String,
    accent: Color,
    soft: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(soft)
            .padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(
                    accent.copy(
                        alpha = 0.10f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(21.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(11.dp)
        )

        Column {
            Text(
                text = label,
                color = SettingsInk,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = description,
                color = SettingsMuted,
                fontSize = 9.sp,
                lineHeight = 12.5.sp
            )
        }
    }
}
