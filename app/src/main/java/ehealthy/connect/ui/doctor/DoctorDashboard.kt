package ehealthy.connect.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.patientDashboard.Appointment
import ehealthy.connect.ui.patientDashboard.PatientAvatar
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

/** What the UI actually uses — built from [DoctorProfileRow] via [toDoctorProfile]. */
data class DoctorProfile(
    val id: String,
    val name: String,
    val surname: String,
    val practiceName: String?,
    val discipline: String?,
    val profileImageUrl: String?,
    val verificationStatus: String?
)

/** Shape of the row decoded straight from Supabase's `doctors` table —
 *  only the columns MainActivity.kt's Columns.list(...) actually selects. */
@Serializable
data class DoctorProfileRow(
    val id: String? = null,
    val name: String? = null,
    val surname: String? = null,
    @SerialName("practice_name") val practiceName: String? = null,
    val discipline: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null
)

fun DoctorProfileRow.toDoctorProfile(): DoctorProfile = DoctorProfile(
    id = id ?: "",
    name = name ?: "",
    surname = surname ?: "",
    practiceName = practiceName,
    discipline = discipline,
    profileImageUrl = profileImageUrl,
    verificationStatus = verificationStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboard(
    doctorProfile: DoctorProfile?,
    isLoadingProfile: Boolean,
    isUploadingPhoto: Boolean,
    onUploadPhoto: () -> Unit,
    onLogout: () -> Unit,
    fetchAppointments: suspend (doctorId: String) -> Result<List<Appointment>>,
    onUpdateAppointmentStatus: suspend (appointmentId: String, newStatus: String) -> Result<Unit>,
    onNavigatePrescriptions: () -> Unit
) {
    val background = Color(0xFFF0F4F8)
    val navy = Color(0xFF0F1F3D)
    val green = Color(0xFF10B981)
    val accent = Color(0xFF3B82F6)
    val muted = Color(0xFF64748B)

    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(DoctorTab.HOME) }

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoadingAppointments by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(doctorProfile?.id, refreshTrigger) {
        val doctorId = doctorProfile?.id
        if (doctorId.isNullOrBlank()) return@LaunchedEffect
        isLoadingAppointments = true
        fetchAppointments(doctorId)
            .onSuccess { appointments = it; loadError = null }
            .onFailure { loadError = it.message ?: "Failed to load appointments." }
        isLoadingAppointments = false
    }

    // Bridges DoctorAppointmentsTab's plain callback to the suspend
    // update function MainActivity actually provides, and re-fetches on
    // success so status changes (confirm/cancel) show up immediately.
    val handleStatusUpdate: (String, String) -> Unit = { appointmentId, newStatus ->
        scope.launch {
            onUpdateAppointmentStatus(appointmentId, newStatus)
                .onSuccess { refreshTrigger++ }
                .onFailure { loadError = it.message ?: "Couldn't update that appointment." }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("e-Health Connect", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("your practice, organized", color = green, fontSize = 11.sp)
                    }
                },
                actions = {
                    PatientAvatar(
                        avatarUrl = doctorProfile?.profileImageUrl,
                        name = doctorProfile?.name ?: "",
                        size = 36.dp,
                        onClick = { selectedTab = DoctorTab.PROFILE }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            DoctorBottomNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    ) { paddingValues ->
        when (selectedTab) {
            DoctorTab.HOME -> HomeTabContent(
                modifier = Modifier.padding(paddingValues),
                doctorProfile = doctorProfile,
                isLoadingProfile = isLoadingProfile,
                background = background,
                navy = navy,
                accent = accent,
                muted = muted,
                appointments = appointments,
                isLoadingAppointments = isLoadingAppointments,
                loadError = loadError,
                onNavigateAppointments = { selectedTab = DoctorTab.APPOINTMENTS }
            )

            DoctorTab.APPOINTMENTS -> DoctorAppointmentsTab(
                modifier = Modifier.padding(paddingValues),
                appointments = appointments,
                isLoading = isLoadingAppointments,
                loadError = loadError,
                onUpdateStatus = handleStatusUpdate
            )

            DoctorTab.PATIENTS -> PatientsTabContent(
                modifier = Modifier.padding(paddingValues),
                appointments = appointments,
                isLoading = isLoadingAppointments,
                background = background,
                navy = navy,
                muted = muted
            )

            DoctorTab.PROFILE -> ProfileTabContent(
                modifier = Modifier.padding(paddingValues),
                doctorProfile = doctorProfile,
                isUploadingPhoto = isUploadingPhoto,
                background = background,
                navy = navy,
                muted = muted,
                onUploadPhoto = onUploadPhoto,
                onLogout = onLogout,
                onNavigatePrescriptions = onNavigatePrescriptions
            )
        }
    }
}

@Composable
private fun HomeTabContent(
    modifier: Modifier,
    doctorProfile: DoctorProfile?,
    isLoadingProfile: Boolean,
    background: Color,
    navy: Color,
    accent: Color,
    muted: Color,
    appointments: List<Appointment>,
    isLoadingAppointments: Boolean,
    loadError: String?,
    onNavigateAppointments: () -> Unit
) {
    val pending = remember(appointments) { appointments.filter { it.status == "pending" } }
    val upcoming = remember(appointments) { appointments.filter { it.status != "cancelled" } }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(background).padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            val name = doctorProfile?.let { "Dr. ${it.name}" } ?: "Doctor"
            Text("Welcome back, $name 👋", color = navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Here's what's on your schedule.", color = muted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
        }

        if (!isLoadingProfile && doctorProfile?.verificationStatus == "pending") {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Verification pending", color = Color(0xFF92400E), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "An administrator is reviewing your documents. Your profile isn't visible to patients yet.",
                            color = Color(0xFF92400E), fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = "Pending", value = if (isLoadingAppointments) "–" else pending.size.toString(),
                    accent = Color(0xFFF59E0B), muted = muted, modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                StatCard(
                    label = "Upcoming", value = if (isLoadingAppointments) "–" else upcoming.size.toString(),
                    accent = accent, muted = muted, modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text("UPCOMING APPOINTMENTS", color = navy, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (isLoadingAppointments) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
        } else if (loadError != null) {
            item { Text(loadError, color = Color(0xFFEF4444), fontSize = 13.sp) }
        } else if (upcoming.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No upcoming appointments.", color = Color(0xFF64748B), fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(upcoming.take(3), key = { it.id }) { appt ->
                MiniAppointmentCard(appt)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (upcoming.size > 3) {
                item {
                    TextButton(onClick = onNavigateAppointments) {
                        Text("View all appointments →", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, muted: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = modifier, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MiniAppointmentCard(appt: Appointment) {
    val statusColor = when (appt.status) {
        "confirmed" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "cancelled" -> Color(0xFFEF4444)
        else -> Color(0xFF94A3B8)
    }
    val statusBg = when (appt.status) {
        "confirmed" -> Color(0xFFF0FDF4)
        "pending" -> Color(0xFFFFFBEB)
        "cancelled" -> Color(0xFFFEF2F2)
        else -> Color(0xFFF8FAFF)
    }

    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(statusColor))
        Column(modifier = Modifier.padding(18.dp)) {
            Text(appt.patient_name ?: "Unknown patient", color = Color(0xFF0F1F3D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("📅 ${appt.date ?: "-"}   🕐 ${appt.time ?: "-"}", color = Color(0xFF1E293B), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("📝 ${appt.reason ?: "General consultation"}", color = Color(0xFF64748B), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(statusBg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(appt.status?.replaceFirstChar { it.uppercase() } ?: "Unknown", color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Built from the same appointments already fetched — grouped by patient
 *  name, since appointments carry patient_name but the Appointment model
 *  doesn't expose a queryable patient_id yet. */
@Composable
private fun PatientsTabContent(
    modifier: Modifier,
    appointments: List<Appointment>,
    isLoading: Boolean,
    background: Color,
    navy: Color,
    muted: Color
) {
    data class PatientSummary(val name: String, val visitCount: Int, val lastDate: String?)

    val patients = remember(appointments) {
        appointments
            .filter { !it.patient_name.isNullOrBlank() }
            .groupBy { it.patient_name!! }
            .map { (name, appts) ->
                PatientSummary(
                    name = name,
                    visitCount = appts.size,
                    lastDate = appts.mapNotNull { it.date }.maxOrNull()
                )
            }
            .sortedBy { it.name }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Patients", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            }
            patients.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Patients you've seen will show up here.", color = muted, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(patients, key = { it.name }) { patient ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEFF1FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(patient.name, color = navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${patient.visitCount} appointment${if (patient.visitCount == 1) "" else "s"}" +
                                                (patient.lastDate?.let { " · last on $it" } ?: ""),
                                        color = muted, fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ProfileTabContent(
    modifier: Modifier,
    doctorProfile: DoctorProfile?,
    isUploadingPhoto: Boolean,
    background: Color,
    navy: Color,
    muted: Color,
    onUploadPhoto: () -> Unit,
    onLogout: () -> Unit,
    onNavigatePrescriptions: () -> Unit
) {
    Column(modifier = modifier.fillMaxSize().background(background).padding(24.dp)) {
        Box(contentAlignment = Alignment.BottomEnd) {
            PatientAvatar(avatarUrl = doctorProfile?.profileImageUrl, name = doctorProfile?.name ?: "", size = 96.dp)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .clickable(enabled = !isUploadingPhoto, onClick = onUploadPhoto),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingPhoto) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = navy)
                } else {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = "Change photo", tint = navy, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(doctorProfile?.let { "Dr. ${it.name} ${it.surname}" } ?: "Doctor", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        doctorProfile?.discipline?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(it, color = muted, fontSize = 13.sp)
        }
        doctorProfile?.practiceName?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(it, color = muted, fontSize = 13.sp)
        }

        doctorProfile?.verificationStatus?.let { status ->
            Spacer(modifier = Modifier.height(10.dp))
            val (bg, fg, label) = when (status) {
                "approved" -> Triple(Color(0xFFF0FDF4), Color(0xFF10B981), "Verified")
                "pending" -> Triple(Color(0xFFFFFBEB), Color(0xFFF59E0B), "Pending verification")
                else -> Triple(Color(0xFFFEF2F2), Color(0xFFEF4444), "Not verified")
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        TextButton(onClick = onUploadPhoto, contentPadding = PaddingValues(0.dp), enabled = !isUploadingPhoto) {
            Text(if (isUploadingPhoto) "Uploading…" else "Change photo", color = Color(0xFF3B82F6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("MORE", color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigatePrescriptions)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💊", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Text("Prescriptions", color = navy, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = muted, modifier = Modifier.size(18.dp))
            }
        }
        // TODO: My Time Slots / Financial Records / Location / Subscriptions
        // rows go here as each gets built — same Card pattern as above.

        Spacer(modifier = Modifier.height(12.dp))

        Spacer(modifier = Modifier.height(28.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth().clickable(onClick = onLogout)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(14.dp))
                Text("Logout", color = Color(0xFFEF4444), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}