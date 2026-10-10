package ehealthy.connect.ui.doctorDashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.patientDashboard.Appointment
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

private enum class DoctorCallWindowState {
    NOT_APPLICABLE,
    TOO_EARLY,
    OPEN,
    CLOSED
}

private val ActionInk = Color(0xFF171B27)
private val ActionMuted = Color(0xFF6C7280)
private val ActionTeal = Color(0xFF119E95)
private val ActionTealSoft = Color(0xFFE8F7F5)
private val ActionBlue = Color(0xFF2F6FED)
private val ActionBlueSoft = Color(0xFFEAF2FF)
private val ActionGreen = Color(0xFF18A572)
private val ActionGreenSoft = Color(0xFFE9FAF2)
private val ActionAmber = Color(0xFFF59E0B)
private val ActionAmberSoft = Color(0xFFFFF4DD)
private val ActionRed = Color(0xFFE53935)
private val ActionRedSoft = Color(0xFFFFEEEE)
private val ActionBorder = Color(0xFFE3E8EF)

@RequiresApi(Build.VERSION_CODES.O)
private fun doctorCallWindowState(
    appt: Appointment
): DoctorCallWindowState {
    val date =
        appt.date
            ?: return DoctorCallWindowState.NOT_APPLICABLE

    val time =
        appt.time
            ?.take(5)
            ?: return DoctorCallWindowState.NOT_APPLICABLE

    val scheduledAt =
        try {
            LocalDateTime.parse(
                "${date}T$time:00"
            )
        } catch (e: Exception) {
            return DoctorCallWindowState.NOT_APPLICABLE
        }

    val minutesUntilStart =
        Duration.between(
            LocalDateTime.now(),
            scheduledAt
        ).toMinutes()

    return when {
        minutesUntilStart > 5 ->
            DoctorCallWindowState.TOO_EARLY

        minutesUntilStart >= -60 ->
            DoctorCallWindowState.OPEN

        else ->
            DoctorCallWindowState.CLOSED
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorAppointmentActionSheet(
    appointment: Appointment,
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onStartCall: () -> Unit = {},
    onVerifyCompletionCode: suspend (String) -> Result<Unit> = {
        Result.failure(
            Exception("Not available")
        )
    }
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val scope =
        rememberCoroutineScope()

    var showCancelConfirm by remember {
        mutableStateOf(false)
    }

    var showCodeEntry by remember {
        mutableStateOf(false)
    }

    var codeInput by remember {
        mutableStateOf("")
    }

    var isVerifying by remember {
        mutableStateOf(false)
    }

    var verifyError by remember {
        mutableStateOf<String?>(null)
    }

    val status =
        appointment.status
            ?.lowercase()
            .orEmpty()

    val statusColor =
        when (status) {
            "confirmed", "accepted" ->
                ActionGreen

            "pending" ->
                ActionAmber

            "completed" ->
                ActionBlue

            "cancelled", "declined" ->
                ActionRed

            else ->
                ActionMuted
        }

    val statusBackground =
        when (status) {
            "confirmed", "accepted" ->
                ActionGreenSoft

            "pending" ->
                ActionAmberSoft

            "completed" ->
                ActionBlueSoft

            "cancelled", "declined" ->
                ActionRedSoft

            else ->
                Color(0xFFF0F2F5)
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 6.dp,
                    bottom = 24.dp
                )
        ) {

            // PREMIUM PATIENT SUMMARY
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ActionTealSoft
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = ActionTeal,
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(11.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text =
                                    appointment.patient_name
                                        ?: "Unknown patient",
                                color = ActionInk,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Spacer(
                                modifier = Modifier.height(2.dp)
                            )

                            Text(
                                text =
                                    appointment.reason
                                        ?: "General consultation",
                                color = ActionMuted,
                                fontSize = 10.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(50)
                                )
                                .background(
                                    statusBackground
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 5.dp
                                )
                        ) {
                            Text(
                                text =
                                    appointment.status
                                        ?.replaceFirstChar {
                                            it.uppercase()
                                        }
                                        ?: "Unknown",
                                color = statusColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        DoctorActionInfoChip(
                            icon =
                                Icons.Outlined.CalendarMonth,
                            value =
                                appointment.date ?: "-",
                            accent = ActionBlue,
                            background =
                                ActionBlueSoft,
                            modifier =
                                Modifier.weight(1f)
                        )

                        DoctorActionInfoChip(
                            icon =
                                Icons.Outlined.Schedule,
                            value =
                                appointment.time ?: "-",
                            accent = ActionTeal,
                            background =
                                ActionTealSoft,
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Appointment actions",
                color = ActionInk,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = "Choose what you want to do with this appointment",
                color = ActionMuted,
                fontSize = 10.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            DoctorSheetActionCard(
                icon = Icons.Outlined.Info,
                label = "View details",
                description =
                    "See appointment, payment and visit information",
                tint = ActionBlue,
                soft = ActionBlueSoft,
                onClick = onViewDetails
            )

            if (status == "pending") {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DoctorSheetActionCard(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Confirm appointment",
                    description =
                        "Accept this patient's booking request",
                    tint = ActionGreen,
                    soft = ActionGreenSoft,
                    onClick = onConfirm
                )
            }

            if (
                appointment.appointment_type == "online" &&
                status == "confirmed"
            ) {
                val windowState =
                    doctorCallWindowState(
                        appointment
                    )

                val isActive =
                    windowState ==
                            DoctorCallWindowState.OPEN

                val callLabel =
                    when (windowState) {
                        DoctorCallWindowState.TOO_EARLY ->
                            "Video consultation not open yet"

                        DoctorCallWindowState.OPEN ->
                            "Start video consultation"

                        DoctorCallWindowState.CLOSED ->
                            "Consultation window closed"

                        DoctorCallWindowState.NOT_APPLICABLE ->
                            "Start video consultation"
                    }

                val callDescription =
                    when (windowState) {
                        DoctorCallWindowState.TOO_EARLY ->
                            "Available 5 minutes before the scheduled time"

                        DoctorCallWindowState.OPEN ->
                            "The consultation window is active now"

                        DoctorCallWindowState.CLOSED ->
                            "The appointment call window has already ended"

                        DoctorCallWindowState.NOT_APPLICABLE ->
                            "Open the secure consultation room"
                    }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DoctorSheetActionCard(
                    icon = Icons.Outlined.Videocam,
                    label = callLabel,
                    description = callDescription,
                    tint =
                        if (isActive) {
                            ActionTeal
                        } else {
                            ActionMuted
                        },
                    soft =
                        if (isActive) {
                            ActionTealSoft
                        } else {
                            Color(0xFFF0F2F5)
                        },
                    onClick = {
                        if (isActive) {
                            onStartCall()
                        }
                    }
                )
            }

            if (
                appointment.appointment_type != "online" &&
                status == "confirmed"
            ) {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DoctorSheetActionCard(
                    icon = Icons.Outlined.CheckCircle,
                    label = "Complete in-person visit",
                    description =
                        "Enter the patient's 6-digit completion code",
                    tint = ActionTeal,
                    soft = ActionTealSoft,
                    onClick = {
                        showCodeEntry = true
                    }
                )
            }

            if (status != "cancelled") {
                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                DoctorSheetActionCard(
                    icon = Icons.Outlined.Cancel,
                    label = "Cancel appointment",
                    description =
                        "The patient will be notified about the cancellation",
                    tint = ActionRed,
                    soft = ActionRedSoft,
                    onClick = {
                        showCancelConfirm = true
                    }
                )
            }
        }
    }

    if (showCodeEntry) {
        AlertDialog(
            onDismissRequest = {
                if (!isVerifying) {
                    showCodeEntry = false
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color.White,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                RoundedCornerShape(15.dp)
                            )
                            .background(
                                ActionTealSoft
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = ActionTeal,
                            modifier =
                                Modifier.size(24.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Complete consultation",
                        color = ActionInk,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text =
                            "Ask the patient for their 6-digit completion code. A successful verification marks the visit complete.",
                        color = ActionMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = {
                            if (it.length <= 6) {
                                codeInput =
                                    it.filter(
                                        Char::isDigit
                                    )
                            }
                        },
                        label = {
                            Text("6-digit code")
                        },
                        singleLine = true,
                        enabled = !isVerifying,
                        shape = RoundedCornerShape(15.dp),
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedBorderColor =
                                        ActionTeal,
                                    unfocusedBorderColor =
                                        ActionBorder
                                ),
                        modifier =
                            Modifier.fillMaxWidth()
                    )

                    verifyError?.let {
                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Text(
                            text = it,
                            color = ActionRed,
                            fontSize = 10.5.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isVerifying = true
                            verifyError = null

                            onVerifyCompletionCode(
                                codeInput
                            )
                                .onSuccess {
                                    showCodeEntry = false
                                    onDismiss()
                                }
                                .onFailure {
                                    verifyError =
                                        it.message
                                            ?: "Incorrect code — please try again."
                                }

                            isVerifying = false
                        }
                    },
                    enabled =
                        !isVerifying &&
                                codeInput.length == 6,
                    shape =
                        RoundedCornerShape(14.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                ActionTeal
                        )
                ) {
                    Text(
                        text =
                            if (isVerifying) {
                                "Verifying..."
                            } else {
                                "Verify & complete"
                            },
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCodeEntry = false
                    },
                    enabled = !isVerifying
                ) {
                    Text(
                        text = "Not now",
                        color = ActionMuted
                    )
                }
            }
        )
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = {
                showCancelConfirm = false
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color.White,
            title = {
                Column {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(
                                RoundedCornerShape(15.dp)
                            )
                            .background(
                                ActionRedSoft
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.Cancel,
                            contentDescription = null,
                            tint = ActionRed,
                            modifier =
                                Modifier.size(24.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Cancel appointment?",
                        color = ActionInk,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            },
            text = {
                Text(
                    text =
                        "This booking will be cancelled and the patient will be notified.",
                    color = ActionMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        onCancel()
                    },
                    shape =
                        RoundedCornerShape(14.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                ActionRed
                        )
                ) {
                    Text(
                        text = "Cancel appointment",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCancelConfirm = false
                    }
                ) {
                    Text(
                        text = "Keep appointment",
                        color = ActionMuted
                    )
                }
            }
        )
    }
}

@Composable
private fun DoctorSheetActionCard(
    icon: ImageVector,
    label: String,
    description: String,
    tint: Color,
    soft: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),
        shape = RoundedCornerShape(19.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = soft
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 13.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(
                        RoundedCornerShape(13.dp)
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.65f
                        )
                    ),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier =
                        Modifier.size(21.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(11.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    color =
                        if (tint == ActionRed) {
                            ActionRed
                        } else {
                            ActionInk
                        },
                    fontSize = 12.5.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = description,
                    color = ActionMuted,
                    fontSize = 9.sp,
                    lineHeight = 12.5.sp
                )
            }

            Text(
                text = "›",
                color = tint,
                fontSize = 23.sp
            )
        }
    }
}

@Composable
private fun DoctorActionInfoChip(
    icon: ImageVector,
    value: String,
    accent: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(Color.White)
            .padding(
                horizontal = 10.dp,
                vertical = 9.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(background),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier =
                    Modifier.size(15.dp)
            )
        }

        Spacer(
            modifier = Modifier.width(7.dp)
        )

        Text(
            text = value,
            color = ActionInk,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun DoctorAppointmentDetailsDialog(
    appt: Appointment,
    onDismiss: () -> Unit
) {
    val status =
        appt.status
            ?.lowercase()
            .orEmpty()

    val statusColor =
        when (status) {
            "confirmed", "accepted" ->
                ActionGreen

            "pending" ->
                ActionAmber

            "completed" ->
                ActionBlue

            "cancelled", "declined" ->
                ActionRed

            else ->
                ActionMuted
        }

    val statusBackground =
        when (status) {
            "confirmed", "accepted" ->
                ActionGreenSoft

            "pending" ->
                ActionAmberSoft

            "completed" ->
                ActionBlueSoft

            "cancelled", "declined" ->
                ActionRedSoft

            else ->
                Color(0xFFF0F2F5)
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = {
            Column {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                ActionTealSoft
                            ),
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Icon(
                            imageVector =
                                Icons.Outlined.Person,
                            contentDescription = null,
                            tint = ActionTeal,
                            modifier =
                                Modifier.size(23.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(11.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                appt.patient_name
                                    ?: "Unknown patient",
                            color = ActionInk,
                            fontSize = 15.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )

                        Text(
                            text =
                                appt.reason
                                    ?: "General consultation",
                            color = ActionMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(50)
                        )
                        .background(
                            statusBackground
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        )
                ) {
                    Text(
                        text =
                            appt.status
                                ?.replaceFirstChar {
                                    it.uppercase()
                                }
                                ?: "Unknown",
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(9.dp)
            ) {
                AppointmentDetailsRow(
                    icon =
                        Icons.Outlined.CalendarMonth,
                    label = "Date",
                    value =
                        appt.date ?: "-",
                    accent = ActionBlue,
                    background =
                        ActionBlueSoft
                )

                AppointmentDetailsRow(
                    icon =
                        Icons.Outlined.Schedule,
                    label = "Time",
                    value =
                        appt.time ?: "-",
                    accent = ActionTeal,
                    background =
                        ActionTealSoft
                )

                AppointmentDetailsRow(
                    icon =
                        Icons.Outlined.Info,
                    label = "Reason",
                    value =
                        appt.reason
                            ?: "General consultation",
                    accent = ActionAmber,
                    background =
                        ActionAmberSoft
                )

                appt.payment_method?.let {
                    AppointmentDetailsRow(
                        icon =
                            Icons.Outlined.Payments,
                        label =
                            "Payment method",
                        value = it,
                        accent = ActionGreen,
                        background =
                            ActionGreenSoft
                    )
                }

                appt.amount_paid?.let {
                    AppointmentDetailsRow(
                        icon =
                            Icons.Outlined.Payments,
                        label = "Amount paid",
                        value =
                            "R${"%.2f".format(it)}",
                        accent = ActionGreen,
                        background =
                            ActionGreenSoft
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape =
                    RoundedCornerShape(14.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            ActionTeal
                    )
            ) {
                Text(
                    text = "Done",
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun AppointmentDetailsRow(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    background: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    background.copy(
                        alpha = 0.68f
                    )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            ),
        border =
            BorderStroke(
                1.dp,
                accent.copy(alpha = 0.10f)
            )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(
                        RoundedCornerShape(11.dp)
                    )
                    .background(Color.White),
                contentAlignment =
                    Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier =
                        Modifier.size(17.dp)
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {
                Text(
                    text = label.uppercase(),
                    color = ActionMuted,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = value,
                    color = ActionInk,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
