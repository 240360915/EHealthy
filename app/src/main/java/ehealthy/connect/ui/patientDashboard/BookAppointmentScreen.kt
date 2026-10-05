package ehealthy.connect.ui.patientDashboard

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.patient.AvailableSlot
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.data.patient.ReserveSlotResult
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.UUID
import androidx.compose.material3.OutlinedTextFieldDefaults
@Serializable
data class DoctorBookingInfo(
    val id: String,
    val name: String,
    val surname: String,
    val discipline: String? = null,
    val hourlyRate: Double? = null,
    val operatingHours: String? = null
) {

    val fullName: String
        get() =
            "Dr. $name $surname"
                .trim()

    val specialty: String
        get() =
            discipline
                ?.takeIf { it.isNotBlank() }
                ?: "General Practitioner"
}

enum class AppointmentType {

    ONLINE,

    IN_PERSON;

    val databaseValue: String
        get() =
            when (this) {

                ONLINE ->
                    "online"

                IN_PERSON ->
                    "in_person"
            }
}

/**
 * Kept temporarily because MainActivity still references it.
 *
 * New bookings no longer use this object to write appointments.
 * The secure reserve_slot() RPC performs the booking.
 */
data class BookingSubmission(
    val date: String,
    val time: String,
    val reason: String,
    val fee: Double,
    val paymentReference: String,
    val appointmentType: AppointmentType
)

private enum class BookingStep(
    val number: Int,
    val title: String
) {

    DATE(
        1,
        "Date"
    ),

    TIME(
        2,
        "Time"
    ),

    TYPE(
        3,
        "Visit"
    ),

    DETAILS(
        4,
        "Details"
    )
}

private val BookingHeroStart =
    Color(0xFF0B3B60)

private val BookingHeroEnd =
    Color(0xFF087F8C)

