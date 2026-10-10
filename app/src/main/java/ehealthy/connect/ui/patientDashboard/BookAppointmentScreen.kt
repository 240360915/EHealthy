package ehealthy.connect.ui.patientDashboard
import ehealthy.connect.data.patient.BookScheduledAppointmentResult
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
import androidx.compose.material.icons.outlined.Lock
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
 * New scheduled bookings no longer use this object to write appointments.
 * The secure book_scheduled_appointment_demo() RPC performs the booking.
 */
data class BookingSubmission(
    val date: String,
    val time: String,
    val reason: String,
    val fee: Double,
    val paymentReference: String,
    val appointmentType: AppointmentType
)

// A clear patient-facing message when checkout is cancelled or booking fails.
private data class DemoPayNotice(
    val title: String,
    val message: String
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

    DETAILS(
        3,
        "Details"
    )
}

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

    var dateConflictMessage by remember {
        mutableStateOf<String?>(null)
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
        mutableStateOf<BookScheduledAppointmentResult?>(
            null
        )
    }

    // Display the checkout before making any booking request. Cancelling never calls Supabase.
    var showDemoPayCheckout by remember { mutableStateOf(false) }
    var demoPayNotice by remember { mutableStateOf<DemoPayNotice?>(null) }

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
        dateConflictMessage = null
        selectedSlot = null

        availableSlots =
            emptyList()

        alternativeSlots =
            emptyList()


        val alreadyHasAppointment =
            PatientRepository
                .getMyAppointments()
                .getOrNull()
                ?.any { appointment ->

                    appointment.date == date &&

                            (
                                    appointment.status
                                        ?.lowercase()
                                        ?: ""
                                    ) !in setOf(
                        "cancelled",
                        "canceled"
                    )
                }
                ?: false

        if (alreadyHasAppointment) {

            dateConflictMessage =
                "You already have an appointment on $date. " +
                        "EHealthy allows only one appointment per patient per day. " +
                        "Please choose another date."

            isLoadingSlots =
                false

            return
        }

        PatientRepository
            .getAvailableSlots(
                doctorId = currentDoctor.id,
                date = date
            )
            .onSuccess { slots ->

                val openSlots =
                    slots.filter {
                        !it.is_booked &&
                                isFutureSlot(it)
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
                                                isFutureSlot(slot) &&

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

        reason =
            ""

        bookingError =
            null

        bookingRequestId =
            UUID.randomUUID()
                .toString()

        scope.launch {

            loadSlots(
                value
            )
        }
    }

    fun submitBooking() {
        // A single checkout confirmation can create only one booking request.
        if (isBooking || bookingResult != null) return

        val slot = selectedSlot
        if (slot == null) {
            showDemoPayCheckout = false
            bookingError = "Choose an available time."
            demoPayNotice = DemoPayNotice(
                title = "Appointment details needed",
                message = "Please choose an available appointment time before continuing."
            )
            return
        }

        if (reason.trim().length < 3) {
            showDemoPayCheckout = false
            bookingError = "Please briefly tell the doctor why you need the appointment."
            demoPayNotice = DemoPayNotice(
                title = "Appointment details needed",
                message = "Please add a short reason for your consultation before continuing."
            )
            return
        }

        bookingError = null
        isBooking = true

        scope.launch {
            try {
                PatientRepository
                    .bookScheduledAppointment(
                        slotId = slot.id,
                        appointmentType = AppointmentType.ONLINE.databaseValue,
                        paymentChoice = "demo_pay",
                        reason = reason.trim(),
                        idempotencyKey = bookingRequestId
                    )
                    .onSuccess { result ->
                        // The server performs the booking and writes the DemoPay record.
                        bookingResult = result
                        showDemoPayCheckout = false
                    }
                    .onFailure { error ->
                        showDemoPayCheckout = false
                        bookingError = friendlyBookingError(error.message)
                        demoPayNotice = DemoPayNotice(
                            title = "Payment not completed",
                            message = "We couldn't finish your appointment payment and booking. " +
                                    "No real money was charged. Please check your details and try again."
                        )
                    }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                // A network timeout may occur after the server has saved a booking.
                // Ask the patient to check their appointments before retrying.
                showDemoPayCheckout = false
                bookingError = "Could not verify your booking. Check My Appointments before trying again."
                demoPayNotice = DemoPayNotice(
                    title = "Unable to verify payment",
                    message = "We couldn't confirm the booking response. Check My Appointments " +
                            "before trying again, in case the appointment was already saved."
                )
            } finally {
                isBooking = false
            }
        }
    }

    fun cancelCheckout() {
        if (isBooking) return
        showDemoPayCheckout = false
        bookingError = null
        demoPayNotice = DemoPayNotice(
            title = "Payment cancelled",
            message = "You cancelled the checkout. No appointment was created " +
                    "and no payment was recorded. You can return to checkout whenever you're ready."
        )
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

            else ->

                BookingStep.DETAILS
        }

    // Payment is requested only after the patient explicitly taps Complete Payment.
    // The existing backend still uses DemoPay: a virtual transaction, not a bank charge.
    if (showDemoPayCheckout && bookingResult == null) {
        AlertDialog(
            onDismissRequest = {
                if (!isBooking) cancelCheckout()
            },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = PatientColors.AppointmentAccent
                )
            },
            title = {
                Text(
                    "EHealthy Checkout",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Review your appointment details before completing payment.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PatientColors.AppointmentCard
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            Text(
                                doctor?.fullName ?: "Your doctor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            HorizontalDivider(
                                color = PatientColors.AppointmentAccent.copy(alpha = 0.12f)
                            )
                            Text("Date: ${selectedSlot?.date.orEmpty()}")
                            Text("Time: ${selectedSlot?.time?.let(::displayTime).orEmpty()}")
                            Text("Consultation: Online")
                            HorizontalDivider(
                                color = PatientColors.AppointmentAccent.copy(alpha = 0.12f)
                            )
                            Text(
                                "Amount: ${doctor?.hourlyRate?.let { "R %.2f".format(it) } ?: "Confirmed by server"}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = PatientColors.AppointmentAccent
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = PatientColors.Purple
                        )
                        Text(
                            "DemoPay checkout: no actual bank or card payment is taken. " +
                                    "Completing this step records a demonstration payment and " +
                                    "books your appointment, subject to availability. " +
                                    "The final fee is confirmed by the server.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { submitBooking() },
                    enabled = !isBooking
                ) {
                    if (isBooking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(17.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text("Processing...")
                    } else {
                        Text("Complete Payment")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = ::cancelCheckout,
                    enabled = !isBooking
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Cancellation or a failed checkout request must produce immediate visible feedback.
    demoPayNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { demoPayNotice = null },
            title = {
                Text(notice.title, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(notice.message)
            },
            confirmButton = {
                Button(onClick = {
                    demoPayNotice = null
                    showDemoPayCheckout = true
                }) {
                    Text("Try again")
                }
            },
            dismissButton = {
                TextButton(onClick = { demoPayNotice = null }) {
                    Text("Back to booking")
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

                    doctor = doctor!!,

                    selectedSlot = selectedSlot,

                    reasonIsValid =
                        reason.trim().length >= 3,

                    isBooking = isBooking,

                    onBook = {
                        bookingError = null
                        demoPayNotice = null
                        showDemoPayCheckout = true
                    }
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
                            .padding(innerPadding),

                    doctor =
                        doctor!!,

                    result =
                        bookingResult!!,

                    appointmentDate =
                        selectedSlot?.date.orEmpty(),

                    appointmentTime =
                        selectedSlot?.time.orEmpty(),

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

                        alternativeSlots =
                            emptyList()

                        reason =
                            ""

                        bookingError =
                            null

                        dateConflictMessage =
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

                                blockedMessage =
                                    dateConflictMessage,

                                onRetry = {

                                    scope.launch {

                                        loadSlots(
                                            selectedDate
                                        )
                                    }
                                },

                                onSelect = { slot ->

                                    selectedSlot =
                                        slot

                                    bookingError =
                                        null

                                    bookingRequestId =
                                        UUID.randomUUID()
                                            .toString()
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

                                    bookingError =
                                        null

                                    bookingRequestId =
                                        UUID.randomUUID()
                                            .toString()
                                }
                            )
                        }
                    }

                    item {

                        AnimatedVisibility(
                            visible =
                                selectedSlot != null
                        ) {

                            OnlineConsultationNotice()
                        }
                    }

                    item {

                        AnimatedVisibility(
                            visible =
                                selectedSlot != null
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

                                        bookingRequestId =
                                            UUID.randomUUID()
                                                .toString()
                                    }
                                }
                            )
                        }
                    }

                    item {

                        AnimatedVisibility(
                            visible =
                                selectedSlot != null
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
                        "Complete the steps below to reserve your online consultation.",
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
                        "${activeStep.number}/${BookingStep.entries.size}",
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
            "Choose a date for your online consultation. Only one appointment is allowed per day."
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
                            selectedDate.ifBlank {
                                "Select appointment date"
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
    blockedMessage: String?,
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


            blockedMessage != null -> {

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
                                PatientColors.AppointmentCard
                        ),
                    border =
                        BorderStroke(
                            1.dp,
                            PatientColors.AppointmentAccent
                                .copy(
                                    alpha = 0.16f
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
                                PatientColors.AppointmentAccent,
                            modifier =
                                Modifier.size(
                                    27.dp
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
                                "One appointment per day",
                            color =
                                PatientColors.TextPrimary,
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
                                    5.dp
                                )
                        )

                        Text(
                            text =
                                blockedMessage,
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                11.sp,
                            lineHeight =
                                16.sp,
                            textAlign =
                                TextAlign.Center
                        )
                    }
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
private fun OnlineConsultationNotice() {

    BookingSectionCard(
        icon =
            Icons.Outlined.VideoCall,
        title =
            "Online consultation",
        subtitle =
            "All initial EHealthy appointments start online."
    ) {

        Column(
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.VideoCall,
                    contentDescription =
                        null,
                    tint =
                        PatientColors.AppointmentAccent,
                    modifier =
                        Modifier.size(
                            18.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            9.dp
                        )
                )

                Text(
                    text =
                        "You will meet the doctor online at the selected date and time.",
                    modifier =
                        Modifier.weight(1f),
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        11.sp,
                    lineHeight =
                        16.sp
                )
            }

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
                        PatientColors.DoctorAccent,
                    modifier =
                        Modifier.size(
                            18.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            9.dp
                        )
                )

                Text(
                    text =
                        "If a physical examination is needed after the online consultation, you can request an in-person follow-up from the doctor.",
                    modifier =
                        Modifier.weight(1f),
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        11.sp,
                    lineHeight =
                        16.sp
                )
            }

            Row(
                verticalAlignment =
                    Alignment.Top
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Payments,
                    contentDescription =
                        null,
                    tint =
                        PatientColors.Purple,
                    modifier =
                        Modifier.size(
                            18.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            9.dp
                        )
                )

                Text(
                    text =
                        "Online bookings currently use EHealthy DemoPay. These are demonstration payments; no real money is charged.",
                    modifier =
                        Modifier.weight(1f),
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        11.sp,
                    lineHeight =
                        16.sp
                )
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
                        "When you complete payment, the server checks the selected slot and makes sure you do not already have an appointment that day.",
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
    reasonIsValid: Boolean,
    isBooking: Boolean,
    onBook: () -> Unit
){

    val readyToReserve =
        selectedSlot != null &&
                reasonIsValid


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
                            if (
                                selectedSlot == null
                            ) {
                                "Choose an appointment time"
                            } else {
                                "${
                                    displayTime(
                                        selectedSlot.time
                                    )
                                } • Online"
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
                            "Processing your appointment...",
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
                                "Pay for Appointment"
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
                        "Your selected time is confirmed when you complete payment.",
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
    result: BookScheduledAppointmentResult,
    appointmentDate: String,
    appointmentTime: String,
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
                        "Appointment booked successfully",
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
                        "Your online appointment with ${doctor.fullName} has been secured.",
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
                                appointmentDate
                        )


                        ConfirmationRow(
                            label =
                                "Time",
                            value =
                                displayTime(
                                    appointmentTime
                                )
                        )


                        ConfirmationRow(
                            label =
                                "Visit",
                            value =
                                "Online consultation"
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
                                if (
                                    result.currency.uppercase() == "ZAR"
                                ) {
                                    "R %.2f".format(result.amount)
                                } else {
                                    "${result.currency} %.2f".format(result.amount)
                                }

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
                                RoundedCornerShape(16.dp)
                            )
                            .background(
                                PatientColors.PurpleSoft
                            )
                            .padding(13.dp),

                    verticalAlignment =
                        Alignment.Top
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(31.dp)
                                .clip(CircleShape)
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
                                Modifier.size(16.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.width(9.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                if (
                                    result.payment_status.equals(
                                        "paid",
                                        ignoreCase = true
                                    )
                                ) {
                                    "Payment completed with DemoPay"
                                } else {
                                    "Payment status: ${
                                        result.payment_status
                                            .replaceFirstChar {
                                                it.uppercase()
                                            }
                                    }"
                                },

                            color =
                                PatientColors.TextPrimary,

                            fontWeight =
                                FontWeight.Bold,

                            fontSize =
                                11.5.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(2.dp)
                        )

                        Text(
                            text =
                                "Payment reference: ${result.payment_reference}. " +
                                        "DemoPay records a demonstration payment; " +
                                        "no real money was charged.",

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

    val friendlyMessage =
        when {

            message.contains(
                "physical visit arrangement",
                ignoreCase = true
            ) -> {
                "You already have a physical visit arranged for this date. " +
                        "Please choose another date or review your existing visit."
            }

            message.contains(
                "one appointment per day",
                ignoreCase = true
            ) ||
                    message.contains(
                        "already have an appointment",
                        ignoreCase = true
                    ) -> {
                "You already have an appointment on this date. " +
                        "Only one active appointment per day is allowed."
            }

            message.contains(
                "time slot",
                ignoreCase = true
            ) &&
                    message.contains(
                        "booked",
                        ignoreCase = true
                    ) -> {
                "This time slot is no longer available. Please choose another time."
            }

            message.contains(
                "already passed",
                ignoreCase = true
            ) -> {
                "This appointment time has already passed. Please choose another date or time."
            }

            else -> {
                "We couldn't complete your booking. Please try again or choose another appointment time."
            }
        }

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
                    text = friendlyMessage,
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

private fun isFutureSlot(
    slot: AvailableSlot
): Boolean {

    val dateParts =
        slot.date
            .trim()
            .split("-")

    val timeParts =
        slot.time
            .trim()
            .split(":")

    if (
        dateParts.size < 3 ||
        timeParts.size < 2
    ) {
        // Do not hide a slot if its format is unexpected.
        // The server still performs the final time validation.
        return true
    }

    val year =
        dateParts[0]
            .toIntOrNull()
            ?: return true

    val month =
        dateParts[1]
            .toIntOrNull()
            ?: return true

    val day =
        dateParts[2]
            .toIntOrNull()
            ?: return true

    val hour =
        timeParts[0]
            .toIntOrNull()
            ?: return true

    val minute =
        timeParts[1]
            .toIntOrNull()
            ?: return true

    val second =
        timeParts
            .getOrNull(2)
            ?.toIntOrNull()
            ?: 0

    val johannesburgTimeZone =
        java.util.TimeZone.getTimeZone(
            "Africa/Johannesburg"
        )

    val slotCalendar =
        Calendar.getInstance(
            johannesburgTimeZone
        ).apply {

            clear()

            set(
                year,
                month - 1,
                day,
                hour,
                minute,
                second
            )
        }

    val now =
        Calendar.getInstance(
            johannesburgTimeZone
        )

    return slotCalendar.timeInMillis >
            now.timeInMillis
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

        original.contains(
            "one appointment per day",
            ignoreCase = true
        ) ||
                original.contains(
                    "already have an appointment",
                    ignoreCase = true
                ) ->

            "You already have an appointment on this date. " +
                    "You can only have one appointment per day. " +
                    "Please choose another date."

        original.isNotBlank() ->
            original

        else ->
            "We couldn't reserve your appointment. Please try again."
    }
}