package ehealthy.connect.ui.doctor

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/* Table `availability_slots`: doctor_id, slot_date ("yyyy-MM-dd"),
   slot_time (24hr "HH:mm"), status ("open" | "closed" | "booked"). See
   DoctorAuthHelper.kt for fetchDoctorTimeSlots / saveDoctorTimeSlots. */
/* Table `time_slots`: id, doctor_id, date ("yyyy-MM-dd"), time ("HH:mm:ss"),
   is_booked (bool), created_at. A row's existence = the doctor made that
   time available; is_booked flags whether a patient has taken it. See
   DoctorAuthHelper.kt for fetchDoctorTimeSlots / saveDoctorTimeSlots. */
@Serializable
data class TimeSlotRow(
    val doctor_id: String,
    val date: String,
    val time: String,
    val is_booked: Boolean = false
)

// What's shown on screen for one slot chip.
private data class Slot(val time24: String, val status: String) // "open" | "closed" | "booked"

private val intervalOptions = listOf(15, 30, 45, 60)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DoctorTimeSlots(
    doctorId: String?,
    onBack: () -> Unit,
    fetchSlots: suspend (doctorId: String, date: String) -> Result<List<TimeSlotRow>>,
    saveSlots: suspend (
        doctorId: String,
        date: String,
        openOrBookedRows: List<TimeSlotRow>,
        closedTimes: List<String>
    ) -> Result<Unit>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val calendar = remember { Calendar.getInstance() }

    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) } // 0-based
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    var fromHour by remember { mutableStateOf(8) }
    var fromMinute by remember { mutableStateOf(0) }
    var toHour by remember { mutableStateOf(17) }
    var toMinute by remember { mutableStateOf(0) }

    var intervalMinutes by remember { mutableStateOf(30) }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }

    var slots by remember { mutableStateOf<List<Slot>>(emptyList()) }
    var generating by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val slotDateString = "%04d-%02d-%02d".format(year, month + 1, day)

    fun prettyDate(): String {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        return SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun to12Hour(time24: String): String {
        val parts = time24.split(":")
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val period = if (h < 12) "AM" else "PM"
        val hour12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(hour12, m, period)
    }

    /** Builds every slot time between From and To at the chosen interval. */
    fun buildTimeList(): List<String> {
        val startTotal = fromHour * 60 + fromMinute
        val endTotal = toHour * 60 + toMinute
        val result = mutableListOf<String>()
        var t = startTotal
        while (t < endTotal) {
            result.add("%02d:%02d".format(t / 60, t % 60))
            t += intervalMinutes
        }
        return result
    }

    /**
     * Generates the slot list for the chosen date/range/interval, keeping the
     * status of any slot that was already saved for that date (so an already
     * "booked" slot doesn't reset to "open" just because the doctor regenerated).
     */
    suspend fun generateSlots() {
        val id = doctorId ?: return
        generating = true
        fetchSlots(id, slotDateString)
            .onSuccess { existing ->
                // .take(5) strips the seconds Postgres returns ("08:00:00" -> "08:00")
                // so it matches the "HH:mm" format buildTimeList() generates.
                val existingByTime = existing.associateBy { it.time.take(5) }
                slots = buildTimeList().map { time ->
                    val existingRow = existingByTime[time]
                    val status = when {
                        existingRow == null -> "closed"
                        existingRow.is_booked -> "booked"
                        else -> "open"
                    }
                    Slot(time24 = time, status = status)
                }
            }
            .onFailure {
                Toast.makeText(context, "Could not generate slots: ${it.message}", Toast.LENGTH_LONG).show()
            }
        generating = false
    }

    // Generate once the doctor id resolves, using today's default range.
    LaunchedEffect(doctorId) { generateSlots() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Time Slots", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { generateSlots() } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0F1F3D), modifier = Modifier.fillMaxWidth()) {
                Text(
                    "🕐  Manage Available Slots",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Set the time slots you are available for on a specific date. Patients will only be able to book from these slots.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            FieldLabel("DATE")
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = slotDateString,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> year = y; month = m; day = d },
                                year, month, day
                            ).show()
                        }
                )
            }
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("FROM")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "%02d:%02d".format(fromHour, fromMinute),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    TimePickerDialog(context, { _, h, m -> fromHour = h; fromMinute = m }, fromHour, fromMinute, true).show()
                                }
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("TO")
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "%02d:%02d".format(toHour, toMinute),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    TimePickerDialog(context, { _, h, m -> toHour = h; toMinute = m }, toHour, toMinute, true).show()
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            FieldLabel("INTERVAL")
            ExposedDropdownMenuBox(
                expanded = intervalDropdownExpanded,
                onExpandedChange = { intervalDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = "$intervalMinutes min",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalDropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = intervalDropdownExpanded, onDismissRequest = { intervalDropdownExpanded = false }) {
                    intervalOptions.forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text("$minutes min") },
                            onClick = { intervalMinutes = minutes; intervalDropdownExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { scope.launch { generateSlots() } },
                enabled = !generating && doctorId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (generating) "Generating..." else "Generate Slots")
            }

            Spacer(Modifier.height(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(Color(0xFF22C55E)); Spacer(Modifier.width(4.dp)); Text("Open (patients can book)", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(Color(0xFFF59E0B)); Spacer(Modifier.width(4.dp)); Text("Already booked", fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(MaterialTheme.colorScheme.outline); Spacer(Modifier.width(4.dp)); Text("Closed (click to open)", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            if (slots.isEmpty() && !generating) {
                Text("No slots generated yet — set your range above and tap Generate Slots.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { slot ->
                        SlotChip(
                            slot = slot,
                            label = to12Hour(slot.time24),
                            onClick = {
                                if (slot.status != "booked") {
                                    slots = slots.map {
                                        if (it.time24 == slot.time24) it.copy(status = if (it.status == "open") "closed" else "open")
                                        else it
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val id = doctorId ?: return@Button
                    scope.launch {
                        saving = true
                        Toast.makeText(context, "Saving...", Toast.LENGTH_SHORT).show()
                        val openOrBooked = slots.filter { it.status != "closed" }
                            .map { TimeSlotRow(doctor_id = id, date = slotDateString, time = it.time24, is_booked = it.status == "booked") }
                        val closedTimes = slots.filter { it.status == "closed" }.map { it.time24 }
                        saveSlots(id, slotDateString, openOrBooked, closedTimes)
                            .onSuccess {
                                val openCount = slots.count { it.status == "open" }
                                Toast.makeText(context, "Saved $openCount open slots for ${prettyDate()}", Toast.LENGTH_LONG).show()
                            }
                            .onFailure {
                                Toast.makeText(context, "Could not save: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        saving = false
                    }
                },
                enabled = !saving && slots.isNotEmpty() && doctorId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saving) "Saving..." else "💾 Save Availability")
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { slots = slots.map { if (it.status != "booked") it.copy(status = "closed") else it } },
                    modifier = Modifier.weight(1f)
                ) { Text("Close All") }

                OutlinedButton(
                    onClick = { slots = slots.map { if (it.status != "booked") it.copy(status = "open") else it } },
                    modifier = Modifier.weight(1f)
                ) { Text("Open All") }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
}

@Composable
private fun SlotChip(slot: Slot, label: String, onClick: () -> Unit) {
    val (bg, fg, border) = when (slot.status) {
        "open" -> Triple(Color(0xFF22C55E).copy(alpha = 0.14f), Color(0xFF1B7A43), Color(0xFF22C55E).copy(alpha = 0.5f))
        "booked" -> Triple(Color(0xFFF59E0B).copy(alpha = 0.16f), Color(0xFF8A6D1D), Color(0xFFF59E0B).copy(alpha = 0.5f))
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outline)
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.clickable(enabled = slot.status != "booked") { onClick() }
    ) {
        Text(
            label,
            color = fg,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}