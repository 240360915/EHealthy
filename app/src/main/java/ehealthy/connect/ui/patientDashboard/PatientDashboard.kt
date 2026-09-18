package ehealthy.connect.ui.patientDashboard

import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.style.TextAlign
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class Appointment(
    val id: String,
    val patient_name: String? = null,
    val reason: String? = null,
    val date: String? = null,
    val time: String? = null,
    val status: String? = null,
    val payment_method: String? = null,
    val amount_paid: Double? = null,
    val doctor_id: String? = null,
    val appointment_type: String? = null
)

@Serializable
data class Review(
    val id: String,
    val patient_id: String,
    val doctor_id: String,
    val appointment_id: String? = null,
    val rating: Int,
    val comment: String? = null,
    val created_at: String
)

data class ReviewDisplay(
    val review: Review,
    val doctorName: String
)

data class QuickAction(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@RequiresApi(Build.VERSION_CODES.O)
fun isUpcomingAppointment(appt: Appointment): Boolean {
    if (appt.status == "cancelled") return false

    val date = appt.date ?: return true
    return date >= LocalDate.now().toString()
}

/**
 * Parses this appointment's date + time into a LocalDateTime,
 * or null if either field is missing/unparseable.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun appointmentDateTime(appt: Appointment): LocalDateTime? {
    val date = appt.date ?: return null
    val time = appt.time?.take(5) ?: return null

    return try {
        LocalDateTime.parse("${date}T$time:00")
    } catch (_: Exception) {
        null
    }
}

/**
 * Online appointment call window:
 * - Opens 5 minutes before scheduled time.
 * - Remains open until 60 minutes after scheduled time.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun callWindowState(appt: Appointment): CallWindowState {
    val dateTime = appointmentDateTime(appt)
        ?: return CallWindowState.NOT_APPLICABLE

    val now = LocalDateTime.now()
    val minutesUntilStart = Duration.between(now, dateTime).toMinutes()

    return when {
        minutesUntilStart > 5 -> CallWindowState.TOO_EARLY
        minutesUntilStart >= -60 -> CallWindowState.OPEN
        else -> CallWindowState.CLOSED
    }
}

private enum class CallWindowState {
    NOT_APPLICABLE,
    TOO_EARLY,
    OPEN,
    CLOSED
}

/**
 * Finds the first non-cancelled appointment due to start within
 * the next 5 minutes.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun findReminderAppointment(
    appointments: List<Appointment>,
    now: LocalDateTime
): Appointment? {
    return appointments.firstOrNull { appt ->
        if (appt.status == "cancelled") {
            return@firstOrNull false
        }

        val dateTime = appointmentDateTime(appt)
            ?: return@firstOrNull false

        val minutesUntilStart = Duration.between(now, dateTime).toMinutes()

        minutesUntilStart in 0..5
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboard(
    patientName: String,
    patientAvatarUrl: String?,
    isUploadingPhoto: Boolean,

    onNavigateFindDoctors: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigateHealthTips: () -> Unit,
    onNavigateSettings: () -> Unit,

    onUploadPhoto: () -> Unit,
    onLogout: () -> Unit,

    fetchAppointments: suspend () -> Result<List<Appointment>>,
    fetchReviews: suspend () -> Result<List<ReviewDisplay>>,

    /*
     * Used by the appointment details dialog to load the
     * doctor belonging to the appointment.
     */
    fetchDoctor: suspend (doctorId: String) -> Result<DoctorProfile>,

    /*
     * Navigates from the appointment details to the existing
     * DoctorProfileScreen.
     */
    onViewDoctorProfile: (doctorId: String) -> Unit,

    onRescheduleAppointment: (String) -> Unit,
    cancelAppointment: suspend (String) -> Result<Unit>,

    onStartCall: (String) -> Unit = {}
) {
    val background = MaterialTheme.colorScheme.background
    val navy = MaterialTheme.colorScheme.onBackground
    val green = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    var selectedTab by remember {
        mutableStateOf(PatientTab.HOME)
    }

    var appointments by remember {
        mutableStateOf<List<Appointment>>(emptyList())
    }

    var isLoadingAppointments by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    var reviews by remember {
        mutableStateOf<List<ReviewDisplay>>(emptyList())
    }

    var isLoadingReviews by remember {
        mutableStateOf(true)
    }

    /*
     * Appointment selected from the appointment card.
     * This opens AppointmentActionSheet.
     */
    var selectedAppointment by remember {
        mutableStateOf<Appointment?>(null)
    }

    /*
     * Appointment whose "View Details" action was selected.
     */
    var showDetailsForAppointment by remember {
        mutableStateOf<Appointment?>(null)
    }

    var isCancelling by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    /*
     * Ticks every 30 seconds so appointment reminders and
     * online call windows stay current.
     */
    var now by remember {
        mutableStateOf(LocalDateTime.now())
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalDateTime.now()
        }
    }

    /*
     * Prevents the reminder from appearing repeatedly after
     * the patient dismisses it.
     */
    var dismissedReminderIds by remember {
        mutableStateOf(setOf<String>())
    }

    val reminderAppointment = remember(
        appointments,
        now,
        dismissedReminderIds
    ) {
        findReminderAppointment(
            appointments,
            now
        )?.takeIf {
            it.id !in dismissedReminderIds
        }
    }

    /*
     * Load appointments.
     */
    LaunchedEffect(Unit) {
        val result = fetchAppointments()

        isLoadingAppointments = false

        result
            .onSuccess {
                appointments = it
            }
            .onFailure {
                loadError =
                    it.message ?: "Failed to load appointments."
            }
    }

    /*
     * Load reviews.
     */
    LaunchedEffect(Unit) {
        val result = fetchReviews()

        isLoadingReviews = false

        result.onSuccess {
            reviews = it
        }
    }

    val onTabSelected: (PatientTab) -> Unit = { tab ->
        when (tab) {
            PatientTab.FIND_DOCTORS ->
                onNavigateFindDoctors()

            PatientTab.APPOINTMENTS ->
                onNavigateAppointments()

            else ->
                selectedTab = tab
        }
    }

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

                        Text(
                            "health-care made easier for you",
                            color = green,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    PatientAvatar(
                        avatarUrl = patientAvatarUrl,
                        name = patientName,
                        size = 36.dp,
                        onClick = {
                            selectedTab = PatientTab.PROFILE
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },

        bottomBar = {
            PatientBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
        }
    ) { paddingValues ->

        when (selectedTab) {

            PatientTab.HOME -> {
                HomeTabContent(
                    modifier = Modifier.padding(paddingValues),
                    patientName = patientName,
                    background = background,
                    navy = navy,
                    accent = accent,
                    muted = muted,
                    appointments = appointments,
                    isLoadingAppointments = isLoadingAppointments,
                    loadError = loadError,
                    reviews = reviews,
                    isLoadingReviews = isLoadingReviews,
                    onNavigateFindDoctors = onNavigateFindDoctors,
                    onNavigateAppointments = onNavigateAppointments,
                    onNavigateMedicalRecords = onNavigateMedicalRecords,
                    onNavigateHealthTips = onNavigateHealthTips,

                    /*
                     * Clicking an appointment card opens
                     * the appointment action sheet.
                     */
                    onAppointmentClick = {
                        selectedAppointment = it
                    },

                    onStartCall = onStartCall
                )
            }

            PatientTab.PROFILE -> {
                ProfileTabContent(
                    modifier = Modifier.padding(paddingValues),
                    patientName = patientName,
                    patientAvatarUrl = patientAvatarUrl,
                    isUploadingPhoto = isUploadingPhoto,
                    background = background,
                    navy = navy,
                    muted = muted,
                    onUploadPhoto = onUploadPhoto,
                    onNavigateSettings = onNavigateSettings,
                    onNavigateMedicalRecords = onNavigateMedicalRecords,
                    onLogout = onLogout
                )
            }

            else -> Unit
        }
    }

    /*
     * ---------------------------------------------------------
     * APPOINTMENT ACTION SHEET
     * ---------------------------------------------------------
     *
     * Appointment card
     *      ↓
     * selectedAppointment
     *      ↓
     * AppointmentActionSheet
     */
    selectedAppointment?.let { appt ->

        AppointmentActionSheet(
            appointment = appt,

            onDismiss = {
                selectedAppointment = null
            },

            /*
             * User chooses "View Details".
             */
            onViewDetails = {
                showDetailsForAppointment = appt
                selectedAppointment = null
            },

            onReschedule = {
                selectedAppointment = null
                onRescheduleAppointment(appt.id)
            },

            onCancel = {
                scope.launch {
                    isCancelling = true

                    cancelAppointment(appt.id)
                        .onSuccess {
                            appointments = appointments.map {
                                if (it.id == appt.id) {
                                    it.copy(status = "cancelled")
                                } else {
                                    it
                                }
                            }
                        }

                    isCancelling = false
                    selectedAppointment = null
                }
            }
        )
    }

    /*
     * ---------------------------------------------------------
     * APPOINTMENT DETAILS
     * ---------------------------------------------------------
     *
     * View Details
     *      ↓
     * AppointmentDetailsDialog
     *      ↓
     * Doctor card
     *      ↓
     * DoctorProfileScreen
     */
    showDetailsForAppointment?.let { appt ->

        AppointmentDetailsDialog(
            appt = appt,

            fetchDoctor = fetchDoctor,

            onViewDoctorProfile = { doctorId ->
                showDetailsForAppointment = null
                onViewDoctorProfile(doctorId)
            },

            onDismiss = {
                showDetailsForAppointment = null
            }
        )
    }

    /*
     * Emergency "be ready" reminder.
     */
    reminderAppointment?.let { appt ->

        AppointmentReminderPopup(
            appointment = appt,

            onDismiss = {
                dismissedReminderIds =
                    dismissedReminderIds + appt.id
            },

            onViewAppointment = {
                dismissedReminderIds =
                    dismissedReminderIds + appt.id

                selectedAppointment = appt
            }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AppointmentReminderPopup(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onViewAppointment: () -> Unit
) {
    val dateTime = remember(appointment) {
        appointmentDateTime(appointment)
    }

    val minutesLeft = remember(
        appointment,
        dateTime
    ) {
        dateTime
            ?.let {
                Duration
                    .between(LocalDateTime.now(), it)
                    .toMinutes()
                    .coerceAtLeast(0)
            }
            ?: 0
    }

    val visitLabel =
        if (appointment.appointment_type == "online") {
            "video call"
        } else {
            "in-person visit"
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = false
        )
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFFBEB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    if (minutesLeft <= 0)
                        "Your appointment is starting"
                    else
                        "Appointment in $minutesLeft min",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    "You have a $visitLabel scheduled at " +
                            "${appointment.time?.take(5) ?: "-"}. " +
                            "Please be ready.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                appointment.reason
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Reason: $it",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onViewAppointment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F1F3D)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "View Appointment",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "I'm ready, dismiss",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HomeTabContent(
    modifier: Modifier,
    patientName: String,
    background: Color,
    navy: Color,
    accent: Color,
    muted: Color,
    appointments: List<Appointment>,
    isLoadingAppointments: Boolean,
    loadError: String?,
    reviews: List<ReviewDisplay>,
    isLoadingReviews: Boolean,
    onNavigateFindDoctors: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigateHealthTips: () -> Unit,
    onAppointmentClick: (Appointment) -> Unit,
    onStartCall: (String) -> Unit
) {
    val upcoming = remember(appointments) {
        appointments.filter {
            isUpcomingAppointment(it)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(background)
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
                "Everything you need, one tap away.",
                color = muted,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            val actions = listOf(
                QuickAction(
                    "Medical Records",
                    "View your health history",
                    Icons.Outlined.Description,
                    onNavigateMedicalRecords
                ),
                QuickAction(
                    "Health Tips",
                    "Wellness advice & guidance",
                    Icons.Outlined.Lightbulb,
                    onNavigateHealthTips
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                actions.forEachIndexed { index, action ->

                    QuickActionCard(
                        action = action,
                        accent = accent,
                        navy = navy,
                        muted = muted,
                        modifier = Modifier.weight(1f)
                    )

                    if (index < actions.lastIndex) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                "UPCOMING APPOINTMENTS",
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
                Text(
                    loadError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

        } else if (upcoming.isEmpty()) {

            item {
                EmptyAppointmentsCard(
                    onFindDoctor = onNavigateFindDoctors
                )
            }

        } else {

            items(upcoming.take(3)) { appt ->

                AppointmentCard(
                    appt = appt,
                    onClick = {
                        onAppointmentClick(appt)
                    },
                    onStartCall = {
                        onStartCall(appt.id)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (upcoming.size > 3) {

                item {
                    TextButton(
                        onClick = onNavigateAppointments
                    ) {
                        Text(
                            "View all appointments →",
                            color = accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "REVIEWS",
                color = navy,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (isLoadingReviews) {

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

        } else if (reviews.isEmpty()) {

            item {
                EmptyReviewsCard(
                    onGoRate = onNavigateAppointments
                )
            }

        } else {

            items(reviews.take(3)) { rd ->

                ReviewCard(rd)

                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                TextButton(
                    onClick = onNavigateAppointments
                ) {
                    Text(
                        "Rate another visit →",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProfileTabContent(
    modifier: Modifier,
    patientName: String,
    patientAvatarUrl: String?,
    isUploadingPhoto: Boolean,
    background: Color,
    navy: Color,
    muted: Color,
    onUploadPhoto: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onLogout: () -> Unit
) {
    var showPhotoViewer by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp)
    ) {

        EditablePatientAvatar(
            avatarUrl = patientAvatarUrl,
            name = patientName,
            size = 96.dp,
            isUploading = isUploadingPhoto,
            onViewPhoto = {
                showPhotoViewer = true
            },
            onChangePhoto = onUploadPhoto
        )

        if (
            showPhotoViewer &&
            !patientAvatarUrl.isNullOrBlank()
        ) {
            PhotoViewerDialog(
                photoUrl = patientAvatarUrl,
                onDismiss = {
                    showPhotoViewer = false
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            patientName,
            color = navy,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (patientAvatarUrl.isNullOrBlank()) {

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF6FF)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Add a profile picture",
                        color = navy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Upload a photo, or we'll keep showing your initial.",
                        color = muted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    TextButton(
                        onClick = onUploadPhoto,
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isUploadingPhoto
                    ) {
                        Text(
                            if (isUploadingPhoto)
                                "Uploading…"
                            else
                                "Upload photo →",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        ProfileMenuItem(
            Icons.Outlined.Description,
            "Medical Records",
            navy,
            onClick = onNavigateMedicalRecords
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProfileMenuItem(
            Icons.Outlined.Settings,
            "Settings",
            navy,
            onClick = onNavigateSettings
        )

        Spacer(modifier = Modifier.height(8.dp))

        ProfileMenuItem(
            Icons.Outlined.ExitToApp,
            "Logout",
            MaterialTheme.colorScheme.error,
            onClick = onLogout
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                label,
                color = tint,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
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
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = action.onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                action.label,
                color = navy,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                action.description,
                color = muted,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun EmptyAppointmentsCard(
    onFindDoctor: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
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
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "No upcoming appointments.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(onClick = onFindDoctor) {
                Text(
                    "Find a doctor to book one",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyReviewsCard(
    onGoRate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                Icons.Outlined.RateReview,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "You haven't left any reviews yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(onClick = onGoRate) {
                Text(
                    "Rate a completed visit",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(
    rd: ReviewDisplay
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rd.doctorName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Row {
                    repeat(5) { i ->
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint =
                                if (i < rd.review.rating)
                                    Color(0xFFF59E0B)
                                else
                                    Color(0xFFE2E8F0),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            rd.review.comment
                ?.takeIf { it.isNotBlank() }
                ?.let {

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AppointmentCard(
    appt: Appointment,
    onClick: () -> Unit,
    onStartCall: () -> Unit
) {
    val statusColor = when (appt.status) {
        "confirmed" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "rescheduled" -> MaterialTheme.colorScheme.primary
        "cancelled" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
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
        "cancelled" -> "Cancelled"
        else ->
            appt.status?.replaceFirstChar {
                it.uppercase()
            } ?: "Unknown"
    }

    val isOnline =
        appt.appointment_type == "online"

    val isCancelled =
        appt.status == "cancelled"

    val isConfirmed =
        appt.status == "confirmed"

    val windowState =
        if (isOnline && !isCancelled)
            callWindowState(appt)
        else
            CallWindowState.NOT_APPLICABLE

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {

                Text(
                    "📅 ${appt.date ?: "-"}   🕐 ${appt.time ?: "-"}",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "📝 ${appt.reason ?: "General consultation"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )

                appt.payment_method?.let {

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "💳 Paid via $it",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                appt.amount_paid?.let {

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "💰 R %.2f".format(it),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                ) {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isOnline && !isCancelled) {

                    Spacer(modifier = Modifier.height(12.dp))

                    val isActive =
                        isConfirmed &&
                                windowState == CallWindowState.OPEN

                    Button(
                        onClick = {
                            if (isActive) {
                                onStartCall()
                            }
                        },
                        enabled = isActive,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F1F3D),
                            disabledContainerColor = Color(0xFFE2E8F0)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {

                        Icon(
                            Icons.Outlined.Videocam,
                            contentDescription = null,
                            tint =
                                if (isActive)
                                    Color.White
                                else
                                    Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            when {
                                !isConfirmed ->
                                    "Waiting for doctor to accept"

                                windowState == CallWindowState.TOO_EARLY ->
                                    "Start Call (available 5 min before)"

                                windowState == CallWindowState.OPEN ->
                                    "Start Call Now"

                                windowState == CallWindowState.CLOSED ->
                                    "Call window closed"

                                else ->
                                    "Start Call Now"
                            },
                            color =
                                if (isActive)
                                    Color.White
                                else
                                    Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Appointment details dialog.
 *
 * The doctor information is loaded using the appointment's
 * doctor_id. The doctor card itself is clickable and opens
 * the existing DoctorProfileScreen.
 */
@Composable
private fun AppointmentDetailsDialog(
    appt: Appointment,
    fetchDoctor: suspend (doctorId: String) -> Result<DoctorProfile>,
    onViewDoctorProfile: (doctorId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var doctor by remember(appt.doctor_id) {
        mutableStateOf<DoctorProfile?>(null)
    }

    var isLoadingDoctor by remember(appt.doctor_id) {
        mutableStateOf(false)
    }

    var doctorError by remember(appt.doctor_id) {
        mutableStateOf<String?>(null)
    }

    /*
     * Load the doctor when the details dialog opens.
     */
    LaunchedEffect(appt.doctor_id) {

        val doctorId = appt.doctor_id

        if (doctorId.isNullOrBlank()) {
            doctorError = "Doctor profile is unavailable."
            return@LaunchedEffect
        }

        isLoadingDoctor = true
        doctorError = null

        fetchDoctor(doctorId)
            .onSuccess {
                doctor = it
            }
            .onFailure {
                doctorError =
                    it.message ?: "Could not load doctor profile."
            }

        isLoadingDoctor = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                "Appointment Details",
                fontWeight = FontWeight.Bold
            )
        },

        text = {

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                /*
                 * -------------------------------------------------
                 * DOCTOR PROFILE CARD
                 * -------------------------------------------------
                 */
                if (isLoadingDoctor) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                } else if (doctor != null) {

                    val doc = doctor!!

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onViewDoctorProfile(doc.id)
                            }
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            /*
                             * Circular doctor profile image.
                             */
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary
                                    ),
                                contentAlignment = Alignment.Center
                            ) {

                                if (!doc.profile_image_url.isNullOrBlank()) {

                                    AsyncImage(
                                        model = doc.profile_image_url,
                                        contentDescription =
                                            "Doctor profile picture",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(CircleShape)
                                    )

                                } else {

                                    Text(
                                        "${doc.name?.firstOrNull() ?: ' '}" +
                                                "${doc.surname?.firstOrNull() ?: ' '}",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    "Doctor",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )

                                Text(
                                    "Dr. ${doc.name ?: ""} ${doc.surname ?: ""}".trim(),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    doc.discipline
                                        ?: "General Practitioner",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "View doctor profile →",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                } else if (doctorError != null) {

                    Text(
                        doctorError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(
                            vertical = 8.dp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                /*
                 * -------------------------------------------------
                 * APPOINTMENT INFORMATION
                 * -------------------------------------------------
                 */

                Text(
                    "Appointment",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Date: ${appt.date ?: "-"}",
                    fontSize = 13.sp
                )

                Text(
                    "Time: ${appt.time ?: "-"}",
                    fontSize = 13.sp
                )

                Text(
                    "Reason: ${appt.reason ?: "General consultation"}",
                    fontSize = 13.sp
                )

                Text(
                    "Status: ${
                        appt.status
                            ?.replaceFirstChar { it.uppercase() }
                            ?: "Unknown"
                    }",
                    fontSize = 13.sp
                )

                Text(
                    "Visit type: ${
                        if (appt.appointment_type == "online")
                            "Online"
                        else
                            "In Person"
                    }",
                    fontSize = 13.sp
                )

                appt.payment_method?.let {
                    Text(
                        "Payment method: $it",
                        fontSize = 13.sp
                    )
                }

                appt.amount_paid?.let {
                    Text(
                        "Amount paid: R %.2f".format(it),
                        fontSize = 13.sp
                    )
                }
            }
        },

        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close")
            }
        }
    )
}