private val SuccessGreen =
    Color(0xFF15803D)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun BookAppointmentScreen(
    isLoading: Boolean,
    errorMessage: String?,
    fetchDoctor: suspend () -> Result<DoctorBookingInfo>,
    fetchAvailableTimes: suspend (date: String) -> Result<Set<String>>,
    onBack: () -> Unit,
    onConfirmBooking: (BookingSubmission) -> Unit
) {

    val scope =
        rememberCoroutineScope()

    var doctor by remember {
        mutableStateOf<DoctorBookingInfo?>(
            null
        )
    }

    var isLoadingDoctor by remember {
        mutableStateOf(true)
    }

    var doctorError by remember {
        mutableStateOf<String?>(null)
    }

    var selectedDate by rememberSaveable {
        mutableStateOf("")
    }

    var availableSlots by remember {
        mutableStateOf<List<AvailableSlot>>(
            emptyList()
        )
    }

    var alternativeSlots by remember {
        mutableStateOf<List<AvailableSlot>>(
            emptyList()
        )
    }

    var isLoadingAlternatives by remember {
        mutableStateOf(false)
    }

    var selectedSlot by remember {
        mutableStateOf<AvailableSlot?>(
            null
        )
    }

    var isLoadingSlots by remember {
        mutableStateOf(false)
    }

    var slotError by remember {
        mutableStateOf<String?>(null)
    }

    var appointmentType by rememberSaveable {
        mutableStateOf<AppointmentType?>(
            null
        )
    }

    var reason by rememberSaveable {
        mutableStateOf("")
    }

    var bookingError by remember {
        mutableStateOf<String?>(null)
    }

    var isBooking by remember {
        mutableStateOf(false)
    }

    var bookingResult by remember {
        mutableStateOf<ReserveSlotResult?>(
            null
        )
    }

    var bookingRequestId by remember {
        mutableStateOf(
            UUID.randomUUID()
                .toString()
        )
    }

    suspend fun loadDoctor() {

        isLoadingDoctor = true

        doctorError = null

        fetchDoctor()
            .onSuccess {
                doctor = it
            }
            .onFailure {

                doctorError =
                    it.message
                        ?: "Unable to load this doctor."
            }

        isLoadingDoctor = false
    }

    suspend fun loadSlots(
        date: String
    ) {

        val currentDoctor =
            doctor ?: return

        isLoadingSlots = true
        isLoadingAlternatives = false

        slotError = null
        selectedSlot = null

        availableSlots =
            emptyList()

        alternativeSlots =
            emptyList()


        PatientRepository
            .getAvailableSlots(
                doctorId = currentDoctor.id,
                date = date
            )
            .onSuccess { slots ->

                val openSlots =
                    slots.filter {
                        !it.is_booked
                    }

                availableSlots =
                    openSlots


                /*
                 * If this date has no availability,
                 * find the doctor's next available dates.
                 */
                if (openSlots.isEmpty()) {

                    isLoadingAlternatives =
                        true


                    PatientRepository
                        .getAvailableSlots(
                            doctorId =
                                currentDoctor.id,
                            date =
                                null
                        )
                        .onSuccess { allSlots ->

                            val futureSlots =
                                allSlots
                                    .filter { slot ->

                                        !slot.is_booked &&

                                                /*
                                                 * YYYY-MM-DD strings can be
                                                 * compared safely in this format.
                                                 */
                                                slot.date > date
                                    }
                                    .sortedWith(
                                        compareBy<AvailableSlot> {
                                            it.date
                                        }.thenBy {
                                            it.time
                                        }
                                    )


                            /*
                             * Only show the next 3 dates.
                             *
                             * We keep every available time
                             * belonging to those dates.
                             */
                            val nextDates =
                                futureSlots
                                    .map {
                                        it.date
                                    }
                                    .distinct()
                                    .take(3)
                                    .toSet()


                            alternativeSlots =
                                futureSlots.filter {
                                    it.date in nextDates
                                }
                        }
                        .onFailure {

                            /*
                             * The selected date query worked,
                             * so don't turn this into a full
                             * booking error just because
                             * suggestions could not load.
                             */
                            alternativeSlots =
                                emptyList()
                        }


                    isLoadingAlternatives =
                        false
                }
            }
            .onFailure {

                slotError =
                    it.message
                        ?: "Unable to load this doctor's availability."
            }


        isLoadingSlots = false
    }

    fun chooseDate(
        value: String
    ) {

        selectedDate =
            value

        appointmentType =
            null

        reason =
            ""

        bookingError =
            null

        scope.launch {

            loadSlots(
                value
            )
        }
    }

    fun submitBooking() {

        val slot =
            selectedSlot

        val type =
            appointmentType

        if (
            slot == null
        ) {

            bookingError =
                "Choose an available time."

            return
        }

        if (
            type == null
        ) {

            bookingError =
                "Choose whether this will be online or in person."

            return
        }

        if (
            reason.trim()
                .length < 3
        ) {

            bookingError =
                "Please briefly tell the doctor why you need the appointment."

            return
        }

        bookingError =
            null

        isBooking =
            true

        scope.launch {

            PatientRepository
                .reserveSlot(
                    slotId =
                        slot.id,

                    appointmentType =
                        type.databaseValue,

                    reason =
                        reason.trim(),

                    idempotencyKey =
                        bookingRequestId
                )
                .onSuccess { result ->

                    bookingResult =
                        result
                }
                .onFailure {

                    bookingError =
                        friendlyBookingError(
                            it.message
                        )

                    /*
                     * Generate a new key only if the server rejected
                     * the request and the patient changes/retries the
                     * booking.
                     *
                     * A retry of an uncertain network request should
                     * keep the same key.
                     */
                }

            isBooking =
                false
        }
    }

    LaunchedEffect(Unit) {

        loadDoctor()
    }

    val activeStep =

        when {

            selectedDate
                .isBlank() ->

                BookingStep.DATE

            selectedSlot == null ->

                BookingStep.TIME

            appointmentType == null ->

                BookingStep.TYPE

            else ->

                BookingStep.DETAILS
        }

    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                "Book appointment",
                            fontWeight =
                                FontWeight.Bold
                        )

                        doctor?.let {

                            Text(
                                text =
                                    it.fullName,
                                fontSize =
                                    11.sp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )
                        }
                    }
                },

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
                bookingResult == null &&
                doctor != null
            ) {

                BookingBottomBar(

                    doctor =
                        doctor!!,

                    selectedSlot =
                        selectedSlot,

                    appointmentType =
                        appointmentType,

                    isBooking =
                        isBooking,

                    onBook =
                        ::submitBooking
                )
            }
        }

    ) { innerPadding ->

        when {

            isLoadingDoctor -> {

                LoadingBookingScreen(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            )
                )
            }

            doctor == null -> {

                BookingErrorScreen(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),

                    message =
                        doctorError
                            ?: "Doctor unavailable.",

                    onRetry = {

                        scope.launch {
                            loadDoctor()
                        }
                    },

                    onBack =
                        onBack
                )
            }

            bookingResult != null -> {

                BookingSuccessScreen(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),

                    doctor =
                        doctor!!,

                    result =
                        bookingResult!!,

                    appointmentType =
                        appointmentType
                            ?: AppointmentType
                                .IN_PERSON,

                    onDone =
                        onBack,

                    onBookAnother = {

                        bookingResult =
                            null

                        selectedDate =
                            ""

                        selectedSlot =
                            null

                        availableSlots =
                            emptyList()

                        appointmentType =
                            null

                        reason =
                            ""

                        bookingError =
                            null

                        bookingRequestId =
                            UUID.randomUUID()
                                .toString()
                    }
                )
            }

            else -> {

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
                                150.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )

                ) {

                    item {

                        BookingHero(
                            doctor =
                                doctor!!
                        )
                    }

                    item {

                        BookingProgress(
                            activeStep =
                                activeStep
                        )
                    }

                    item {

                        DateSection(

                            selectedDate =
                                selectedDate,

                            onDateSelected =
                                ::chooseDate
                        )
                    }

                    item {

                        AnimatedVisibility(
                            visible =
                                selectedDate
                                    .isNotBlank()
                        ) {

                            SlotSection(

                                selectedDate =
                                    selectedDate,

                                slots =
                                    availableSlots,

                                alternativeSlots =
                                    alternativeSlots,

                                selectedSlot =
                                    selectedSlot,

                                isLoading =
                                    isLoadingSlots,

                                isLoadingAlternatives =
                                    isLoadingAlternatives,

                                error =
                                    slotError,

                                onRetry = {

                                    scope.launch {

                                        loadSlots(
                                            selectedDate
                                        )
                                    }
                                },

                                onSelect = {

                                    selectedSlot =
                                        it

                                    appointmentType =
                                        null

                                    bookingError =
                                        null
                                },

                                onSelectAlternative = { slot ->

                                    /*
                                     * Automatically switch to the suggested date
                                     * and select the chosen time.
                                     */
                                    selectedDate =
                                        slot.date

                                    selectedSlot =
                                        slot

                                    availableSlots =
                                        alternativeSlots.filter {
                                            it.date == slot.date
                                        }

                                    alternativeSlots =
                                        emptyList()

                                    appointmentType =
                                        null

                                    bookingError =
                                        null
                                }
                            )
                        }
                    }

                    item {

                        AnimatedVisibility(
                            visible =
                                selectedSlot != null
                        ) {

                            VisitTypeSection(

                                selected =
                                    appointmentType,

                                onSelect = {

                                    appointmentType =
                                        it

                                    bookingError =
                                        null
                                }
                            )
                        }
                    }

                    item {

                        AnimatedVisibility(
                            visible =
                                appointmentType !=
                                        null
                        ) {

                            ReasonSection(

                                reason =
                                    reason,

                                onReasonChange = {

                                    if (
                                        it.length <= 500
                                    ) {

                                        reason =
                                            it

                                        bookingError =
                                            null
                                    }
                                }
                            )
                        }
                    }

                    item {

                        AnimatedVisibility(
                            visible =
                                appointmentType !=
                                        null
                        ) {

                            PriceInformationCard(
                                doctor =
                                    doctor!!
                            )
                        }
                    }

                    if (
                        bookingError != null ||
                        errorMessage != null
                    ) {

                        item {

                            BookingErrorMessage(
                                message =
                                    bookingError
                                        ?: errorMessage
                                            .orEmpty()
                            )
                        }
                    }

                    item {

                        SecureBookingNotice()
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingHero(
    doctor: DoctorBookingInfo
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
                .clip(
                    RoundedCornerShape(
                        28.dp
                    )
                )
                .background(
                    PatientColors
                        .ConsultationGradient
                )
                .padding(
                    20.dp
                )
    ) {

        /*
         * Decorative background circles
         */
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .size(
                        110.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.07f
                        )
                    )
        )


        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
                    .size(
                        60.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.05f
                        )
                    )
        )


        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            /*
             * Small booking label
             */
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                35.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    11.dp
                                )
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.16f
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
                            Color.White,
                        modifier =
                            Modifier.size(
                                18.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            9.dp
                        )
                )


                Text(
                    text =
                        "BOOK APPOINTMENT",
                    color =
                        Color.White.copy(
                            alpha = 0.90f
                        ),
                    fontSize =
                        9.5.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    letterSpacing =
                        0.7.sp
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
                    doctor.fullName,
                color =
                    Color.White,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    21.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            Text(
                text =
                    doctor.specialty,
                color =
                    Color.White.copy(
                        alpha = 0.84f
                    ),
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.Medium
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * Doctor booking information
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        9.dp
                    )
            ) {

                /*
                 * Operating hours
                 */
                doctor.operatingHours
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let { hours ->

                        Row(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clip(
                                        RoundedCornerShape(
                                            15.dp
                                        )
                                    )
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.13f
                                        )
                                    )
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 9.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Schedule,
                                contentDescription =
                                    null,
                                tint =
                                    Color.White,
                                modifier =
                                    Modifier.size(
                                        16.dp
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        6.dp
                                    )
                            )


                            Column {

                                Text(
                                    text =
                                        "Availability",
                                    color =
                                        Color.White.copy(
                                            alpha = 0.70f
                                        ),
                                    fontSize =
                                        8.5.sp
                                )


                                Text(
                                    text =
                                        hours,
                                    color =
                                        Color.White,
                                    fontSize =
                                        10.5.sp,
                                    fontWeight =
                                        FontWeight.SemiBold,
                                    maxLines =
                                        1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )
                            }
                        }
                    }


                /*
                 * Estimated consultation fee
                 */
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.13f
                                )
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 9.dp
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
                            Color.White,
                        modifier =
                            Modifier.size(
                                16.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )


                    Column {

                        Text(
                            text =
                                "Estimated fee",
                            color =
                                Color.White.copy(
                                    alpha = 0.70f
                                ),
                            fontSize =
                                8.5.sp
                        )


                        Text(
                            text =
                                doctor.hourlyRate
                                    ?.let {
                                        "R %.2f".format(it)
                                    }
                                    ?: "To be confirmed",
                            color =
                                Color.White,
                            fontSize =
                                10.5.sp,
                            fontWeight =
                                FontWeight.Bold
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
             * Small message
             */
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Lock,
                    contentDescription =
                        null,
                    tint =
                        Color.White.copy(
                            alpha = 0.75f
                        ),
                    modifier =
                        Modifier.size(
                            14.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            5.dp
                        )
                )


                Text(
                    text =
                        "Choose an available slot to continue",
                    color =
                        Color.White.copy(
                            alpha = 0.76f
                        ),
                    fontSize =
                        10.5.sp
                )
            }
        }
    }
}

