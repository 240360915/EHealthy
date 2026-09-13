package ehealthy.connect.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: String,
    val patient_name: String? = null,
    val reason: String? = null,
    val date: String? = null,
    val time: String? = null,
    val status: String? = null,
    val payment_method: String? = null,
    val amount_paid: Double? = null
)

data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboard(
    patientName: String,
    onNavigateProfile: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigateFindDoctors: () -> Unit,
    onNavigateHealthTips: () -> Unit,
    onLogout: () -> Unit,
    fetchAppointments: suspend () -> Result<List<Appointment>>
) {
    val background = Color(0xFFF0F4F8)
    val navy = Color(0xFF0F1F3D)
    val green = Color(0xFF10B981)
    val accent = Color(0xFF3B82F6)
    val muted = Color(0xFF64748B)

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoadingAppointments by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val result = fetchAppointments()
        isLoadingAppointments = false
        result
            .onSuccess { appointments = it }
            .onFailure { loadError = it.message ?: "Failed to load appointments." }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "PATIENT MENU",
                    color = muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 8.dp)
                )
                DrawerItem(Icons.Outlined.Person, "My Profile") {
                    scope.launch { drawerState.close() }; onNavigateProfile()
                }
                DrawerItem(Icons.Outlined.CalendarMonth, "Appointments") {
                    scope.launch { drawerState.close() }; onNavigateAppointments()
                }
                DrawerItem(Icons.Outlined.Description, "Medical Records") {
                    scope.launch { drawerState.close() }; onNavigateMedicalRecords()
                }
                DrawerItem(Icons.Outlined.Search, "Find Doctors") {
                    scope.launch { drawerState.close() }; onNavigateFindDoctors()
                }
                DrawerItem(Icons.Outlined.Lightbulb, "Health Tips") {
                    scope.launch { drawerState.close() }; onNavigateHealthTips()
                }
                DrawerItem(Icons.Outlined.ExitToApp, "Logout", tint = Color(0xFFEF4444)) {
                    scope.launch { drawerState.close() }; onLogout()
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "e-Health Connect",
                                color = navy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text("health-care made easier for you", color = green, fontSize = 11.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = navy)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(background)
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                item {
                    Text(
                        "Welcome back, $patientName 👋",
                        color = navy,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Select a doctor to book, message, or start a consultation.",
                        color = muted, fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                item {
                    val actions = listOf(
                        QuickAction(
                            "Records",
                            Icons.Outlined.Description,
                            onNavigateMedicalRecords
                        ),
                        QuickAction(
                            "Appointments",
                            Icons.Outlined.CalendarMonth,
                            onNavigateAppointments
                        ),
                        QuickAction("Health Tips", Icons.Outlined.Lightbulb, onNavigateHealthTips),
                        QuickAction("Profile", Icons.Outlined.Person, onNavigateProfile)
                    )
                    Column {
                        for (row in actions.chunked(2)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                row.forEach { action ->
                                    QuickActionCard(
                                        action,
                                        accent,
                                        navy,
                                        muted,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Text(
                        "MY APPOINTMENTS",
                        color = navy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (isLoadingAppointments) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                } else if (loadError != null) {
                    item {
                        Text(loadError ?: "", color = Color(0xFFEF4444), fontSize = 13.sp)
                    }
                } else if (appointments.isEmpty()) {
                    item {
                        EmptyAppointmentsCard(onNavigateFindDoctors)
                    }
                } else {
                    items(appointments) { appt ->
                        AppointmentCard(appt)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    tint: Color = Color(0xFF0F1F3D),
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null, tint = tint) },
        label = { Text(label, color = tint) },
        selected = false,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    accent: Color,
    navy: Color,
    muted: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(action.label, color = navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Tap to open",
                color = muted,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = action.onClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Open →", color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EmptyAppointmentsCard(onFindDoctor: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("No appointments yet.", color = Color(0xFF64748B), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(onClick = onFindDoctor) {
                Text(
                    "Find a doctor to book one",
                    color = Color(0xFF3B82F6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AppointmentCard(appt: Appointment) {
    val statusColor = when (appt.status) {
        "confirmed" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "rescheduled" -> Color(0xFF3B82F6)
        "cancelled" -> Color(0xFFEF4444)
        else -> Color(0xFF94A3B8)
    }
    val statusBg = when (appt.status) {
        "confirmed" -> Color(0xFFF0FDF4)
        "pending" -> Color(0xFFFFFBEB)
        "rescheduled" -> Color(0xFFEFF6FF)
        "cancelled" -> Color(0xFFFEF2F2)
        else -> Color(0xFFF8FAFF)
    }
    val statusLabel = when (appt.status) {
        "confirmed" -> "Accepted & Paid"
        "cancelled" -> "Rejected & Refunded"
        else -> appt.status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxWidth()
                    .background(statusColor)
            )
        }
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "📅 ${appt.date ?: "-"}   🕐 ${appt.time ?: "-"}",
                color = Color(0xFF1E293B), fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "📝 ${appt.reason ?: "General consultation"}",
                color = Color(0xFF64748B), fontSize = 13.sp
            )
            appt.payment_method?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("💳 Paid via $it", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            appt.amount_paid?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("💰 R ${"%.2f".format(it)}", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}