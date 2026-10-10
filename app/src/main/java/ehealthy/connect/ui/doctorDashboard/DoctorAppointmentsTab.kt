package ehealthy.connect.ui.doctorDashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.patientDashboard.Appointment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Same visual language as the patient dashboard.
private val AppointmentsBackground = Color(0xFFF7F9FC)
private val AppointmentInk = Color(0xFF171B27)
private val AppointmentMuted = Color(0xFF6C7280)
private val AppointmentTeal = Color(0xFF119E95)
private val AppointmentTealSoft = Color(0xFFE8F7F5)
private val AppointmentBlue = Color(0xFF2F6FED)
private val AppointmentBlueSoft = Color(0xFFEAF2FF)
private val AppointmentAmber = Color(0xFFF59E0B)
private val AppointmentAmberSoft = Color(0xFFFFF4DD)
private val AppointmentGreen = Color(0xFF18A572)
private val AppointmentGreenSoft = Color(0xFFE9FAF2)
private val AppointmentRed = Color(0xFFE53935)
private val AppointmentRedSoft = Color(0xFFFFEEEE)
private val AppointmentPurpleSoft = Color(0xFFF7F0FA)
private val AppointmentBorder = Color(0xFFE2E7ED)

enum class DoctorAppointmentFilter(val label: String) {
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}
private fun isAppointmentExpired(
    appointment: Appointment
): Boolean {

    val date =
        appointment.date
            ?: return false

    val time =
        appointment.time
            ?: return false

    return try {

        val cleanTime =
            if (time.length >= 8) {
                time.take(8)
            } else {
                time
            }

        val appointmentDateTime =
            LocalDateTime.parse(
                "$date $cleanTime",
                DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd HH:mm:ss"
                )
            )

        appointmentDateTime.isBefore(
            LocalDateTime.now()
        )

    } catch (_: Exception) {

        false
    }
}

