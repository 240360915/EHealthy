package ehealthy.connect.ui.patientDashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class Appointment(
    val id: String,
    val patient_name: String? = null,
    val reason: String? = null,
    val date: String? = null,
    val time: String? = null,
    val status: String? = null,
    val payment_method: String? = null,
    val amount_paid: Double? = null
)

@Serializable
data class Review(
    val id: String,
    val patient_id: String,
    val doctor_id: String,
    val appointment_id: String? = null,
    val rating: Int,
    val comment: String? = null,
    val created_at: String
)

data class ReviewDisplay(
    val review: Review,
    val doctorName: String
)

data class QuickAction(
    val label: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@RequiresApi(Build.VERSION_CODES.O)
fun isUpcomingAppointment(appt: Appointment): Boolean {
    if (appt.status == "cancelled") return false
    val date = appt.date ?: return true
    return date >= LocalDate.now().toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboard(
    patientName: String,
    patientAvatarUrl: String?,
    isUploadingPhoto: Boolean,
    onNavigateFindDoctors: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigateHealthTips: () -> Unit,
    onUploadPhoto: () -> Unit,
    onLogout: () -> Unit,
    fetchAppointments: suspend () -> Result<List<Appointment>>,
    fetchReviews: suspend () -> Result<List<ReviewDisplay>>
) {
    val background = Color(0xFFF0F4F8)
    val navy = Color(0xFF0F1F3D)
    val green = Color(0xFF10B981)
    val accent = Color(0xFF3B82F6)
    val muted = Color(0xFF64748B)

    var selectedTab by remember { mutableStateOf(PatientTab.HOME) }

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoadingAppointments by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var reviews by remember { mutableStateOf<List<ReviewDisplay>>(emptyList()) }
    var isLoadingReviews by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = fetchAppointments()
        isLoadingAppointments = false
        result
            .onSuccess { appointments = it }
            .onFailure { loadError = it.message ?: "Failed to load appointments." }
    }

    LaunchedEffect(Unit) {
        val result = fetchReviews()
        isLoadingReviews = false
        result.onSuccess { reviews = it }
    }

    val onTabSelected: (PatientTab) -> Unit = { tab ->
        when (tab) {
            PatientTab.FIND_DOCTORS -> onNavigateFindDoctors()
            PatientTab.APPOINTMENTS -> onNavigateAppointments()
            else -> selectedTab = tab
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "e-Health Connect",
                            color = navy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text("health-care made easier for you", color = green, fontSize = 11.sp)
                    }
                },
                actions = {
                    PatientAvatar(
                        avatarUrl = patientAvatarUrl,
                        name = patientName,
                        size = 36.dp,
                        onClick = { selectedTab = PatientTab.PROFILE }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            PatientBottomNavBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
        }
    ) { paddingValues ->
        when (selectedTab) {
            PatientTab.HOME -> HomeTabContent(
                modifier = Modifier.padding(paddingValues),
                patientName = patientName,
                background = background,
                navy = navy,
                accent = accent,
                muted = muted,
                appointments = appointments,
                isLoadingAppointments = isLoadingAppointments,
                loadError = loadError,
                reviews = reviews,
                isLoadingReviews = isLoadingReviews,
                onNavigateFindDoctors = onNavigateFindDoctors,
                onNavigateAppointments = onNavigateAppointments,
                onNavigateMedicalRecords = onNavigateMedicalRecords,
                onNavigateHealthTips = onNavigateHealthTips
            )

            PatientTab.PROFILE -> ProfileTabContent(
                modifier = Modifier.padding(paddingValues),
                patientName = patientName,
                patientAvatarUrl = patientAvatarUrl,
                isUploadingPhoto = isUploadingPhoto,
                background = background,
                navy = navy,
                muted = muted,
                onUploadPhoto = onUploadPhoto,
                onNavigateMedicalRecords = onNavigateMedicalRecords,
                onLogout = onLogout
            )

            else -> {}
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HomeTabContent(
    modifier: Modifier,
    patientName: String,
    background: Color,
    navy: Color,
    accent: Color,
    muted: Color,
    appointments: List<Appointment>,
    isLoadingAppointments: Boolean,
    loadError: String?,
    reviews: List<ReviewDisplay>,
    isLoadingReviews: Boolean,
    onNavigateFindDoctors: () -> Unit,
    onNavigateAppointments: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onNavigateHealthTips: () -> Unit
) {
    val upcoming = remember(appointments) { appointments.filter { isUpcomingAppointment(it) } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 20.dp)
    ) {
        item {
            Text(
                "Welcome back, $patientName 👋",
                color = navy,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Everything you need, one tap away.", color = muted, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            val actions = listOf(
                QuickAction(
                    "Medical Records",
                    "View your health history",
                    Icons.Outlined.Description,
                    onNavigateMedicalRecords
                ),
                QuickAction(
                    "Health Tips",
                    "Wellness advice & guidance",
                    Icons.Outlined.Lightbulb,
                    onNavigateHealthTips
                )
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                actions.forEachIndexed { index, action ->
                    QuickActionCard(action, accent, navy, muted, modifier = Modifier.weight(1f))
                    if (index < actions.lastIndex) Spacer(modifier = Modifier.width(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(
                "UPCOMING APPOINTMENTS",
                color = navy, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (isLoadingAppointments) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accent)
                }
            }
        } else if (loadError != null) {
            item { Text(loadError, color = Color(0xFFEF4444), fontSize = 13.sp) }
        } else if (upcoming.isEmpty()) {
            item { EmptyAppointmentsCard(onNavigateFindDoctors) }
        } else {
            items(upcoming.take(3)) { appt ->
                AppointmentCard(appt)
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (upcoming.size > 3) {
                item {
                    TextButton(onClick = onNavigateAppointments) {
                        Text(
                            "View all appointments →",
                            color = accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "REVIEWS",
                color = navy, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (isLoadingReviews) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = accent)
                }
            }
        } else if (reviews.isEmpty()) {
            item { EmptyReviewsCard(onNavigateAppointments) }
        } else {
            items(reviews.take(3)) { rd ->
                ReviewCard(rd)
                Spacer(modifier = Modifier.height(12.dp))
            }
            item {
                TextButton(onClick = onNavigateAppointments) {
                    Text(
                        "Rate another visit →",
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ProfileTabContent(
    modifier: Modifier,
    patientName: String,
    patientAvatarUrl: String?,
    isUploadingPhoto: Boolean,
    background: Color,
    navy: Color,
    muted: Color,
    onUploadPhoto: () -> Unit,
    onNavigateMedicalRecords: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            PatientAvatar(avatarUrl = patientAvatarUrl, name = patientName, size = 96.dp)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .clickable(enabled = !isUploadingPhoto, onClick = onUploadPhoto),
                contentAlignment = Alignment.Center
            ) {
                if (isUploadingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = navy
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Change photo",
                        tint = navy,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(patientName, color = navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        if (patientAvatarUrl.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Add a profile picture",
                        color = navy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Upload a photo, or we'll keep showing your initial.",
                        color = muted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onUploadPhoto,
                        contentPadding = PaddingValues(0.dp),
                        enabled = !isUploadingPhoto
                    ) {
                        Text(
                            if (isUploadingPhoto) "Uploading…" else "Upload photo →",
                            color = Color(0xFF3B82F6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        ProfileMenuItem(
            Icons.Outlined.Description,
            "Medical Records",
            navy,
            onClick = onNavigateMedicalRecords
        )
        Spacer(modifier = Modifier.height(8.dp))
        ProfileMenuItem(Icons.Outlined.ExitToApp, "Logout", Color(0xFFEF4444), onClick = onLogout)
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    accent: Color,
    navy: Color,
    muted: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = action.onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(action.label, color = navy, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(action.description, color = muted, fontSize = 11.5.sp)
        }
    }
}

@Composable
private fun EmptyAppointmentsCard(onFindDoctor: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("No upcoming appointments.", color = Color(0xFF64748B), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(onClick = onFindDoctor) {
                Text(
                    "Find a doctor to book one",
                    color = Color(0xFF3B82F6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyReviewsCard(onGoRate: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.RateReview,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("You haven't left any reviews yet.", color = Color(0xFF64748B), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(onClick = onGoRate) {
                Text(
                    "Rate a completed visit",
                    color = Color(0xFF3B82F6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(rd: ReviewDisplay) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rd.doctorName,
                    color = Color(0xFF0F1F3D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Row {
                    repeat(5) { i ->
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (i < rd.review.rating) Color(0xFFF59E0B) else Color(0xFFE2E8F0),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            rd.review.comment?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it, color = Color(0xFF64748B), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun AppointmentCard(appt: Appointment) {
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxWidth()
                    .background(statusColor)
            )
        }
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "📅 ${appt.date ?: "-"}   🕐 ${appt.time ?: "-"}",
                color = Color(0xFF1E293B),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "📝 ${appt.reason ?: "General consultation"}",
                color = Color(0xFF64748B),
                fontSize = 13.sp
            )
            appt.payment_method?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("💳 Paid via $it", color = Color(0xFF64748B), fontSize = 13.sp)
            }
            appt.amount_paid?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text("💰 R %.2f".format(it), color = Color(0xFF64748B), fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}