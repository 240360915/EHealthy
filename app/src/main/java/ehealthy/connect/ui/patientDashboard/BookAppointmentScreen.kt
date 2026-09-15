package ehealthy.connect.ui.patientDashboard

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
data class DoctorBookingInfo(
    val id: String,
    val name: String,
    val surname: String,
    val discipline: String? = null,
    val hourlyRate: Double? = null,
    val operatingHours: String? = null
)

data class BookingSubmission(
    val date: String,
    val time: String,
    val reason: String,
    val fee: Double,
    val paymentReference: String
)
// South African public holidays. Verify exact dates each year —
// Easter-based holidays (Good Friday / Family Day) shift annually.
private val publicHolidays = setOf(
    "2026-01-01", // New Year's Day
    "2026-03-21", // Human Rights Day
    "2026-04-03", // Good Friday
    "2026-04-06", // Family Day
    "2026-04-27", // Freedom Day
    "2026-05-01", // Workers' Day
    "2026-06-16", // Youth Day
    "2026-08-09", // National Women's Day
    "2026-09-24", // Heritage Day
    "2026-12-16", // Day of Reconciliation
    "2026-12-25", // Christmas Day
    "2026-12-26"  // Day of Goodwill
)
private val navy = Color(0xFF0B1828)
private val ink = Color(0xFF0F1F3D)
private val muted = Color(0xFF64748B)
private val bg = Color(0xFFF4F7FA)
private val teal = Color(0xFF0D9488)
private val tealSoft = Color(0xFFCCFBF1)
private val red = Color(0xFFDC2626)

