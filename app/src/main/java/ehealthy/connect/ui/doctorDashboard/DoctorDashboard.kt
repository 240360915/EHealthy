package ehealthy.connect.ui.doctorDashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.MoreTime
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Groups2
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ehealthy.connect.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.patientDashboard.Appointment
import ehealthy.connect.ui.patientDashboard.PatientAvatar
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

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

@RequiresApi(Build.VERSION_CODES.O)
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
    onNavigatePrescriptions: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateTimeSlots: () -> Unit,
    onNavigateEarnings: () -> Unit = {},
    onNavigateAvailability: () -> Unit = {},
    onStartCall: (appointmentId: String) -> Unit = {},
    onVerifyCompletionCode: suspend (appointmentId: String, code: String) -> Result<Unit> = { _, _ -> Result.failure(Exception("Not available")) },
    onOpenPatientFile: (appointmentId: String) -> Unit = {}
) {
    val background = MaterialTheme.colorScheme.background
    val navy = MaterialTheme.colorScheme.onBackground
    val green = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

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
                    IconButton(onClick = onNavigateEarnings) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = "Demo earnings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                            .clickable(onClick = onNavigateAvailability)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                "Availability",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    PatientAvatar(
                        avatarUrl = doctorProfile?.profileImageUrl,
                        name = doctorProfile?.name ?: "",
                        size = 36.dp,
                        onClick = { selectedTab = DoctorTab.PROFILE }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            DoctorBottomNavBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = slideInHorizontally(animationSpec = tween(260)) { fullWidth ->
                    if (forward) fullWidth / 4 else -fullWidth / 4
                } + fadeIn(animationSpec = tween(260))
                val exit = slideOutHorizontally(animationSpec = tween(200)) { fullWidth ->
                    if (forward) -fullWidth / 4 else fullWidth / 4
                } + fadeOut(animationSpec = tween(180))
                enter togetherWith exit
            },
            label = "doctorTab"
        ) { tab ->
            when (tab) {
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
                    onNavigateAppointments = { selectedTab = DoctorTab.APPOINTMENTS },
                    onNavigatePrescriptions = onNavigatePrescriptions,
                    onNavigateTimeSlots = onNavigateTimeSlots,
                    onNavigateAvailability = onNavigateAvailability
                )

                DoctorTab.APPOINTMENTS -> DoctorAppointmentsTab(
                    modifier = Modifier.padding(paddingValues),
                    appointments = appointments,
                    isLoading = isLoadingAppointments,
                    loadError = loadError,
                    onUpdateStatus = handleStatusUpdate,
                    onStartCall = onStartCall,
                    onVerifyCompletionCode = onVerifyCompletionCode,
                    onOpenPatientFile = onOpenPatientFile
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
                    onNavigatePrescriptions = onNavigatePrescriptions,
                    onNavigateSettings = onNavigateSettings,
                    onNavigateTimeSlots = onNavigateTimeSlots


                )
            }
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
    onNavigateAppointments: () -> Unit,
    onNavigatePrescriptions: () -> Unit,
    onNavigateTimeSlots: () -> Unit,
    onNavigateAvailability: () -> Unit
) {
    val pending = remember(appointments) {
        appointments.filter { it.status.equals("pending", ignoreCase = true) }
    }
    val completed = remember(appointments) {
        appointments.filter { it.status.equals("completed", ignoreCase = true) }
    }
    val upcoming = remember(appointments) {
        appointments
            .filter {
                !it.status.equals("cancelled", ignoreCase = true) &&
                        !it.status.equals("completed", ignoreCase = true)
            }
            .sortedWith(compareBy({ it.date ?: "9999-99-99" }, { it.time ?: "99:99" }))
    }
    val patientCount = remember(appointments) {
        appointments.mapNotNull { it.patient_name?.takeIf(String::isNotBlank) }.distinct().size
    }
    val nextAppointment = upcoming.firstOrNull()

    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(background),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            PremiumDoctorHero(
                doctorProfile = doctorProfile,
                greeting = greeting,
                today = today,
                nextAppointment = nextAppointment,
                onViewSchedule = onNavigateAppointments
            )
        }

        if (!isLoadingProfile && doctorProfile?.verificationStatus == "pending") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E8)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.PendingActions,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Verification in progress",
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Your profile will be visible to patients after approval.",
                                color = Color(0xFF92400E),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            DashboardSectionHeader(
                title = "Today's overview",
                action = "View details",
                onAction = onNavigateAppointments
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PremiumOverviewCard(
                    label = "Pending",
                    value = if (isLoadingAppointments) "–" else pending.size.toString(),
                    subtitle = "Needs attention",
                    imageRes = R.drawable.dashboard_pending,
                    accent = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                PremiumOverviewCard(
                    label = "Upcoming",
                    value = if (isLoadingAppointments) "–" else upcoming.size.toString(),
                    subtitle = "Scheduled",
                    imageRes = R.drawable.dashboard_upcoming,
                    accent = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
                PremiumOverviewCard(
                    label = "Completed",
                    value = if (isLoadingAppointments) "–" else completed.size.toString(),
                    subtitle = "Finished",
                    imageRes = R.drawable.dashboard_completed,
                    accent = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                PremiumOverviewCard(
                    label = "Patients",
                    value = if (isLoadingAppointments) "–" else patientCount.toString(),
                    subtitle = "Unique seen",
                    imageRes = R.drawable.dashboard_patients,
                    accent = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                "Quick actions",
                color = navy,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 18.dp, top = 26.dp, bottom = 12.dp)
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumQuickActionCard(
                        title = "Appointments",
                        subtitle = "Review your schedule and consultations",
                        action = "Open schedule",
                        imageRes = R.drawable.dashboard_appointments,
                        accent = Color(0xFF2563EB),
                        onClick = onNavigateAppointments,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumQuickActionCard(
                        title = "Time slots",
                        subtitle = "Manage availability and working hours",
                        action = "Set working hours",
                        imageRes = R.drawable.dashboard_timeslots,
                        accent = Color(0xFF0891B2),
                        onClick = onNavigateTimeSlots,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumQuickActionCard(
                        title = "Prescriptions",
                        subtitle = "Create and review patient prescriptions",
                        action = "Manage medicine",
                        imageRes = R.drawable.dashboard_prescriptions,
                        accent = Color(0xFFDB2777),
                        onClick = onNavigatePrescriptions,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumQuickActionCard(
                        title = "Availability",
                        subtitle = "Update consultation status and availability",
                        action = "Update status",
                        imageRes = R.drawable.dashboard_availability,
                        accent = Color(0xFF7C3AED),
                        onClick = onNavigateAvailability,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 10.dp, top = 26.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Next appointment",
                    color = navy,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                if (upcoming.isNotEmpty()) {
                    TextButton(onClick = onNavigateAppointments) {
                        Text("View all", color = accent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        when {
            isLoadingAppointments -> {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accent)
                    }
                }
            }
            loadError != null -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            loadError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            nextAppointment == null -> {
                item {
                    PremiumEmptyAppointmentCard(onNavigateTimeSlots)
                }
            }
            else -> {
                item {
                    FeaturedAppointmentCard(
                        appointment = nextAppointment,
                        onClick = onNavigateAppointments
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumDoctorHero(
    doctorProfile: DoctorProfile?,
    greeting: String,
    today: String,
    nextAppointment: Appointment?,
    onViewSchedule: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = Color(0xFF1769AA)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(primary, Color(0xFF118C8B), secondary)
                )
            )
            .padding(start = 20.dp, top = 22.dp, end = 16.dp, bottom = 22.dp)
    ) {
        // Large doctor profile image on the right, matching the reference design.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(126.dp)
                .offset(x = 8.dp, y = 14.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            PatientAvatar(
                avatarUrl = doctorProfile?.profileImageUrl,
                name = doctorProfile?.name ?: "",
                size = 118.dp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.72f)
        ) {
            Text(
                today.uppercase(),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "$greeting, Dr. ${doctorProfile?.name?.takeIf { it.isNotBlank() } ?: "Doctor"}",
                color = Color.White,
                fontSize = 24.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                if (nextAppointment != null)
                    "Your next consultation is coming up."
                else
                    "Your schedule is clear for now.\nKeep making a difference!",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .clickable(onClick = onViewSchedule)
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.EventAvailable,
                        contentDescription = null,
                        tint = primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Manage your schedule",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (nextAppointment != null)
                            "${nextAppointment.date ?: "Date pending"} • ${nextAppointment.time ?: "Time pending"}"
                        else
                            "Add or review your available time slots",
                        color = Color.White.copy(alpha = 0.80f),
                        fontSize = 9.5.sp,
                        maxLines = 1
                    )
                }
                Text(
                    "›",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
private fun DashboardSectionHeader(
    title: String,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) {
            Text(
                "$action  ›",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PremiumOverviewCard(
    label: String,
    value: String,
    subtitle: String,
    imageRes: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(156.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.055f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.10f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color.White.copy(alpha = 0.30f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 7.4.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PremiumQuickActionCard(
    title: String,
    subtitle: String,
    action: String,
    imageRes: Int,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(178.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.055f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        "$action  ›",
                        color = accent,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumEmptyAppointmentCard(
    onManageTimeSlots: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "No upcoming appointments",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Your next consultation will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onManageTimeSlots) {
                Text(
                    "Manage time slots",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeaturedAppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    val statusColor = appointmentStatusColor(appointment.status)
    val initials = appointment.patient_name
        ?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.joinToString("") { it.first().uppercase() }
        ?.ifBlank { "P" }
        ?: "P"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        appointment.patient_name ?: "Unknown patient",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        appointment.reason ?: "General consultation",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                StatusPill(appointment.status, statusColor)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppointmentInfoChip(
                    icon = Icons.Outlined.CalendarMonth,
                    text = appointment.date ?: "Date pending",
                    modifier = Modifier.weight(1f)
                )
                AppointmentInfoChip(
                    icon = Icons.Outlined.Schedule,
                    text = appointment.time ?: "Time pending",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AppointmentInfoChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusPill(
    status: String?,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            status?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun appointmentStatusColor(status: String?): Color =
    when (status?.lowercase()) {
        "confirmed", "accepted" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "completed" -> Color(0xFF2563EB)
        "cancelled", "declined" -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
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
            .padding(24.dp)
    ) {
        Text("Patients", color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
    onNavigatePrescriptions: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateTimeSlots: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            PatientAvatar(avatarUrl = doctorProfile?.profileImageUrl, name = doctorProfile?.name ?: "", size = 96.dp)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
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
            val (fg, label) = when (status) {
                "approved" -> Color(0xFF10B981) to "Verified"
                "pending" -> Color(0xFFF59E0B) to "Pending verification"
                else -> Color(0xFFEF4444) to "Not verified"
            }
            val bg = fg.copy(alpha = 0.15f)
            Box(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        ProfileMenuItem(Icons.Outlined.Medication, "Prescriptions", navy, onClick = onNavigatePrescriptions)
        Spacer(modifier = Modifier.height(8.dp))
        ProfileMenuItem(Icons.Outlined.Schedule, "My Time Slots", navy, onClick = onNavigateTimeSlots)
        Spacer(modifier = Modifier.height(8.dp))
        ProfileMenuItem(Icons.Outlined.Settings, "Settings", navy, onClick = onNavigateSettings)
        ProfileMenuItem(Icons.Outlined.ExitToApp, "Logout", MaterialTheme.colorScheme.error, onClick = onLogout)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
