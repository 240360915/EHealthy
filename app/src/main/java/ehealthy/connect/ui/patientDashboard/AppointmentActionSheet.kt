package ehealthy.connect.ui.patientDashboard

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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onRequestCompletionCode: suspend (String) -> Result<String>
) {

    val sheetState =
        rememberModalBottomSheetState()

    val scope =
        rememberCoroutineScope()


    var showCancelConfirm by remember {
        mutableStateOf(false)
    }

    var isRequestingCode by remember {
        mutableStateOf(false)
    }

    var displayedCode by remember {
        mutableStateOf(
            appointment.completion_code
        )
    }

    var codeError by remember {
        mutableStateOf<String?>(null)
    }

    var showCodeDialog by remember {
        mutableStateOf(false)
    }


    val isOnline =
        appointment.appointment_type
            ?.equals(
                "online",
                ignoreCase = true
            ) == true


    val normalizedStatus =
        appointment.status
            ?.lowercase()
            ?.trim()


    val canModify =
        normalizedStatus !in setOf(
            "cancelled",
            "completed",
            "rejected",
            "declined"
        )


    val canUseCompletionCode =
        !isOnline &&
                normalizedStatus == "confirmed"

    val statusColor =
        when (appointment.status) {

            "confirmed" ->
                PatientColors.SuccessAccent

            "pending" ->
                PatientColors.ReviewAccent

            "rescheduled" ->
                PatientColors.AppointmentAccent

            "cancelled" ->
                PatientColors.Red

            else ->
                PatientColors.TextSecondary
        }


    val statusBackground =
        when (appointment.status) {

            "confirmed" ->
                PatientColors.SuccessCard

            "pending" ->
                PatientColors.ReviewCard

            "rescheduled" ->
                PatientColors.AppointmentCard

            "cancelled" ->
                PatientColors.RedSoft

            else ->
                PatientColors.NeutralCard
        }


    val statusLabel =
        when (appointment.status) {

            "confirmed" ->
                "Confirmed"

            "cancelled" ->
                "Cancelled"

            else ->
                appointment.status
                    ?.replaceFirstChar {
                        it.uppercase()
                    }
                    ?: "Unknown"
        }


    val visitAccent =
        if (isOnline) {
            PatientColors.AppointmentAccent
        } else {
            PatientColors.DoctorAccent
        }


    val visitBackground =
        if (isOnline) {
            PatientColors.AppointmentCard
        } else {
            PatientColors.DoctorCard
        }


    ModalBottomSheet(
        onDismissRequest =
            onDismiss,

        sheetState =
            sheetState,

        containerColor =
            Color.White,

        shape =
            RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp
                    )
                    .padding(
                        bottom = 24.dp
                    )
        ) {

            /*
             * Header
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                48.dp
                            )
                            .background(
                                PatientColors.AppointmentCard,
                                RoundedCornerShape(
                                    15.dp
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.CalendarMonth,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.AppointmentAccent,
                        modifier =
                            Modifier.size(
                                23.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Appointment options",
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            17.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )


                    Text(
                        text =
                            "Manage this appointment",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp
                    )
                }


                Box(
                    modifier =
                        Modifier
                            .background(
                                statusBackground,
                                RoundedCornerShape(
                                    20.dp
                                )
                            )
                            .padding(
                                horizontal = 9.dp,
                                vertical = 5.dp
                            )
                ) {

                    Text(
                        text =
                            statusLabel,
                        color =
                            statusColor,
                        fontSize =
                            9.5.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * Appointment summary
             */
            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(
                        20.dp
                    ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.NeutralCard
                    ),
                border =
                    BorderStroke(
                        width = 1.dp,
                        color =
                            PatientColors.AppointmentAccent
                                .copy(alpha = 0.08f)
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            14.dp
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        AppointmentSummaryBox(
                            modifier =
                                Modifier.weight(1f),
                            icon =
                                Icons.Outlined.CalendarMonth,
                            label =
                                "Date",
                            value =
                                appointment.date ?: "-",
                            accent =
                                PatientColors.AppointmentAccent,
                            background =
                                PatientColors.AppointmentCard
                        )


                        AppointmentSummaryBox(
                            modifier =
                                Modifier.weight(1f),
                            icon =
                                Icons.Outlined.Schedule,
                            label =
                                "Time",
                            value =
                                appointment.time ?: "-",
                            accent =
                                PatientColors.DoctorAccent,
                            background =
                                PatientColors.DoctorCard
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                9.dp
                            )
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    visitBackground,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                                .padding(
                                    11.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                if (isOnline) {
                                    Icons.Outlined.VideoCall
                                } else {
                                    Icons.Outlined.LocationOn
                                },
                            contentDescription =
                                null,
                            tint =
                                visitAccent,
                            modifier =
                                Modifier.size(
                                    18.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    if (isOnline) {
                                        "Online consultation"
                                    } else {
                                        "In-person appointment"
                                    },
                                color =
                                    visitAccent,
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize =
                                    11.sp
                            )


                            Text(
                                text =
                                    appointment.reason
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?: "General consultation",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp,
                                maxLines =
                                    2
                            )
                        }
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        17.dp
                    )
            )


            Text(
                text =
                    "Actions",
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    10.sp,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier.padding(
                        horizontal = 3.dp
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        7.dp
                    )
            )


            /*
             * View details
             */
            SheetActionRow(
                icon =
                    Icons.Outlined.Info,
                title =
                    "View details",
                subtitle =
                    "See your complete appointment information",
                tint =
                    PatientColors.AppointmentAccent,
                background =
                    PatientColors.AppointmentCard,
                onClick =
                    onViewDetails
            )


            /*
             * Reschedule
             */
            if (canModify) {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                SheetActionRow(
                    icon =
                        Icons.Outlined.CalendarMonth,
                    title =
                        "Reschedule appointment",
                    subtitle =
                        "Choose a different appointment time",
                    tint =
                        PatientColors.DoctorAccent,
                    background =
                        PatientColors.DoctorCard,
                    onClick =
                        onReschedule
                )
            }


            /*
             * Completion code
             */
            if (canUseCompletionCode) {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                SheetActionRow(
                    icon =
                        Icons.Outlined.Key,
                    title =
                        if (displayedCode != null) {
                            "View completion code"
                        } else {
                            "Request completion code"
                        },
                    subtitle =
                        if (displayedCode != null) {
                            "Show the code for this appointment"
                        } else {
                            "Generate a code for your doctor after the visit"
                        },
                    tint =
                        PatientColors.Purple,
                    background =
                        PatientColors.PurpleSoft,
                    onClick = {

                        if (displayedCode != null) {

                            showCodeDialog =
                                true

                        } else {

                            scope.launch {

                                isRequestingCode =
                                    true

                                codeError =
                                    null


                                onRequestCompletionCode(
                                    appointment.id
                                )
                                    .onSuccess { code ->

                                        displayedCode =
                                            code

                                        showCodeDialog =
                                            true
                                    }
                                    .onFailure {

                                        codeError =
                                            it.message
                                                ?: "Could not generate a code. Please try again."
                                    }


                                isRequestingCode =
                                    false
                            }
                        }
                    }
                )


                if (isRequestingCode) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.PurpleSoft,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                                .padding(
                                    11.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator(
                            color =
                                PatientColors.Purple,
                            modifier =
                                Modifier.size(
                                    18.dp
                                ),
                            strokeWidth =
                                2.dp
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    9.dp
                                )
                        )


                        Text(
                            text =
                                "Generating your completion code...",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }
                }


                codeError?.let { error ->

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.RedSoft,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                                .padding(
                                    11.dp
                                ),
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Info,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Red,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Text(
                            text =
                                error,
                            modifier =
                                Modifier.weight(1f),
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp,
                            lineHeight =
                                15.sp
                        )
                    }
                }
            }


            /*
             * Cancel
             */
            if (canModify) {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                SheetActionRow(
                    icon =
                        Icons.Outlined.Cancel,
                    title =
                        "Cancel appointment",
                    subtitle =
                        "Cancel this scheduled visit",
                    tint =
                        PatientColors.Red,
                    background =
                        PatientColors.RedSoft,
                    onClick = {
                        showCancelConfirm =
                            true
                    }
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )
        }
    }


    /*
     * Completion code dialog
     */
    if (
        showCodeDialog &&
        displayedCode != null
    ) {

        AlertDialog(
            onDismissRequest = {
                showCodeDialog =
                    false
            },

            shape =
                RoundedCornerShape(
                    26.dp
                ),

            containerColor =
                Color.White,

            title = {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    58.dp
                                )
                                .background(
                                    PatientColors.PurpleSoft,
                                    CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Key,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Purple,
                            modifier =
                                Modifier.size(
                                    28.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    Text(
                        text =
                            "Completion code",
                        color =
                            PatientColors.TextPrimary,
                        fontSize =
                            18.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }
            },

            text = {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "Give this code to your doctor only after your consultation is complete.",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            11.sp,
                        lineHeight =
                            16.sp,
                        textAlign =
                            TextAlign.Center
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                16.dp
                            )
                    )


                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                19.dp
                            ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PatientColors.PurpleSoft
                            ),
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color =
                                    PatientColors.Purple
                                        .copy(alpha = 0.12f)
                            ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 20.dp,
                                        horizontal = 12.dp
                                    ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "YOUR CODE",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        6.dp
                                    )
                            )


                            Text(
                                text =
                                    displayedCode ?: "",
                                color =
                                    PatientColors.Purple,
                                fontSize =
                                    30.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing =
                                    5.sp
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.ReviewCard,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                                .padding(
                                    11.dp
                                ),
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Info,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.ReviewAccent,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Text(
                            text =
                                "Do not share this code before the appointment has finished.",
                            modifier =
                                Modifier.weight(1f),
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.sp,
                            lineHeight =
                                14.sp
                        )
                    }
                }
            },

            confirmButton = {

                Button(
                    onClick = {
                        showCodeDialog =
                            false
                    },
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PatientColors.Purple,
                            contentColor =
                                Color.White
                        )
                ) {

                    Text(
                        text =
                            "Done",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        )
    }


    /*
     * Cancel confirmation
     */
    if (showCancelConfirm) {

        AlertDialog(
            onDismissRequest = {
                showCancelConfirm =
                    false
            },

            shape =
                RoundedCornerShape(
                    26.dp
                ),

            containerColor =
                Color.White,

            title = {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    58.dp
                                )
                                .background(
                                    PatientColors.RedSoft,
                                    CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Cancel,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Red,
                            modifier =
                                Modifier.size(
                                    28.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )


                    Text(
                        text =
                            "Cancel appointment?",
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            18.sp,
                        textAlign =
                            TextAlign.Center
                    )
                }
            },

            text = {

                Column(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "Are you sure you want to cancel this appointment? This action cannot be undone.",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            11.sp,
                        lineHeight =
                            16.sp,
                        textAlign =
                            TextAlign.Center
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.RedSoft,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                                .padding(
                                    11.dp
                                ),
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Info,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Red,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Text(
                            text =
                                "Your doctor will be notified that the appointment was cancelled.",
                            modifier =
                                Modifier.weight(1f),
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.sp,
                            lineHeight =
                                14.sp
                        )
                    }
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        showCancelConfirm =
                            false

                        onCancel()
                    },
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PatientColors.Red,
                            contentColor =
                                Color.White
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Cancel,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                17.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )


                    Text(
                        text =
                            "Yes, cancel",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showCancelConfirm =
                            false
                    }
                ) {

                    Text(
                        text =
                            "Keep appointment",
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        )
    }
}


