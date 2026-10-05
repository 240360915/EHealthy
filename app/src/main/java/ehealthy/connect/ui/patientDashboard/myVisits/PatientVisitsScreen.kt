package ehealthy.connect.ui.patientDashboard.myVisits

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.patient.PatientAppointment
import ehealthy.connect.data.patient.PatientDoctorSummary
import ehealthy.connect.data.patient.PatientRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private enum class VisitTab(
    val title: String
) {
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

private val VisitSuccess = Color(0xFF15803D)
private val VisitWarning = Color(0xFFF59E0B)
private val VisitCancelled = Color(0xFFB91C1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientVisitsScreen(
    onBack: () -> Unit,
    onReschedule: (appointmentId: String) -> Unit,
    onMessageDoctor: (doctorId: String) -> Unit,
    onJoinScheduledCall: (appointment: PatientAppointment) -> Unit,
    onRateAppointment: (appointment: PatientAppointment) -> Unit
) {

    val scope = rememberCoroutineScope()

    var appointments by remember {
        mutableStateOf<List<PatientAppointment>>(
            emptyList()
        )
    }

    var doctors by remember {
        mutableStateOf<Map<String, PatientDoctorSummary>>(
            emptyMap()
        )
    }

    var selectedTab by remember {
        mutableStateOf(
            VisitTab.UPCOMING
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var cancellingAppointment by remember {
        mutableStateOf<PatientAppointment?>(null)
    }

    var cancellationReason by remember {
        mutableStateOf("")
    }

    var cancellationError by remember {
        mutableStateOf<String?>(null)
    }

    var isCancelling by remember {
        mutableStateOf(false)
    }

    suspend fun loadAppointments(
        refreshing: Boolean = false
    ) {

        if (refreshing) {
            isRefreshing = true
        } else {
            isLoading = true
        }

        errorMessage = null

        PatientRepository
            .getMyAppointments()
            .onSuccess { result ->

                appointments = result

                val loadedDoctors =
                    mutableMapOf<
                            String,
                            PatientDoctorSummary
                            >()

                val doctorIds =
                    result
                        .mapNotNull {
                            it.doctor_id
                        }
                        .distinct()

                for (doctorId in doctorIds) {

                    PatientRepository
                        .getDoctor(
                            doctorId
                        )
                        .getOrNull()
                        ?.let { doctor ->

                            loadedDoctors[
                                doctorId
                            ] = doctor
                        }
                }

                doctors = loadedDoctors
            }
            .onFailure { error ->

                errorMessage =
                    error.message
                        ?: "Unable to load your appointments."
            }

        isLoading = false
        isRefreshing = false
    }

    suspend fun performCancellation(
        appointment: PatientAppointment
    ) {

        if (
            cancellationReason
                .trim()
                .length < 3
        ) {

            cancellationError =
                "Please enter a short cancellation reason."

            return
        }

        isCancelling = true
        cancellationError = null

        PatientRepository
            .cancelAppointment(
                appointmentId =
                    appointment.id,
                reason =
                    cancellationReason.trim(),
                idempotencyKey =
                    UUID.randomUUID()
                        .toString()
            )
            .onSuccess {

                cancellingAppointment =
                    null

                cancellationReason = ""

                loadAppointments(
                    refreshing = true
                )
            }
            .onFailure { error ->

                cancellationError =
                    friendlyCancellationError(
                        error.message
                    )
            }

        isCancelling = false
    }

    LaunchedEffect(Unit) {
        loadAppointments()
    }

    val today =
        remember {

            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )
                .format(
                    Date()
                )
        }

    val visibleAppointments =
        appointments.filter { appointment ->

            when (selectedTab) {

                VisitTab.UPCOMING ->

                    appointment.status
                        .isUpcomingStatus() &&
                            appointment.date
                                .orEmpty() >= today

                VisitTab.COMPLETED ->

                    appointment.status
                        .equals(
                            "completed",
                            ignoreCase = true
                        )

                VisitTab.CANCELLED ->

                    appointment.status
                        .isCancelledStatus()
            }
        }

    cancellingAppointment
        ?.let { appointment ->

            CancelAppointmentDialog(
                appointment =
                    appointment,
                reason =
                    cancellationReason,
                error =
                    cancellationError,
                isCancelling =
                    isCancelling,
                onReasonChange = {
                    if (it.length <= 300) {
                        cancellationReason = it
                        cancellationError = null
                    }
                },
                onDismiss = {

                    if (!isCancelling) {

                        cancellingAppointment =
                            null

                        cancellationReason =
                            ""

                        cancellationError =
                            null
                    }
                },
                onConfirm = {

                    scope.launch {

                        performCancellation(
                            appointment
                        )
                    }
                }
            )
        }

    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        topBar = {

            TopAppBar(

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                },

                title = {

                    Column {

                        Text(
                            text =
                                "My visits",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Appointments and consultations",
                            fontSize =
                                11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                },

                actions = {

                    IconButton(
                        enabled =
                            !isRefreshing,
                        onClick = {

                            scope.launch {

                                loadAppointments(
                                    refreshing = true
                                )
                            }
                        }
                    ) {

                        if (isRefreshing) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        21.dp
                                    ),
                                strokeWidth =
                                    2.dp
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Filled.Refresh,
                                contentDescription =
                                    "Refresh"
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
        }

    ) { innerPadding ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
        ) {

            VisitsSummary(
                appointments =
                    appointments,
                today =
                    today
            )

            VisitTabs(
                selected =
                    selectedTab,
                onSelected = {
                    selectedTab = it
                }
            )

            when {

                isLoading -> {

                    VisitsLoading()
                }

                errorMessage != null -> {

                    VisitsError(
                        message =
                            errorMessage
                                ?: "Unable to load visits.",
                        onRetry = {

                            scope.launch {
                                loadAppointments()
                            }
                        }
                    )
                }

                visibleAppointments
                    .isEmpty() -> {

                    EmptyVisits(
                        tab =
                            selectedTab
                    )
                }

                else -> {

                    LazyColumn(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                start =
                                    16.dp,
                                end =
                                    16.dp,
                                top =
                                    10.dp,
                                bottom =
                                    30.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                12.dp
                            )
                    ) {

                        items(
                            items =
                                visibleAppointments,
                            key = {
                                it.id
                            }
                        ) { appointment ->

                            VisitCard(
                                appointment =
                                    appointment,

                                doctor =
                                    appointment
                                        .doctor_id
                                        ?.let {
                                            doctors[it]
                                        },

                                onMessage = {

                                    appointment
                                        .doctor_id
                                        ?.let {
                                            onMessageDoctor(
                                                it
                                            )
                                        }
                                },

                                onReschedule = {

                                    onReschedule(
                                        appointment.id
                                    )
                                },

                                onCancel = {

                                    cancellationReason =
                                        ""

                                    cancellationError =
                                        null

                                    cancellingAppointment =
                                        appointment
                                },

                                onJoinCall = {

                                    onJoinScheduledCall(
                                        appointment
                                    )
                                },

                                onRate = {

                                    onRateAppointment(
                                        appointment
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisitsSummary(
    appointments: List<PatientAppointment>,
    today: String
) {

    val upcoming =
        appointments.count { appointment ->

            appointment.status
                .isUpcomingStatus() &&
                    appointment.date
                        .orEmpty() >= today
        }

    val completed =
        appointments.count { appointment ->

            appointment.status
                .equals(
                    "completed",
                    ignoreCase = true
                )
        }

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        12.dp
                ),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults
                .elevatedCardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(
                                alpha = 0.45f
                            )
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        18.dp
                    ),
            horizontalArrangement =
                Arrangement.SpaceEvenly
        ) {

            VisitSummaryItem(
                value =
                    upcoming.toString(),
                label =
                    "Upcoming",
                icon =
                    Icons.Outlined.CalendarMonth
            )

            SummaryDivider()

            VisitSummaryItem(
                value =
                    completed.toString(),
                label =
                    "Completed",
                icon =
                    Icons.Filled.CheckCircle
            )

            SummaryDivider()

            VisitSummaryItem(
                value =
                    appointments
                        .size
                        .toString(),
                label =
                    "Total",
                icon =
                    Icons.Outlined.History
            )
        }
    }
}

@Composable
private fun VisitSummaryItem(
    value: String,
    label: String,
    icon: ImageVector
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            tint =
                MaterialTheme
                    .colorScheme
                    .primary,
            modifier =
                Modifier.size(
                    19.dp
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        Text(
            text =
                value,
            fontSize =
                19.sp,
            fontWeight =
                FontWeight.Bold
        )

        Text(
            text =
                label,
            fontSize =
                10.5.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryDivider() {

    Box(
        modifier =
            Modifier
                .width(
                    1.dp
                )
                .height(
                    42.dp
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .outlineVariant
                )
    )
}

@Composable
private fun VisitTabs(
    selected: VisitTab,
    onSelected: (VisitTab) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            )
    ) {

        VisitTab.entries
            .forEach { tab ->

                FilterChip(
                    selected =
                        selected == tab,
                    onClick = {
                        onSelected(
                            tab
                        )
                    },
                    label = {
                        Text(
                            tab.title
                        )
                    }
                )
            }
    }
}

@Composable
private fun VisitCard(
    appointment: PatientAppointment,
    doctor: PatientDoctorSummary?,
    onMessage: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit,
    onJoinCall: () -> Unit,
    onRate: () -> Unit
) {

    val status =
        appointment.status
            .orEmpty()
            .lowercase()

    val isActive =
        appointment.status
            .isUpcomingStatus()

    val isCompleted =
        status == "completed"

    val isOnline =
        appointment
            .appointment_type ==
                "online"

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape =
                        CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Person,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.padding(
                                10.dp
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
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
                            doctor
                                ?.fullName
                                ?: "Doctor",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            15.5.sp,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        text =
                            doctor
                                ?.discipline
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Medical appointment",
                        fontSize =
                            11.5.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                VisitStatusChip(
                    status =
                        status
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            VisitInfoRow(
                icon =
                    Icons.Outlined.CalendarMonth,
                label =
                    "Date",
                value =
                    appointment.date
                        ?: "Not set"
            )

            VisitInfoRow(
                icon =
                    Icons.Outlined.Schedule,
                label =
                    "Time",
                value =
                    appointment.time
                        ?.let {
                            formatVisitTime(
                                it
                            )
                        }
                        ?: "Not set"
            )

            VisitInfoRow(
                icon =
                    if (isOnline) {
                        Icons.Outlined.VideoCall
                    } else {
                        Icons.Outlined.LocationOn
                    },
                label =
                    "Visit",
                value =
                    if (isOnline) {
                        "Online"
                    } else {
                        "In person"
                    }
            )

            appointment.reason
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let { reason ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )

                    Surface(
                        shape =
                            RoundedCornerShape(
                                12.dp
                            ),
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                                .copy(
                                    alpha = 0.55f
                                )
                    ) {

                        Column(
                            modifier =
                                Modifier.padding(
                                    12.dp
                                )
                        ) {

                            Text(
                                text =
                                    "Reason",
                                fontSize =
                                    10.5.sp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )

                            Text(
                                text =
                                    reason,
                                fontSize =
                                    12.5.sp
                            )
                        }
                    }
                }

            appointment.price_minor
                ?.let { amount ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Payments,
                            contentDescription =
                                null,
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
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

                        Text(
                            text =
                                "Booking price",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                            fontSize =
                                11.5.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.weight(
                                    1f
                                )
                        )

                        Text(
                            text =
                                formatVisitPrice(
                                    amount,
                                    appointment.currency
                                        ?: "ZAR"
                                ),
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            HorizontalDivider()

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            when {

                isActive -> {

                    ActiveVisitActions(
                        online =
                            isOnline,
                        onMessage =
                            onMessage,
                        onReschedule =
                            onReschedule,
                        onCancel =
                            onCancel,
                        onJoinCall =
                            onJoinCall
                    )
                }

                isCompleted -> {

                    CompletedVisitActions(
                        onMessage =
                            onMessage,
                        onRate =
                            onRate
                    )
                }

                else -> {

                    Text(
                        text =
                            appointment
                                .cancelled_reason
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.let {
                                    "Cancellation reason: $it"
                                }
                                ?: "This appointment is no longer active.",
                        fontSize =
                            11.5.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveVisitActions(
    online: Boolean,
    onMessage: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit,
    onJoinCall: () -> Unit
) {

    Column(
        verticalArrangement =
            Arrangement.spacedBy(
                9.dp
            )
    ) {

        if (online) {

            Button(
                onClick =
                    onJoinCall,
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        13.dp
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.VideoCall,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            7.dp
                        )
                )

                Text(
                    "Open scheduled call"
                )
            }
        }

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    8.dp
                )
        ) {

            OutlinedButton(
                onClick =
                    onMessage,
                modifier =
                    Modifier.weight(
                        1f
                    ),
                shape =
                    RoundedCornerShape(
                        13.dp
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Chat,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            5.dp
                        )
                )

                Text(
                    "Message"
                )
            }

            OutlinedButton(
                onClick =
                    onReschedule,
                modifier =
                    Modifier.weight(
                        1f
                    ),
                shape =
                    RoundedCornerShape(
                        13.dp
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.EventRepeat,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            5.dp
                        )
                )

                Text(
                    "Reschedule"
                )
            }
        }

        TextButton(
            onClick =
                onCancel,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text =
                    "Cancel appointment",
                color =
                    MaterialTheme
                        .colorScheme
                        .error
            )
        }
    }
}

@Composable
private fun CompletedVisitActions(
    onMessage: () -> Unit,
    onRate: () -> Unit
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(
                9.dp
            )
    ) {

        OutlinedButton(
            onClick =
                onMessage,
            modifier =
                Modifier.weight(
                    1f
                ),
            shape =
                RoundedCornerShape(
                    13.dp
                )
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Chat,
                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.width(
                        5.dp
                    )
            )

            Text(
                "Message"
            )
        }

        Button(
            onClick =
                onRate,
            modifier =
                Modifier.weight(
                    1f
                ),
            shape =
                RoundedCornerShape(
                    13.dp
                )
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.RateReview,
                contentDescription =
                    null
            )

            Spacer(
                modifier =
                    Modifier.width(
                        5.dp
                    )
            )

            Text(
                "Rate visit"
            )
        }
    }
}

@Composable
private fun VisitStatusChip(
    status: String
) {

    val background =
        when {

            status == "completed" ->

                VisitSuccess.copy(
                    alpha = 0.12f
                )

            status.isCancelledStatus() ->

                VisitCancelled.copy(
                    alpha = 0.12f
                )

            status == "pending" ->

                VisitWarning.copy(
                    alpha = 0.14f
                )

            else ->

                MaterialTheme
                    .colorScheme
                    .primaryContainer
        }

    val foreground =
        when {

            status == "completed" ->
                VisitSuccess

            status.isCancelledStatus() ->
                VisitCancelled

            status == "pending" ->
                VisitWarning

            else ->
                MaterialTheme
                    .colorScheme
                    .primary
        }

    Surface(
        color =
            background,
        shape =
            RoundedCornerShape(
                50
            )
    ) {

        Text(
            text =
                status
                    .ifBlank {
                        "pending"
                    }
                    .replace(
                        "_",
                        " "
                    )
                    .replaceFirstChar {
                        it.uppercase()
                    },
            color =
                foreground,
            fontSize =
                10.5.sp,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier.padding(
                    horizontal =
                        9.dp,
                    vertical =
                        5.dp
                )
        )
    }
}

@Composable
private fun VisitInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        3.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            tint =
                MaterialTheme
                    .colorScheme
                    .primary,
            modifier =
                Modifier.size(
                    17.dp
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
                label,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize =
                11.5.sp,
            modifier =
                Modifier.width(
                    54.dp
                )
        )

        Text(
            text =
                value,
            fontSize =
                12.5.sp,
            fontWeight =
                FontWeight.SemiBold,
            modifier =
                Modifier.weight(
                    1f
                ),
            maxLines =
                1,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CancelAppointmentDialog(
    appointment: PatientAppointment,
    reason: String,
    error: String?,
    isCancelling: Boolean,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                "Cancel appointment?"
            )
        },

        text = {

            Column {

                Text(
                    text =
                        "The appointment will be cancelled and its reserved slot can become available again."
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                OutlinedTextField(
                    value =
                        reason,
                    onValueChange =
                        onReasonChange,
                    modifier =
                        Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            "Cancellation reason"
                        )
                    },
                    minLines =
                        2,
                    maxLines =
                        4,
                    enabled =
                        !isCancelling
                )

                if (error != null) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )

                    Text(
                        text =
                            error,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        fontSize =
                            11.5.sp
                    )
                }

                if (
                    (
                            appointment.amount_paid
                                ?: 0.0
                            ) > 0.0
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .secondaryContainer
                                        .copy(
                                            alpha = 0.5f
                                        ),
                                    RoundedCornerShape(
                                        10.dp
                                    )
                                )
                                .padding(
                                    10.dp
                                ),
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Info,
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
                                    7.dp
                                )
                        )

                        Text(
                            text =
                                "A previous payment exists. Cancelling the visit does not automatically mean a refund has been processed.",
                            fontSize =
                                10.5.sp
                        )
                    }
                }
            }
        },

        confirmButton = {

            Button(
                onClick =
                    onConfirm,
                enabled =
                    !isCancelling
            ) {

                if (isCancelling) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                17.dp
                            ),
                        strokeWidth =
                            2.dp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                7.dp
                            )
                    )

                    Text(
                        "Cancelling..."
                    )

                } else {

                    Text(
                        "Cancel appointment"
                    )
                }
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss,
                enabled =
                    !isCancelling
            ) {

                Text(
                    "Keep appointment"
                )
            }
        }
    )
}

