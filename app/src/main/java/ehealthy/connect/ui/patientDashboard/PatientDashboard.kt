package ehealthy.connect.ui.patientDashboard
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ehealthy.connect.data.patient.PatientAppointment
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.data.patient.PatientReviewDisplay
import ehealthy.connect.ml.DoctorRecommendation
import ehealthy.connect.ml.DoctorRecommendationEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.AutoAwesome


/**
 * These three types are intentionally kept in this package for compatibility with
 * the existing MainActivity and patient appointment screens. The repository uses
 * its own DTOs and maps them into these UI models.
 */
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
    val appointment_type: String? = null,
    val slot_id: String? = null,
    val price_minor: Int? = null,
    val currency: String? = null,
    val completion_code: String? = null,
    val cancelled_reason: String? = null
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

private data class QuickAction(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val backgroundColor: Color,
    val onClick: () -> Unit
)

private enum class CallWindowState { NOT_APPLICABLE, TOO_EARLY, OPEN, CLOSED }

@RequiresApi(Build.VERSION_CODES.O)
private fun Appointment.dateTimeOrNull(): LocalDateTime? {
    val safeDate = date ?: return null
    val safeTime = time?.take(8)?.let {
        when (it.length) {
            5 -> "$it:00"
            else -> it
        }
    } ?: return null

    return runCatching { LocalDateTime.parse("${safeDate}T$safeTime") }.getOrNull()
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Appointment.isUpcoming(now: LocalDateTime = LocalDateTime.now()): Boolean {
    if (status.equals("cancelled", ignoreCase = true) ||
        status.equals("completed", ignoreCase = true) ||
        status.equals("declined", ignoreCase = true)
    ) return false
    val appointmentDateTime = dateTimeOrNull() ?: return date?.let {
        runCatching { LocalDate.parse(it) >= now.toLocalDate() }.getOrDefault(false)
    } ?: false
    return appointmentDateTime >= now.minusMinutes(60)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun Appointment.callWindowState(now: LocalDateTime = LocalDateTime.now()): CallWindowState {
    val appointmentDateTime = dateTimeOrNull() ?: return CallWindowState.NOT_APPLICABLE
    val minutesUntilStart = Duration.between(now, appointmentDateTime).toMinutes()
    return when {
        minutesUntilStart > 5 -> CallWindowState.TOO_EARLY
        minutesUntilStart >= -60 -> CallWindowState.OPEN
        else -> CallWindowState.CLOSED
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun greetingForNow(): String {
    return when (LocalTime.now().hour) {
        in 0..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

private fun PatientAppointment.toUiAppointment() =
    Appointment(
        id = id,
        patient_name = patient_name,
        reason = reason,
        date = date,
        time = time,
        status = status,
        payment_method = payment_method,
        amount_paid = amount_paid,
        doctor_id = doctor_id,
        appointment_type = appointment_type,
        slot_id = slot_id,
        price_minor = price_minor,
        currency = currency,
        completion_code = completion_code,
        cancelled_reason = cancelled_reason
    )

private fun PatientReviewDisplay.toUiReview() = ReviewDisplay(
    review = Review(
        id = review.id,
        patient_id = review.patient_id,
        doctor_id = review.doctor_id,
        appointment_id = review.appointment_id,
        rating = review.rating,
        comment = review.comment,
        created_at = review.created_at
    ),
    doctorName = doctorName
)

/**
 * Modern patient dashboard.
 *
 * The function keeps the previous callback parameters so your existing MainActivity still
 * compiles while we migrate screen-by-screen. Data is now loaded through PatientRepository,
 * which resolves patient profile IDs correctly and uses the protected Supabase RPCs.
 */
@Suppress("UNUSED_PARAMETER")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboard(
    patientName: String,
    patientAvatarUrl: String?,
    isUploadingPhoto: Boolean,
    onNavigateFindDoctors: () -> Unit,
    onNavigateConsultations: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigatePrescriptions: () -> Unit,
    onNavigateHealthTips: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onUploadPhoto: () -> Unit,
    onLogout: () -> Unit,
    fetchAppointments: suspend () -> Result<List<Appointment>>,
    fetchReviews: suspend () -> Result<List<ReviewDisplay>>,
    onRescheduleAppointment: (String) -> Unit,
    cancelAppointment: suspend (String) -> Result<Unit>,
    onStartCall: (String) -> Unit = {},
    // Existing MainActivity callers continue compiling; wire these to the
    // dedicated routes for direct dashboard shortcuts (see README).
    onNavigateHealthProfile: () -> Unit = onNavigateSettings,
    onNavigateInvoices: () -> Unit = onNavigateSettings
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableStateOf(PatientTab.HOME) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var reviews by remember { mutableStateOf<List<ReviewDisplay>>(emptyList()) }
    var unreadNotificationCount by remember {
        mutableStateOf(0)
    }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var detailAppointment by remember { mutableStateOf<Appointment?>(null) }
    var isCancelling by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var dismissedReminderIds by remember { mutableStateOf(setOf<String>()) }

    // Reuse the SAME on-device Decision Tree classifier as Find Doctors.
    // Never manufacture a recommendation when the saved profile has no symptoms.
    var recommendation by remember { mutableStateOf<DoctorRecommendation?>(null) }
    var recommendationLoading by remember { mutableStateOf(true) }
    var healthProfileHasSymptoms by remember { mutableStateOf(false) }
    var recommendationLoadError by remember { mutableStateOf(false) }

    suspend fun refreshDashboard() {

        isLoading = true
        loadError = null

        val appointmentsResult =
            PatientRepository.getMyAppointments()

        val reviewsResult =
            PatientRepository.getMyReviews()

        val notificationsResult =
            PatientRepository.getMyNotifications()


        appointmentsResult
            .onSuccess {

                appointments =
                    it.map { appointment ->
                        appointment.toUiAppointment()
                    }
            }
            .onFailure {

                loadError =
                    it.message
                        ?: "We could not load your appointments."
            }


        reviewsResult
            .onSuccess {

                reviews =
                    it.map { review ->
                        review.toUiReview()
                    }
            }


        notificationsResult
            .onSuccess { notifications ->

                unreadNotificationCount =
                    notifications.count {
                        !it.is_read
                    }
            }


        isLoading = false
    }

    suspend fun refreshSmartRecommendation() {
        recommendationLoading = true
        recommendationLoadError = false
        // Independent of appointment loading: a profile error must not hide visits.
        PatientRepository.getMyHealthProfile()
            .onSuccess { profile ->
                healthProfileHasSymptoms = !profile.current_symptoms.isNullOrBlank()
                val result = runCatching {
                    DoctorRecommendationEngine.recommend(profile)
                }
                recommendation = result.getOrNull()
                recommendationLoadError = result.isFailure
            }
            .onFailure {
                recommendation = null
                healthProfileHasSymptoms = false
                recommendationLoadError = true
            }
        recommendationLoading = false
    }

    LaunchedEffect(refreshKey) {
        // Load profile and appointments concurrently for a responsive home screen.
        launch { refreshDashboard() }
        launch { refreshSmartRecommendation() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000.milliseconds)
            now = LocalDateTime.now()
        }
    }

    val upcomingAppointments = remember(appointments, now) {
        appointments
            .filter { it.isUpcoming(now) }
            .sortedBy { it.dateTimeOrNull() ?: LocalDateTime.MAX }
    }

    val nextAppointment = upcomingAppointments.firstOrNull()

    val reminderAppointment = remember(upcomingAppointments, now, dismissedReminderIds) {
        upcomingAppointments.firstOrNull { appointment ->
            val dt = appointment.dateTimeOrNull() ?: return@firstOrNull false
            val minutes = Duration.between(now, dt).toMinutes()
            minutes in 0..5 && appointment.id !in dismissedReminderIds
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PatientTopBar(
                patientName = patientName,
                patientAvatarUrl = patientAvatarUrl,

                onAvatarClick = {
                    selectedTab =
                        PatientTab.PROFILE
                },

                onNotificationsClick =
                    onNavigateNotifications,

                hasUnreadNotifications =
                    unreadNotificationCount > 0,

                onRefresh = {
                    refreshKey++
                },

                refreshing =
                    isLoading
            )
        },
        bottomBar = {
            PatientBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    when (tab) {
                        PatientTab.FIND_DOCTORS -> onNavigateFindDoctors()
                        PatientTab.APPOINTMENTS -> onNavigateAppointments()
                        else -> selectedTab = tab
                    }
                }
            )
        }
    ) { paddingValues ->
        when (selectedTab) {
            PatientTab.HOME -> ModernHomeTab(
                modifier = Modifier.padding(paddingValues),
                patientName = patientName,
                appointments = appointments,
                upcomingAppointments = upcomingAppointments,
                nextAppointment = nextAppointment,
                reviews = reviews,
                isLoading = isLoading,
                loadError = loadError,
                recommendation = recommendation,
                recommendationLoading = recommendationLoading,
                healthProfileHasSymptoms = healthProfileHasSymptoms,
                recommendationLoadError = recommendationLoadError,
                onRetry = { refreshKey++ },
                onNavigateFindDoctors = onNavigateFindDoctors,
                onNavigateConsultations = onNavigateConsultations,
                onNavigateAppointments = onNavigateAppointments,
                onNavigateMedicalRecords = onNavigateMedicalRecords,
                onNavigatePrescriptions = onNavigatePrescriptions,
                onNavigateHealthTips = onNavigateHealthTips,
                onNavigateHealthProfile = onNavigateHealthProfile,
                onNavigateInvoices = onNavigateInvoices,
                onNavigateNotifications = onNavigateNotifications,
                onAppointmentClick = { selectedAppointment = it },
                onStartCall = onStartCall
            )

            PatientTab.PROFILE -> ModernProfileTab(
                modifier = Modifier.padding(paddingValues),
                patientName = patientName,
                patientAvatarUrl = patientAvatarUrl,
                isUploadingPhoto = isUploadingPhoto,
                onUploadPhoto = onUploadPhoto,
                onNavigateSettings = onNavigateSettings,
                onNavigateMedicalRecords = onNavigateMedicalRecords,
                onNavigateAppointments = onNavigateAppointments,
                onLogout = onLogout
            )

            else -> Unit
        }
    }

    selectedAppointment?.let { appointment ->

        AppointmentActionSheet(
            appointment = appointment,

            onDismiss = {
                selectedAppointment = null
            },

            onViewDetails = {
                detailAppointment = appointment
                selectedAppointment = null
            },

            onReschedule = {
                selectedAppointment = null
                onRescheduleAppointment(
                    appointment.id
                )
            },

            onCancel = {

                scope.launch {

                    if (isCancelling) {
                        return@launch
                    }

                    isCancelling = true

                    PatientRepository.cancelAppointment(
                        appointmentId = appointment.id,
                        reason = "Cancelled by patient"
                    )
                        .onSuccess { result ->

                            appointments =
                                appointments.map {

                                    if (it.id == appointment.id) {

                                        it.copy(
                                            status =
                                                result.appointment_status
                                        )

                                    } else {

                                        it
                                    }
                                }

                            snackbarHostState.showSnackbar(
                                "Appointment cancelled."
                            )
                        }
                        .onFailure {

                            snackbarHostState.showSnackbar(
                                it.message
                                    ?: "Could not cancel the appointment."
                            )
                        }

                    isCancelling = false
                    selectedAppointment = null
                }
            },

            onRequestCompletionCode = { appointmentId ->

                val result =
                    PatientRepository
                        .requestCompletionCode(
                            appointmentId
                        )
                        .map {
                            it.completion_code
                        }


                result.onSuccess { code ->

                    appointments =
                        appointments.map {

                            if (it.id == appointmentId) {

                                it.copy(
                                    completion_code = code
                                )

                            } else {

                                it
                            }
                        }
                }


                result
            }
        )
    }

    detailAppointment?.let { appointment ->
        AppointmentDetailsDialog(
            appointment = appointment,
            onDismiss = { detailAppointment = null }
        )
    }

    reminderAppointment?.let { appointment ->
        AppointmentReminderPopup(
            appointment = appointment,
            onDismiss = {
                dismissedReminderIds = dismissedReminderIds + appointment.id
            },
            onViewAppointment = {
                dismissedReminderIds = dismissedReminderIds + appointment.id
                selectedAppointment = appointment
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientTopBar(
    patientName: String,
    patientAvatarUrl: String?,
    onAvatarClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    hasUnreadNotifications: Boolean,
    onRefresh: () -> Unit,
    refreshing: Boolean
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "HeaderAnimation"
        )

    val refreshRotation by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue =
            if (refreshing) {
                360f
            } else {
                0f
            },
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 900,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Restart
            ),
        label =
            "RefreshRotation"
    )


    TopAppBar(
        title = {

            Column {

                Text(
                    text =
                        "eHealth Connect",
                    fontSize =
                        17.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    color =
                        PatientColors
                            .TextPrimary
                )

                Text(
                    text =
                        "Your care, connected",
                    fontSize =
                        10.5.sp,
                    color =
                        PatientColors
                            .Primary,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        },

        actions = {

            /*
             * Notification button
             */
            Box(
                modifier =
                    Modifier
                        .size(
                            42.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(
                            PatientColors
                                .AppointmentCard
                        )
                        .clickable(
                            onClick =
                                onNotificationsClick
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.NotificationsActive,
                    contentDescription =
                        "Notifications",
                    tint =
                        PatientColors
                            .AppointmentAccent,
                    modifier =
                        Modifier.size(
                            20.dp
                        )
                )


                /*
                 * Notification indicator
                 */
                if (hasUnreadNotifications) {

                    Box(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.TopEnd
                                )
                                .padding(
                                    top = 7.dp,
                                    end = 7.dp
                                )
                                .size(
                                    7.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    PatientColors.Red
                                )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )


            /*
             * Refresh
             */
            IconButton(
                onClick =
                    onRefresh,
                enabled =
                    !refreshing
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                38.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    13.dp
                                )
                            )
                            .background(
                                PatientColors
                                    .DoctorCard
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Refresh,
                        contentDescription =
                            "Refresh dashboard",
                        tint =
                            PatientColors
                                .DoctorAccent,
                        modifier =
                            Modifier
                                .size(
                                    19.dp
                                )
                                .rotate(
                                    refreshRotation
                                )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )


            /*
             * Avatar with active ring
             */
            PatientHeaderAvatar(
                patientName =
                    patientName,
                patientAvatarUrl =
                    patientAvatarUrl,
                onClick =
                    onAvatarClick
            )


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )
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
}
@Composable
private fun PatientHeaderAvatar(
    patientName: String,
    patientAvatarUrl: String?,
    onClick: () -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "AvatarStatus"
        )

    val pulse by
    infiniteTransition.animateFloat(
        initialValue =
            0.55f,
        targetValue =
            1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis =
                            1200,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label =
            "AvatarPulse"
    )


    Box(
        modifier =
            Modifier.size(
                46.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        /*
         * Soft outer ring
         */
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(
                        CircleShape
                    )
                    .background(
                        PatientColors
                            .Primary
                            .copy(
                                alpha =
                                    0.10f
                            )
                    )
        )


        PatientAvatar(
            avatarUrl =
                patientAvatarUrl,
            name =
                patientName,
            size =
                38.dp,
            onClick =
                onClick
        )


        /*
         * Online indicator
         */
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
                    .size(
                        13.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surface
                    )
                    .padding(
                        2.dp
                    )
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(
                            pulse
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            PatientColors
                                .SuccessAccent
                        )
            )
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun ModernHomeTab(
    modifier: Modifier,
    patientName: String,
    appointments: List<Appointment>,
    upcomingAppointments: List<Appointment>,
    nextAppointment: Appointment?,
    reviews: List<ReviewDisplay>,
    isLoading: Boolean,
    loadError: String?,
    recommendation: DoctorRecommendation?,
    recommendationLoading: Boolean,
    healthProfileHasSymptoms: Boolean,
    recommendationLoadError: Boolean,
    onRetry: () -> Unit,
    onNavigateFindDoctors: () -> Unit,
    onNavigateConsultations: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigatePrescriptions: () -> Unit,
    onNavigateHealthTips: () -> Unit,
    onNavigateHealthProfile: () -> Unit,
    onNavigateInvoices: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onAppointmentClick: (Appointment) -> Unit,
    onStartCall: (String) -> Unit
) {
    val firstName = patientName.trim().substringBefore(" ").ifBlank { "there" }

    // Fixed, accessible two-column layout: no sideways swipe to find an invoice.
    val actions = listOf(
        QuickAction(
            label = "Find doctors",
            description = "Explore verified professionals",
            icon = Icons.Outlined.Search,
            accentColor = PatientColors.DoctorAccent,
            backgroundColor = PatientColors.DoctorCard,
            onClick = onNavigateFindDoctors
        ),
        QuickAction(
            label = "Appointments",
            description = "Bookings and visits",
            icon = Icons.Outlined.CalendarMonth,
            accentColor = PatientColors.AppointmentAccent,
            backgroundColor = PatientColors.AppointmentCard,
            onClick = onNavigateAppointments
        ),
        QuickAction(
            label = "Health profile",
            description = "Update your questionnaire",
            icon = Icons.Outlined.HealthAndSafety,
            accentColor = PatientColors.TipsAccent,
            backgroundColor = PatientColors.TipsCard,
            onClick = onNavigateHealthProfile
        ),
        QuickAction(
            label = "Prescriptions",
            description = "View and export PDFs",
            icon = Icons.Outlined.Medication,
            accentColor = PatientColors.SuccessAccent,
            backgroundColor = PatientColors.SuccessCard,
            onClick = onNavigatePrescriptions
        ),
        QuickAction(
            label = "My invoices",
            description = "Visit fees and PDF copies",
            icon = Icons.Outlined.Payments,
            accentColor = PatientColors.Primary,
            backgroundColor = PatientColors.AppointmentCard,
            onClick = onNavigateInvoices
        ),
        QuickAction(
            label = "Medical records",
            description = "Your healthcare history",
            icon = Icons.Outlined.Description,
            accentColor = PatientColors.RecordsAccent,
            backgroundColor = PatientColors.RecordsCard,
            onClick = onNavigateMedicalRecords
        ),
        QuickAction(
            label = "Notifications",
            description = "Latest care updates",
            icon = Icons.Outlined.NotificationsActive,
            accentColor = PatientColors.ReviewAccent,
            backgroundColor = PatientColors.ReviewCard,
            onClick = onNavigateNotifications
        ),
        QuickAction(
            label = "Health tips",
            description = "Daily wellness guidance",
            icon = Icons.Outlined.Lightbulb,
            accentColor = PatientColors.TipsAccent,
            backgroundColor = PatientColors.TipsCard,
            onClick = onNavigateHealthTips
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    text = "${greetingForNow()}, $firstName",
                    fontSize = 27.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Your health, made simpler.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        item { OnDemandCareHero(onClick = onNavigateConsultations) }

        item {
            DashboardStatsRow(
                upcomingCount = upcomingAppointments.size,
                completedCount = appointments.count {
                    it.status.equals("completed", ignoreCase = true)
                },
                reviewCount = reviews.size
            )
        }

        item {
            SectionHeader(
                title = "Smart Care",
                subtitle = "Guidance from your saved health profile"
            )
        }
        item {
            SmartCareRecommendationCard(
                recommendation = recommendation,
                loading = recommendationLoading,
                hasSymptoms = healthProfileHasSymptoms,
                hasError = recommendationLoadError,
                onFindDoctors = onNavigateFindDoctors,
                onHealthProfile = onNavigateHealthProfile
            )
        }

        item {
            SectionHeader(
                title = "Quick access",
                subtitle = "All your healthcare tools in one place"
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                actions.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { action ->
                            QuickActionCard(action = action, modifier = Modifier.weight(1f))
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Next appointment", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Your upcoming care",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = onNavigateAppointments) { Text("See all") }
            }
        }

        when {
            isLoading -> item { DashboardLoadingCard() }
            loadError != null -> item {
                DashboardErrorCard(message = loadError, onRetry = onRetry)
            }
            nextAppointment == null -> item {
                EmptyNextAppointmentCard(onFindDoctor = onNavigateFindDoctors)
            }
            else -> item {
                Box(Modifier.padding(horizontal = 18.dp)) {
                    NextAppointmentCard(
                        appointment = nextAppointment,
                        onClick = { onAppointmentClick(nextAppointment) },
                        onStartCall = { onStartCall(nextAppointment.id) }
                    )
                }
            }
        }

        if (upcomingAppointments.size > 1) {
            item {
                SectionHeader(
                    title = "Coming up",
                    subtitle = "Your next ${minOf(3, upcomingAppointments.size - 1)} visits"
                )
            }
            items(upcomingAppointments.drop(1).take(3), key = { it.id }) { appointment ->
                Box(Modifier.padding(horizontal = 18.dp)) {
                    CompactAppointmentCard(
                        appointment = appointment,
                        onClick = { onAppointmentClick(appointment) }
                    )
                }
            }
        }

        item { HealthTrustCard(onHealthTips = onNavigateHealthTips) }

        if (reviews.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Your feedback",
                    subtitle = "Recent doctor reviews"
                )
            }
            items(reviews.take(2), key = { it.review.id }) { review ->
                Box(Modifier.padding(horizontal = 18.dp)) { ReviewCard(review) }
            }
        }
    }
}

@Composable
private fun SmartCareRecommendationCard(
    recommendation: DoctorRecommendation?,
    loading: Boolean,
    hasSymptoms: Boolean,
    hasError: Boolean,
    onFindDoctors: () -> Unit,
    onHealthProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
        shape = RoundedCornerShape(23.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, PatientColors.Primary.copy(alpha = 0.19f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(19.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(43.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PatientColors.DoctorCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = PatientColors.DoctorAccent,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "SMART SPECIALTY ROUTING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PatientColors.DoctorAccent,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        "Find the right kind of doctor",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(Modifier.height(15.dp))

            val action = when {
                loading -> "Find doctors"
                recommendation != null -> "Explore doctors"
                !hasSymptoms && !hasError -> "Update health profile"
                else -> "Find doctors"
            }
            when {
                loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(11.dp))
                    Text(
                        "Checking your saved questionnaire...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                recommendation != null -> {
                    Text(
                        recommendation.specialty,
                        color = PatientColors.Primary,
                        fontSize = 23.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Suggested from your health questionnaire. Browse doctors with this specialty in Find Doctors.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                hasError -> {
                    Text(
                        "Recommendations are temporarily unavailable.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "You can still browse verified doctors.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text("Make it personal", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Add your current symptoms to your Health Profile to get a suggested specialty.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(15.dp))
            Button(
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
                onClick = if (action == "Update health profile") onHealthProfile else onFindDoctors,
                colors = ButtonDefaults.buttonColors(containerColor = PatientColors.Primary)
            ) {
                Text(action, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(9.dp))
            Text(
                "Educational specialty guidance only — not a diagnosis. If symptoms are urgent, seek immediate medical help.",
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OnDemandCareHero(
    onClick: () -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "ConsultationHeroAnimation"
        )

    val pulse by
    infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1100,
                        easing = FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "AvailabilityPulse"
    )

    val artworkMovement by
    infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 2500,
                        easing = FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "ArtworkMovement"
    )

    PatientAnimatedClickableContainer(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp
                )
    ) { interactionSource ->

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource =
                            interactionSource,
                        indication =
                            null,
                        onClick =
                            onClick
                    ),
            shape =
                RoundedCornerShape(
                    30.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.Transparent
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        0.dp
                )
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            PatientColors
                                .ConsultationGradient
                        )
                        .padding(
                            horizontal = 22.dp,
                            vertical = 22.dp
                        )
            ) {

                /*
                 * Decorative artwork on right side
                 */
                HeroMedicalArtwork(
                    movement =
                        artworkMovement,
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .size(
                                135.dp
                            )
                            .alpha(
                                0.85f
                            )
                )

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth(
                                0.73f
                            )
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        10.dp
                                    )
                                    .alpha(
                                        pulse
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        Color(
                                            0xFF86EFAC
                                        )
                                    )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )

                        Text(
                            text =
                                "ONLINE HEALTHCARE",
                            color =
                                Color.White
                                    .copy(
                                        alpha =
                                            0.92f
                                    ),
                            fontSize =
                                10.5.sp,
                            fontWeight =
                                FontWeight.Bold,
                            letterSpacing =
                                0.8.sp
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )

                    Text(
                        text =
                            "Healthcare,\non your terms.",
                        color =
                            Color.White,
                        fontSize =
                            25.sp,
                        fontWeight =
                            FontWeight.ExtraBold,
                        lineHeight =
                            29.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )

                    Text(
                        text =
                            "Find a healthcare professional and schedule a convenient online consultation.",
                        color =
                            Color.White
                                .copy(
                                    alpha =
                                        0.84f
                                ),
                        fontSize =
                            12.5.sp,
                        lineHeight =
                            18.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            15.dp
                                        )
                                    )
                                    .background(
                                        Color.White
                                    )
                                    .padding(
                                        horizontal =
                                            15.dp,
                                        vertical =
                                            11.dp
                                    )
                        ) {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.VideoCall,
                                    contentDescription =
                                        null,
                                    tint =
                                        PatientColors.Teal,
                                    modifier =
                                        Modifier.size(
                                            19.dp
                                        )
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            8.dp
                                        )
                                )

                                Text(
                                    text =
                                        "Consult a doctor",
                                    color =
                                        PatientColors
                                            .TextPrimary,
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontSize =
                                        12.5.sp
                                )
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.width(
                                    12.dp
                                )
                        )

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        39.dp
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        Color.White
                                            .copy(
                                                alpha =
                                                    0.16f
                                            )
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Filled.ArrowForward,
                                contentDescription =
                                    null,
                                tint =
                                    Color.White,
                                modifier =
                                    Modifier.size(
                                        19.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun HeroMedicalArtwork(
    movement: Float,
    modifier: Modifier = Modifier
) {

    Canvas(
        modifier =
            modifier
    ) {

        val centerX =
            size.width / 2f

        val centerY =
            size.height / 2f

        /*
         * Large soft circle
         */
        drawCircle(
            color =
                Color.White.copy(
                    alpha = 0.10f
                ),
            radius =
                size.minDimension *
                        0.43f,
            center =
                Offset(
                    x =
                        centerX,
                    y =
                        centerY +
                                (
                                        movement *
                                                5f
                                        )
                )
        )

        /*
         * Inner circle
         */
        drawCircle(
            color =
                Color.White.copy(
                    alpha = 0.10f
                ),
            radius =
                size.minDimension *
                        0.31f,
            center =
                Offset(
                    centerX,
                    centerY
                ),
            style =
                Stroke(
                    width =
                        3.dp.toPx()
                )
        )

        /*
         * Medical cross
         */
        val crossSize =
            size.minDimension *
                    0.16f

        drawLine(
            color =
                Color.White.copy(
                    alpha = 0.90f
                ),
            start =
                Offset(
                    centerX,
                    centerY -
                            crossSize
                ),
            end =
                Offset(
                    centerX,
                    centerY +
                            crossSize
                ),
            strokeWidth =
                7.dp.toPx(),
            cap =
                StrokeCap.Round
        )

        drawLine(
            color =
                Color.White.copy(
                    alpha = 0.90f
                ),
            start =
                Offset(
                    centerX -
                            crossSize,
                    centerY
                ),
            end =
                Offset(
                    centerX +
                            crossSize,
                    centerY
                ),
            strokeWidth =
                7.dp.toPx(),
            cap =
                StrokeCap.Round
        )

        /*
         * Heartbeat line
         */
        val startX =
            size.width *
                    0.12f

        val baseY =
            size.height *
                    0.78f

        val heartbeatColor =
            Color.White.copy(
                alpha = 0.76f
            )

        val stroke =
            2.5.dp.toPx()

        drawLine(
            heartbeatColor,
            Offset(
                startX,
                baseY
            ),
            Offset(
                size.width *
                        0.31f,
                baseY
            ),
            stroke,
            StrokeCap.Round
        )

        drawLine(
            heartbeatColor,
            Offset(
                size.width *
                        0.31f,
                baseY
            ),
            Offset(
                size.width *
                        0.40f,
                baseY -
                        13.dp.toPx()
            ),
            stroke,
            StrokeCap.Round
        )

        drawLine(
            heartbeatColor,
            Offset(
                size.width *
                        0.40f,
                baseY -
                        13.dp.toPx()
            ),
            Offset(
                size.width *
                        0.49f,
                baseY +
                        16.dp.toPx()
            ),
            stroke,
            StrokeCap.Round
        )

        drawLine(
            heartbeatColor,
            Offset(
                size.width *
                        0.49f,
                baseY +
                        16.dp.toPx()
            ),
            Offset(
                size.width *
                        0.58f,
                baseY -
                        7.dp.toPx()
            ),
            stroke,
            StrokeCap.Round
        )

        drawLine(
            heartbeatColor,
            Offset(
                size.width *
                        0.58f,
                baseY -
                        7.dp.toPx()
            ),
            Offset(
                size.width *
                        0.68f,
                baseY
            ),
            stroke,
            StrokeCap.Round
        )

        drawLine(
            heartbeatColor,
            Offset(
                size.width *
                        0.68f,
                baseY
            ),
            Offset(
                size.width *
                        0.88f,
                baseY
            ),
            stroke,
            StrokeCap.Round
        )
    }
}
@Composable
private fun DashboardStatsRow(
    upcomingCount: Int,
    completedCount: Int,
    reviewCount: Int
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp
                ),
        horizontalArrangement =
            Arrangement.spacedBy(
                10.dp
            )
    ) {

        MiniStat(
            modifier =
                Modifier.weight(1f),
            value =
                upcomingCount.toString(),
            label =
                "Upcoming",
            icon =
                Icons.Outlined.Schedule,
            tint =
                PatientColors.AppointmentAccent,
            background =
                PatientColors.AppointmentCard
        )

        MiniStat(
            modifier =
                Modifier.weight(1f),
            value =
                completedCount.toString(),
            label =
                "Completed",
            icon =
                Icons.Outlined.HealthAndSafety,
            tint =
                PatientColors.SuccessAccent,
            background =
                PatientColors.SuccessCard
        )

        MiniStat(
            modifier =
                Modifier.weight(1f),
            value =
                reviewCount.toString(),
            label =
                "Reviews",
            icon =
                Icons.Filled.Star,
            tint =
                PatientColors.ReviewAccent,
            background =
                PatientColors.ReviewCard
        )
    }
}

@Composable
private fun MiniStat(
    modifier: Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    background: Color
) {

    Card(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),
        border =
            BorderStroke(
                1.dp,
                tint.copy(
                    alpha = 0.10f
                )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    14.dp
                )
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            36.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                11.dp
                            )
                        )
                        .background(
                            tint.copy(
                                alpha = 0.12f
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
                        tint,
                    modifier =
                        Modifier.size(
                            19.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    value,
                fontSize =
                    21.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                color =
                    PatientColors.TextPrimary
            )

            Text(
                text =
                    label,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    10.5.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 18.dp)) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    modifier: Modifier = Modifier
) {

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .patientPressAnimation(
                    interactionSource
                )
                .clickable(
                    interactionSource =
                        interactionSource,
                    indication =
                        null,
                    onClick =
                        action.onClick
                ),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    action.backgroundColor
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    action.accentColor
                        .copy(
                            alpha = 0.12f
                        )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            46.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                15.dp
                            )
                        )
                        .background(
                            action.accentColor
                                .copy(
                                    alpha = 0.12f
                                )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        action.icon,
                    contentDescription =
                        null,
                    tint =
                        action.accentColor,
                    modifier =
                        Modifier.size(
                            23.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )

            Text(
                text =
                    action.label,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    14.sp,
                maxLines = 2,
                lineHeight = 17.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    action.description,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    11.5.sp,
                lineHeight =
                    16.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = "Open",
                    color =
                        action.accentColor,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            4.dp
                        )
                )

                Icon(
                    imageVector =
                        Icons.Filled.ArrowForward,
                    contentDescription =
                        null,
                    tint =
                        action.accentColor,
                    modifier =
                        Modifier.size(
                            14.dp
                        )
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun NextAppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit,
    onStartCall: () -> Unit
) {

    val isOnline =
        appointment.appointment_type
            .equals(
                "online",
                ignoreCase = true
            )

    val callState =
        appointment.callWindowState()

    val confirmed =
        appointment.status
            .equals(
                "confirmed",
                ignoreCase = true
            )

    val canCall =
        isOnline &&
                confirmed &&
                callState == CallWindowState.OPEN

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "CallButtonPulse"
        )

    val callPulse by
    infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 900,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label =
            "CallPulseScale"
    )


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .patientPressAnimation(
                    interactionSource
                )
                .clickable(
                    interactionSource =
                        interactionSource,
                    indication =
                        null,
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(
                26.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isOnline) {
                        PatientColors
                            .AppointmentCard
                    } else {
                        PatientColors
                            .DoctorCard
                    }
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (isOnline) {
                        PatientColors
                            .AppointmentAccent
                            .copy(
                                alpha = 0.12f
                            )
                    } else {
                        PatientColors
                            .DoctorAccent
                            .copy(
                                alpha = 0.12f
                            )
                    }
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    3.dp,
                pressedElevation =
                    6.dp
            )
    ) {

        Column {

            /*
             * Accent strip
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            5.dp
                        )
                        .background(
                            if (isOnline) {
                                Brush.horizontalGradient(
                                    listOf(
                                        PatientColors
                                            .AppointmentAccent,
                                        PatientColors
                                            .Primary
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        PatientColors
                                            .DoctorAccent,
                                        PatientColors
                                            .Green
                                    )
                                )
                            }
                        )
            )


            Column(
                modifier =
                    Modifier.padding(
                        18.dp
                    )
            ) {

                /*
                 * Header
                 */
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    52.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        17.dp
                                    )
                                )
                                .background(
                                    if (isOnline) {
                                        PatientColors
                                            .AppointmentAccent
                                            .copy(
                                                alpha =
                                                    0.12f
                                            )
                                    } else {
                                        PatientColors
                                            .DoctorAccent
                                            .copy(
                                                alpha =
                                                    0.12f
                                            )
                                    }
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                if (isOnline) {
                                    Icons.Outlined.Videocam
                                } else {
                                    Icons.Outlined.CalendarMonth
                                },
                            contentDescription =
                                null,
                            tint =
                                if (isOnline) {
                                    PatientColors
                                        .AppointmentAccent
                                } else {
                                    PatientColors
                                        .DoctorAccent
                                },
                            modifier =
                                Modifier.size(
                                    25.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(
                                13.dp
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
                                appointment.reason
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "General consultation",
                            color =
                                PatientColors
                                    .TextPrimary,
                            fontSize =
                                16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )

                        Text(
                            text =
                                if (isOnline) {
                                    "Online consultation"
                                } else {
                                    "In-person consultation"
                                },
                            color =
                                if (isOnline) {
                                    PatientColors
                                        .AppointmentAccent
                                } else {
                                    PatientColors
                                        .DoctorAccent
                                },
                            fontSize =
                                11.5.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }


                    AppointmentStatusChip(
                        appointment.status
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                /*
                 * Date and time boxes
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    AppointmentDetailBox(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        icon =
                            Icons.Outlined.CalendarMonth,
                        label =
                            "DATE",
                        value =
                            appointment.date
                                ?: "Pending",
                        accentColor =
                            PatientColors
                                .AppointmentAccent
                    )


                    AppointmentDetailBox(
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        icon =
                            Icons.Outlined.Schedule,
                        label =
                            "TIME",
                        value =
                            appointment.time
                                ?.take(
                                    5
                                )
                                ?: "Pending",
                        accentColor =
                            PatientColors
                                .DoctorAccent
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )


                /*
                 * Security status
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .background(
                                PatientColors
                                    .SuccessCard
                            )
                            .padding(
                                horizontal =
                                    12.dp,
                                vertical =
                                    11.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    32.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    PatientColors
                                        .SuccessAccent
                                        .copy(
                                            alpha =
                                                0.12f
                                        )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Shield,
                            contentDescription =
                                null,
                            tint =
                                PatientColors
                                    .SuccessAccent,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(
                                10.dp
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
                                "Secure appointment",
                            color =
                                PatientColors
                                    .SuccessAccent,
                            fontSize =
                                11.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                if (
                                    appointment.slot_id != null
                                ) {
                                    "Reserved from the doctor's published availability"
                                } else {
                                    "Appointment details verified"
                                },
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                10.5.sp,
                            lineHeight =
                                14.sp
                        )
                    }
                }


                /*
                 * Video consultation button
                 */
                AnimatedVisibility(
                    visible =
                        isOnline
                ) {

                    Column {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    16.dp
                                )
                        )


                        Button(
                            onClick =
                                onStartCall,
                            enabled =
                                canCall,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(
                                        50.dp
                                    )
                                    .scale(
                                        if (canCall) {
                                            callPulse
                                        } else {
                                            1f
                                        }
                                    ),
                            shape =
                                RoundedCornerShape(
                                    16.dp
                                ),
                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            PatientColors
                                                .DoctorAccent,

                                        contentColor =
                                            Color.White,

                                        disabledContainerColor =
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant,

                                        disabledContentColor =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.VideoCall,
                                contentDescription =
                                    null,
                                modifier =
                                    Modifier.size(
                                        20.dp
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        8.dp
                                    )
                            )


                            Text(
                                text =
                                    when {
                                        !confirmed ->
                                            "Waiting for doctor confirmation"

                                        callState == CallWindowState.TOO_EARLY ->
                                            "Opens 5 minutes before"

                                        callState == CallWindowState.OPEN ->
                                            "Join consultation"

                                        callState == CallWindowState.CLOSED ->
                                            "Consultation window closed"

                                        else ->
                                            "Video consultation"
                                    },
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize =
                                    13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentDetailBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color
) {

    Row(
        modifier =
            modifier
                .clip(
                    RoundedCornerShape(
                        15.dp
                    )
                )
                .background(
                    Color.White.copy(
                        alpha = 0.65f
                    )
                )
                .padding(
                    horizontal = 11.dp,
                    vertical = 11.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        34.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            10.dp
                        )
                    )
                    .background(
                        accentColor.copy(
                            alpha = 0.10f
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
                    accentColor,
                modifier =
                    Modifier.size(
                        17.dp
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.width(
                    9.dp
                )
        )


        Column {

            Text(
                text =
                    label,
                color =
                    PatientColors
                        .TextSecondary,
                fontSize =
                    8.5.sp,
                fontWeight =
                    FontWeight.Bold,
                letterSpacing =
                    0.5.sp
            )

            Text(
                text =
                    value,
                color =
                    PatientColors
                        .TextPrimary,
                fontSize =
                    11.5.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
@Composable
private fun AppointmentStatusChip(status: String?) {
    val normalized = status?.lowercase().orEmpty()
    val (background, foreground, label) = when (normalized) {
        "confirmed" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), "Confirmed")
        "completed" -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), "Completed")
        "cancelled" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), "Cancelled")
        "pending" -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), "Pending")
        else -> Triple(Color(0xFFE2E8F0), Color(0xFF475569), status ?: "Unknown")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = foreground, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CompactAppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {

    val isOnline =
        appointment.appointment_type
            .equals(
                "online",
                ignoreCase = true
            )

    val accentColor =
        if (isOnline) {
            PatientColors.AppointmentAccent
        } else {
            PatientColors.DoctorAccent
        }

    val backgroundColor =
        if (isOnline) {
            PatientColors.AppointmentCard
        } else {
            PatientColors.DoctorCard
        }

    val interactionSource =
        remember {
            MutableInteractionSource()
        }


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .patientPressAnimation(
                    interactionSource
                )
                .clickable(
                    interactionSource =
                        interactionSource,
                    indication = null,
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    backgroundColor
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    accentColor.copy(
                        alpha = 0.10f
                    )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp,
                pressedElevation = 4.dp
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

            /*
             * Consultation icon
             */
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(
                            RoundedCornerShape(
                                15.dp
                            )
                        )
                        .background(
                            accentColor.copy(
                                alpha = 0.12f
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        if (isOnline) {
                            Icons.Outlined.Videocam
                        } else {
                            Icons.Outlined.CalendarMonth
                        },
                    contentDescription =
                        null,
                    tint =
                        accentColor,
                    modifier =
                        Modifier.size(23.dp)
                )
            }


            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )


            /*
             * Appointment information
             */
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        appointment.reason
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: "General consultation",
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1
                )


                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.CalendarMonth,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.TextSecondary,
                        modifier =
                            Modifier.size(14.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )


                    Text(
                        text =
                            appointment.date
                                ?: "Date pending",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Box(
                        modifier =
                            Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(
                                    PatientColors.TextSecondary
                                        .copy(
                                            alpha = 0.45f
                                        )
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Icon(
                        imageVector =
                            Icons.Outlined.Schedule,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.TextSecondary,
                        modifier =
                            Modifier.size(14.dp)
                    )


                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )


                    Text(
                        text =
                            appointment.time
                                ?.take(5)
                                ?: "Pending",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                /*
                 * Consultation type
                 */
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    accentColor.copy(
                                        alpha = 0.11f
                                    )
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                    ) {

                        Text(
                            text =
                                if (isOnline) {
                                    "Online"
                                } else {
                                    "In person"
                                },
                            color =
                                accentColor,
                            fontSize =
                                9.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.width(10.dp)
            )


            /*
             * Right side
             */
            Column(
                horizontalAlignment =
                    Alignment.End
            ) {

                AppointmentStatusChip(
                    appointment.status
                )


                Spacer(
                    modifier =
                        Modifier.height(11.dp)
                )


                Box(
                    modifier =
                        Modifier
                            .size(31.dp)
                            .clip(CircleShape)
                            .background(
                                accentColor.copy(
                                    alpha = 0.10f
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.ArrowForward,
                        contentDescription =
                            "Open appointment",
                        tint =
                            accentColor,
                        modifier =
                            Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardLoadingCard() {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "DashboardShimmer"
        )

    val shimmerPosition by
    infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 1100f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1300,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Restart
            ),
        label =
            "ShimmerPosition"
    )

    val baseColor =
        MaterialTheme
            .colorScheme
            .surfaceVariant
            .copy(alpha = 0.45f)

    val highlightColor =
        MaterialTheme
            .colorScheme
            .surface
            .copy(alpha = 0.95f)

    val shimmerBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    baseColor,
                    highlightColor,
                    baseColor
                ),
            start =
                Offset(
                    shimmerPosition - 220f,
                    0f
                ),
            end =
                Offset(
                    shimmerPosition,
                    0f
                )
        )


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 18.dp
                ),
        shape =
            RoundedCornerShape(
                26.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            /*
             * Top section
             */
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                ShimmerBox(
                    modifier =
                        Modifier.size(
                            52.dp
                        ),
                    brush =
                        shimmerBrush,
                    shape =
                        RoundedCornerShape(
                            17.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            13.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    ShimmerBox(
                        modifier =
                            Modifier
                                .fillMaxWidth(
                                    0.70f
                                )
                                .height(
                                    16.dp
                                ),
                        brush =
                            shimmerBrush
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    ShimmerBox(
                        modifier =
                            Modifier
                                .fillMaxWidth(
                                    0.42f
                                )
                                .height(
                                    11.dp
                                ),
                        brush =
                            shimmerBrush
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            12.dp
                        )
                )


                ShimmerBox(
                    modifier =
                        Modifier
                            .width(
                                65.dp
                            )
                            .height(
                                26.dp
                            ),
                    brush =
                        shimmerBrush,
                    shape =
                        RoundedCornerShape(
                            20.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        20.dp
                    )
            )


            /*
             * Date and time placeholders
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                ShimmerBox(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .height(
                                58.dp
                            ),
                    brush =
                        shimmerBrush,
                    shape =
                        RoundedCornerShape(
                            15.dp
                        )
                )


                ShimmerBox(
                    modifier =
                        Modifier
                            .weight(
                                1f
                            )
                            .height(
                                58.dp
                            ),
                    brush =
                        shimmerBrush,
                    shape =
                        RoundedCornerShape(
                            15.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            /*
             * Bottom information placeholder
             */
            ShimmerBox(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),
                brush =
                    shimmerBrush,
                shape =
                    RoundedCornerShape(
                        15.dp
                    )
            )
        }
    }
}
@Composable
private fun ShimmerBox(
    modifier: Modifier,
    brush: Brush,
    shape: RoundedCornerShape =
        RoundedCornerShape(
            8.dp
        )
) {

    Box(
        modifier =
            modifier
                .clip(
                    shape
                )
                .background(
                    brush
                )
    )
}

@Composable
private fun DashboardErrorCard(
    message: String,
    onRetry: () -> Unit
) {

    val friendlyMessage =
        when {
            message.contains(
                "network",
                ignoreCase = true
            ) ||
                    message.contains(
                        "internet",
                        ignoreCase = true
                    ) ->
                "We couldn't connect. Check your internet connection and try again."

            message.contains(
                "timeout",
                ignoreCase = true
            ) ->
                "The connection is taking longer than expected. Please try again."

            else ->
                "We couldn't load your healthcare information right now. Your data is safe — please try again."
        }


    val infiniteTransition =
        rememberInfiniteTransition(
            label = "ErrorAnimation"
        )

    val pulse by
    infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1400,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "ErrorPulse"
    )


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        shape =
            RoundedCornerShape(26.dp),
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
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            DashboardErrorIllustration(
                pulse = pulse
            )


            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )


            Text(
                text =
                    "Something went wrong",
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    17.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )


            Text(
                text =
                    friendlyMessage,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    12.5.sp,
                lineHeight =
                    18.sp,
                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )


            Button(
                onClick =
                    onRetry,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(49.dp),
                shape =
                    RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            PatientColors.Red,
                        contentColor =
                            Color.White
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Refresh,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(19.dp)
                )


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                Text(
                    text = "Try again",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun DashboardErrorIllustration(
    pulse: Float
) {

    Box(
        modifier =
            Modifier
                .size(105.dp)
                .scale(pulse),
        contentAlignment =
            Alignment.Center
    ) {

        /*
         * Soft outer background
         */
        Box(
            modifier =
                Modifier
                    .size(94.dp)
                    .clip(CircleShape)
                    .background(
                        PatientColors.Red
                            .copy(alpha = 0.08f)
                    )
        )


        Canvas(
            modifier =
                Modifier.size(76.dp)
        ) {

            val center =
                Offset(
                    x = size.width / 2f,
                    y = size.height / 2f
                )


            /*
             * Outer warning circle
             */
            drawCircle(
                color =
                    PatientColors.Red,
                radius =
                    size.minDimension * 0.36f,
                center =
                    center,
                style =
                    Stroke(
                        width =
                            4.dp.toPx()
                    )
            )


            /*
             * Exclamation line
             */
            drawLine(
                color =
                    PatientColors.Red,
                start =
                    Offset(
                        x = center.x,
                        y =
                            center.y -
                                    15.dp.toPx()
                    ),
                end =
                    Offset(
                        x = center.x,
                        y =
                            center.y +
                                    5.dp.toPx()
                    ),
                strokeWidth =
                    5.dp.toPx(),
                cap =
                    StrokeCap.Round
            )


            /*
             * Exclamation dot
             */
            drawCircle(
                color =
                    PatientColors.Red,
                radius =
                    3.dp.toPx(),
                center =
                    Offset(
                        x = center.x,
                        y =
                            center.y +
                                    16.dp.toPx()
                    )
            )
        }
    }
}
@Composable
private fun EmptyNextAppointmentCard(
    onFindDoctor: () -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "EmptyAppointmentAnimation"
        )

    val pulse by
    infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1400,
                        easing = FastOutSlowInEasing
                    ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "EmptyAppointmentPulse"
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
        shape =
            RoundedCornerShape(26.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.DoctorCard
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.DoctorAccent
                        .copy(alpha = 0.10f)
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 22.dp,
                        vertical = 24.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            EmptyAppointmentIllustration(
                pulse = pulse
            )

            Spacer(
                modifier =
                    Modifier.height(18.dp)
            )

            Text(
                text = "No upcoming appointments",
                color =
                    PatientColors.TextPrimary,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    "When you book a consultation, your next appointment will appear here.",
                color =
                    PatientColors.TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(20.dp)
            )

            Button(
                onClick =
                    onFindDoctor,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .scale(pulse),
                shape =
                    RoundedCornerShape(16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            PatientColors.DoctorAccent,
                        contentColor =
                            Color.White
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Search,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(19.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )

                Text(
                    text = "Find a doctor",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
@Composable
private fun EmptyAppointmentIllustration(
    pulse: Float
) {

    Box(
        modifier =
            Modifier
                .size(130.dp)
                .scale(pulse),
        contentAlignment =
            Alignment.Center
    ) {

        /*
         * Soft background circle
         */
        Box(
            modifier =
                Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(
                        PatientColors.DoctorAccent
                            .copy(alpha = 0.08f)
                    )
        )

        Canvas(
            modifier =
                Modifier.size(100.dp)
        ) {

            val centerX =
                size.width / 2f

            val centerY =
                size.height / 2f

            /*
             * Calendar body
             */
            drawRoundRect(
                color =
                    PatientColors.DoctorAccent,
                topLeft =
                    Offset(
                        x = size.width * 0.18f,
                        y = size.height * 0.24f
                    ),
                size =
                    androidx.compose.ui.geometry.Size(
                        width = size.width * 0.64f,
                        height = size.height * 0.55f
                    ),
                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        x = 12.dp.toPx(),
                        y = 12.dp.toPx()
                    ),
                style =
                    Stroke(
                        width = 3.dp.toPx()
                    )
            )

            /*
             * Calendar top divider
             */
            drawLine(
                color =
                    PatientColors.DoctorAccent,
                start =
                    Offset(
                        x = size.width * 0.18f,
                        y = size.height * 0.39f
                    ),
                end =
                    Offset(
                        x = size.width * 0.82f,
                        y = size.height * 0.39f
                    ),
                strokeWidth =
                    3.dp.toPx(),
                cap =
                    StrokeCap.Round
            )

            /*
             * Calendar hooks
             */
            drawLine(
                color =
                    PatientColors.DoctorAccent,
                start =
                    Offset(
                        x = size.width * 0.34f,
                        y = size.height * 0.17f
                    ),
                end =
                    Offset(
                        x = size.width * 0.34f,
                        y = size.height * 0.30f
                    ),
                strokeWidth =
                    4.dp.toPx(),
                cap =
                    StrokeCap.Round
            )

            drawLine(
                color =
                    PatientColors.DoctorAccent,
                start =
                    Offset(
                        x = size.width * 0.66f,
                        y = size.height * 0.17f
                    ),
                end =
                    Offset(
                        x = size.width * 0.66f,
                        y = size.height * 0.30f
                    ),
                strokeWidth =
                    4.dp.toPx(),
                cap =
                    StrokeCap.Round
            )

            /*
             * Medical cross
             */
            drawLine(
                color =
                    PatientColors.AppointmentAccent,
                start =
                    Offset(
                        x = centerX,
                        y = centerY
                    ),
                end =
                    Offset(
                        x = centerX,
                        y = centerY +
                                18.dp.toPx()
                    ),
                strokeWidth =
                    5.dp.toPx(),
                cap =
                    StrokeCap.Round
            )

            drawLine(
                color =
                    PatientColors.AppointmentAccent,
                start =
                    Offset(
                        x = centerX -
                                9.dp.toPx(),
                        y = centerY +
                                9.dp.toPx()
                    ),
                end =
                    Offset(
                        x = centerX +
                                9.dp.toPx(),
                        y = centerY +
                                9.dp.toPx()
                    ),
                strokeWidth =
                    5.dp.toPx(),
                cap =
                    StrokeCap.Round
            )
        }
    }
}

@Composable
private fun HealthTrustCard(
    onHealthTips: () -> Unit
) {

    val healthTips =
        remember {
            listOf(
                "Drink enough water throughout the day.",
                "A short daily walk can support your physical and mental wellbeing.",
                "Try to maintain a consistent sleep schedule.",
                "Regular health check-ups can help identify problems early."
            )
        }

    var currentTipIndex by
    remember {
        mutableStateOf(0)
    }

    LaunchedEffect(Unit) {

        while (true) {

            delay(5000)

            currentTipIndex =
                (currentTipIndex + 1) %
                        healthTips.size
        }
    }


    val infiniteTransition =
        rememberInfiniteTransition(
            label = "HealthTipAnimation"
        )

    val heartbeatProgress by
    infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 850,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label =
            "HealthHeartbeat"
    )


    val interactionSource =
        remember {
            MutableInteractionSource()
        }


    Card(
        modifier =
            Modifier
                .padding(
                    horizontal = 18.dp
                )
                .fillMaxWidth()
                .patientPressAnimation(
                    interactionSource
                )
                .clickable(
                    interactionSource =
                        interactionSource,
                    indication = null,
                    onClick =
                        onHealthTips
                ),
        shape =
            RoundedCornerShape(
                26.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.TipsCard
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.TipsAccent
                        .copy(
                            alpha = 0.13f
                        )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 5.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        19.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * Animated health illustration
             */
            WellnessIllustration(
                progress =
                    heartbeatProgress
            )


            Spacer(
                modifier =
                    Modifier.width(
                        16.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    PatientColors
                                        .TipsAccent
                                        .copy(
                                            alpha =
                                                0.12f
                                        )
                                )
                                .padding(
                                    horizontal =
                                        9.dp,
                                    vertical =
                                        5.dp
                                )
                    ) {

                        Text(
                            text =
                                "HEALTH TIP",
                            color =
                                PatientColors
                                    .TipsAccent,
                            fontSize =
                                9.sp,
                            fontWeight =
                                FontWeight.Bold,
                            letterSpacing =
                                0.6.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            9.dp
                        )
                )


                Text(
                    text =
                        "Stay healthy every day",
                    color =
                        PatientColors
                            .TextPrimary,
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                AnimatedContent(
                    targetState =
                        healthTips[
                            currentTipIndex
                        ],
                    transitionSpec = {
                        fadeIn(
                            animationSpec =
                                tween(400)
                        ) togetherWith
                                fadeOut(
                                    animationSpec =
                                        tween(250)
                                )
                    },
                    label =
                        "HealthTipChange"
                ) { tip ->

                    Text(
                        text =
                            tip,
                        color =
                            PatientColors
                                .TextSecondary,
                        fontSize =
                            11.5.sp,
                        lineHeight =
                            16.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            11.dp
                        )
                )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Explore health tips",
                        color =
                            PatientColors
                                .TipsAccent,
                        fontSize =
                            11.5.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )


                    Icon(
                        imageVector =
                            Icons.Filled.ArrowForward,
                        contentDescription =
                            null,
                        tint =
                            PatientColors
                                .TipsAccent,
                        modifier =
                            Modifier.size(
                                15.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun WellnessIllustration(
    progress: Float
) {

    Box(
        modifier =
            Modifier
                .size(
                    82.dp
                )
                .scale(
                    progress
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(
                            24.dp
                        )
                    )
                    .background(
                        PatientColors
                            .TipsAccent
                            .copy(
                                alpha =
                                    0.09f
                            )
                    )
        )


        Canvas(
            modifier =
                Modifier.size(
                    62.dp
                )
        ) {

            val centerY =
                size.height / 2f


            /*
             * Heartbeat background line
             */
            drawLine(
                color =
                    PatientColors
                        .TipsAccent
                        .copy(
                            alpha = 0.22f
                        ),
                start =
                    Offset(
                        0f,
                        centerY
                    ),
                end =
                    Offset(
                        size.width,
                        centerY
                    ),
                strokeWidth =
                    2.dp.toPx(),
                cap =
                    StrokeCap.Round
            )


            /*
             * ECG line
             */
            val color =
                PatientColors
                    .TipsAccent

            val width =
                3.dp.toPx()


            drawLine(
                color,
                Offset(
                    size.width * 0.05f,
                    centerY
                ),
                Offset(
                    size.width * 0.25f,
                    centerY
                ),
                width,
                StrokeCap.Round
            )


            drawLine(
                color,
                Offset(
                    size.width * 0.25f,
                    centerY
                ),
                Offset(
                    size.width * 0.35f,
                    centerY -
                            8.dp.toPx()
                ),
                width,
                StrokeCap.Round
            )


            drawLine(
                color,
                Offset(
                    size.width * 0.35f,
                    centerY -
                            8.dp.toPx()
                ),
                Offset(
                    size.width * 0.46f,
                    centerY +
                            15.dp.toPx()
                ),
                width,
                StrokeCap.Round
            )


            drawLine(
                color,
                Offset(
                    size.width * 0.46f,
                    centerY +
                            15.dp.toPx()
                ),
                Offset(
                    size.width * 0.57f,
                    centerY -
                            16.dp.toPx()
                ),
                width,
                StrokeCap.Round
            )


            drawLine(
                color,
                Offset(
                    size.width * 0.57f,
                    centerY -
                            16.dp.toPx()
                ),
                Offset(
                    size.width * 0.68f,
                    centerY
                ),
                width,
                StrokeCap.Round
            )


            drawLine(
                color,
                Offset(
                    size.width * 0.68f,
                    centerY
                ),
                Offset(
                    size.width * 0.95f,
                    centerY
                ),
                width,
                StrokeCap.Round
            )
        }


        /*
         * Small medical badge
         */
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
                    .size(
                        25.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        PatientColors
                            .DoctorAccent
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.HealthAndSafety,
                contentDescription =
                    null,
                tint =
                    Color.White,
                modifier =
                    Modifier.size(
                        14.dp
                    )
            )
        }
    }
}

@Composable
private fun ReviewCard(
    reviewDisplay: ReviewDisplay
) {

    val review =
        reviewDisplay.review

    val rating =
        review.rating.coerceIn(
            0,
            5
        )

    val doctorName =
        reviewDisplay.doctorName
            .ifBlank {
                "Healthcare professional"
            }

    val initials =
        doctorName
            .trim()
            .split(" ")
            .filter {
                it.isNotBlank()
            }
            .take(2)
            .joinToString("") {
                it.first()
                    .uppercase()
            }
            .ifBlank {
                "DR"
            }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                24.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.ReviewCard
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.ReviewAccent
                        .copy(
                            alpha = 0.13f
                        )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column {

            /*
             * Gold top accent
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            4.dp
                        )
                        .background(
                            Brush.horizontalGradient(
                                colors =
                                    listOf(
                                        PatientColors
                                            .ReviewAccent,
                                        PatientColors
                                            .TipsAccent
                                    )
                            )
                        )
            )


            Column(
                modifier =
                    Modifier.padding(
                        17.dp
                    )
            ) {

                /*
                 * Doctor information
                 */
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    46.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    PatientColors
                                        .ReviewAccent
                                        .copy(
                                            alpha = 0.13f
                                        )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                initials,
                            color =
                                PatientColors
                                    .ReviewAccent,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                14.sp
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
                                doctorName,
                            color =
                                PatientColors
                                    .TextPrimary,
                            fontSize =
                                14.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )


                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.HealthAndSafety,
                                contentDescription =
                                    null,
                                tint =
                                    PatientColors
                                        .DoctorAccent,
                                modifier =
                                    Modifier.size(
                                        14.dp
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        4.dp
                                    )
                            )


                            Text(
                                text =
                                    "Your consultation",
                                color =
                                    PatientColors
                                        .TextSecondary,
                                fontSize =
                                    10.5.sp
                            )
                        }
                    }


                    /*
                     * Rating badge
                     */
                    Row(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    PatientColors
                                        .ReviewAccent
                                        .copy(
                                            alpha = 0.12f
                                        )
                                )
                                .padding(
                                    horizontal =
                                        10.dp,
                                    vertical =
                                        7.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.Star,
                            contentDescription =
                                null,
                            tint =
                                PatientColors
                                    .ReviewAccent,
                            modifier =
                                Modifier.size(
                                    15.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    4.dp
                                )
                        )


                        Text(
                            text =
                                "$rating.0",
                            color =
                                PatientColors
                                    .ReviewAccent,
                            fontSize =
                                11.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            15.dp
                        )
                )


                /*
                 * Stars
                 */
                Row {

                    repeat(5) { index ->

                        Icon(
                            imageVector =
                                Icons.Filled.Star,
                            contentDescription =
                                null,
                            tint =
                                if (
                                    index < rating
                                ) {
                                    PatientColors
                                        .ReviewAccent
                                } else {
                                    PatientColors
                                        .ReviewAccent
                                        .copy(
                                            alpha =
                                                0.18f
                                        )
                                },
                            modifier =
                                Modifier.size(
                                    18.dp
                                )
                        )


                        if (
                            index < 4
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.width(
                                        2.dp
                                    )
                            )
                        }
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            13.dp
                        )
                )


                /*
                 * Comment area
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .background(
                                Color.White
                                    .copy(
                                        alpha =
                                            0.58f
                                    )
                            )
                            .padding(
                                13.dp
                            )
                ) {

                    Column {

                        Text(
                            text = "“",
                            color =
                                PatientColors
                                    .ReviewAccent,
                            fontSize =
                                29.sp,
                            fontWeight =
                                FontWeight.ExtraBold,
                            lineHeight =
                                20.sp
                        )


                        Text(
                            text =
                                review.comment
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "No written comment was added.",
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                12.sp,
                            lineHeight =
                                17.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                /*
                 * Review date
                 */
                Text(
                    text =
                        review.created_at
                            .takeIf {
                                it.length >= 10
                            }
                            ?.take(10)
                            ?.let {
                                "Reviewed on $it"
                            }
                            ?: "Previous review",
                    color =
                        PatientColors
                            .TextSecondary
                            .copy(
                                alpha = 0.75f
                            ),
                    fontSize =
                        10.sp
                )
            }
        }
    }
}

@Composable
private fun ModernProfileTab(
    modifier: Modifier,
    patientName: String,
    patientAvatarUrl: String?,
    isUploadingPhoto: Boolean,
    onUploadPhoto: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PatientAvatar(
                        avatarUrl = patientAvatarUrl,
                        name = patientName,
                        size = 84.dp,
                        onClick = onUploadPhoto
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(patientName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Patient account",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onUploadPhoto,
                        enabled = !isUploadingPhoto,
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(17.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Change profile photo")
                        }
                    }
                }
            }
        }

        item {
            ProfileActionCard(
                icon = Icons.Outlined.CalendarMonth,
                title = "My appointments",
                subtitle = "Upcoming, completed and cancelled visits",
                onClick = onNavigateAppointments
            )
        }
        item {
            ProfileActionCard(
                icon = Icons.Outlined.Description,
                title = "Medical records",
                subtitle = "View your care history",
                onClick = onNavigateMedicalRecords
            )
        }
        item {
            ProfileActionCard(
                icon = Icons.Outlined.Settings,
                title = "Settings & privacy",
                subtitle = "Theme, account and privacy controls",
                onClick = onNavigateSettings
            )
        }
        item {
            ProfileActionCard(
                icon = Icons.Outlined.ExitToApp,
                title = "Sign out",
                subtitle = "Sign out of this device",
                destructive = true,
                onClick = onLogout
            )
        }
    }
}