@Composable
private fun SheetActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),
        shape =
            RoundedCornerShape(
                17.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        13.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(
                            39.dp
                        )
                        .background(
                            tint.copy(
                                alpha = 0.11f
                            ),
                            RoundedCornerShape(
                                12.dp
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        null,
                    tint =
                        tint,
                    modifier =
                        Modifier.size(
                            19.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        11.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        title,
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        12.5.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )


                Text(
                    text =
                        subtitle,
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        9.5.sp,
                    lineHeight =
                        13.sp
                )
            }


            Icon(
                imageVector =
                    Icons.Outlined.ChevronRight,
                contentDescription =
                    null,
                tint =
                    tint.copy(
                        alpha = 0.65f
                    ),
                modifier =
                    Modifier.size(
                        19.dp
                    )
            )
        }
    }
}


@Composable
private fun AppointmentSummaryBox(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    background: Color
) {

    Row(
        modifier =
            modifier
                .background(
                    background,
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .padding(
                    10.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        30.dp
                    )
                    .background(
                        accent.copy(
                            alpha = 0.10f
                        ),
                        RoundedCornerShape(
                            9.dp
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    accent,
                modifier =
                    Modifier.size(
                        15.dp
                    )
            )
        }


        Spacer(
            modifier =
                Modifier.width(
                    7.dp
                )
        )


        Column {

            Text(
                text =
                    label,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    8.5.sp
            )


            Text(
                text =
                    value,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    10.5.sp,
                fontWeight =
                    FontWeight.Bold,
                maxLines =
                    1
            )
        }
    }
}