@Composable
private fun VisitsLoading() {

    Box(
        modifier =
            Modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator()

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Text(
                text =
                    "Loading your visits...",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VisitsError(
    message: String,
    onRetry: () -> Unit
) {

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

            Text(
                text =
                    "Couldn't load visits",
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )

            Text(
                text =
                    message,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Button(
                onClick =
                    onRetry
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.Refresh,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            7.dp
                        )
                )

                Text(
                    "Try again"
                )
            }
        }
    }
}

@Composable
private fun EmptyVisits(
    tab: VisitTab
) {

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

            Surface(
                shape =
                    CircleShape,
                color =
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
            ) {

                Icon(
                    imageVector =
                        when (tab) {

                            VisitTab.UPCOMING ->
                                Icons.Outlined.CalendarMonth

                            VisitTab.COMPLETED ->
                                Icons.Filled.CheckCircle

                            VisitTab.CANCELLED ->
                                Icons.Filled.Close
                        },
                    contentDescription =
                        null,
                    modifier =
                        Modifier.padding(
                            17.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    when (tab) {

                        VisitTab.UPCOMING ->
                            "No upcoming visits"

                        VisitTab.COMPLETED ->
                            "No completed visits"

                        VisitTab.CANCELLED ->
                            "No cancelled visits"
                    },
                fontSize =
                    18.sp,
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
                    when (tab) {

                        VisitTab.UPCOMING ->
                            "Your next appointment will appear here."

                        VisitTab.COMPLETED ->
                            "Completed visits will appear here."

                        VisitTab.CANCELLED ->
                            "Cancelled appointments will appear here."
                    },
                textAlign =
                    TextAlign.Center,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

private fun String?
        .isUpcomingStatus(): Boolean {

    return when (
        this?.lowercase()
    ) {

        "pending",
        "confirmed",
        "approved",
        "scheduled",
        "rescheduled",
        "in_progress" ->
            true

        else ->
            false
    }
}

private fun String?
        .isCancelledStatus(): Boolean {

    return when (
        this?.lowercase()
    ) {

        "cancelled",
        "canceled",
        "rejected" ->
            true

        else ->
            false
    }
}

private fun formatVisitTime(
    raw: String
): String {

    val value =
        raw.trim()
            .take(
                5
            )

    val parts =
        value.split(
            ":"
        )

    if (parts.size < 2) {
        return raw
    }

    val hour =
        parts[0]
            .toIntOrNull()
            ?: return raw

    val minute =
        parts[1]

    val period =
        if (hour < 12) {
            "AM"
        } else {
            "PM"
        }

    val displayHour =
        when {

            hour == 0 ->
                12

            hour > 12 ->
                hour - 12

            else ->
                hour
        }

    return "$displayHour:$minute $period"
}

private fun formatVisitPrice(
    minor: Int,
    currency: String
): String {

    val amount =
        minor / 100.0

    return if (
        currency.equals(
            "ZAR",
            ignoreCase = true
        )
    ) {

        "R %.2f".format(
            amount
        )

    } else {

        "$currency %.2f".format(
            amount
        )
    }
}

private fun friendlyCancellationError(
    message: String?
): String {

    val value =
        message.orEmpty()

    return when {

        value.contains(
            "completed",
            ignoreCase = true
        ) ->
            "A completed appointment cannot be cancelled."

        value.contains(
            "not found",
            ignoreCase = true
        ) ->
            "This appointment could not be found."

        value.contains(
            "permission",
            ignoreCase = true
        ) ->
            "You do not have permission to cancel this appointment."

        value.isNotBlank() ->
            value

        else ->
            "We couldn't cancel this appointment. Please try again."
    }
}