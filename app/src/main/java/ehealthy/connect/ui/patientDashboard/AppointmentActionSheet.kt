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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentActionSheet(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit,
    onRequestCompletionCode: suspend (String) -> Result<String> = { Result.failure(Exception("Not available")) }
) {
    val navy = Color(0xFF0F1F3D)
    val muted = Color(0xFF64748B)
    val danger = Color(0xFFEF4444)
    val teal = Color(0xFF0D9488)
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    var showCancelConfirm by remember { mutableStateOf(false) }
    var isRequestingCode by remember { mutableStateOf(false) }
    var displayedCode by remember { mutableStateOf(appointment.completion_code) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var showCodeDialog by remember { mutableStateOf(false) }

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

            // Reschedule/Cancel don't make sense for an already-canceled appointment.
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

            // In-person visits release funds only once the doctor enters this
            // code — request it (or view it again) once the appointment has
            // been accepted by the doctor.
            if (appointment.appointment_type != "online" && appointment.status == "confirmed") {
                SheetActionRow(
                    icon = Icons.Outlined.Key,
                    label = if (displayedCode != null) "View Completion Code" else "Request Completion Code",
                    tint = teal,
                    onClick = {
                        if (displayedCode != null) {
                            showCodeDialog = true
                        } else {
                            scope.launch {
                                isRequestingCode = true
                                codeError = null
                                onRequestCompletionCode(appointment.id)
                                    .onSuccess {
                                        displayedCode = it
                                        showCodeDialog = true
                                    }
                                    .onFailure {
                                        codeError = it.message ?: "Could not generate a code. Please try again."
                                    }
                                isRequestingCode = false
                            }
                        }
                    }
                )
                if (isRequestingCode) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = teal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Generating code…", color = muted, fontSize = 12.sp)
                    }
                }
                codeError?.let {
                    Text(it, color = danger, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (showCodeDialog && displayedCode != null) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text("Your Completion Code") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Give this code to your doctor once your visit is done. They'll enter it to confirm the appointment is complete.",
                        color = muted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF9))
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            displayedCode ?: "",
                            color = teal,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCodeDialog = false }) {
                    Text("Done", color = teal, fontWeight = FontWeight.Bold)
                }
            }
        )
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