@Composable
private fun BookingProgress(
    activeStep: BookingStep
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text = "Appointment setup",
                    color =
                        PatientColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text =
                        "Complete the steps below to reserve your visit.",
                    color =
                        PatientColors.TextSecondary,
                    fontSize = 10.5.sp
                )
            }


            /*
             * Current progress label
             */
            Box(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                20.dp
                            )
                        )
                        .background(
                            PatientColors.AppointmentCard
                        )
                        .padding(
                            horizontal = 9.dp,
                            vertical = 5.dp
                        )
            ) {

                Text(
                    text =
                        "${activeStep.number}/4",
                    color =
                        PatientColors.AppointmentAccent,
                    fontSize = 10.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(16.dp)
        )


        /*
         * Step circles + connector lines
         */
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            BookingStep.entries
                .forEachIndexed { index, step ->

                    val completed =
                        step.number <
                                activeStep.number

                    val active =
                        step == activeStep


                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        38.dp
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        when {

                                            completed ->
                                                PatientColors
                                                    .SuccessAccent

                                            active ->
                                                PatientColors
                                                    .AppointmentAccent

                                            else ->
                                                MaterialTheme
                                                    .colorScheme
                                                    .surfaceVariant
                                        }
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            if (completed) {

                                Icon(
                                    imageVector =
                                        Icons.Filled.CheckCircle,
                                    contentDescription =
                                        "Completed",
                                    tint =
                                        Color.White,
                                    modifier =
                                        Modifier.size(
                                            20.dp
                                        )
                                )

                            } else {

                                Text(
                                    text =
                                        step.number
                                            .toString(),
                                    color =
                                        if (active) {
                                            Color.White
                                        } else {
                                            PatientColors
                                                .TextSecondary
                                        },
                                    fontSize =
                                        12.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold
                                )
                            }
                        }
                    }


                    /*
                     * Connector between steps
                     */
                    if (
                        index <
                        BookingStep.entries.lastIndex
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(
                                        3.dp
                                    )
                                    .background(
                                        if (
                                            step.number <
                                            activeStep.number
                                        ) {
                                            PatientColors
                                                .SuccessAccent
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .surfaceVariant
                                        },
                                        RoundedCornerShape(
                                            10.dp
                                        )
                                    )
                        )
                    }
                }
        }


        Spacer(
            modifier =
                Modifier.height(8.dp)
        )


        /*
         * Labels
         */
        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            BookingStep.entries
                .forEach { step ->

                    val completed =
                        step.number <
                                activeStep.number

                    val active =
                        step == activeStep


                    Text(
                        text =
                            step.title,
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        textAlign =
                            TextAlign.Center,
                        color =
                            when {

                                completed ->
                                    PatientColors
                                        .SuccessAccent

                                active ->
                                    PatientColors
                                        .AppointmentAccent

                                else ->
                                    PatientColors
                                        .TextSecondary
                            },
                        fontSize =
                            9.5.sp,
                        fontWeight =
                            if (
                                active ||
                                completed
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                    )
                }
        }


        Spacer(
            modifier =
                Modifier.height(5.dp)
        )


        /*
         * Active step message
         */
        Text(
            text =
                when (activeStep) {

                    BookingStep.DATE ->
                        "Start by choosing an appointment date."

                    BookingStep.TIME ->
                        "Now select an available time."

                    BookingStep.TYPE ->
                        "Choose how you would like to attend."

                    BookingStep.DETAILS ->
                        "Almost done — add your visit details."
                },
            modifier =
                Modifier.fillMaxWidth(),
            color =
                PatientColors.TextSecondary,
            fontSize =
                10.5.sp,
            textAlign =
                TextAlign.Center
        )
    }
}

