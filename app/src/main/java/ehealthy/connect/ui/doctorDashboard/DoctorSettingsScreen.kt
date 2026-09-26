package ehealthy.connect.ui.doctorDashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.material3.TextButton
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
    var settings by remember { mutableStateOf(DoctorAccountSettings()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = fetchSettings()
        isLoading = false
        result.onSuccess { settings = it }
    }

    fun update(new: DoctorAccountSettings) {
        settings = new
        onUpdateSetting(new)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            DoctorSettingsSection("Appearance") {
                DoctorSettingsToggleRow(
                    Icons.Outlined.DarkMode, "Dark mode",
                    "Switch between light and dark appearance",
                    isDarkMode, onToggleDarkMode
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            DoctorSettingsSection("Notifications") {
                DoctorSettingsToggleRow(
                    Icons.Outlined.Email, "New appointment requests",
                    "Get notified when a patient books with you",
                    settings.email_notifications
                ) { update(settings.copy(email_notifications = it)) }
                DoctorSettingsToggleRow(
                    Icons.Outlined.Sms, "SMS reminders",
                    "Text reminders before your scheduled appointments",
                    settings.sms_notifications
                ) { update(settings.copy(sms_notifications = it)) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DoctorSettingsSection("Privacy") {
                DoctorSettingsToggleRow(
                    Icons.Outlined.Visibility, "Profile visible to patients",
                    "Patients can find and book with you when this is on",
                    settings.profile_visible
                ) { update(settings.copy(profile_visible = it)) }
                DoctorSettingsLinkRow(Icons.Outlined.Lock, "Privacy Policy", onViewPrivacyPolicy)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DoctorSettingsSection(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
    Spacer(modifier = Modifier.height(8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column { content() }
    }
}

@Composable
private fun DoctorSettingsToggleRow(
    icon: ImageVector, label: String, description: String, checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 12.dp))
            Text(
                label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp,
            modifier = Modifier.padding(start = 36.dp) // lines up under the label, past the icon
        )
    }
}

@Composable
private fun DoctorSettingsLinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 12.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}