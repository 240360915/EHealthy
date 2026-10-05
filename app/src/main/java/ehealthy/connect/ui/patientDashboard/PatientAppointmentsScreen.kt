package ehealthy.connect.ui.patientDashboard

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppointmentCategory(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

@RequiresApi(Build.VERSION_CODES.O)
fun categorize(appt: Appointment): AppointmentCategory {
    if (appt.status == "cancelled") return AppointmentCategory.CANCELLED
    val date = appt.date
    return if (appt.status == "confirmed" && date != null && date < LocalDate.now().toString()) {
        AppointmentCategory.COMPLETED
    } else {
        AppointmentCategory.UPCOMING
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAppointmentsScreen(
    onBack: () -> Unit,
    onFindDoctors: () -> Unit,
    onViewDoctorProfile: (String) -> Unit,
    initialCategory: AppointmentCategory = AppointmentCategory.ALL,

    fetchDoctor: suspend (String) -> Result<DoctorProfile>,

    fetchAppointments: suspend () -> Result<List<Appointment>>,

    fetchReviewedAppointmentIds: suspend () -> Result<Set<String>>,

    isSubmittingReview: Boolean,

    onSubmitReview: (
        String,
        String?,
        Int,
        String
    ) -> Unit,

    onRescheduleAppointment: (String) -> Unit,
    cancelAppointment: suspend (String) -> Result<Unit>,
    onRequestCompletionCode: suspend (String) -> Result<String> = { Result.failure(Exception("Not available")) }
) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var reviewedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var reviewTargetAppointmentId by remember { mutableStateOf<String?>(null) }
    var detailsAppointment by remember { mutableStateOf<Appointment?>(null) }
    var selectedAppointment by remember { mutableStateOf<Appointment?>(null) }
    var isCancelling by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val navy = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val teal = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background

    LaunchedEffect(Unit) {
        val apptResult = fetchAppointments()
        val reviewResult = fetchReviewedAppointmentIds()
        isLoading = false
        apptResult
            .onSuccess { appointments = it.sortedByDescending { a -> a.date ?: "" } }
            .onFailure { loadError = it.message ?: "Failed to load appointments." }
        reviewResult.onSuccess { reviewedIds = it }
    }

    val filtered = remember(appointments, selectedCategory) {
        if (selectedCategory == AppointmentCategory.ALL) appointments
        else appointments.filter { categorize(it) == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {

                    Column {

                        Text(
                            text = "My Appointments",
                            color = PatientColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Text(
                            text =
                                if (appointments.isEmpty()) {
                                    "Manage your visits"
                                } else {
                                    "${appointments.size} appointment${
                                        if (appointments.size == 1) "" else "s"
                                    }"
                                },
                            color = PatientColors.TextSecondary,
                            fontSize = 10.5.sp
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
                                        PatientColors.AppointmentCard,
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
                                    PatientColors.AppointmentAccent,
                                modifier =
                                    Modifier.size(20.dp)
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
            )
        },
        containerColor = bg
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 6.dp,
                            bottom = 6.dp
                        )
            ) {

                Text(
                    text = "Your visits",
                    modifier =
                        Modifier.padding(
                            horizontal = 20.dp
                        ),
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        13.5.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            )
                            .padding(
                                horizontal = 20.dp
                            ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    AppointmentCategory.entries
                        .forEach { cat ->

                            val count =
                                if (
                                    cat ==
                                    AppointmentCategory.ALL
                                ) {
                                    appointments.size
                                } else {
                                    appointments.count {
                                        categorize(it) == cat
                                    }
                                }


                            val accent =
                                when (cat) {

                                    AppointmentCategory.ALL ->
                                        PatientColors.AppointmentAccent

                                    AppointmentCategory.UPCOMING ->
                                        PatientColors.DoctorAccent

                                    AppointmentCategory.COMPLETED ->
                                        PatientColors.SuccessAccent

                                    AppointmentCategory.CANCELLED ->
                                        PatientColors.Red
                                }


                            val background =
                                when (cat) {

                                    AppointmentCategory.ALL ->
                                        PatientColors.AppointmentCard

                                    AppointmentCategory.UPCOMING ->
                                        PatientColors.DoctorCard

                                    AppointmentCategory.COMPLETED ->
                                        PatientColors.SuccessCard

                                    AppointmentCategory.CANCELLED ->
                                        PatientColors.RedSoft
                                }


                            FilterChip(
                                selected =
                                    selectedCategory == cat,

                                onClick = {
                                    selectedCategory = cat
                                },

                                label = {

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically
                                    ) {

                                        Text(
                                            text =
                                                cat.label,
                                            fontSize =
                                                11.5.sp,
                                            fontWeight =
                                                if (
                                                    selectedCategory == cat
                                                ) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Medium
                                                }
                                        )


                                        if (
                                            count > 0
                                        ) {

                                            Spacer(
                                                modifier =
                                                    Modifier.width(
                                                        6.dp
                                                    )
                                            )


                                            Box(
                                                modifier =
                                                    Modifier
                                                        .background(
                                                            if (
                                                                selectedCategory == cat
                                                            ) {
                                                                Color.White.copy(
                                                                    alpha = 0.18f
                                                                )
                                                            } else {
                                                                accent.copy(
                                                                    alpha = 0.10f
                                                                )
                                                            },
                                                            CircleShape
                                                        )
                                                        .padding(
                                                            horizontal = 6.dp,
                                                            vertical = 2.dp
                                                        )
                                            ) {

                                                Text(
                                                    text =
                                                        count.toString(),
                                                    color =
                                                        if (
                                                            selectedCategory == cat
                                                        ) {
                                                            Color.White
                                                        } else {
                                                            accent
                                                        },
                                                    fontSize =
                                                        9.sp,
                                                    fontWeight =
                                                        FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                },

                                shape =
                                    RoundedCornerShape(
                                        22.dp
                                    ),

                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor =
                                            accent,
                                        selectedLabelColor =
                                            Color.White,
                                        containerColor =
                                            background,
                                        labelColor =
                                            accent
                                    ),

                                border =
                                    null
                            )
                        }
                }
            }

            when{
                isLoading -> {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    24.dp
                                ),
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
                                        PatientColors.AppointmentCard
                                ),
                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 0.dp
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 34.dp,
                                        vertical = 28.dp
                                    ),
                                horizontalAlignment =
                                    Alignment.CenterHorizontally
                            ) {

                                Box(
                                    modifier =
                                        Modifier
                                            .size(
                                                64.dp
                                            )
                                            .background(
                                                PatientColors.AppointmentAccent
                                                    .copy(alpha = 0.09f),
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
                                        strokeWidth =
                                            2.5.dp,
                                        color =
                                            PatientColors.AppointmentAccent
                                    )
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            14.dp
                                        )
                                )


                                Text(
                                    text =
                                        "Loading your appointments",
                                    color =
                                        PatientColors.TextPrimary,
                                    fontWeight =
                                        FontWeight.Bold,
                                    fontSize =
                                        14.sp
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            4.dp
                                        )
                                )


                                Text(
                                    text =
                                        "Getting your latest visits...",
                                    color =
                                        PatientColors.TextSecondary,
                                    fontSize =
                                        10.5.sp
                                )
                            }
                        }
                    }
                }


                loadError != null -> {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    24.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Card(
                            modifier =
                                Modifier.fillMaxWidth(),
                            shape =
                                RoundedCornerShape(
                                    23.dp
                                ),
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
                                    defaultElevation = 0.dp
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            22.dp
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
                                                PatientColors.Red
                                                    .copy(alpha = 0.09f),
                                                CircleShape
                                            ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.CalendarMonth,
                                        contentDescription =
                                            null,
                                        tint =
                                            PatientColors.Red,
                                        modifier =
                                            Modifier.size(
                                                29.dp
                                            )
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
                                        "Couldn't load appointments",
                                    color =
                                        PatientColors.TextPrimary,
                                    fontWeight =
                                        FontWeight.ExtraBold,
                                    fontSize =
                                        15.sp
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            5.dp
                                        )
                                )


                                Text(
                                    text =
                                        loadError
                                            ?: "Something went wrong while loading your appointments.",
                                    color =
                                        PatientColors.TextSecondary,
                                    fontSize =
                                        11.sp,
                                    lineHeight =
                                        16.sp,
                                    textAlign =
                                        TextAlign.Center
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            10.dp
                                        )
                                )


                                TextButton(
                                    onClick =
                                        onBack
                                ) {

                                    Text(
                                        text =
                                            "Go back",
                                        color =
                                            PatientColors.Red,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }


                filtered.isEmpty() -> {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    24.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(
                                            78.dp
                                        )
                                        .background(
                                            when (
                                                selectedCategory
                                            ) {

                                                AppointmentCategory.CANCELLED ->
                                                    PatientColors.RedSoft

                                                AppointmentCategory.COMPLETED ->
                                                    PatientColors.SuccessCard

                                                AppointmentCategory.UPCOMING ->
                                                    PatientColors.DoctorCard

                                                AppointmentCategory.ALL ->
                                                    PatientColors.AppointmentCard
                                            },
                                            CircleShape
                                        ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.CalendarMonth,
                                    contentDescription =
                                        null,
                                    tint =
                                        when (
                                            selectedCategory
                                        ) {

                                            AppointmentCategory.CANCELLED ->
                                                PatientColors.Red

                                            AppointmentCategory.COMPLETED ->
                                                PatientColors.SuccessAccent

                                            AppointmentCategory.UPCOMING ->
                                                PatientColors.DoctorAccent

                                            AppointmentCategory.ALL ->
                                                PatientColors.AppointmentAccent
                                        },
                                    modifier =
                                        Modifier.size(
                                            35.dp
                                        )
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        14.dp
                                    )
                            )


                            Text(
                                text =
                                    when (
                                        selectedCategory
                                    ) {

                                        AppointmentCategory.ALL ->
                                            "No appointments yet"

                                        AppointmentCategory.UPCOMING ->
                                            "No upcoming appointments"

                                        AppointmentCategory.COMPLETED ->
                                            "No completed appointments"

                                        AppointmentCategory.CANCELLED ->
                                            "No cancelled appointments"
                                    },
                                color =
                                    PatientColors.TextPrimary,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                fontSize =
                                    15.sp,
                                textAlign =
                                    TextAlign.Center
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )


                            Text(
                                text =
                                    when (
                                        selectedCategory
                                    ) {

                                        AppointmentCategory.ALL ->
                                            "When you book a doctor, your appointment will appear here."

                                        AppointmentCategory.UPCOMING ->
                                            "You don't currently have any upcoming visits."

                                        AppointmentCategory.COMPLETED ->
                                            "Completed consultations will appear here."

                                        AppointmentCategory.CANCELLED ->
                                            "Cancelled appointments will appear here."
                                    },
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    11.sp,
                                lineHeight =
                                    16.sp,
                                textAlign =
                                    TextAlign.Center
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )


                            if (
                                selectedCategory ==
                                AppointmentCategory.ALL ||
                                selectedCategory ==
                                AppointmentCategory.UPCOMING
                            ) {

                                TextButton(
                                    onClick =
                                        onFindDoctors
                                ) {

                                    Text(
                                        text =
                                            "Find a doctor",
                                        color =
                                            PatientColors.DoctorAccent,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }

                            } else {

                                TextButton(
                                    onClick = {
                                        selectedCategory =
                                            AppointmentCategory.ALL
                                    }
                                ) {

                                    Text(
                                        text =
                                            "View all appointments",
                                        color =
                                            PatientColors.AppointmentAccent,
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    reviewTargetAppointmentId?.let { apptId ->
        ReviewDialog(
            isSubmitting = isSubmittingReview,
            onDismiss = { reviewTargetAppointmentId = null },
            onSubmit = { rating, comment ->
                onSubmitReview(apptId, null, rating, comment)
                reviewTargetAppointmentId = null
            }
        )
    }

    detailsAppointment?.let { appt ->
        AppointmentListDetailsDialog(appt = appt, onDismiss = { detailsAppointment = null })
    }

    selectedAppointment?.let { appt ->
        AppointmentActionSheet(
            appointment = appt,
            onDismiss = { selectedAppointment = null },
            onViewDetails = {
                detailsAppointment = appt
                selectedAppointment = null
            },
            onReschedule = {
                selectedAppointment = null
                onRescheduleAppointment(appt.id)
            },
            onCancel = {
                scope.launch {
                    isCancelling = true
                    cancelAppointment(appt.id).onSuccess {
                        appointments = appointments.map {
                            if (it.id == appt.id) it.copy(status = "cancelled") else it
                        }
                    }
                    isCancelling = false
                    selectedAppointment = null
                }
            },
            onRequestCompletionCode = { appointmentId ->
                val result = onRequestCompletionCode(appointmentId)
                result.onSuccess { code ->
                    appointments = appointments.map {
                        if (it.id == appointmentId) it.copy(completion_code = code) else it
                    }
                }
                result
            }
        )
    }
}

@Composable
private fun AppointmentListDetailsDialog(
    appt: Appointment,
    onDismiss: () -> Unit
) {

    val isOnline =
        appt.appointment_type
            .equals(
                "online",
                ignoreCase = true
            )


    val statusColor =
        when (appt.status) {

            "confirmed" ->
                PatientColors.SuccessAccent

            "pending" ->
                PatientColors.ReviewAccent

            "rescheduled" ->
                PatientColors.AppointmentAccent

            "cancelled" ->
                PatientColors.Red

            else ->
                PatientColors.TextSecondary
        }


    val statusBackground =
        when (appt.status) {

            "confirmed" ->
                PatientColors.SuccessCard

            "pending" ->
                PatientColors.ReviewCard

            "rescheduled" ->
                PatientColors.AppointmentCard

            "cancelled" ->
                PatientColors.RedSoft

            else ->
                PatientColors.NeutralCard
        }


    val statusLabel =
        when (appt.status) {

            "confirmed" ->
                "Accepted & Paid"

            "cancelled" ->
                "Rejected & Refunded"

            else ->
                appt.status
                    ?.replaceFirstChar {
                        it.uppercase()
                    }
                    ?: "Unknown"
        }


    AlertDialog(
        onDismissRequest =
            onDismiss,

        shape =
            RoundedCornerShape(
                25.dp
            ),

        containerColor =
            MaterialTheme.colorScheme.surface,

        title = {

            Column(
                modifier =
                    Modifier.fillMaxWidth()
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
                                .size(
                                    42.dp
                                )
                                .background(
                                    PatientColors.AppointmentCard,
                                    RoundedCornerShape(
                                        13.dp
                                    )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.CalendarMonth,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.AppointmentAccent,
                            modifier =
                                Modifier.size(
                                    21.dp
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
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Appointment details",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                17.sp
                        )


                        Text(
                            text =
                                "Your visit information",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }
                }
            }
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                /*
                 * Status
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                statusBackground,
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
                                    9.dp
                                )
                                .background(
                                    statusColor,
                                    CircleShape
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
                            "Status",
                        modifier =
                            Modifier.weight(1f),
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp
                    )


                    Text(
                        text =
                            statusLabel,
                        color =
                            statusColor,
                        fontSize =
                            11.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                /*
                 * Date and time
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    AppointmentDetailMiniCard(
                        modifier =
                            Modifier.weight(1f),
                        icon =
                            Icons.Outlined.CalendarMonth,
                        label =
                            "Date",
                        value =
                            appt.date ?: "-",
                        accent =
                            PatientColors.AppointmentAccent,
                        background =
                            PatientColors.AppointmentCard
                    )


                    AppointmentDetailMiniCard(
                        modifier =
                            Modifier.weight(1f),
                        icon =
                            Icons.Outlined.Schedule,
                        label =
                            "Time",
                        value =
                            appt.time ?: "-",
                        accent =
                            PatientColors.DoctorAccent,
                        background =
                            PatientColors.DoctorCard
                    )
                }


                /*
                 * Visit type
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isOnline) {
                                    PatientColors.AppointmentCard
                                } else {
                                    PatientColors.DoctorCard
                                },
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

                    Icon(
                        imageVector =
                            if (isOnline) {
                                Icons.Outlined.VideoCall
                            } else {
                                Icons.Outlined.LocationOn
                            },
                        contentDescription =
                            null,
                        tint =
                            if (isOnline) {
                                PatientColors.AppointmentAccent
                            } else {
                                PatientColors.DoctorAccent
                            },
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


                    Column {

                        Text(
                            text =
                                "Visit type",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                9.5.sp
                        )


                        Text(
                            text =
                                if (isOnline) {
                                    "Online consultation"
                                } else {
                                    "In-person appointment"
                                },
                            color =
                                if (isOnline) {
                                    PatientColors.AppointmentAccent
                                } else {
                                    PatientColors.DoctorAccent
                                },
                            fontSize =
                                11.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }


                /*
                 * Reason for visit
                 */
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                PatientColors.NeutralCard,
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .padding(
                                12.dp
                            )
                ) {

                    Text(
                        text =
                            "Reason for visit",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            9.5.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )


                    Text(
                        text =
                            appt.reason
                                ?: "General consultation",
                        color =
                            PatientColors.TextPrimary,
                        fontSize =
                            11.5.sp,
                        lineHeight =
                            16.sp
                    )
                }


                /*
                 * Payment information
                 */
                if (
                    appt.payment_method != null ||
                    appt.amount_paid != null
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.SuccessCard,
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

                        Icon(
                            imageVector =
                                Icons.Outlined.Payments,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.SuccessAccent,
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


                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    appt.payment_method
                                        ?.let {
                                            "Paid via $it"
                                        }
                                        ?: "Payment",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp
                            )


                            appt.amount_paid
                                ?.let { amount ->

                                    Text(
                                        text =
                                            "R %.2f"
                                                .format(
                                                    amount
                                                ),
                                        color =
                                            PatientColors.SuccessAccent,
                                        fontSize =
                                            12.5.sp,
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    )
                                }
                        }
                    }
                }


                cancellationExplanation(appt)
                    ?.let { explanation ->

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        PatientColors.RedSoft,
                                        RoundedCornerShape(
                                            15.dp
                                        )
                                    )
                                    .padding(
                                        12.dp
                                    )
                        ) {

                            Text(
                                text =
                                    explanation,
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
        },

        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    text =
                        "Close",
                    color =
                        PatientColors.AppointmentAccent,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun AppointmentDetailMiniCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accent: Color,
    background: Color
) {

    Row(
        modifier =
            modifier
                .background(
                    background,
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .padding(
                    11.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        31.dp
                    )
                    .background(
                        accent.copy(
                            alpha = 0.10f
                        ),
                        RoundedCornerShape(
                            9.dp
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
                        16.dp
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.width(
                    7.dp
                )
        )


        Column {

            Text(
                text =
                    label,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    8.5.sp
            )


            Text(
                text =
                    value,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    10.5.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}
@Composable
private fun ReviewDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {

    var rating by remember {
        mutableIntStateOf(5)
    }

    var comment by remember {
        mutableStateOf("")
    }


    val ratingText =
        when (rating) {
            1 -> "Poor"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Very good"
            else -> "Excellent"
        }


    AlertDialog(
        onDismissRequest = {
            if (!isSubmitting) {
                onDismiss()
            }
        },

        shape =
            RoundedCornerShape(
                26.dp
            ),

        containerColor =
            MaterialTheme.colorScheme.surface,

        title = {

            Column(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                58.dp
                            )
                            .background(
                                PatientColors.ReviewCard,
                                CircleShape
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Star,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.ReviewAccent,
                        modifier =
                            Modifier.size(
                                29.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            11.dp
                        )
                )


                Text(
                    text =
                        "Rate your visit",
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.ExtraBold,
                    fontSize =
                        18.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        "How was your consultation?",
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        10.5.sp
                )
            }
        },

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                /*
                 * Rating card
                 */
                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                PatientColors.ReviewCard
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
                                .padding(
                                    15.dp
                                ),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Row(
                            horizontalArrangement =
                                Arrangement.Center,
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            repeat(5) { index ->

                                Icon(
                                    imageVector =
                                        Icons.Filled.Star,
                                    contentDescription =
                                        "Rate ${index + 1} stars",
                                    tint =
                                        if (
                                            index < rating
                                        ) {
                                            PatientColors.ReviewAccent
                                        } else {
                                            Color(0xFFE2E8F0)
                                        },
                                    modifier =
                                        Modifier
                                            .size(
                                                39.dp
                                            )
                                            .padding(
                                                3.dp
                                            )
                                            .clickableStar {
                                                rating =
                                                    index + 1
                                            }
                                )
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    7.dp
                                )
                        )


                        Text(
                            text =
                                "$rating / 5 • $ratingText",
                            color =
                                PatientColors.ReviewAccent,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                11.5.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )


                /*
                 * Comment
                 */
                OutlinedTextField(
                    value =
                        comment,

                    onValueChange = {
                        comment = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text(
                            "Share your experience"
                        )
                    },

                    placeholder = {
                        Text(
                            text =
                                "Tell us what went well or what could be improved.",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    },

                    minLines =
                        3,

                    maxLines =
                        5,

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    enabled =
                        !isSubmitting,

                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor =
                                PatientColors.ReviewAccent,

                            cursorColor =
                                PatientColors.ReviewAccent,

                            focusedLabelColor =
                                PatientColors.ReviewAccent,

                            unfocusedBorderColor =
                                PatientColors.ReviewAccent
                                    .copy(alpha = 0.18f)
                        ),

                    supportingText = {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text =
                                    "Optional",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.weight(1f)
                            )


                            Text(
                                text =
                                    "${comment.length} characters",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp
                            )
                        }
                    }
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                PatientColors.PurpleSoft,
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .padding(
                                11.dp
                            ),
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Text(
                        text =
                            "Your review helps other patients understand what to expect.",
                        modifier =
                            Modifier.weight(1f),
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.sp,
                        lineHeight =
                            14.sp
                    )
                }
            }
        },

        confirmButton = {

            Button(
                onClick = {
                    onSubmit(
                        rating,
                        comment.trim()
                    )
                },

                enabled =
                    !isSubmitting,

                shape =
                    RoundedCornerShape(
                        14.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            PatientColors.ReviewAccent,
                        contentColor =
                            Color.White
                    )
            ) {

                if (isSubmitting) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                17.dp
                            ),
                        strokeWidth =
                            2.dp,
                        color =
                            Color.White
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                7.dp
                            )
                    )


                    Text(
                        text =
                            "Submitting..."
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Filled.Star,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            "Submit review",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss,
                enabled =
                    !isSubmitting
            ) {

                Text(
                    text =
                        "Cancel",
                    color =
                        PatientColors.TextSecondary,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    )
}

@SuppressLint("SuspiciousModifierThen")
private fun Modifier.clickableStar(
    onClick: () -> Unit
): Modifier =
    this.then(
        clickable(
            onClick = onClick
        )
    )

@Composable
private fun AppointmentDetailCard(
    appt: Appointment,
    fetchDoctor: suspend (String) -> Result<DoctorProfile>,
    onViewDoctorProfile: (String) -> Unit,
    showLeaveReview: Boolean,
    onLeaveReview: () -> Unit,
    onClick: () -> Unit
) {

    var doctor by remember {
        mutableStateOf<DoctorProfile?>(null)
    }


    LaunchedEffect(appt.doctor_id) {

        val doctorId =
            appt.doctor_id

        if (doctorId != null) {

            fetchDoctor(doctorId)
                .onSuccess {
                    doctor = it
                }
        }
    }


    val statusColor =
        when (appt.status) {

            "confirmed" ->
                PatientColors.SuccessAccent

            "pending" ->
                PatientColors.ReviewAccent

            "rescheduled" ->
                PatientColors.AppointmentAccent

            "cancelled" ->
                PatientColors.Red

            else ->
                PatientColors.TextSecondary
        }


    val statusBackground =
        when (appt.status) {

            "confirmed" ->
                PatientColors.SuccessCard

            "pending" ->
                PatientColors.ReviewCard

            "rescheduled" ->
                PatientColors.AppointmentCard

            "cancelled" ->
                PatientColors.RedSoft

            else ->
                PatientColors.NeutralCard
        }


    val statusLabel =
        when (appt.status) {

            "confirmed" ->
                "Accepted & Paid"

            "cancelled" ->
                "Rejected & Refunded"

            else ->
                appt.status
                    ?.replaceFirstChar {
                        it.uppercase()
                    }
                    ?: "Unknown"
        }


    val isOnline =
        appt.appointment_type
            .equals(
                "online",
                ignoreCase = true
            )


    val appointmentAccent =
        if (isOnline) {
            PatientColors.AppointmentAccent
        } else {
            PatientColors.DoctorAccent
        }


    val appointmentBackground =
        if (isOnline) {
            PatientColors.AppointmentCard
        } else {
            PatientColors.DoctorCard
        }


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),

        shape =
            RoundedCornerShape(
                21.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                width = 1.dp,
                color =
                    appointmentAccent
                        .copy(alpha = 0.10f)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column {

            /*
             * Small coloured accent strip
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            4.dp
                        )
                        .background(
                            appointmentAccent
                        )
            )


            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                /*
                 * Doctor section
                 */
                doctor?.let { doctorInfo ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    appt.doctor_id
                                        ?.let {
                                            onViewDoctorProfile(
                                                it
                                            )
                                        }
                                },
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        val firstInitial =
                            doctorInfo.name
                                ?.trim()
                                ?.firstOrNull()
                                ?.uppercaseChar()
                                ?: 'D'

                        val lastInitial =
                            doctorInfo.surname
                                ?.trim()
                                ?.firstOrNull()
                                ?.uppercaseChar()
                                ?: 'R'


                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        48.dp
                                    )
                                    .background(
                                        appointmentBackground,
                                        CircleShape
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(
                                            39.dp
                                        )
                                        .background(
                                            appointmentAccent,
                                            CircleShape
                                        ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(
                                    text =
                                        "$firstInitial$lastInitial",
                                    color =
                                        Color.White,
                                    fontSize =
                                        14.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.width(
                                    11.dp
                                )
                        )


                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    "Dr. ${
                                        doctorInfo.name ?: ""
                                    } ${
                                        doctorInfo.surname ?: ""
                                    }"
                                        .trim(),
                                color =
                                    PatientColors.TextPrimary,
                                fontSize =
                                    14.5.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )


                            doctorInfo.discipline
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let { discipline ->

                                    Spacer(
                                        modifier =
                                            Modifier.height(
                                                2.dp
                                            )
                                    )


                                    Text(
                                        text =
                                            discipline,
                                        color =
                                            PatientColors.TextSecondary,
                                        fontSize =
                                            10.5.sp
                                    )
                                }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        3.dp
                                    )
                            )


                            Text(
                                text =
                                    "View doctor profile",
                                color =
                                    appointmentAccent,
                                fontSize =
                                    10.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }


                        Icon(
                            imageVector =
                                Icons.Outlined.ChevronRight,
                            contentDescription =
                                "View doctor",
                            tint =
                                PatientColors.TextSecondary,
                            modifier =
                                Modifier.size(
                                    20.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )
                }


                /*
                 * Date + status
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Row(
                        modifier =
                            Modifier.weight(1f),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        33.dp
                                    )
                                    .background(
                                        appointmentBackground,
                                        RoundedCornerShape(
                                            10.dp
                                        )
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.CalendarMonth,
                                contentDescription =
                                    null,
                                tint =
                                    appointmentAccent,
                                modifier =
                                    Modifier.size(
                                        17.dp
                                    )
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Column {

                            Text(
                                text =
                                    appt.date ?: "-",
                                color =
                                    PatientColors.TextPrimary,
                                fontSize =
                                    12.5.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )


                            Text(
                                text =
                                    appt.time ?: "-",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.sp
                            )
                        }
                    }


                    Box(
                        modifier =
                            Modifier
                                .background(
                                    statusBackground,
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .padding(
                                    horizontal = 9.dp,
                                    vertical = 5.dp
                                )
                    ) {

                        Text(
                            text =
                                statusLabel,
                            color =
                                statusColor,
                            fontSize =
                                9.5.sp,
                            fontWeight =
                                FontWeight.Bold
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
                 * Visit type
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                appointmentBackground,
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .padding(
                                11.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            if (isOnline) {
                                Icons.Outlined.VideoCall
                            } else {
                                Icons.Outlined.LocationOn
                            },
                        contentDescription =
                            null,
                        tint =
                            appointmentAccent,
                        modifier =
                            Modifier.size(
                                18.dp
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
                            if (isOnline) {
                                "Online consultation"
                            } else {
                                "In-person appointment"
                            },
                        color =
                            appointmentAccent,
                        fontSize =
                            11.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            11.dp
                        )
                )


                /*
                 * Reason
                 */
                Row(
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Schedule,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.TextSecondary,
                        modifier =
                            Modifier.size(
                                16.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                7.dp
                            )
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Reason for visit",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                9.5.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )


                        Text(
                            text =
                                appt.reason
                                    ?: "General consultation",
                            color =
                                PatientColors.TextPrimary,
                            fontSize =
                                11.5.sp,
                            lineHeight =
                                16.sp
                        )
                    }
                }


                /*
                 * Payment details
                 */
                if (
                    appt.payment_method != null ||
                    appt.amount_paid != null
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                11.dp
                            )
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.SuccessCard,
                                    RoundedCornerShape(
                                        13.dp
                                    )
                                )
                                .padding(
                                    10.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Payments,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.SuccessAccent,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    7.dp
                                )
                        )


                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    appt.payment_method
                                        ?.let {
                                            "Paid via $it"
                                        }
                                        ?: "Payment",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp
                            )


                            appt.amount_paid
                                ?.let { amount ->

                                    Text(
                                        text =
                                            "R %.2f"
                                                .format(
                                                    amount
                                                ),
                                        color =
                                            PatientColors.SuccessAccent,
                                        fontSize =
                                            12.sp,
                                        fontWeight =
                                            FontWeight.ExtraBold
                                    )
                                }
                        }
                    }
                }


                cancellationExplanation(appt)
                    ?.let { explanation ->

                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )


                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        PatientColors.RedSoft,
                                        RoundedCornerShape(
                                            13.dp
                                        )
                                    )
                                    .padding(
                                        10.dp
                                    )
                        ) {

                            Text(
                                text =
                                    explanation,
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.5.sp,
                                lineHeight =
                                    15.sp
                            )
                        }
                    }


                if (showLeaveReview) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    TextButton(
                        onClick =
                            onLeaveReview
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.Star,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.ReviewAccent,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    6.dp
                                )
                        )


                        Text(
                            text =
                                "Leave a review",
                            color =
                                PatientColors.ReviewAccent,
                            fontSize =
                                11.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