@Composable
private fun DateSection(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {

    val context =
        androidx.compose.ui.platform
            .LocalContext
            .current

    val calendar =
        remember {
            Calendar.getInstance()
        }

    BookingSectionCard(
        icon =
            Icons.Outlined.CalendarMonth,
        title =
            "Choose a date",
        subtitle =
            "Select when you would like to see the doctor."
    ) {

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {

                        DatePickerDialog(
                            context,

                            { _, year, month, day ->

                                onDateSelected(
                                    "%04d-%02d-%02d"
                                        .format(
                                            year,
                                            month + 1,
                                            day
                                        )
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
                                    System.currentTimeMillis() -
                                            1000
                            }
                            .show()
                    },
            shape =
                RoundedCornerShape(
                    18.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (
                            selectedDate.isBlank()
                        ) {
                            PatientColors
                                .AppointmentCard
                                .copy(
                                    alpha = 0.55f
                                )
                        } else {
                            PatientColors
                                .SuccessCard
                        }
                ),
            border =
                BorderStroke(
                    width = 1.dp,
                    color =
                        if (
                            selectedDate.isBlank()
                        ) {
                            PatientColors
                                .AppointmentAccent
                                .copy(
                                    alpha = 0.15f
                                )
                        } else {
                            PatientColors
                                .SuccessAccent
                                .copy(
                                    alpha = 0.18f
                                )
                        }
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            15.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                44.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    13.dp
                                )
                            )
                            .background(
                                if (
                                    selectedDate.isBlank()
                                ) {
                                    PatientColors
                                        .AppointmentAccent
                                        .copy(
                                            alpha = 0.12f
                                        )
                                } else {
                                    PatientColors
                                        .SuccessAccent
                                        .copy(
                                            alpha = 0.12f
                                        )
                                }
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            if (
                                selectedDate.isBlank()
                            ) {
                                Icons.Outlined.CalendarMonth
                            } else {
                                Icons.Filled.CheckCircle
                            },
                        contentDescription =
                            null,
                        tint =
                            if (
                                selectedDate.isBlank()
                            ) {
                                PatientColors
                                    .AppointmentAccent
                            } else {
                                PatientColors
                                    .SuccessAccent
                            },
                        modifier =
                            Modifier.size(
                                21.dp
                            )
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
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            if (
                                selectedDate.isBlank()
                            ) {
                                "Select appointment date"
                            } else {
                                selectedDate
                            },
                        color =
                            PatientColors
                                .TextPrimary,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.5.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )


                    Text(
                        text =
                            if (
                                selectedDate.isBlank()
                            ) {
                                "Tap to open the calendar"
                            } else {
                                "Selected • Tap to change"
                            },
                        color =
                            PatientColors
                                .TextSecondary,
                        fontSize =
                            10.5.sp
                    )
                }


                Text(
                    text =
                        if (
                            selectedDate.isBlank()
                        ) {
                            "Choose"
                        } else {
                            "Change"
                        },
                    color =
                        if (
                            selectedDate.isBlank()
                        ) {
                            PatientColors
                                .AppointmentAccent
                        } else {
                            PatientColors
                                .SuccessAccent
                        },
                    fontSize =
                        10.5.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SlotSection(
    selectedDate: String,
    slots: List<AvailableSlot>,
    alternativeSlots: List<AvailableSlot>,
    selectedSlot: AvailableSlot?,
    isLoading: Boolean,
    isLoadingAlternatives: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSelect: (AvailableSlot) -> Unit,
    onSelectAlternative: (AvailableSlot) -> Unit
) {

    BookingSectionCard(
        icon =
            Icons.Outlined.Schedule,
        title =
            "Available times",
        subtitle =
            "Live availability for $selectedDate"
    ) {

        when {

            isLoading -> {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 18.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                27.dp
                            ),
                        strokeWidth =
                            2.5.dp,
                        color =
                            PatientColors
                                .AppointmentAccent
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                9.dp
                            )
                    )


                    Text(
                        text =
                            "Checking available times...",
                        color =
                            PatientColors
                                .TextSecondary,
                        fontSize =
                            11.5.sp
                    )
                }
            }


            error != null -> {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            17.dp
                        ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                PatientColors.RedSoft
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            PatientColors.Red
                                .copy(
                                    alpha = 0.12f
                                )
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

                        Icon(
                            imageVector =
                                Icons.Outlined.Info,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Red,
                            modifier =
                                Modifier.size(
                                    26.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    7.dp
                                )
                        )


                        Text(
                            text =
                                "Couldn't load available times",
                            color =
                                PatientColors
                                    .TextPrimary,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                13.sp,
                            textAlign =
                                TextAlign.Center
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    4.dp
                                )
                        )


                        Text(
                            text =
                                "Please try loading the doctor's availability again.",
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                10.5.sp,
                            textAlign =
                                TextAlign.Center
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )


                        FilledTonalButton(
                            onClick =
                                onRetry
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Filled.Refresh,
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
                                    "Try again",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }
            }


            slots.isEmpty() -> {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Schedule,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        modifier =
                            Modifier.size(
                                34.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )

                    Text(
                        text =
                            "No available slots on $selectedDate",
                        fontWeight =
                            FontWeight.Bold,
                        textAlign =
                            TextAlign.Center
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )

                    Text(
                        text =
                            "This doctor is not available on this date.",
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        fontSize =
                            12.sp,
                        textAlign =
                            TextAlign.Center
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )


                    when {

                        isLoadingAlternatives -> {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        26.dp
                                    ),
                                strokeWidth =
                                    2.5.dp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )

                            Text(
                                text =
                                    "Finding the next available appointments...",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                fontSize =
                                    11.5.sp,
                                textAlign =
                                    TextAlign.Center
                            )
                        }


                        alternativeSlots.isEmpty() -> {

                            Text(
                                text =
                                    "This doctor currently has no other published appointment slots.",
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                                fontSize =
                                    12.sp,
                                textAlign =
                                    TextAlign.Center
                            )
                        }


                        else -> {

                            HorizontalDivider()

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        14.dp
                                    )
                            )


                            Text(
                                text =
                                    "Next available appointments",
                                modifier =
                                    Modifier.fillMaxWidth(),
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize =
                                    13.5.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )


                            alternativeSlots
                                .groupBy {
                                    it.date
                                }
                                .toSortedMap()
                                .forEach { (date, dateSlots) ->

                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    bottom =
                                                        14.dp
                                                )
                                    ) {

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
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary,
                                                modifier =
                                                    Modifier.size(
                                                        18.dp
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
                                                    date,
                                                fontWeight =
                                                    FontWeight.SemiBold,
                                                fontSize =
                                                    12.5.sp
                                            )
                                        }


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
                                                    .horizontalScroll(
                                                        rememberScrollState()
                                                    ),
                                            horizontalArrangement =
                                                Arrangement.spacedBy(
                                                    8.dp
                                                )
                                        ) {

                                            dateSlots
                                                .sortedBy {
                                                    it.time
                                                }
                                                .forEach { slot ->

                                                    Surface(
                                                        modifier =
                                                            Modifier.clickable {

                                                                onSelectAlternative(
                                                                    slot
                                                                )
                                                            },
                                                        shape =
                                                            RoundedCornerShape(
                                                                10.dp
                                                            ),
                                                        color =
                                                            MaterialTheme
                                                                .colorScheme
                                                                .primaryContainer
                                                    ) {

                                                        Text(
                                                            text =
                                                                displayTime(
                                                                    slot.time
                                                                ),
                                                            modifier =
                                                                Modifier.padding(
                                                                    horizontal =
                                                                        14.dp,
                                                                    vertical =
                                                                        10.dp
                                                                ),
                                                            color =
                                                                MaterialTheme
                                                                    .colorScheme
                                                                    .onPrimaryContainer,
                                                            fontWeight =
                                                                FontWeight.SemiBold,
                                                            fontSize =
                                                                12.sp
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


            else -> {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            9.dp
                        )
                ) {

                    Text(
                        text =
                            "${slots.size} available time${
                                if (slots.size == 1) {
                                    ""
                                } else {
                                    "s"
                                }
                            }",
                        color =
                            PatientColors.SuccessAccent,
                        fontSize =
                            10.5.sp,
                        fontWeight =
                            FontWeight.Bold
                    )


                    slots
                        .sortedBy {
                            it.time
                        }
                        .forEach { slot ->

                            SlotOption(
                                slot =
                                    slot,
                                selected =
                                    selectedSlot
                                        ?.id ==
                                            slot.id,
                                onClick = {
                                    onSelect(slot)
                                }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun SlotOption(
    slot: AvailableSlot,
    selected: Boolean,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        PatientColors
                            .AppointmentCard
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surface
                    }
            ),
        border =
            BorderStroke(
                width =
                    if (selected) {
                        1.5.dp
                    } else {
                        1.dp
                    },
                color =
                    if (selected) {
                        PatientColors
                            .AppointmentAccent
                    } else {
                        PatientColors
                            .AppointmentAccent
                            .copy(
                                alpha = 0.10f
                            )
                    }
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (selected) {
                        2.dp
                    } else {
                        0.dp
                    }
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 13.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            37.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            if (selected) {
                                PatientColors
                                    .AppointmentAccent
                            } else {
                                PatientColors
                                    .AppointmentAccent
                                    .copy(
                                        alpha = 0.10f
                                    )
                            }
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        if (selected) {
                            Icons.Filled.CheckCircle
                        } else {
                            Icons.Outlined.Schedule
                        },
                    contentDescription =
                        null,
                    tint =
                        if (selected) {
                            Color.White
                        } else {
                            PatientColors
                                .AppointmentAccent
                        },
                    modifier =
                        Modifier.size(
                            18.dp
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
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        displayTime(
                            slot.time
                        ),
                    color =
                        PatientColors
                            .TextPrimary,
                    fontWeight =
                        FontWeight.ExtraBold,
                    fontSize =
                        14.5.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(
                    text =
                        "Available",
                    color =
                        PatientColors
                            .SuccessAccent,
                    fontSize =
                        10.sp,
                    fontWeight =
                        FontWeight.SemiBold
                )
            }


            if (selected) {

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
                                    .AppointmentAccent
                                    .copy(
                                        alpha = 0.11f
                                    )
                            )
                            .padding(
                                horizontal = 9.dp,
                                vertical = 5.dp
                            )
                ) {

                    Text(
                        text =
                            "Selected",
                        color =
                            PatientColors
                                .AppointmentAccent,
                        fontSize =
                            9.5.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitTypeSection(
    selected: AppointmentType?,
    onSelect: (AppointmentType) -> Unit
) {

    BookingSectionCard(
        icon =
            Icons.Outlined.VideoCall,
        title =
            "How will you attend?",
        subtitle =
            "Choose the consultation format that works for you."
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    11.dp
                )
        ) {

            VisitTypeOption(
                icon =
                    Icons.Outlined.LocationOn,

                title =
                    "In-person visit",

                description =
                    "Attend the doctor's practice at your selected date and time.",

                badge =
                    "AT PRACTICE",

                selected =
                    selected ==
                            AppointmentType.IN_PERSON,

                accent =
                    PatientColors.DoctorAccent,

                background =
                    PatientColors.DoctorCard,

                onClick = {
                    onSelect(
                        AppointmentType.IN_PERSON
                    )
                }
            )


            VisitTypeOption(
                icon =
                    Icons.Outlined.VideoCall,

                title =
                    "Online consultation",

                description =
                    "Join your scheduled video consultation directly from EHealthy.",

                badge =
                    "VIDEO CALL",

                selected =
                    selected ==
                            AppointmentType.ONLINE,

                accent =
                    PatientColors.AppointmentAccent,

                background =
                    PatientColors.AppointmentCard,

                onClick = {
                    onSelect(
                        AppointmentType.ONLINE
                    )
                }
            )
        }
    }
}

@Composable
private fun VisitTypeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    badge: String,
    selected: Boolean,
    accent: Color,
    background: Color,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        background
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surface
                    }
            ),
        border =
            BorderStroke(
                width =
                    if (selected) {
                        1.5.dp
                    } else {
                        1.dp
                    },
                color =
                    if (selected) {
                        accent
                    } else {
                        accent.copy(
                            alpha = 0.10f
                        )
                    }
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    if (selected) {
                        2.dp
                    } else {
                        0.dp
                    }
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * Consultation icon
             */
            Box(
                modifier =
                    Modifier
                        .size(
                            47.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(
                            accent.copy(
                                alpha =
                                    if (selected) {
                                        0.16f
                                    } else {
                                        0.09f
                                    }
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
                            23.dp
                        )
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

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            title,
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            13.5.sp,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )


                    Box(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    accent.copy(
                                        alpha = 0.10f
                                    )
                                )
                                .padding(
                                    horizontal = 7.dp,
                                    vertical = 4.dp
                                )
                    ) {

                        Text(
                            text =
                                badge,
                            color =
                                accent,
                            fontSize =
                                8.sp,
                            fontWeight =
                                FontWeight.ExtraBold,
                            letterSpacing =
                                0.4.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )


                Text(
                    text =
                        description,
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        11.sp,
                    lineHeight =
                        16.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )


            /*
             * Selection indicator
             */
            Box(
                modifier =
                    Modifier
                        .size(
                            25.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            if (selected) {
                                accent
                            } else {
                                accent.copy(
                                    alpha = 0.08f
                                )
                            }
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                if (selected) {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            "Selected",
                        tint =
                            Color.White,
                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )

                } else {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    8.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    accent.copy(
                                        alpha = 0.30f
                                    )
                                )
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasonSection(
    reason: String,
    onReasonChange: (String) -> Unit
) {

    BookingSectionCard(
        icon =
            Icons.Outlined.Info,
        title =
            "Reason for visit",
        subtitle =
            "Give the doctor a short description before the appointment."
    ) {

        val isValid =
            reason.trim().length >= 3


        OutlinedTextField(
            value =
                reason,

            onValueChange =
                onReasonChange,

            modifier =
                Modifier.fillMaxWidth(),

            placeholder = {

                Text(
                    text =
                        "Example: recurring headache for three days",
                    color =
                        PatientColors.TextSecondary
                            .copy(alpha = 0.75f),
                    fontSize =
                        12.sp
                )
            },

            minLines =
                4,

            maxLines =
                6,

            shape =
                RoundedCornerShape(
                    18.dp
                ),

            leadingIcon = {

                Box(
                    modifier =
                        Modifier
                            .padding(
                                start = 4.dp
                            )
                            .size(
                                36.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    11.dp
                                )
                            )
                            .background(
                                PatientColors.AppointmentAccent
                                    .copy(
                                        alpha = 0.09f
                                    )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Info,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.AppointmentAccent,
                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )
                }
            },

            colors =
                OutlinedTextFieldDefaults.colors(

                    focusedBorderColor =
                        if (isValid) {
                            PatientColors.SuccessAccent
                        } else {
                            PatientColors.AppointmentAccent
                        },

                    unfocusedBorderColor =
                        PatientColors.AppointmentAccent
                            .copy(alpha = 0.15f),

                    focusedContainerColor =
                        PatientColors.AppointmentCard
                            .copy(alpha = 0.35f),

                    unfocusedContainerColor =
                        MaterialTheme
                            .colorScheme
                            .surface,

                    cursorColor =
                        PatientColors.AppointmentAccent
                ),

            supportingText = {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    if (
                        reason.isNotBlank()
                    ) {

                        Text(
                            text =
                                if (isValid) {
                                    "Looks good"
                                } else {
                                    "Add a little more detail"
                                },
                            color =
                                if (isValid) {
                                    PatientColors.SuccessAccent
                                } else {
                                    PatientColors.ReviewAccent
                                },
                            fontSize =
                                10.sp,
                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.weight(1f)
                    )


                    Text(
                        text =
                            "${reason.length}/500",
                        color =
                            if (
                                reason.length >= 450
                            ) {
                                PatientColors.ReviewAccent
                            } else {
                                PatientColors.TextSecondary
                            },
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.Medium
                    )
                }
            }
        )


        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )


        /*
         * Privacy notice
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
                        PatientColors.PurpleSoft
                    )
                    .padding(
                        12.dp
                    ),
            verticalAlignment =
                Alignment.Top
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            30.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            PatientColors.Purple
                                .copy(alpha = 0.11f)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Lock,
                    contentDescription =
                        null,
                    tint =
                        PatientColors.Purple,
                    modifier =
                        Modifier.size(
                            15.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "Keep it relevant",
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        11.5.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(
                    text =
                        "Describe only what the doctor needs for this appointment. Avoid passwords, banking details or unrelated private information.",
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
}

@Composable
private fun PriceInformationCard(
    doctor: DoctorBookingInfo
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.AppointmentCard
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.AppointmentAccent
                        .copy(alpha = 0.12f)
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
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
                                43.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    13.dp
                                )
                            )
                            .background(
                                PatientColors.AppointmentAccent
                                    .copy(alpha = 0.11f)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Payments,
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
                            11.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Consultation fee",
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.5.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )


                    Text(
                        text =
                            "Estimated booking amount",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp
                    )
                }


                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            doctor.hourlyRate
                                ?.let {
                                    "R %.2f".format(it)
                                }
                                ?: "—",
                        color =
                            PatientColors.AppointmentAccent,
                        fontSize =
                            21.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Text(
                        text =
                            "estimate",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            9.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        13.dp
                    )
            )


            HorizontalDivider(
                color =
                    PatientColors.AppointmentAccent
                        .copy(alpha = 0.10f)
            )


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Info,
                    contentDescription =
                        null,
                    tint =
                        PatientColors.AppointmentAccent,
                    modifier =
                        Modifier.size(
                            16.dp
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
                        "The final booking price is confirmed by the server when your appointment is reserved.",
                    modifier =
                        Modifier.weight(1f),
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
}

@Composable
private fun SecureBookingNotice() {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.PurpleSoft
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.Purple
                        .copy(alpha = 0.10f)
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        14.dp
                    ),
            verticalAlignment =
                Alignment.Top
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            38.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .background(
                            PatientColors.Purple
                                .copy(alpha = 0.11f)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Lock,
                    contentDescription =
                        null,
                    tint =
                        PatientColors.Purple,
                    modifier =
                        Modifier.size(
                            19.dp
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
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "Secure reservation",
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        12.5.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )


                Text(
                    text =
                        "When you reserve, the server locks the selected slot before creating your appointment so two patients cannot book the same time.",
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
}

@Composable
private fun BookingBottomBar(
    doctor: DoctorBookingInfo,
    selectedSlot: AvailableSlot?,
    appointmentType: AppointmentType?,
    isBooking: Boolean,
    onBook: () -> Unit
) {

    val readyToReserve =
        selectedSlot != null &&
                appointmentType != null


    Surface(
        color =
            MaterialTheme.colorScheme.surface,
        shadowElevation =
            14.dp,
        tonalElevation =
            3.dp
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
        ) {

            /*
             * Booking summary
             */
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
                                39.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .background(
                                if (readyToReserve) {
                                    PatientColors.SuccessCard
                                } else {
                                    PatientColors.AppointmentCard
                                }
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            if (readyToReserve) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.Schedule
                            },
                        contentDescription =
                            null,
                        tint =
                            if (readyToReserve) {
                                PatientColors.SuccessAccent
                            } else {
                                PatientColors.AppointmentAccent
                            },
                        modifier =
                            Modifier.size(
                                19.dp
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
                            when {

                                selectedSlot == null ->
                                    "Choose an appointment time"

                                appointmentType == null ->
                                    displayTime(
                                        selectedSlot.time
                                    )

                                else ->
                                    "${
                                        displayTime(
                                            selectedSlot.time
                                        )
                                    } • ${
                                        if (
                                            appointmentType ==
                                            AppointmentType.ONLINE
                                        ) {
                                            "Online"
                                        } else {
                                            "In person"
                                        }
                                    }"
                            },
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.5.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )


                    Text(
                        text =
                            doctor.hourlyRate
                                ?.let {
                                    "Estimated fee • R %.2f"
                                        .format(it)
                                }
                                ?: "Fee confirmed during reservation",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.sp
                    )
                }


                Box(
                    modifier =
                        Modifier
                            .size(
                                31.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                PatientColors.PurpleSoft
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Lock,
                        contentDescription =
                            "Secure booking",
                        tint =
                            PatientColors.Purple,
                        modifier =
                            Modifier.size(
                                15.dp
                            )
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            /*
             * Main reserve button
             */
            Button(
                onClick =
                    onBook,

                enabled =
                    !isBooking &&
                            readyToReserve,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            55.dp
                        ),

                shape =
                    RoundedCornerShape(
                        16.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            PatientColors.AppointmentAccent,

                        contentColor =
                            Color.White,

                        disabledContainerColor =
                            PatientColors.AppointmentAccent
                                .copy(alpha = 0.18f),

                        disabledContentColor =
                            PatientColors.TextSecondary
                    )
            ) {

                if (isBooking) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                20.dp
                            ),
                        strokeWidth =
                            2.dp,
                        color =
                            Color.White
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )


                    Text(
                        text =
                            "Reserving your appointment...",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            12.5.sp
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Outlined.Lock,
                        contentDescription =
                            null,
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
                            if (readyToReserve) {
                                "Reserve appointment"
                            } else {
                                "Complete appointment details"
                            },
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.sp
                    )
                }
            }


            if (readyToReserve) {

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        "Your selected slot will be checked again when you reserve.",
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        9.5.sp,
                    textAlign =
                        TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BookingSuccessScreen(
    modifier: Modifier,
    doctor: DoctorBookingInfo,
    result: ReserveSlotResult,
    appointmentType: AppointmentType,
    onDone: () -> Unit,
    onBookAnother: () -> Unit
) {

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    16.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    28.dp
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
                        PatientColors.SuccessAccent
                            .copy(alpha = 0.10f)
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 4.dp
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

                /*
                 * Success illustration
                 */
                Box(
                    modifier =
                        Modifier
                            .size(
                                88.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                PatientColors.SuccessCard
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    64.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    PatientColors.SuccessAccent
                                        .copy(alpha = 0.12f)
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.CheckCircle,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.SuccessAccent,
                            modifier =
                                Modifier.size(
                                    43.dp
                                )
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            15.dp
                        )
                )


                Text(
                    text =
                        "Appointment reserved",
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        22.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
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
                        "Your appointment slot with ${doctor.fullName} has been secured.",
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        12.sp,
                    lineHeight =
                        17.sp,
                    textAlign =
                        TextAlign.Center
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )


                /*
                 * Doctor summary
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
                                PatientColors.DoctorCard
                        ),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    14.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        43.dp
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        PatientColors.DoctorAccent
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    doctor.name
                                        .take(1)
                                        .uppercase(),
                                color =
                                    Color.White,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                fontSize =
                                    17.sp
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
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    doctor.fullName,
                                color =
                                    PatientColors.TextPrimary,
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize =
                                    13.5.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        2.dp
                                    )
                            )


                            Text(
                                text =
                                    doctor.specialty,
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.5.sp
                            )
                        }


                        Box(
                            modifier =
                                Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            20.dp
                                        )
                                    )
                                    .background(
                                        PatientColors.SuccessCard
                                    )
                                    .padding(
                                        horizontal = 9.dp,
                                        vertical = 5.dp
                                    )
                        ) {

                            Text(
                                text =
                                    result.appointment_status
                                        .replaceFirstChar {
                                            it.uppercase()
                                        },
                                color =
                                    PatientColors.SuccessAccent,
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
                        Modifier.height(
                            12.dp
                        )
                )


                /*
                 * Appointment details
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
                                PatientColors.NeutralCard
                        ),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        )
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                14.dp
                            )
                    ) {

                        Text(
                            text =
                                "Appointment details",
                            color =
                                PatientColors.TextPrimary,
                            fontSize =
                                12.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )


                        ConfirmationRow(
                            label =
                                "Date",
                            value =
                                result.appointment_date
                        )


                        ConfirmationRow(
                            label =
                                "Time",
                            value =
                                displayTime(
                                    result.appointment_time
                                )
                        )


                        ConfirmationRow(
                            label =
                                "Visit",
                            value =
                                if (
                                    appointmentType ==
                                    AppointmentType.ONLINE
                                ) {
                                    "Online consultation"
                                } else {
                                    "In-person appointment"
                                }
                        )


                        ConfirmationRow(
                            label =
                                "Status",
                            value =
                                result.appointment_status
                                    .replaceFirstChar {
                                        it.uppercase()
                                    }
                        )


                        ConfirmationRow(
                            label =
                                "Server price",
                            value =
                                formatMinorAmount(
                                    result.amount_minor,
                                    result.payment_currency
                                )
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
                 * Payment information
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .background(
                                PatientColors.PurpleSoft
                            )
                            .padding(
                                13.dp
                            ),
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    31.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    PatientColors.Purple
                                        .copy(alpha = 0.11f)
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Info,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Purple,
                            modifier =
                                Modifier.size(
                                    16.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Payment not processed yet",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                11.5.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )


                        Text(
                            text =
                                "This reservation does not mean a payment has been processed. Payment will be handled separately when the payment provider is connected.",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp,
                            lineHeight =
                                15.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            17.dp
                        )
                )


                Button(
                    onClick =
                        onDone,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                52.dp
                            ),
                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PatientColors.SuccessAccent,
                            contentColor =
                                Color.White
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                18.dp
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
                            "Done",
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )


                TextButton(
                    onClick =
                        onBookAnother
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
                            "Book another appointment",
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

@Composable
private fun ConfirmationRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 7.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text =
                label,
            modifier =
                Modifier.weight(1f),
            color =
                PatientColors.TextSecondary,
            fontSize =
                11.sp
        )


        Text(
            text =
                value,
            color =
                PatientColors.TextPrimary,
            fontWeight =
                FontWeight.Bold,
            fontSize =
                11.5.sp,
            maxLines =
                1,
            overflow =
                TextOverflow.Ellipsis,
            textAlign =
                TextAlign.End
        )
    }
}

@Composable
private fun BookingSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
                .animateContentSize(),
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
                    PatientColors.AppointmentAccent
                        .copy(alpha = 0.08f)
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
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
                                42.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    13.dp
                                )
                            )
                            .background(
                                PatientColors.AppointmentAccent
                                    .copy(alpha = 0.10f)
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
                            PatientColors.AppointmentAccent,
                        modifier =
                            Modifier.size(
                                20.dp
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
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            title,
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            14.5.sp
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
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp,
                        lineHeight =
                            15.sp
                    )
                }
            }


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
private fun BookingErrorMessage(
    message: String
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(
                17.dp
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

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        13.dp
                    ),
            verticalAlignment =
                Alignment.Top
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            31.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            PatientColors.Red
                                .copy(alpha = 0.10f)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Info,
                    contentDescription =
                        null,
                    tint =
                        PatientColors.Red,
                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        "Booking issue",
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        11.5.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(
                    text =
                        message,
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
}

@Composable
private fun LoadingBookingScreen(
    modifier: Modifier
) {

    Box(
        modifier =
            modifier,
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
                        horizontal = 32.dp,
                        vertical = 27.dp
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
                            .clip(
                                CircleShape
                            )
                            .background(
                                PatientColors.AppointmentAccent
                                    .copy(alpha = 0.09f)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                31.dp
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
                        "Preparing your appointment",
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
                        "Loading doctor information and availability...",
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        10.5.sp,
                    textAlign =
                        TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BookingErrorScreen(
    modifier: Modifier,
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {

    Box(
        modifier =
            modifier
                .padding(
                    20.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    25.dp
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
                        PatientColors.Red
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
                            23.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                70.dp
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                PatientColors.RedSoft
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Info,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.Red,
                        modifier =
                            Modifier.size(
                                31.dp
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
                        "Unable to start booking",
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        18.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    textAlign =
                        TextAlign.Center
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )


                Text(
                    text =
                        message,
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        11.5.sp,
                    lineHeight =
                        17.sp,
                    textAlign =
                        TextAlign.Center
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            17.dp
                        )
                )


                Button(
                    onClick =
                        onRetry,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                50.dp
                            ),
                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PatientColors.AppointmentAccent,
                            contentColor =
                                Color.White
                        )
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
                        text =
                            "Try again",
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
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
                            PatientColors.TextSecondary,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun displayTime(
    value: String
): String {

    val clean =
        value
            .trim()
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
        return value
    }

    val hour =
        parts[0]
            .toIntOrNull()
            ?: return value

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

    val twelveHour =
        when {

            hour == 0 ->
                12

            hour > 12 ->
                hour - 12

            else ->
                hour
        }

    return "$twelveHour:$minute $period"
}

private fun formatMinorAmount(
    amountMinor: Int,
    currency: String
): String {

    val amount =
        amountMinor / 100.0

    return when (
        currency.uppercase()
    ) {

        "ZAR" ->
            "R %.2f".format(
                amount
            )

        else ->
            "$currency %.2f".format(
                amount
            )
    }
}

private fun friendlyBookingError(
    message: String?
): String {

    val original =
        message
            ?.trim()
            .orEmpty()

    return when {

        original.contains(
            "already been booked",
            ignoreCase = true
        ) ->

            "Someone booked this slot just before you. Choose another available time."

        original.contains(
            "past",
            ignoreCase = true
        ) ->

            "That appointment time is no longer available. Choose another slot."

        original.contains(
            "not approved",
            ignoreCase = true
        ) ->

            "This doctor is temporarily unavailable for new appointments."

        original.contains(
            "consultation fee",
            ignoreCase = true
        ) ->

            "This doctor's consultation fee is not configured correctly."

        original.contains(
            "Patient profile",
            ignoreCase = true
        ) ->

            "Your patient profile could not be found. Sign in again and retry."

        original.isNotBlank() ->
            original

        else ->
            "We couldn't reserve your appointment. Please try again."
    }
}