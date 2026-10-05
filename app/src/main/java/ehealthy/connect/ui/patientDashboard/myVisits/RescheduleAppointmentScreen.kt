package ehealthy.connect.ui.patientDashboard.myVisits

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.patient.AvailableSlot
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.data.patient.RescheduleAppointmentResult
import ehealthy.connect.ui.patientDashboard.Appointment
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

private val RescheduleGreen =
    Color(0xFF15803D)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun RescheduleAppointmentScreen(
    appointment: Appointment,
    isLoading: Boolean,
    errorMessage: String?,
    fetchAvailableTimes: suspend (date: String) -> Result<Set<String>>,
    onBack: () -> Unit,
    onConfirmReschedule: (
        newDate: String,
        newTime: String,
        reason: String
    ) -> Unit
) {

    val context =
        LocalContext.current

    val scope =
        rememberCoroutineScope()

    var selectedDate by rememberSaveable {
        mutableStateOf("")
    }

    var selectedSlot by remember {
        mutableStateOf<AvailableSlot?>(
            null
        )
    }

    var availableSlots by remember {
        mutableStateOf<List<AvailableSlot>>(
            emptyList()
        )
    }

    var reason by rememberSaveable {
        mutableStateOf("")
    }

    var isLoadingSlots by remember {
        mutableStateOf(false)
    }

    var slotError by remember {
        mutableStateOf<String?>(null)
    }

    var rescheduleError by remember {
        mutableStateOf<String?>(null)
    }

    var isRescheduling by remember {
        mutableStateOf(false)
    }

    var result by remember {
        mutableStateOf<RescheduleAppointmentResult?>(
            null
        )
    }

    var idempotencyKey by remember {
        mutableStateOf(
            UUID.randomUUID()
                .toString()
        )
    }

    val doctorId =
        appointment.doctor_id

    suspend fun loadAvailableSlots(
        date: String
    ) {

        val doctor =
            doctorId

        if (
            doctor.isNullOrBlank()
        ) {

            slotError =
                "This appointment does not have a valid doctor."

            return
        }

        isLoadingSlots = true
        slotError = null
        selectedSlot = null

        PatientRepository
            .getAvailableSlots(
                doctorId =
                    doctor,
                date =
                    date
            )
            .onSuccess { slots ->

                availableSlots =
                    slots
                        .filter {
                            !it.is_booked
                        }
                        .sortedBy {
                            it.time
                        }
            }
            .onFailure { error ->

                availableSlots =
                    emptyList()

                slotError =
                    error.message
                        ?: "Could not load available appointment times."
            }

        isLoadingSlots = false
    }

    fun chooseDate(
        date: String
    ) {

        selectedDate =
            date

        selectedSlot =
            null

        reason =
            ""

        rescheduleError =
            null

        scope.launch {

            loadAvailableSlots(
                date
            )
        }
    }

    fun performReschedule() {

        val newSlot =
            selectedSlot

        if (
            newSlot == null
        ) {

            rescheduleError =
                "Choose a new appointment time."

            return
        }

        if (
            reason.trim()
                .length < 3
        ) {

            rescheduleError =
                "Please briefly explain why you want to reschedule."

            return
        }

        if (
            newSlot.date ==
            appointment.date &&
            newSlot.time.take(5) ==
            appointment.time
                ?.take(5)
        ) {

            rescheduleError =
                "Choose a different date or time from your current appointment."

            return
        }

        rescheduleError =
            null

        isRescheduling =
            true

        scope.launch {

            PatientRepository
                .rescheduleAppointment(
                    appointmentId =
                        appointment.id,
                    newSlotId =
                        newSlot.id,
                    reason =
                        reason.trim(),
                    idempotencyKey =
                        idempotencyKey
                )
                .onSuccess { response ->

                    result =
                        response
                }
                .onFailure { error ->

                    rescheduleError =
                        friendlyRescheduleError(
                            error.message
                        )
                }

            isRescheduling =
                false
        }
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
                        onClick =
                            onBack
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
                                "Reschedule visit",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Choose another available time",
                            fontSize =
                                11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
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
        },

        bottomBar = {

            if (
                result == null
            ) {

                RescheduleBottomBar(
                    enabled =
                        selectedSlot != null &&
                                reason.trim()
                                    .length >= 3 &&
                                !isRescheduling,
                    isLoading =
                        isRescheduling,
                    onConfirm =
                        ::performReschedule
                )
            }
        }

    ) { innerPadding ->

        if (
            result != null
        ) {

            RescheduleSuccessScreen(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding
                        ),
                result =
                    result!!,
                onDone =
                    onBack,
                onAnother = {

                    result =
                        null

                    selectedDate =
                        ""

                    selectedSlot =
                        null

                    availableSlots =
                        emptyList()

                    reason =
                        ""

                    rescheduleError =
                        null

                    idempotencyKey =
                        UUID.randomUUID()
                            .toString()
                }
            )

            return@Scaffold
        }

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .imePadding(),

            contentPadding =
                PaddingValues(
                    bottom =
                        130.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    12.dp
                )

        ) {

            item {

                CurrentAppointmentCard(
                    appointment =
                        appointment
                )
            }

            item {

                RescheduleInformationCard()
            }

            item {

                DateSelectionCard(
                    selectedDate =
                        selectedDate,
                    onDateSelected =
                        ::chooseDate
                )
            }

            if (
                selectedDate
                    .isNotBlank()
            ) {

                item {

                    NewTimeCard(
                        slots =
                            availableSlots,
                        selectedSlot =
                            selectedSlot,
                        isLoading =
                            isLoadingSlots,
                        error =
                            slotError,
                        onSelect = {

                            selectedSlot =
                                it

                            rescheduleError =
                                null
                        },
                        onRetry = {

                            scope.launch {

                                loadAvailableSlots(
                                    selectedDate
                                )
                            }
                        }
                    )
                }
            }

            if (
                selectedSlot != null
            ) {

                item {

                    ReasonCard(
                        reason =
                            reason,
                        onReasonChange = {

                            if (
                                it.length <= 400
                            ) {

                                reason =
                                    it

                                rescheduleError =
                                    null
                            }
                        }
                    )
                }
            }

            if (
                rescheduleError != null ||
                errorMessage != null
            ) {

                item {

                    ErrorCard(
                        message =
                            rescheduleError
                                ?: errorMessage
                                    .orEmpty()
                    )
                }
            }

            item {

                SecureRescheduleCard()
            }
        }
    }
}