private val timeSlotOptions =
    listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    isLoading: Boolean,
    errorMessage: String?,
    fetchDoctor: suspend () -> Result<DoctorBookingInfo>,
    fetchBookedTimes: suspend (date: String) -> Result<Set<String>>,
    onBack: () -> Unit,
    onConfirmBooking: (BookingSubmission) -> Unit
) {
    var doctor by remember { mutableStateOf<DoctorBookingInfo?>(null) }
    var isLoadingDoctor by rememberSaveable { mutableStateOf(true) }
    var doctorLoadError by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedDate by rememberSaveable { mutableStateOf("") }
    var bookedTimes by rememberSaveable { mutableStateOf(setOf<String>()) }
    var isLoadingSlots by rememberSaveable { mutableStateOf(false) }
    var selectedTime by rememberSaveable { mutableStateOf<String?>(null) }

    var reason by rememberSaveable { mutableStateOf("") }
    var fee by rememberSaveable { mutableStateOf("") }
    var cardName by rememberSaveable { mutableStateOf("") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var cardExpiry by rememberSaveable { mutableStateOf("") }
    var cardCvv by rememberSaveable { mutableStateOf("") }
    var selectedBank by rememberSaveable { mutableStateOf<String?>(null) }
    var bankMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val banks = listOf(
        "Capitec",
        "FNB",
        "ABSA",
        "Standard Bank",
        "TymeBank",
        "African Bank",
        "Discovery Bank",
        "Investec"
    )

    LaunchedEffect(Unit) {
        val result = fetchDoctor()
        isLoadingDoctor = false
        result
            .onSuccess {
                doctor = it
                if (fee.isBlank()) it.hourlyRate?.let { rate -> fee = "%.2f".format(rate) }
            }
            .onFailure { doctorLoadError = it.message ?: "Could not load doctor details." }
    }

    fun onDateSelected(date: String) {
        selectedDate = date
        selectedTime = null
        bookedTimes = emptySet()
        isLoadingSlots = true
    }

    LaunchedEffect(selectedDate) {
        if (selectedDate.isNotBlank()) {
            val result = fetchBookedTimes(selectedDate)
            isLoadingSlots = false
            result.onSuccess { bookedTimes = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Book Appointment",
                        color = ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (selectedTime != null && doctor != null) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    (localError ?: errorMessage)?.let {
                        Text(it, color = red, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(tealSoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total amount", color = teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "R %.2f".format(fee.toDoubleOrNull() ?: 0.0),
                            color = teal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            localError = null
                            val cleanCardNumber = cardNumber.replace(" ", "")
                            val feeAmount = fee.toDoubleOrNull()
                            when {
                                reason.isBlank() -> localError = "Please enter a reason for your visit."
                                feeAmount == null || feeAmount <= 0 -> localError = "Please enter a valid consultation fee."
                                cardName.isBlank() -> localError = "Please enter the cardholder name."
                                cleanCardNumber.length < 16 -> localError = "Please enter a valid card number."
                                !cardExpiry.matches(Regex("(0[1-9]|1[0-2])/\\d{2}")) -> localError = "Please enter a valid expiry (MM/YY)."
                                isCardExpired(cardExpiry) -> localError = "This card has expired."
                                cardCvv.length < 3 -> localError = "Please enter the CVV."
                                selectedBank == null -> localError = "Please select your bank."
                                else -> {
                                    val reference = "$selectedBank ••••${cleanCardNumber.takeLast(4)}"
                                    onConfirmBooking(
                                        BookingSubmission(
                                            date = selectedDate,
                                            time = selectedTime!!,
                                            reason = reason,
                                            fee = feeAmount,
                                            paymentReference = reference
                                        )
                                    )
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = navy),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Confirm & Pay Booking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        },
        containerColor = bg

    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            when {
                isLoadingDoctor -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = teal)
                    }
                }

                doctorLoadError != null -> {
                    Text(
                        doctorLoadError ?: "",
                        color = red,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                doctor != null -> {
                    val doc = doctor!!

                    DoctorBanner(doc)

                    // ---- DATE ----
                    SectionCard(icon = Icons.Outlined.CalendarMonth, title = "Choose a Date", ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedDate.isBlank()) "" else selectedDate,
                                onValueChange = {},
                                enabled = false,
                                placeholder = { Text("Select appointment date") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = ink,
                                    disabledBorderColor = Color(0xFFCBD5E1),
                                    disabledContainerColor = Color.Transparent,
                                    disabledPlaceholderColor = muted,
                                    disabledTrailingIconColor = muted
                                ),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    Icon(
                                        Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        tint = muted
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        DatePickerDialog(
                                            context,
                                            { _, year, month, day ->
                                                val dateStr = "%04d-%02d-%02d".format(year, month + 1, day)
                                                val picked = Calendar.getInstance().apply { set(year, month, day) }
                                                val dayOfWeek = picked.get(Calendar.DAY_OF_WEEK)
                                                when {
                                                    dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY ->
                                                        dateError = "Doctors aren't available on weekends — please pick a weekday."
                                                    publicHolidays.contains(dateStr) ->
                                                        dateError = "That date is a public holiday — please pick another day."
                                                    else -> {
                                                        dateError = null
                                                        selectedDate = dateStr
                                                        selectedTime = null
                                                    }
                                                }
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        ).apply {
                                            datePicker.minDate = System.currentTimeMillis() - 1000
                                        }.show()
                                    }
                            )
                        }
                    }

                    dateError?.let {
                        Text(
                            it,
                            color = red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)

                        )
                    }

                    // ---- SLOTS ----
                    if (selectedDate.isNotBlank()) {
                        SectionCard(
                            icon = Icons.Outlined.Schedule,
                            title = "Available Time Slots"
                        ) {
                            if (isLoadingSlots) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = teal,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Legend()
                                Spacer(modifier = Modifier.height(12.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    modifier = Modifier.height(((timeSlotOptions.size / 4 + 1) * 52).dp)
                                ) {
                                    items(timeSlotOptions) { slot ->
                                        SlotButton(
                                            label = slot,
                                            isTaken = bookedTimes.contains(slot),
                                            isSelected = selectedTime == slot,
                                            onClick = { selectedTime = slot }
                                        )
                                    }
                                }
                                selectedTime?.let {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(tealSoft)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = teal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Selected: $it",
                                            color = teal,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ---- PAYMENT ----
                    if (selectedTime != null) {
                        SectionCard(icon = Icons.Outlined.CreditCard, title = "Payment Details") {
                            CardPreview(cardName, cardNumber, cardExpiry)
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = reason,
                                onValueChange = { reason = it },
                                label = { Text("Reason for visit") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fee,
                                onValueChange = { fee = it },
                                label = { Text("Consultation fee (R)") },
                                singleLine = true,
                                readOnly = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = cardName,
                                onValueChange = { cardName = it },
                                label = { Text("Cardholder name") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = {
                                    if (it.length <= 19) cardNumber = formatCardNumber(it)
                                },
                                label = { Text("Card number") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = {
                                        if (it.length <= 5) cardExpiry = formatExpiry(it)
                                    },
                                    label = { Text("MM/YY") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = {
                                        if (it.length <= 3) cardCvv = it.filter(Char::isDigit)
                                    },
                                    label = { Text("CVV") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Box {
                                OutlinedTextField(
                                    value = selectedBank ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Your bank") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = teal),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { bankMenuExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = bankMenuExpanded,
                                    onDismissRequest = { bankMenuExpanded = false }) {
                                    banks.forEach { bank ->
                                        DropdownMenuItem(
                                            text = { Text(bank) },
                                            onClick = {
                                                selectedBank = bank; bankMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(tealSoft)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = teal,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Funds are only transferred once the doctor confirms your appointment.",
                                    color = teal, fontSize = 12.sp
                                )
                            }
                        }

                        // ---- SUMMARY + CONFIRM ----

                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DoctorBanner(doc: DoctorBookingInfo) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(colors = listOf(navy, Color(0xFF1A3A5C))))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(teal),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${doc.name.firstOrNull() ?: ' '}${doc.surname.firstOrNull() ?: ' '}".uppercase(),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    "Dr. ${doc.name} ${doc.surname}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${doc.discipline ?: "General Practitioner"}${doc.operatingHours?.let { " · $it" } ?: ""}",
                    color = Color.White.copy(alpha = 0.65f), fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x1A0B1828)
            )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = teal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(teal, "Available")
        LegendItem(red, "Booked")
        LegendItem(navy, "Your pick")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = muted, fontSize = 12.sp)
    }
}

@Composable
private fun SlotButton(label: String, isTaken: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val (bgColor, textColor) = when {
        isSelected -> navy to Color.White
        isTaken -> Color(0xFFFEE2E2) to red
        else -> tealSoft to teal
    }
    Box(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(if (!isTaken) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CardPreview(name: String, number: String, expiry: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors = listOf(Color(0xFF1A6C5E), Color(0xFF26A88A))))
            .padding(20.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(34.dp, 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                number.ifBlank { "•••• •••• •••• ••••" },
                color = Color.White, fontSize = 16.sp, letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("CARD HOLDER", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    Text(
                        name.ifBlank { "YOUR NAME" }.uppercase(),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("EXPIRES", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                    Text(
                        expiry.ifBlank { "MM/YY" },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = muted, fontSize = 13.5.sp)
        Text(value, color = ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatCardNumber(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(16)
    return digits.chunked(4).joinToString(" ")
}

private fun formatExpiry(raw: String): String {
    val digits = raw.filter(Char::isDigit).take(4)
    return if (digits.length >= 2) "${digits.take(2)}/${digits.drop(2)}" else digits
}

private fun isCardExpired(expiry: String): Boolean {
    val match = Regex("(0[1-9]|1[0-2])/(\\d{2})").matchEntire(expiry) ?: return true
    val month = match.groupValues[1].toInt()
    val year = 2000 + match.groupValues[2].toInt()
    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    val currentMonth = cal.get(Calendar.MONTH) + 1
    return year < currentYear || (year == currentYear && month < currentMonth)
}