@Composable
private fun ProfileActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(tint.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
            Icon(
                Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun AppointmentReminderPopup(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onViewAppointment: () -> Unit
) {
    val minutesLeft = remember(appointment) {
        appointment.dateTimeOrNull()?.let {
            Duration.between(LocalDateTime.now(), it).toMinutes().coerceAtLeast(0)
        } ?: 0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF7ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    if (minutesLeft <= 0) "Your appointment is starting" else "Appointment in $minutesLeft min",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Scheduled for ${appointment.time?.take(5) ?: "now"}. Please be ready.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onViewAppointment,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text("View appointment")
                }
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun AppointmentDetailsDialog(
    appointment: Appointment,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appointment details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                DetailRow("Date", appointment.date ?: "-")
                DetailRow("Time", appointment.time?.take(5) ?: "-")
                DetailRow("Reason", appointment.reason ?: "General consultation")
                DetailRow("Status", appointment.status?.replaceFirstChar { it.uppercase() } ?: "Unknown")
                DetailRow(
                    "Visit type",
                    if (appointment.appointment_type == "online") "Online" else "In person"
                )
                appointment.price_minor?.let {
                    DetailRow("Reserved fee", "R %.2f".format(it / 100.0))
                }
                appointment.amount_paid?.let {
                    DetailRow("Amount paid", "R %.2f".format(it))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(0.38f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            value,
            modifier = Modifier.weight(0.62f),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

// Cancellation status is not proof that a payment refund was processed.
fun cancellationExplanation(appointment: Appointment): String? {
    if (appointment.status != "cancelled") return null
    return when (appointment.cancelled_reason) {
        "doctor_no_show" -> "Cancelled because the doctor did not join in time."
        "patient_no_show" -> "Cancelled because the patient did not join in time."
        else -> null
    }
}
