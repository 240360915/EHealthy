package ehealthy.connect.ui.patientDashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val rNavy = Color(0xFF0B1828)
private val rInk = Color(0xFF0F1F3D)
private val rMuted = Color(0xFF64748B)
private val rBg = Color(0xFFF4F7FA)
private val rTeal = Color(0xFF0D9488)
private val rTealSoft = Color(0xFFCCFBF1)
private val rRed = Color(0xFFDC2626)

private val rescheduleTimeSlots =
    listOf("08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00")

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleAppointmentScreen(
    appointment: Appointment,
    isLoading: Boolean,
    errorMessage: String?,
    fetchBookedTimes: suspend (date: String) -> Result<Set<String>>,
    onBack: () -> Unit,
    onConfirmReschedule: (newDate: String, newTime: String) -> Unit
) {
    var selectedDate by rememberSaveable { mutableStateOf(appointment.date ?: "") }
    var selectedTime by rememberSaveable { mutableStateOf(appointment.time) }
    var bookedTimes by rememberSaveable { mutableStateOf(setOf<String>()) }
    var dateError by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoadingSlots by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    LaunchedEffect(selectedDate) {
        if (selectedDate.isNotBlank()) {
            isLoadingSlots = true
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
                        "Reschedule Appointment",
                        color = rInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = rInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = rBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Current appointment",
                        color = rMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "📅 ${appointment.date ?: "-"}   🕐 ${appointment.time ?: "-"}",
                        color = rInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        appointment.reason ?: "General consultation",
                        color = rMuted,
                        fontSize = 13.sp
                    )
                }
            }

            RescheduleSectionCard(icon = Icons.Outlined.CalendarMonth, title = "New Date") {
                dateError?.let {
                    Text(it, color = rRed, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        placeholder = { Text("Select a new date") },
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = rInk,
                            disabledBorderColor = Color(0xFFCBD5E1),
                            disabledContainerColor = Color.Transparent,
                            disabledPlaceholderColor = rMuted,
                            disabledTrailingIconColor = rTeal
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date"
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                                        selectedTime = null
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


            if (selectedDate.isNotBlank()) {
                RescheduleSectionCard(icon = Icons.Outlined.Schedule, title = "New Time") {
                    if (isLoadingSlots) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = rTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            modifier = Modifier.height(((rescheduleTimeSlots.size / 4 + 1) * 52).dp)
                        ) {
                            items(rescheduleTimeSlots) { slot ->
                                RescheduleSlotButton(
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
                                    .background(rTealSoft)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = rTeal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "New time: $it",
                                    color = rTeal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            errorMessage?.let {
                Text(
                    it,
                    color = rRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    val time = selectedTime
                    if (selectedDate.isNotBlank() && time != null) {
                        onConfirmReschedule(selectedDate, time)
                    }
                },
                enabled = !isLoading && selectedDate.isNotBlank() && selectedTime != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = rNavy),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "Confirm New Time",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RescheduleSectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = rTeal, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = rInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun RescheduleSlotButton(
    label: String,
    isTaken: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, textColor) = when {
        isSelected -> rNavy to Color.White
        isTaken -> Color(0xFFFEE2E2) to rRed
        else -> rTealSoft to rTeal
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