private fun matchesFilter(
    appointment: Appointment,
    filter: DoctorAppointmentFilter
): Boolean {

    val status =
        appointment.status
            ?.lowercase()
            .orEmpty()

    val expired =
        isAppointmentExpired(
            appointment
        ) &&
                status != "completed" &&
                status != "cancelled" &&
                status != "declined"

    return when (filter) {

        DoctorAppointmentFilter.UPCOMING -> {

            !expired &&
                    status != "completed" &&
                    status != "cancelled" &&
                    status != "declined"
        }


        DoctorAppointmentFilter.COMPLETED -> {

            status == "completed"
        }


        DoctorAppointmentFilter.CANCELLED -> {

            status == "cancelled" ||
                    status == "declined" ||
                    expired
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DoctorAppointmentsTab(
    modifier: Modifier = Modifier,
    appointments: List<Appointment>,
    isLoading: Boolean,
    loadError: String?,
    onUpdateStatus: (appointmentId: String, newStatus: String) -> Unit,
    onStartCall: (appointmentId: String) -> Unit = {},
    onVerifyCompletionCode: suspend (appointmentId: String, code: String) -> Result<Unit> = { _, _ ->
        Result.failure(Exception("Not available"))
    },
    // Receives the appointment ID; patient identity is validated by the backend RPC.
    onOpenPatientFile: (appointmentId: String) -> Unit = {}
) {
    var selectedFilter by remember {
        mutableStateOf(DoctorAppointmentFilter.UPCOMING)
    }

    var actionTarget by remember {
        mutableStateOf<Appointment?>(null)
    }

    var detailsTarget by remember {
        mutableStateOf<Appointment?>(null)
    }

    val upcomingCount = remember(appointments) {

        appointments.count { appointment ->

            val status =
                appointment.status
                    ?.lowercase()
                    .orEmpty()

            !isAppointmentExpired(
                appointment
            ) &&
                    status != "completed" &&
                    status != "cancelled" &&
                    status != "declined"
        }
    }

    val completedCount = remember(appointments) {
        appointments.count {
            it.status.equals("completed", ignoreCase = true)
        }
    }

    val totalCount = appointments.size

    val filtered = remember(appointments, selectedFilter) {
        appointments
            .filter { matchesFilter(it, selectedFilter) }
            .sortedWith(
                compareBy<Appointment>(
                    { it.date ?: "9999-99-99" },
                    { it.time ?: "99:99" }
                )
            )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppointmentsBackground)
    ) {

        // PAGE TITLE
        Column(
            modifier = Modifier.padding(
                start = 18.dp,
                end = 18.dp,
                top = 18.dp
            )
        ) {
            Text(
                text = "Appointments",
                color = AppointmentInk,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "Manage patient bookings and consultations",
                color = AppointmentMuted,
                fontSize = 10.5.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // SUMMARY STRIP — mirrors the patient My Visits page.
        AppointmentSummaryStrip(
            upcoming = upcomingCount,
            completed = completedCount,
            total = totalCount
        )

        Spacer(modifier = Modifier.height(14.dp))

        // FILTERS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DoctorAppointmentFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = {
                        selectedFilter = filter
                    },
                    label = {
                        Text(
                            filter.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppointmentBlueSoft,
                        selectedLabelColor = AppointmentBlue,
                        containerColor = Color.White,
                        labelColor = AppointmentMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = AppointmentBorder,
                        selectedBorderColor = AppointmentBlue.copy(alpha = 0.20f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppointmentTeal
                    )
                }
            }

            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AppointmentRedSoft
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp
                        ),
                        border = BorderStroke(
                            1.dp,
                            AppointmentRed.copy(alpha = 0.12f)
                        )
                    ) {
                        Text(
                            text = loadError,
                            color = AppointmentRed,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            filtered.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyAppointmentsState(
                        filter = selectedFilter
                    )
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 2.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filtered,
                        key = { it.id }
                    ) { appointment ->
                        DoctorAppointmentListCard(
                            appointment = appointment,
                            onActions = {
                                actionTarget = appointment
                            },
                            onOpenPatientFile = {
                                onOpenPatientFile(appointment.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Keep all existing appointment functionality.
    actionTarget?.let { appointment ->
        DoctorAppointmentActionSheet(
            appointment = appointment,
            onDismiss = {
                actionTarget = null
            },
            onViewDetails = {
                detailsTarget = appointment
                actionTarget = null
            },
            onConfirm = {
                onUpdateStatus(
                    appointment.id,
                    "confirmed"
                )
                actionTarget = null
            },
            onCancel = {
                onUpdateStatus(
                    appointment.id,
                    "cancelled"
                )
                actionTarget = null
            },
            onStartCall = {
                onStartCall(appointment.id)
                actionTarget = null
            },
            onVerifyCompletionCode = { code ->
                onVerifyCompletionCode(
                    appointment.id,
                    code
                )
            }
        )
    }

    detailsTarget?.let { appointment ->
        DoctorAppointmentDetailsDialog(
            appt = appointment,
            onDismiss = {
                detailsTarget = null
            }
        )
    }
}

@Composable
private fun AppointmentSummaryStrip(
    upcoming: Int,
    completed: Int,
    total: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFDCEFEB)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 14.dp
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            AppointmentSummaryItem(
                icon = Icons.Outlined.CalendarMonth,
                value = upcoming.toString(),
                label = "Upcoming",
                color = AppointmentTeal,
                modifier = Modifier.weight(1f)
            )

            VerticalDividerLine()

            AppointmentSummaryItem(
                icon = Icons.Outlined.CheckCircle,
                value = completed.toString(),
                label = "Completed",
                color = AppointmentTeal,
                modifier = Modifier.weight(1f)
            )

            VerticalDividerLine()

            AppointmentSummaryItem(
                icon = Icons.Outlined.Schedule,
                value = total.toString(),
                label = "Total",
                color = AppointmentTeal,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppointmentSummaryItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = AppointmentInk,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = label,
            color = AppointmentMuted,
            fontSize = 8.5.sp
        )
    }
}

@Composable
private fun VerticalDividerLine() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(58.dp)
            .background(
                AppointmentTeal.copy(
                    alpha = 0.22f
                )
            )
    )
}

@Composable
private fun EmptyAppointmentsState(
    filter: DoctorAppointmentFilter
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            1.dp,
            AppointmentBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 22.dp,
                    vertical = 30.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(AppointmentTealSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = AppointmentTeal,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(13.dp))

            Text(
                text = when (filter) {
                    DoctorAppointmentFilter.UPCOMING ->
                        "No upcoming appointments"

                    DoctorAppointmentFilter.COMPLETED ->
                        "No completed appointments"

                    DoctorAppointmentFilter.CANCELLED ->
                        "No cancelled appointments"
                },
                color = AppointmentInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Appointments in this category will appear here.",
                color = AppointmentMuted,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DoctorAppointmentListCard(
    appointment: Appointment,
    onActions: () -> Unit,
    onOpenPatientFile: () -> Unit
) {
    val status = appointment.status?.lowercase().orEmpty()
    // Align with the protected doctor_get_patient_care_file RPC: pending or
    // cancelled appointments must never expose the care-file action.
    val canOpenPatientFile = status == "confirmed" || status == "completed"

    val statusColor = when (status) {
        "confirmed", "accepted" ->
            AppointmentGreen

        "pending" ->
            AppointmentAmber

        "completed" ->
            AppointmentBlue

        "cancelled", "declined" ->
            AppointmentRed

        else ->
            AppointmentMuted
    }

    val statusBackground = when (status) {
        "confirmed", "accepted" ->
            AppointmentGreenSoft

        "pending" ->
            AppointmentAmberSoft

        "completed" ->
            AppointmentBlueSoft

        "cancelled", "declined" ->
            AppointmentRedSoft

        else ->
            Color(0xFFF0F2F5)
    }

    val cardBackground = when (status) {
        "pending" ->
            AppointmentPurpleSoft

        "confirmed", "accepted" ->
            Color(0xFFF1FBF8)

        "completed" ->
            Color(0xFFF2F6FF)

        "cancelled", "declined" ->
            Color(0xFFFFF7F7)

        else ->
            Color.White
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        border = BorderStroke(
            1.dp,
            AppointmentBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            // PATIENT + STATUS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(47.dp)
                        .clip(CircleShape)
                        .background(AppointmentTealSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = AppointmentTeal,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Spacer(modifier = Modifier.width(11.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = appointment.patient_name
                            ?: "Unknown patient",
                        color = AppointmentInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = appointment.reason
                            ?: "General consultation",
                        color = AppointmentMuted,
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusBackground)
                        .padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        )
                ) {
                    Text(
                        text = appointment.status
                            ?.replaceFirstChar {
                                it.uppercase()
                            }
                            ?: "Unknown",
                        color = statusColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // DATE + TIME
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                AppointmentDetailBox(
                    label = "DATE",
                    value = appointment.date ?: "-",
                    icon = Icons.Outlined.CalendarMonth,
                    iconColor = AppointmentBlue,
                    iconBackground = AppointmentBlueSoft,
                    modifier = Modifier.weight(1f)
                )

                AppointmentDetailBox(
                    label = "TIME",
                    value = appointment.time ?: "-",
                    icon = Icons.Outlined.Schedule,
                    iconColor = AppointmentTeal,
                    iconBackground = AppointmentTealSoft,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!appointment.payment_method.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(9.dp))

                AppointmentDetailBox(
                    label = "PAYMENT",
                    value = appointment.payment_method,
                    icon = Icons.Outlined.Payments,
                    iconColor = AppointmentGreen,
                    iconBackground = AppointmentGreenSoft,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ACTION AREA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Color.White.copy(
                            alpha = 0.64f
                        )
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (
                            status == "confirmed" ||
                            status == "accepted"
                        ) {
                            Icons.Outlined.Videocam
                        } else {
                            Icons.Outlined.Description
                        },
                    contentDescription = null,
                    tint = AppointmentTeal,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            if (
                                status == "confirmed" ||
                                status == "accepted"
                            ) {
                                "Consultation ready"
                            } else {
                                "Appointment actions"
                            },
                        color = AppointmentInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "View details, confirm, call or update status",
                        color = AppointmentMuted,
                        fontSize = 8.5.sp
                    )
                }

                IconButton(
                    onClick = onActions,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Appointment actions",
                        tint = AppointmentInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Only an authenticated doctor with a confirmed/completed booking can
            // access this patient's record. The RPC enforces that independently.
            if (canOpenPatientFile) {
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenPatientFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = AppointmentTeal.copy(alpha = 0.35f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppointmentTeal,
                        containerColor = AppointmentTealSoft
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open patient care file",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ACTION AREA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Color.White.copy(
                            alpha = 0.64f
                        )
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (
                            status == "confirmed" ||
                            status == "accepted"
                        ) {
                            Icons.Outlined.Videocam
                        } else {
                            Icons.Outlined.Description
                        },
                    contentDescription = null,
                    tint = AppointmentTeal,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text =
                            if (
                                status == "confirmed" ||
                                status == "accepted"
                            ) {
                                "Consultation ready"
                            } else {
                                "Appointment actions"
                            },
                        color = AppointmentInk,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "View details, confirm, call or update status",
                        color = AppointmentMuted,
                        fontSize = 8.5.sp
                    )
                }

                IconButton(
                    onClick = onActions,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Appointment actions",
                        tint = AppointmentInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentDetailBox(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                Color.White.copy(
                    alpha = 0.74f
                )
            )
            .padding(
                horizontal = 11.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(31.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = label,
                color = AppointmentMuted,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                color = AppointmentInk,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}