@Composable
private fun CurrentAppointmentCard(
    appointment: Appointment
) {

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        8.dp
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
                                alpha =
                                    0.45f
                            )
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Surface(
                    shape =
                        CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.EventRepeat,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimary,
                        modifier =
                            Modifier.padding(
                                9.dp
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            10.dp
                        )
                )

                Column {

                    Text(
                        text =
                            "Current appointment",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            15.sp
                    )

                    Text(
                        text =
                            "This remains active until the server confirms the new slot.",
                        fontSize =
                            10.5.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )

            CurrentDetailRow(
                label =
                    "Date",
                value =
                    appointment.date
                        ?: "Not set"
            )

            CurrentDetailRow(
                label =
                    "Time",
                value =
                    appointment.time
                        ?.let {
                            formatRescheduleTime(
                                it
                            )
                        }
                        ?: "Not set"
            )

            CurrentDetailRow(
                label =
                    "Type",
                value =
                    when (
                        appointment
                            .appointment_type
                    ) {

                        "online" ->
                            "Online consultation"

                        "in_person" ->
                            "In-person appointment"

                        else ->
                            "Appointment"
                    }
            )

            appointment
                .reason
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )

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
                            it,
                        fontSize =
                            12.5.sp
                    )
                }
        }
    }
}

@Composable
private fun CurrentDetailRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        4.dp
                )
    ) {

        Text(
            text =
                label,
            modifier =
                Modifier.width(
                    60.dp
                ),
            fontSize =
                11.5.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text =
                value,
            fontSize =
                12.5.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun RescheduleInformationCard() {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .secondaryContainer
                .copy(
                    alpha =
                        0.55f
                )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    14.dp
                ),
            verticalAlignment =
                Alignment.Top
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Info,
                contentDescription =
                    null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .secondary,
                modifier =
                    Modifier.size(
                        19.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )

            Column {

                Text(
                    text =
                        "Your existing appointment is protected",
                    fontWeight =
                        FontWeight.SemiBold,
                    fontSize =
                        12.5.sp
                )

                Text(
                    text =
                        "The old slot is not released until Supabase successfully locks the new slot and moves the appointment.",
                    fontSize =
                        11.sp,
                    lineHeight =
                        15.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DateSelectionCard(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {

    val context =
        LocalContext.current

    val calendar =
        remember {
            Calendar.getInstance()
        }

    RescheduleSection(
        title =
            "1. Choose new date",
        subtitle =
            "Select a future date to check the doctor's real availability."
    ) {

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->

                                val date =
                                    "%04d-%02d-%02d"
                                        .format(
                                            year,
                                            month + 1,
                                            day
                                        )

                                onDateSelected(
                                    date
                                )
                            },
                            calendar.get(
                                Calendar.YEAR
                            ),
                            calendar.get(
                                Calendar.MONTH
                            ),
                            calendar.get(
                                Calendar.DAY_OF_MONTH
                            )
                        )
                            .apply {

                                datePicker.minDate =
                                    System
                                        .currentTimeMillis() -
                                            1000
                            }
                            .show()
                    },
            shape =
                RoundedCornerShape(
                    14.dp
                ),
            color =
                if (
                    selectedDate
                        .isBlank()
                ) {

                    MaterialTheme
                        .colorScheme
                        .surfaceVariant

                } else {

                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                }
        ) {

            Row(
                modifier =
                    Modifier.padding(
                        15.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.CalendarMonth,
                    contentDescription =
                        null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            10.dp
                        )
                )

                Column {

                    Text(
                        text =
                            if (
                                selectedDate
                                    .isBlank()
                            ) {
                                "Select new date"
                            } else {
                                selectedDate
                            },
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    if (
                        selectedDate
                            .isNotBlank()
                    ) {

                        Text(
                            text =
                                "Tap to choose another date",
                            fontSize =
                                10.5.sp,
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
}

@Composable
private fun NewTimeCard(
    slots: List<AvailableSlot>,
    selectedSlot: AvailableSlot?,
    isLoading: Boolean,
    error: String?,
    onSelect: (AvailableSlot) -> Unit,
    onRetry: () -> Unit
) {

    RescheduleSection(
        title =
            "2. Choose new time",
        subtitle =
            "Only currently available published slots are shown."
    ) {

        when {

            isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                24.dp
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            error != null -> {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            error,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                        textAlign =
                            TextAlign.Center,
                        fontSize =
                            12.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                9.dp
                            )
                    )

                    TextButton(
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
                                    6.dp
                                )
                        )

                        Text(
                            "Retry"
                        )
                    }
                }
            }

            slots.isEmpty() -> {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                14.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Schedule,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                32.dp
                            ),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )

                    Text(
                        text =
                            "No available times",
                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(
                        text =
                            "Choose another date.",
                        fontSize =
                            11.5.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            else -> {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    slots.forEach { slot ->

                        RescheduleSlotOption(
                            slot =
                                slot,
                            selected =
                                selectedSlot
                                    ?.id ==
                                        slot.id,
                            onClick = {
                                onSelect(
                                    slot
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleSlotOption(
    slot: AvailableSlot,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(
                13.dp
            ),
        color =
            if (
                selected
            ) {

                MaterialTheme
                    .colorScheme
                    .primaryContainer

            } else {

                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(
                        alpha =
                            0.55f
                    )
            }
    ) {

        Row(
            modifier =
                Modifier.padding(
                    14.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape =
                    CircleShape,
                color =
                    if (
                        selected
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surface
                    }
            ) {

                Icon(
                    imageVector =
                        if (
                            selected
                        ) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.Schedule
                        },
                    contentDescription =
                        null,
                    tint =
                        if (
                            selected
                        ) {
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .primary
                        },
                    modifier =
                        Modifier.padding(
                            7.dp
                        )
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
                        formatRescheduleTime(
                            slot.time
                        ),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Available",
                    color =
                        RescheduleGreen,
                    fontSize =
                        10.5.sp
                )
            }

            if (
                selected
            ) {

                Text(
                    text =
                        "Selected",
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReasonCard(
    reason: String,
    onReasonChange: (String) -> Unit
) {

    RescheduleSection(
        title =
            "3. Why are you rescheduling?",
        subtitle =
            "A short reason helps keep the appointment history clear."
    ) {

        OutlinedTextField(
            value =
                reason,
            onValueChange =
                onReasonChange,
            modifier =
                Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Example: I won't be available at the original time"
                )
            },
            minLines =
                3,
            maxLines =
                5,
            supportingText = {

                Text(
                    text =
                        "${reason.length}/400",
                    modifier =
                        Modifier.fillMaxWidth(),
                    textAlign =
                        TextAlign.End
                )
            }
        )
    }
}

@Composable
private fun SecureRescheduleCard() {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(
                    alpha =
                        0.55f
                )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    14.dp
                ),
            verticalAlignment =
                Alignment.Top
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Lock,
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
                    Modifier.width(
                        9.dp
                    )
            )

            Column {

                Text(
                    text =
                        "Atomic rescheduling",
                    fontWeight =
                        FontWeight.SemiBold,
                    fontSize =
                        12.5.sp
                )

                Text(
                    text =
                        "The server locks the old and new slots together. If the new slot cannot be secured, your current appointment remains unchanged.",
                    fontSize =
                        11.sp,
                    lineHeight =
                        15.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RescheduleSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                18.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Text(
                text =
                    title,
                fontSize =
                    15.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            Text(
                text =
                    subtitle,
                fontSize =
                    10.5.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            content()
        }
    }
}

@Composable
private fun ErrorCard(
    message: String
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                14.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .errorContainer
    ) {

        Text(
            text =
                message,
            color =
                MaterialTheme
                    .colorScheme
                    .onErrorContainer,
            modifier =
                Modifier.padding(
                    13.dp
                ),
            fontSize =
                12.5.sp
        )
    }
}

@Composable
private fun RescheduleBottomBar(
    enabled: Boolean,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {

    Surface(
        shadowElevation =
            10.dp,
        tonalElevation =
            4.dp
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            16.dp,
                        vertical =
                            12.dp
                    )
        ) {

            Button(
                onClick =
                    onConfirm,
                enabled =
                    enabled,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            54.dp
                        ),
                shape =
                    RoundedCornerShape(
                        14.dp
                    )
            ) {

                if (
                    isLoading
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                19.dp
                            ),
                        strokeWidth =
                            2.dp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                8.dp
                            )
                    )

                    Text(
                        "Rescheduling..."
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Outlined.EventRepeat,
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
                        text =
                            "Confirm new appointment",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RescheduleSuccessScreen(
    modifier: Modifier,
    result: RescheduleAppointmentResult,
    onDone: () -> Unit,
    onAnother: () -> Unit
) {

    Box(
        modifier =
            modifier
                .padding(
                    22.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        ElevatedCard(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    24.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        24.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Surface(
                    shape =
                        CircleShape,
                    color =
                        RescheduleGreen
                            .copy(
                                alpha =
                                    0.12f
                            )
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            null,
                        tint =
                            RescheduleGreen,
                        modifier =
                            Modifier
                                .padding(
                                    15.dp
                                )
                                .size(
                                    40.dp
                                )
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
                        "Appointment rescheduled",
                    fontSize =
                        21.sp,
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
                        "Your new appointment time has been securely reserved.",
                    textAlign =
                        TextAlign.Center,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                ResultRow(
                    label =
                        "Date",
                    value =
                        result
                            .appointment_date
                )

                ResultRow(
                    label =
                        "Time",
                    value =
                        formatRescheduleTime(
                            result
                                .appointment_time
                        )
                )

                ResultRow(
                    label =
                        "Status",
                    value =
                        result
                            .appointment_status
                            .replace(
                                "_",
                                " "
                            )
                            .replaceFirstChar {
                                it.uppercase()
                            }
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Button(
                    onClick =
                        onDone,
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            14.dp
                        )
                ) {

                    Text(
                        text =
                            "Done",
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                TextButton(
                    onClick =
                        onAnother
                ) {

                    Text(
                        "Choose another time"
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        6.dp
                ),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text =
                label,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize =
                12.5.sp
        )

        Text(
            text =
                value,
            fontWeight =
                FontWeight.SemiBold,
            fontSize =
                12.5.sp
        )
    }
}

private fun formatRescheduleTime(
    raw: String
): String {

    val clean =
        raw.trim()
            .take(
                5
            )

    val parts =
        clean.split(
            ":"
        )

    if (
        parts.size < 2
    ) {
        return raw
    }

    val hour =
        parts[0]
            .toIntOrNull()
            ?: return raw

    val minute =
        parts[1]

    val period =
        if (
            hour < 12
        ) {
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

private fun friendlyRescheduleError(
    message: String?
): String {

    val value =
        message
            .orEmpty()

    return when {

        value.contains(
            "Legacy",
            ignoreCase = true
        ) ->

            "This older appointment was created before secure slot reservations were enabled. Cancel it and make a new appointment instead."

        value.contains(
            "same doctor",
            ignoreCase = true
        ) ->

            "You can only move this appointment to another slot belonging to the same doctor."

        value.contains(
            "booked",
            ignoreCase = true
        ) ->

            "Another patient booked this time before you. Choose another available slot."

        value.contains(
            "available",
            ignoreCase = true
        ) ->

            "That time is no longer available. Please choose another slot."

        value.contains(
            "completed",
            ignoreCase = true
        ) ->

            "Completed appointments cannot be rescheduled."

        value.contains(
            "cancelled",
            ignoreCase = true
        ) ->

            "Cancelled appointments cannot be rescheduled."

        value.contains(
            "permission",
            ignoreCase = true
        ) ->

            "You do not have permission to reschedule this appointment."

        value.isNotBlank() ->
            value

        else ->
            "We couldn't reschedule the appointment. Please try another time."
    }
}