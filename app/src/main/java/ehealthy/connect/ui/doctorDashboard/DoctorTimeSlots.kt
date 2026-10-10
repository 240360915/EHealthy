package ehealthy.connect.ui.doctorDashboard

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

@Serializable
data class TimeSlotRow(
    val doctor_id: String,
    val date: String,
    val time: String,
    val is_booked: Boolean = false
)

private data class Slot(
    val time24: String,
    val status: String
)

private val intervalOptions = listOf(15, 30, 45, 60)

// Patient-side matching visual language.
private val TimeSlotsBackground = Color(0xFFF7F9FC)
private val TimeSlotsInk = Color(0xFF171B27)
private val TimeSlotsMuted = Color(0xFF6C7280)
private val TimeSlotsTeal = Color(0xFF119E95)
private val TimeSlotsTealSoft = Color(0xFFE8F7F5)
private val TimeSlotsBlue = Color(0xFF2F6FED)
private val TimeSlotsBlueSoft = Color(0xFFEAF2FF)
private val TimeSlotsAmber = Color(0xFFF59E0B)
private val TimeSlotsAmberSoft = Color(0xFFFFF4DD)
private val TimeSlotsGreen = Color(0xFF18A572)
private val TimeSlotsGreenSoft = Color(0xFFE9FAF2)
private val TimeSlotsGreySoft = Color(0xFFF0F2F5)
private val TimeSlotsBorder = Color(0xFFE3E8EF)

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun DoctorTimeSlots(
    doctorId: String?,
    onBack: () -> Unit,
    fetchSlots: suspend (
        doctorId: String,
        date: String
    ) -> Result<List<TimeSlotRow>>,
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

    var year by remember {
        mutableStateOf(calendar.get(Calendar.YEAR))
    }
    var month by remember {
        mutableStateOf(calendar.get(Calendar.MONTH))
    }
    var day by remember {
        mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH))
    }

    var fromHour by remember { mutableStateOf(8) }
    var fromMinute by remember { mutableStateOf(0) }
    var toHour by remember { mutableStateOf(17) }
    var toMinute by remember { mutableStateOf(0) }

    var intervalMinutes by remember { mutableStateOf(30) }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }

    var slots by remember {
        mutableStateOf<List<Slot>>(emptyList())
    }

    var generating by remember {
        mutableStateOf(false)
    }

    var saving by remember {
        mutableStateOf(false)
    }

    val slotDateString =
        "%04d-%02d-%02d".format(
            year,
            month + 1,
            day
        )

    fun prettyDate(): String {
        val selected = Calendar.getInstance()
        selected.set(year, month, day)

        return SimpleDateFormat(
            "EEE, d MMM yyyy",
            Locale.getDefault()
        ).format(selected.time)
    }

    fun to12Hour(time24: String): String {
        val parts = time24.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val period =
            if (hour < 12) "AM" else "PM"

        val displayHour =
            if (hour % 12 == 0) 12 else hour % 12

        return "%d:%02d %s".format(
            displayHour,
            minute,
            period
        )
    }

    fun buildTimeList(): List<String> {
        val startTotal =
            fromHour * 60 + fromMinute

        val endTotal =
            toHour * 60 + toMinute

        val result =
            mutableListOf<String>()

        var current = startTotal

        while (current < endTotal) {
            result.add(
                "%02d:%02d".format(
                    current / 60,
                    current % 60
                )
            )

            current += intervalMinutes
        }

        return result
    }

    suspend fun generateSlots() {
        val id = doctorId ?: return

        generating = true

        fetchSlots(
            id,
            slotDateString
        )
            .onSuccess { existing ->
                val existingByTime =
                    existing.associateBy {
                        it.time.take(5)
                    }

                slots =
                    buildTimeList()
                        .map { time ->
                            val existingRow =
                                existingByTime[time]

                            val status =
                                when {
                                    existingRow == null ->
                                        "closed"

                                    existingRow.is_booked ->
                                        "booked"

                                    else ->
                                        "open"
                                }

                            Slot(
                                time24 = time,
                                status = status
                            )
                        }
            }
            .onFailure {
                Toast.makeText(
                    context,
                    "Could not generate slots: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        generating = false
    }

    LaunchedEffect(
        doctorId,
        slotDateString
    ) {
        slots = emptyList()
        generateSlots()
    }

    val openCount =
        slots.count {
            it.status == "open"
        }

    val bookedCount =
        slots.count {
            it.status == "booked"
        }

    val closedCount =
        slots.count {
            it.status == "closed"
        }

    Scaffold(
        containerColor = TimeSlotsBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "My Time Slots",
                            color = TimeSlotsInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )

                        Text(
                            text = "Manage your consultation schedule",
                            color = TimeSlotsMuted,
                            fontSize = 10.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TimeSlotsTealSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TimeSlotsTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                generateSlots()
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(TimeSlotsBlueSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = TimeSlotsBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor = Color.White
                        )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 14.dp,
                    bottom = 28.dp
                )
        ) {

            ScheduleHeroCard(
                prettyDate = prettyDate(),
                openCount = openCount,
                bookedCount = bookedCount
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            TimeSlotsSectionHeader(
                title = "Schedule setup",
                subtitle = "Choose a date, working hours and slot length"
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    ),
                border = BorderStroke(
                    1.dp,
                    TimeSlotsBorder
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    SchedulePickerLabel(
                        text = "DATE"
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = slotDateString,
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = TimeSlotsTeal
                                )
                            },
                            colors = timeSlotFieldColors(),
                            shape = RoundedCornerShape(15.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            year = y
                                            month = m
                                            day = d
                                        },
                                        year,
                                        month,
                                        day
                                    ).show()
                                }
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            SchedulePickerLabel(
                                text = "FROM"
                            )

                            TimePickerField(
                                value =
                                    "%02d:%02d".format(
                                        fromHour,
                                        fromMinute
                                    ),
                                iconColor = TimeSlotsBlue,
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            fromHour = h
                                            fromMinute = m
                                        },
                                        fromHour,
                                        fromMinute,
                                        true
                                    ).show()
                                }
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            SchedulePickerLabel(
                                text = "TO"
                            )

                            TimePickerField(
                                value =
                                    "%02d:%02d".format(
                                        toHour,
                                        toMinute
                                    ),
                                iconColor = TimeSlotsTeal,
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            toHour = h
                                            toMinute = m
                                        },
                                        toHour,
                                        toMinute,
                                        true
                                    ).show()
                                }
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    SchedulePickerLabel(
                        text = "INTERVAL"
                    )

                    ExposedDropdownMenuBox(
                        expanded =
                            intervalDropdownExpanded,
                        onExpandedChange = {
                            intervalDropdownExpanded = it
                        }
                    ) {

                        OutlinedTextField(
                            value = "$intervalMinutes min",
                            onValueChange = {},
                            readOnly = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.SettingsSuggest,
                                    contentDescription = null,
                                    tint = TimeSlotsTeal
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults
                                    .TrailingIcon(
                                        expanded =
                                            intervalDropdownExpanded
                                    )
                            },
                            colors =
                                timeSlotFieldColors(),
                            shape =
                                RoundedCornerShape(15.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded =
                                intervalDropdownExpanded,
                            onDismissRequest = {
                                intervalDropdownExpanded =
                                    false
                            }
                        ) {
                            intervalOptions.forEach {
                                    minutes ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "$minutes min"
                                        )
                                    },
                                    onClick = {
                                        intervalMinutes =
                                            minutes

                                        intervalDropdownExpanded =
                                            false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                generateSlots()
                            }
                        },
                        enabled =
                            !generating &&
                                    doctorId != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape =
                            RoundedCornerShape(16.dp),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        TimeSlotsBlue
                                )
                    ) {

                        if (generating) {
                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )
                        }

                        Text(
                            text =
                                if (generating) {
                                    "Generating..."
                                } else {
                                    "Generate slots"
                                },
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            TimeSlotsSectionHeader(
                title = "Availability",
                subtitle = "Tap a time to open or close it"
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            AvailabilitySummary(
                open = openCount,
                booked = bookedCount,
                closed = closedCount
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (
                slots.isEmpty() &&
                !generating
            ) {

                EmptySlotsCard()

            } else {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(22.dp),
                    colors =
                        CardDefaults
                            .cardColors(
                                containerColor =
                                    Color.White
                            ),
                    elevation =
                        CardDefaults
                            .cardElevation(
                                defaultElevation =
                                    0.dp
                            ),
                    border =
                        BorderStroke(
                            1.dp,
                            TimeSlotsBorder
                        )
                ) {

                    FlowRow(
                        modifier =
                            Modifier.padding(14.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        slots.forEach { slot ->

                            SlotChip(
                                slot = slot,
                                label =
                                    to12Hour(
                                        slot.time24
                                    ),
                                onClick = {

                                    if (
                                        slot.status !=
                                        "booked"
                                    ) {

                                        slots =
                                            slots.map {

                                                if (
                                                    it.time24 ==
                                                    slot.time24
                                                ) {

                                                    it.copy(
                                                        status =
                                                            if (
                                                                it.status ==
                                                                "open"
                                                            ) {
                                                                "closed"
                                                            } else {
                                                                "open"
                                                            }
                                                    )

                                                } else {
                                                    it
                                                }
                                            }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        slots =
                            slots.map {
                                if (
                                    it.status != "booked"
                                ) {
                                    it.copy(
                                        status = "closed"
                                    )
                                } else {
                                    it
                                }
                            }
                    },
                    modifier =
                        Modifier.weight(1f),
                    shape =
                        RoundedCornerShape(15.dp),
                    border =
                        BorderStroke(
                            1.dp,
                            TimeSlotsBorder
                        )
                ) {
                    Text(
                        text = "Close all",
                        color = TimeSlotsMuted,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        slots =
                            slots.map {
                                if (
                                    it.status != "booked"
                                ) {
                                    it.copy(
                                        status = "open"
                                    )
                                } else {
                                    it
                                }
                            }
                    },
                    modifier =
                        Modifier.weight(1f),
                    shape =
                        RoundedCornerShape(15.dp),
                    border =
                        BorderStroke(
                            1.dp,
                            TimeSlotsTeal.copy(
                                alpha = 0.28f
                            )
                        )
                ) {
                    Text(
                        text = "Open all",
                        color = TimeSlotsTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            Button(
                onClick = {
                    val id =
                        doctorId
                            ?: return@Button

                    scope.launch {

                        saving = true

                        Toast.makeText(
                            context,
                            "Saving...",
                            Toast.LENGTH_SHORT
                        ).show()

                        val openOrBooked =
                            slots
                                .filter {
                                    it.status !=
                                            "closed"
                                }
                                .map {
                                    TimeSlotRow(
                                        doctor_id = id,
                                        date =
                                            slotDateString,
                                        time =
                                            it.time24,
                                        is_booked =
                                            it.status ==
                                                    "booked"
                                    )
                                }

                        val closedTimes =
                            slots
                                .filter {
                                    it.status ==
                                            "closed"
                                }
                                .map {
                                    it.time24
                                }

                        saveSlots(
                            id,
                            slotDateString,
                            openOrBooked,
                            closedTimes
                        )
                            .onSuccess {

                                Toast.makeText(
                                    context,
                                    "Saved $openCount open slots for ${prettyDate()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            .onFailure {

                                Toast.makeText(
                                    context,
                                    "Could not save: ${it.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        saving = false
                    }
                },
                enabled =
                    !saving &&
                            slots.isNotEmpty() &&
                            doctorId != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape =
                    RoundedCornerShape(17.dp),
                colors =
                    ButtonDefaults
                        .buttonColors(
                            containerColor =
                                TimeSlotsTeal
                        )
            ) {

                if (saving) {
                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )

                    Spacer(
                        modifier =
                            Modifier.width(8.dp)
                    )
                }

                Text(
                    text =
                        if (saving) {
                            "Saving availability..."
                        } else {
                            "Save availability"
                        },
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }
    }
}

@Composable
private fun ScheduleHeroCard(
    prettyDate: String,
    openCount: Int,
    bookedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    TimeSlotsTealSoft
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(
                        RoundedCornerShape(16.dp)
                    )
                    .background(
                        TimeSlotsTeal.copy(
                            alpha = 0.11f
                        )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = TimeSlotsTeal,
                    modifier =
                        Modifier.size(27.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Manage availability",
                    color = TimeSlotsInk,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = prettyDate,
                    color = TimeSlotsMuted,
                    fontSize = 10.sp
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "$openCount open  •  $bookedCount booked",
                    color = TimeSlotsTeal,
                    fontSize = 9.5.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TimeSlotsSectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            color = TimeSlotsInk,
            fontSize = 17.sp,
            fontWeight =
                FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Text(
            text = subtitle,
            color = TimeSlotsMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun SchedulePickerLabel(
    text: String
) {
    Text(
        text = text,
        color = TimeSlotsMuted,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(5.dp)
    )
}

@Composable
private fun TimePickerField(
    value: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            leadingIcon = {
                Icon(
                    imageVector =
                        Icons.Outlined.LockClock,
                    contentDescription = null,
                    tint = iconColor
                )
            },
            colors =
                timeSlotFieldColors(),
            shape =
                RoundedCornerShape(15.dp),
            modifier =
                Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    onClick = onClick
                )
        )
    }
}

@Composable
private fun AvailabilitySummary(
    open: Int,
    booked: Int,
    closed: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        AvailabilityMiniCard(
            value = open.toString(),
            label = "Open",
            color = TimeSlotsGreen,
            background = TimeSlotsGreenSoft,
            modifier = Modifier.weight(1f)
        )

        AvailabilityMiniCard(
            value = booked.toString(),
            label = "Booked",
            color = TimeSlotsAmber,
            background = TimeSlotsAmberSoft,
            modifier = Modifier.weight(1f)
        )

        AvailabilityMiniCard(
            value = closed.toString(),
            label = "Closed",
            color = TimeSlotsMuted,
            background = TimeSlotsGreySoft,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AvailabilityMiniCard(
    value: String,
    label: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = background
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 12.dp,
                    horizontal = 8.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                color = color,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.ExtraBold
            )

            Text(
                text = label,
                color = TimeSlotsMuted,
                fontSize = 8.5.sp
            )
        }
    }
}

@Composable
private fun EmptySlotsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = Color.White
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
        border =
            BorderStroke(
                1.dp,
                TimeSlotsBorder
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp,
                    vertical = 26.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        TimeSlotsBlueSoft
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = TimeSlotsBlue,
                    modifier =
                        Modifier.size(28.dp)
                )
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "No slots generated",
                color = TimeSlotsInk,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "Choose your working hours above and generate time slots.",
                color = TimeSlotsMuted,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SlotChip(
    slot: Slot,
    label: String,
    onClick: () -> Unit
) {
    val background: Color
    val foreground: Color
    val border: Color

    when (slot.status) {
        "open" -> {
            background =
                TimeSlotsGreenSoft

            foreground =
                TimeSlotsGreen

            border =
                TimeSlotsGreen.copy(
                    alpha = 0.30f
                )
        }

        "booked" -> {
            background =
                TimeSlotsAmberSoft

            foreground =
                TimeSlotsAmber

            border =
                TimeSlotsAmber.copy(
                    alpha = 0.30f
                )
        }

        else -> {
            background =
                TimeSlotsGreySoft

            foreground =
                TimeSlotsMuted

            border =
                TimeSlotsBorder
        }
    }

    Surface(
        shape =
            RoundedCornerShape(14.dp),
        color = background,
        border =
            BorderStroke(
                1.dp,
                border
            ),
        modifier = Modifier.clickable(
            enabled =
                slot.status != "booked",
            onClick = onClick
        )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 10.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(foreground)
            )

            Spacer(
                modifier = Modifier.width(6.dp)
            )

            Text(
                text = label,
                color = foreground,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun timeSlotFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = TimeSlotsTeal,
        unfocusedBorderColor = TimeSlotsBorder,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        cursorColor = TimeSlotsTeal
    )
