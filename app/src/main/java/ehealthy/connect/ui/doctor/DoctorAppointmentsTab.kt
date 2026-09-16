package ehealthy.connect.ui.doctor

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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.patientDashboard.Appointment

private val CardBorder = Color(0xFFE2E8F0)
private val CardRadius = 16.dp

enum class DoctorAppointmentFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled")
}

private fun matchesFilter(appt: Appointment, filter: DoctorAppointmentFilter): Boolean =
    when (filter) {
        DoctorAppointmentFilter.ALL -> true
        DoctorAppointmentFilter.PENDING -> appt.status == "pending"
        DoctorAppointmentFilter.CONFIRMED -> appt.status == "confirmed"
        DoctorAppointmentFilter.CANCELLED -> appt.status == "cancelled"
    }

@Composable
fun DoctorAppointmentsTab(
    modifier: Modifier = Modifier,
    appointments: List<Appointment>,
    isLoading: Boolean,
    loadError: String?,
    onUpdateStatus: (appointmentId: String, newStatus: String) -> Unit
) {
    val background = Color(0xFFF0F4F8)
    val navy = Color(0xFF0F1F3D)
    val accent = Color(0xFF3B82F6)
    val muted = Color(0xFF64748B)

    var selectedFilter by remember { mutableStateOf(DoctorAppointmentFilter.ALL) }
    var actionTarget by remember { mutableStateOf<Appointment?>(null) }
    var detailsTarget by remember { mutableStateOf<Appointment?>(null) }

    val filtered = remember(appointments, selectedFilter) {
        appointments.filter { matchesFilter(it, selectedFilter) }
            .sortedByDescending { it.date ?: "" }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        Text(
            "Appointments",
            color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DoctorAppointmentFilter.entries.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = navy,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = muted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedFilter == filter,
                        borderColor = CardBorder,
                        selectedBorderColor = navy
                    )
                )
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }

            loadError != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(loadError, color = Color(0xFFEF4444), fontSize = 13.sp)
                }
            }

            filtered.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No appointments in this category", color = navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filtered, key = { it.id }) { appt ->
                        DoctorAppointmentListCard(appt = appt, onActions = { actionTarget = appt })
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    actionTarget?.let { appt ->
        DoctorAppointmentActionSheet(
            appointment = appt,
            onDismiss = { actionTarget = null },
            onViewDetails = {
                detailsTarget = appt
                actionTarget = null
            },
            onConfirm = {
                onUpdateStatus(appt.id, "confirmed")
                actionTarget = null
            },
            onCancel = {
                onUpdateStatus(appt.id, "cancelled")
                actionTarget = null
            }
        )
    }

    detailsTarget?.let { appt ->
        DoctorAppointmentDetailsDialog(appt = appt, onDismiss = { detailsTarget = null })
    }
}

@Composable
private fun MiniInfoRow(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = tint, fontSize = 13.sp)
    }
}

@Composable
private fun DoctorAppointmentListCard(appt: Appointment, onActions: () -> Unit) {
    val statusColor = when (appt.status) {
        "confirmed" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "cancelled" -> Color(0xFFEF4444)
        else -> Color(0xFF94A3B8)
    }
    val statusBg = when (appt.status) {
        "confirmed" -> Color(0xFFF0FDF4)
        "pending" -> Color(0xFFFFFBEB)
        "cancelled" -> Color(0xFFFEF2F2)
        else -> Color(0xFFF8FAFF)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(CardRadius),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).height(4.dp).fillMaxWidth().background(statusColor))
        }
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(appt.patient_name ?: "Unknown patient", color = Color(0xFF0F1F3D), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        MiniInfoRow(Icons.Outlined.CalendarMonth, appt.date ?: "-", Color(0xFF1E293B))
                        MiniInfoRow(Icons.Outlined.Schedule, appt.time ?: "-", Color(0xFF1E293B))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    MiniInfoRow(Icons.Outlined.Description, appt.reason ?: "General consultation", Color(0xFF64748B))
                    appt.payment_method?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        MiniInfoRow(Icons.Outlined.Payments, it, Color(0xFF64748B))
                    }
                }
                IconButton(onClick = onActions) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Actions", tint = Color(0xFF64748B))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(statusBg).padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    appt.status?.replaceFirstChar { it.uppercase() } ?: "Unknown",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}