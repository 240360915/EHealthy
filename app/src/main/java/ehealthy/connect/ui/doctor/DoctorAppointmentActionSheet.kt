package ehealthy.connect.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAppointmentActionSheet(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val navy = Color(0xFF0F1F3D)
    val green = Color(0xFF10B981)
    val muted = Color(0xFF64748B)
    val danger = Color(0xFFEF4444)
    val sheetState = rememberModalBottomSheetState()

    var showCancelConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(appointment.patient_name ?: "Unknown patient", color = navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("${appointment.date ?: "-"}  ·  ${appointment.time ?: "-"}", color = muted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))

            DoctorSheetActionRow(icon = Icons.Outlined.Info, label = "View Details", tint = navy, onClick = onViewDetails)

            if (appointment.status == "pending") {
                DoctorSheetActionRow(icon = Icons.Outlined.CheckCircle, label = "Confirm Appointment", tint = green, onClick = onConfirm)
            }
            if (appointment.status != "cancelled") {
                DoctorSheetActionRow(
                    icon = Icons.Outlined.Cancel,
                    label = "Cancel Appointment",
                    tint = danger,
                    onClick = { showCancelConfirm = true }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel this appointment?") },
            text = { Text("The patient will be notified that this appointment was cancelled.") },
            confirmButton = {
                TextButton(onClick = { showCancelConfirm = false; onCancel() }) {
                    Text("Yes, Cancel", color = danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("Keep Appointment")
                }
            }
        )
    }
}

@Composable
private fun DoctorSheetActionRow(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DoctorAppointmentDetailsDialog(appt: Appointment, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(appt.patient_name ?: "Unknown patient", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Date: ${appt.date ?: "-"}")
                Text("Time: ${appt.time ?: "-"}")
                Text("Reason: ${appt.reason ?: "General consultation"}")
                appt.payment_method?.let { Text("Payment method: $it") }
                appt.amount_paid?.let { Text("Amount paid: R${"%.2f".format(it)}") }
                Text("Status: ${appt.status?.replaceFirstChar { c -> c.uppercase() } ?: "Unknown"}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}