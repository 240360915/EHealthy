package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentActionSheet(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit
) {
    val navy = Color(0xFF0F1F3D)
    val muted = Color(0xFF64748B)
    val danger = Color(0xFFEF4444)
    val sheetState = rememberModalBottomSheetState()

    var showCancelConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                "📅 ${appointment.date ?: "-"}   🕐 ${appointment.time ?: "-"}",
                color = navy,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                appointment.reason ?: "General consultation",
                color = muted,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(20.dp))

            SheetActionRow(
                icon = Icons.Outlined.Info,
                label = "View Details",
                tint = navy,
                onClick = onViewDetails
            )

            // Reschedule/Cancel don't make sense for an already-cancelled appointment.
            val canModify = appointment.status != "cancelled"
            if (canModify) {
                SheetActionRow(
                    icon = Icons.Outlined.CalendarMonth,
                    label = "Reschedule",
                    tint = navy,
                    onClick = onReschedule
                )
                SheetActionRow(
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
            text = { Text("This can't be undone. Your doctor will be notified.") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirm = false
                    onCancel()
                }) {
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
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}