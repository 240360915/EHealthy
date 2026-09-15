package ehealthy.connect.ui.patientDashboard

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private val navy = Color(0xFF0B1828)
private val ink = Color(0xFF0F1F3D)
private val muted = Color(0xFF64748B)
private val teal = Color(0xFF0D9488)
private val bg = Color(0xFFF4F7FA)

enum class AppointmentCategory(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

@RequiresApi(Build.VERSION_CODES.O)
fun categorize(appt: Appointment): AppointmentCategory {
    if (appt.status == "cancelled") return AppointmentCategory.CANCELLED
    val date = appt.date
    return if (appt.status == "confirmed" && date != null && date < LocalDate.now().toString()) {
        AppointmentCategory.COMPLETED
    } else {
        AppointmentCategory.UPCOMING
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientAppointmentsScreen(
    onBack: () -> Unit,
    onFindDoctors: () -> Unit,
    fetchAppointments: suspend () -> Result<List<Appointment>>,
    fetchReviewedAppointmentIds: suspend () -> Result<Set<String>>,
    onSubmitReview: (appointmentId: String, doctorId: String?, rating: Int, comment: String) -> Unit,
    isSubmittingReview: Boolean
) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var reviewedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf(AppointmentCategory.ALL) }
    var reviewTargetAppointmentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val apptResult = fetchAppointments()
        val reviewResult = fetchReviewedAppointmentIds()
        isLoading = false
        apptResult
            .onSuccess { appointments = it.sortedByDescending { a -> a.date ?: "" } }
            .onFailure { loadError = it.message ?: "Failed to load appointments." }
        reviewResult.onSuccess { reviewedIds = it }
    }

    val filtered = remember(appointments, selectedCategory) {
        if (selectedCategory == AppointmentCategory.ALL) appointments
        else appointments.filter { categorize(it) == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Appointments", color = ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = bg
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppointmentCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label, fontSize = 13.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = navy,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = muted
                        ),
                        border = null
                    )
                }
            }

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = teal)
                    }
                }
                loadError != null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(loadError ?: "", color = Color(0xFFEF4444), fontSize = 13.sp)
                    }
                }
                filtered.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No appointments in this category.", color = muted, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(onClick = onFindDoctors) {
                                Text("Find a doctor to book one", color = teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { appt ->
                            val category = categorize(appt)
                            AppointmentDetailCard(
                                appt = appt,
                                showLeaveReview = category == AppointmentCategory.COMPLETED && appt.id !in reviewedIds,
                                onLeaveReview = { reviewTargetAppointmentId = appt.id }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }

    reviewTargetAppointmentId?.let { apptId ->
        val appt = appointments.find { it.id == apptId }
        ReviewDialog(
            isSubmitting = isSubmittingReview,
            onDismiss = { reviewTargetAppointmentId = null },
            onSubmit = { rating, comment ->
                onSubmitReview(apptId, null, rating, comment)
                reviewTargetAppointmentId = null
            }
        )
    }
}

@Composable
private fun ReviewDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate your visit", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row {
                    repeat(5) { i ->
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (i < rating) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                            modifier = Modifier
                                .size(32.dp)
                                .padding(2.dp)
                                .then(
                                    Modifier.background(Color.Transparent)
                                )
                                .clickableStar { rating = i + 1 }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(rating, comment) }, enabled = !isSubmitting) {
                Text(if (isSubmitting) "Submitting…" else "Submit Review", color = teal, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = muted) }
        }
    )
}

@SuppressLint("SuspiciousModifierThen")
private fun Modifier.clickableStar(onClick: () -> Unit): Modifier =
    this.then(clickable(onClick = onClick))

@Composable
private fun AppointmentDetailCard(
    appt: Appointment,
    showLeaveReview: Boolean,
    onLeaveReview: () -> Unit
) {
    val statusColor = when (appt.status) {
        "confirmed" -> Color(0xFF10B981)
        "pending" -> Color(0xFFF59E0B)
        "rescheduled" -> Color(0xFF3B82F6)
        "cancelled" -> Color(0xFFEF4444)
        else -> Color(0xFF94A3B8)
    }
    val statusBg = when (appt.status) {
        "confirmed" -> Color(0xFFF0FDF4)
        "pending" -> Color(0xFFFFFBEB)
        "rescheduled" -> Color(0xFFEFF6FF)
        "cancelled" -> Color(0xFFFEF2F2)
        else -> Color(0xFFF8FAFF)
    }
    val statusLabel = when (appt.status) {
        "confirmed" -> "Accepted & Paid"
        "cancelled" -> "Rejected & Refunded"
        else -> appt.status?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("📅 ${appt.date ?: "-"}   🕐 ${appt.time ?: "-"}", color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Box(modifier = Modifier.background(statusBg, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text("📝 ${appt.reason ?: "General consultation"}", color = muted, fontSize = 13.sp)
            appt.payment_method?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("💳 Paid via $it", color = muted, fontSize = 13.sp)
            }
            appt.amount_paid?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("💰 R %.2f".format(it), color = muted, fontSize = 13.sp)
            }
            if (showLeaveReview) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onLeaveReview) {
                    Text("Leave a Review", color = teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}