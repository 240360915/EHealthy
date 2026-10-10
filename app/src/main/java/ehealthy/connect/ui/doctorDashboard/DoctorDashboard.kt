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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.outlined.Info

private val DoctorScreenBackground = Color(0xFFF7F9FC)
private val DoctorInk = Color(0xFF171B27)
private val DoctorMuted = Color(0xFF6C7280)
private val DoctorTeal = Color(0xFF119E95)
private val DoctorTealSoft = Color(0xFFE8F7F5)
private val DoctorBlue = Color(0xFF2F6FED)
private val DoctorBlueSoft = Color(0xFFEAF2FF)
private val DoctorPurple = Color(0xFF8B5CF6)
private val DoctorPurpleSoft = Color(0xFFF4EEFF)
private val DoctorAmber = Color(0xFFF59E0B)
private val DoctorAmberSoft = Color(0xFFFFF4DD)
private val DoctorGreen = Color(0xFF18A572)
private val DoctorGreenSoft = Color(0xFFE9FAF2)
private val DoctorRed = Color(0xFFE53935)
private val DoctorRedSoft = Color(0xFFFFEEEE)
private val DoctorOutline = Color(0xFFE3E8EF)

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
                        Text(
                            "eHealth Connect",
                            color = DoctorInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Your practice, connected",
                            color = DoctorTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
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
                            .background(DoctorTealSoft)
                            .clickable(onClick = onNavigateAvailability)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
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
                                color = DoctorTeal,
                                fontSize = 10.sp,
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
            .background(DoctorScreenBackground),
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
                    colors = CardDefaults.cardColors(containerColor = DoctorAmberSoft),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.PendingActions,
                                contentDescription = null,
                                tint = DoctorAmber,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(11.dp))
                        Column {
                            Text(
                                "Verification in progress",
                                color = DoctorInk,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Your profile becomes visible to patients after approval.",
                                color = DoctorMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            DashboardSectionHeader(
                title = "Today's overview",
                subtitle = "A quick look at your practice",
                action = "View details",
                onAction = onNavigateAppointments
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    PremiumOverviewCard(
                        label = "Pending",
                        value = if (isLoadingAppointments) "–" else pending.size.toString(),
                        subtitle = "Needs attention",
                        imageRes = R.drawable.dashboard_pending,
                        accent = DoctorAmber,
                        modifier = Modifier.width(112.dp)
                    )
                }
                item {
                    PremiumOverviewCard(
                        label = "Upcoming",
                        value = if (isLoadingAppointments) "–" else upcoming.size.toString(),
                        subtitle = "Scheduled",
                        imageRes = R.drawable.dashboard_upcoming,
                        accent = DoctorBlue,
                        modifier = Modifier.width(112.dp)
                    )
                }
                item {
                    PremiumOverviewCard(
                        label = "Completed",
                        value = if (isLoadingAppointments) "–" else completed.size.toString(),
                        subtitle = "Finished",
                        imageRes = R.drawable.dashboard_completed,
                        accent = DoctorGreen,
                        modifier = Modifier.width(112.dp)
                    )
                }
                item {
                    PremiumOverviewCard(
                        label = "Patients",
                        value = if (isLoadingAppointments) "–" else patientCount.toString(),
                        subtitle = "Unique seen",
                        imageRes = R.drawable.dashboard_patients,
                        accent = DoctorPurple,
                        modifier = Modifier.width(112.dp)
                    )
                }
            }
        }

        item {
            DashboardSectionHeader(
                title = "Quick actions",
                subtitle = "Everything you need, one tap away",
                action = "",
                onAction = {}
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PremiumQuickActionCard(
                        title = "Appointments",
                        subtitle = "Review your schedule and consultations",
                        action = "Open schedule",
                        imageRes = R.drawable.dashboard_appointments,
                        accent = DoctorBlue,
                        onClick = onNavigateAppointments,
                        modifier = Modifier.width(235.dp)
                    )
                }
                item {
                    PremiumQuickActionCard(
                        title = "Time slots",
                        subtitle = "Manage availability and working hours",
                        action = "Set hours",
                        imageRes = R.drawable.dashboard_timeslots,
                        accent = Color(0xFF0891B2),
                        onClick = onNavigateTimeSlots,
                        modifier = Modifier.width(235.dp)
                    )
                }
                item {
                    PremiumQuickActionCard(
                        title = "Prescriptions",
                        subtitle = "Create and review patient prescriptions",
                        action = "Manage medicine",
                        imageRes = R.drawable.dashboard_prescriptions,
                        accent = Color(0xFF8B5CF6),
                        onClick = onNavigatePrescriptions,
                        modifier = Modifier.width(235.dp)
                    )
                }
                item {
                    PremiumQuickActionCard(
                        title = "Availability",
                        subtitle = "Update consultation status and availability",
                        action = "Update status",
                        imageRes = R.drawable.dashboard_availability,
                        accent = DoctorTeal,
                        onClick = onNavigateAvailability,
                        modifier = Modifier.width(235.dp)
                    )
                }
            }
        }

        item {
            DashboardSectionHeader(
                title = "Next appointment",
                subtitle = "Your upcoming consultation",
                action = if (upcoming.isNotEmpty()) "See all" else "",
                onAction = onNavigateAppointments
            )
        }

        when {
            isLoadingAppointments -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DoctorTeal)
                    }
                }
            }

            loadError != null -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

                if (upcoming.size > 1) {
                    item {
                        DashboardSectionHeader(
                            title = "Coming up",
                            subtitle = "Your next ${upcoming.drop(1).take(3).size} visits",
                            action = "See all",
                            onAction = onNavigateAppointments
                        )
                    }

                    items(upcoming.drop(1).take(3), key = { it.id }) { appointment ->
                        CompactUpcomingAppointmentCard(
                            appointment = appointment,
                            onClick = onNavigateAppointments
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }
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
    subtitle: String = "",
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 14.dp, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = DoctorInk,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = DoctorMuted,
                    fontSize = 10.sp
                )
            }
        }

        if (action.isNotBlank()) {
            TextButton(onClick = onAction) {
                Text(
                    action,
                    color = DoctorTeal,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
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
        modifier = modifier.height(136.dp),
        shape = RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.07f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.13f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = label,
                    modifier = Modifier.size(34.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                value,
                color = DoctorInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                label,
                color = DoctorInk,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                subtitle,
                color = DoctorMuted,
                fontSize = 7.8.sp,
                maxLines = 1
            )
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
            .height(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = accent.copy(alpha = 0.065f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.13f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.42f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = title,
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(vertical = 7.dp)
            ) {
                Text(
                    title,
                    color = DoctorInk,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    subtitle,
                    color = DoctorMuted,
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    "$action  →",
                    color = accent,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
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
        border = androidx.compose.foundation.BorderStroke(1.dp, DoctorOutline)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = DoctorTealSoft
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            DoctorTeal.copy(alpha = 0.14f)
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(DoctorTeal)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(47.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White.copy(alpha = 0.70f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = DoctorTeal,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(11.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            appointment.patient_name ?: "Unknown patient",
                            color = DoctorInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            appointment.reason ?: "General consultation",
                            color = DoctorTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    StatusPill(appointment.status, statusColor)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
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

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.48f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = DoctorGreen,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Column {
                        Text(
                            "Patient visit",
                            color = DoctorGreen,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tap to view appointment details and actions",
                            color = DoctorMuted,
                            fontSize = 8.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactUpcomingAppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    val statusColor = appointmentStatusColor(appointment.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DoctorBlueSoft.copy(alpha = 0.58f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            DoctorBlue.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.EventNote,
                    contentDescription = null,
                    tint = DoctorBlue,
                    modifier = Modifier.size(21.dp)
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    appointment.patient_name ?: "Unknown patient",
                    color = DoctorInk,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${appointment.date ?: "Date pending"}  •  ${appointment.time ?: "Time pending"}",
                    color = DoctorMuted,
                    fontSize = 9.5.sp
                )
                Text(
                    appointment.reason ?: "General consultation",
                    color = DoctorMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }

            StatusPill(appointment.status, statusColor)
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
    data class PatientSummary(
        val name: String,
        val visitCount: Int,
        val completedCount: Int,
        val upcomingCount: Int,
        val lastDate: String?,
        val lastReason: String?
    )

    val patients = remember(appointments) {
        appointments
            .filter { !it.patient_name.isNullOrBlank() }
            .groupBy { it.patient_name!! }
            .map { (name, appts) ->
                val completed = appts.count {
                    it.status.equals("completed", ignoreCase = true)
                }

                val upcoming = appts.count {
                    val status = it.status?.lowercase().orEmpty()
                    status != "completed" &&
                            status != "cancelled" &&
                            status != "declined"
                }

                val latest = appts
                    .filter { !it.date.isNullOrBlank() }
                    .maxByOrNull { it.date ?: "" }

                PatientSummary(
                    name = name,
                    visitCount = appts.size,
                    completedCount = completed,
                    upcomingCount = upcoming,
                    lastDate = latest?.date,
                    lastReason = latest?.reason
                )
            }
            .sortedWith(
                compareByDescending<PatientSummary> { it.upcomingCount }
                    .thenBy { it.name }
            )
    }

    val totalVisits = remember(appointments) {
        appointments.count {
            !it.patient_name.isNullOrBlank()
        }
    }

    val completedVisits = remember(appointments) {
        appointments.count {
            !it.patient_name.isNullOrBlank() &&
                    it.status.equals("completed", ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DoctorScreenBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(
                        "Patients",
                        color = DoctorInk,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Your connected patient workspace",
                        color = DoctorMuted,
                        fontSize = 10.5.sp
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0F8F86),
                                        Color(0xFF2D6EDC)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(17.dp))
                                        .background(Color.White.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Groups2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(29.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Patient care hub",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        "A quick view of the people you have treated and the visits still ahead.",
                                        color = Color.White.copy(alpha = 0.82f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                PatientHeroMetric(
                                    value = patients.size.toString(),
                                    label = "Patients",
                                    modifier = Modifier.weight(1f)
                                )
                                PatientHeroMetric(
                                    value = totalVisits.toString(),
                                    label = "Visits",
                                    modifier = Modifier.weight(1f)
                                )
                                PatientHeroMetric(
                                    value = completedVisits.toString(),
                                    label = "Completed",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = DoctorTeal)
                        }
                    }
                }

                patients.isEmpty() -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(DoctorTealSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Groups,
                                        contentDescription = null,
                                        tint = DoctorTeal,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }

                                Spacer(Modifier.height(13.dp))

                                Text(
                                    "No connected patients yet",
                                    color = DoctorInk,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    "Patients linked to your appointments will appear here automatically.",
                                    color = DoctorMuted,
                                    fontSize = 10.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                else -> {
                    item {
                        Column {
                            Text(
                                "Patient directory",
                                color = DoctorInk,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${patients.size} patient${if (patients.size == 1) "" else "s"} connected to your practice",
                                color = DoctorMuted,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    items(
                        items = patients,
                        key = { it.name }
                    ) { patient ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(15.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(49.dp)
                                            .clip(CircleShape)
                                            .background(DoctorTealSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            patient.name
                                                .trim()
                                                .split(" ")
                                                .filter { it.isNotBlank() }
                                                .take(2)
                                                .joinToString("") { it.take(1).uppercase() }
                                                .ifBlank { "P" },
                                            color = DoctorTeal,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            patient.name,
                                            color = DoctorInk,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "${patient.visitCount} appointment${if (patient.visitCount == 1) "" else "s"}",
                                            color = DoctorMuted,
                                            fontSize = 9.5.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (patient.upcomingCount > 0) {
                                                    DoctorBlueSoft
                                                } else {
                                                    DoctorGreenSoft
                                                }
                                            )
                                            .padding(horizontal = 9.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            if (patient.upcomingCount > 0) {
                                                "${patient.upcomingCount} upcoming"
                                            } else {
                                                "Up to date"
                                            },
                                            color = if (patient.upcomingCount > 0) {
                                                DoctorBlue
                                            } else {
                                                DoctorGreen
                                            },
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(Modifier.height(13.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PatientStatChip(
                                        icon = Icons.Outlined.CheckCircle,
                                        label = "Completed",
                                        value = patient.completedCount.toString(),
                                        accent = DoctorGreen,
                                        background = DoctorGreenSoft,
                                        modifier = Modifier.weight(1f)
                                    )

                                    PatientStatChip(
                                        icon = Icons.Outlined.EventAvailable,
                                        label = "Upcoming",
                                        value = patient.upcomingCount.toString(),
                                        accent = DoctorBlue,
                                        background = DoctorBlueSoft,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                if (!patient.lastDate.isNullOrBlank() || !patient.lastReason.isNullOrBlank()) {
                                    Spacer(Modifier.height(9.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(DoctorScreenBackground)
                                            .padding(horizontal = 11.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(DoctorPurpleSoft),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.Assignment,
                                                contentDescription = null,
                                                tint = DoctorPurple,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }

                                        Spacer(Modifier.width(9.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "LATEST VISIT",
                                                color = DoctorMuted,
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                listOfNotNull(
                                                    patient.lastDate,
                                                    patient.lastReason
                                                ).joinToString(" • "),
                                                color = DoctorInk,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientHeroMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            label,
            color = Color.White.copy(alpha = 0.76f),
            fontSize = 8.5.sp
        )
    }
}


@Composable
private fun PatientStatChip(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(17.dp)
        )

        Spacer(Modifier.width(7.dp))

        Column {
            Text(
                value,
                color = DoctorInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                label,
                color = DoctorMuted,
                fontSize = 7.5.sp
            )
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
    val verificationStatus =
        doctorProfile?.verificationStatus
            ?.lowercase()
            .orEmpty()

    val verificationColor =
        when (verificationStatus) {
            "approved" -> DoctorGreen
            "pending" -> DoctorAmber
            else -> DoctorRed
        }

    val verificationBackground =
        when (verificationStatus) {
            "approved" -> DoctorGreenSoft
            "pending" -> DoctorAmberSoft
            else -> DoctorRedSoft
        }

    val verificationLabel =
        when (verificationStatus) {
            "approved" -> "Verified professional"
            "pending" -> "Verification pending"
            else -> "Verification required"
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DoctorScreenBackground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp,
                bottom = 30.dp
            )
    ) {
        Text(
            text = "Professional profile",
            color = DoctorInk,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = "Manage your identity, practice and clinical tools",
            color = DoctorMuted,
            fontSize = 10.5.sp
        )

        Spacer(Modifier.height(14.dp))

        // Premium identity hero.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF087F79),
                                Color(0xFF1769AA)
                            )
                        )
                    )
                    .padding(19.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        PatientAvatar(
                            avatarUrl = doctorProfile?.profileImageUrl,
                            name = doctorProfile?.name ?: "",
                            size = 96.dp
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(31.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(
                                    enabled = !isUploadingPhoto,
                                    onClick = onUploadPhoto
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingPhoto) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = DoctorTeal
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.CameraAlt,
                                    contentDescription = "Change profile photo",
                                    tint = DoctorTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(13.dp))

                    Text(
                        text = doctorProfile?.let {
                            "Dr. ${it.name} ${it.surname}"
                        } ?: "Doctor",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    doctorProfile?.discipline
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.82f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                    doctorProfile?.practiceName
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = it,
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 9.5.sp
                            )
                        }

                    Spacer(Modifier.height(13.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (verificationStatus == "approved") {
                                            Color(0xFF65F0C4)
                                        } else if (verificationStatus == "pending") {
                                            Color(0xFFFFD166)
                                        } else {
                                            Color(0xFFFF8B8B)
                                        }
                                    )
                            )

                            Spacer(Modifier.width(7.dp))

                            Text(
                                text = verificationLabel,
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        DoctorProfileMetric(
                            icon = Icons.Outlined.MedicalServices,
                            value = doctorProfile?.discipline
                                ?.takeIf { it.isNotBlank() }
                                ?: "General",
                            label = "Discipline",
                            modifier = Modifier.weight(1f)
                        )

                        DoctorProfileMetric(
                            icon = Icons.Outlined.EventAvailable,
                            value = if (verificationStatus == "approved") {
                                "Active"
                            } else {
                                "Pending"
                            },
                            label = "Profile",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Practice identity card.
        ProfileSectionHeading(
            title = "Practice identity",
            subtitle = "Professional information visible in your workspace"
        )

        Spacer(Modifier.height(9.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                ProfessionalInfoRow(
                    icon = Icons.Outlined.Person,
                    label = "Professional name",
                    value = doctorProfile?.let {
                        "Dr. ${it.name} ${it.surname}"
                    } ?: "Not available",
                    accent = DoctorTeal,
                    soft = DoctorTealSoft
                )

                Spacer(Modifier.height(9.dp))

                ProfessionalInfoRow(
                    icon = Icons.Outlined.MedicalServices,
                    label = "Discipline",
                    value = doctorProfile?.discipline
                        ?.takeIf { it.isNotBlank() }
                        ?: "Not specified",
                    accent = DoctorBlue,
                    soft = DoctorBlueSoft
                )

                Spacer(Modifier.height(9.dp))

                ProfessionalInfoRow(
                    icon = Icons.Outlined.EventNote,
                    label = "Practice",
                    value = doctorProfile?.practiceName
                        ?.takeIf { it.isNotBlank() }
                        ?: "Not specified",
                    accent = DoctorPurple,
                    soft = DoctorPurpleSoft
                )

                Spacer(Modifier.height(9.dp))

                ProfessionalInfoRow(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Verification",
                    value = verificationLabel,
                    accent = verificationColor,
                    soft = verificationBackground
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        ProfileSectionHeading(
            title = "Clinical workspace",
            subtitle = "Manage the tools you use most often"
        )

        Spacer(Modifier.height(9.dp))

        DoctorProfileMenuItem(
            icon = Icons.Outlined.Medication,
            title = "Prescription centre",
            subtitle = "Create medication plans and review refill requests",
            iconColor = DoctorTeal,
            iconBackground = DoctorTealSoft,
            badge = "Rx",
            onClick = onNavigatePrescriptions
        )

        Spacer(Modifier.height(10.dp))

        DoctorProfileMenuItem(
            icon = Icons.Outlined.Schedule,
            title = "Schedule & time slots",
            subtitle = "Control your consultation timetable and open slots",
            iconColor = DoctorBlue,
            iconBackground = DoctorBlueSoft,
            badge = "Manage",
            onClick = onNavigateTimeSlots
        )

        Spacer(Modifier.height(10.dp))

        DoctorProfileMenuItem(
            icon = Icons.Outlined.Settings,
            title = "Settings & privacy",
            subtitle = "Notifications, appearance, privacy and account options",
            iconColor = DoctorPurple,
            iconBackground = DoctorPurpleSoft,
            badge = null,
            onClick = onNavigateSettings
        )

        Spacer(Modifier.height(20.dp))

        ProfileSectionHeading(
            title = "Account",
            subtitle = "Profile photo and session controls"
        )

        Spacer(Modifier.height(9.dp))

        DoctorProfileMenuItem(
            icon = Icons.Outlined.CameraAlt,
            title = if (isUploadingPhoto) {
                "Updating profile photo..."
            } else {
                "Change profile photo"
            },
            subtitle = "Keep your professional profile current",
            iconColor = DoctorAmber,
            iconBackground = DoctorAmberSoft,
            badge = null,
            enabled = !isUploadingPhoto,
            onClick = onUploadPhoto
        )

        Spacer(Modifier.height(10.dp))

        DoctorProfileMenuItem(
            icon = Icons.Outlined.ExitToApp,
            title = "Sign out",
            subtitle = "Securely end this doctor session",
            iconColor = DoctorRed,
            iconBackground = DoctorRedSoft,
            badge = null,
            onClick = onLogout
        )
    }
}

@Composable
private fun DoctorProfileMetric(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.13f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = value,
                color = Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 7.5.sp
            )
        }
    }
}

@Composable
private fun ProfileSectionHeading(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            color = DoctorInk,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            color = DoctorMuted,
            fontSize = 9.5.sp
        )
    }
}

@Composable
private fun ProfessionalInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    soft: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(soft)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(39.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(19.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                color = DoctorMuted,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                color = DoctorInk,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DoctorProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    iconBackground: Color,
    badge: String?,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 15.dp,
                vertical = 14.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(47.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (title == "Sign out") {
                        DoctorRed
                    } else {
                        DoctorInk
                    },
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    color = DoctorMuted,
                    fontSize = 9.5.sp,
                    lineHeight = 13.sp
                )
            }

            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(iconBackground)
                        .padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        )
                ) {
                    Text(
                        text = badge,
                        color = iconColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(7.dp))
            }

            Text(
                text = "›",
                color = iconColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
