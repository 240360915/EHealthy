package ehealthy.connect.ui.patientDashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.serialization.Serializable

/**
 * Full doctor record as stored in the `doctors` table.
 * Every field is nullable so a missing column never breaks decoding —
 * rename any field below that doesn't match your actual column name.
 */
@Serializable
data class DoctorProfile(
    val id: String,
    val name: String? = null,
    val surname: String? = null,
    val discipline: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val hourly_rate: Double? = null,
    val profile_image_url: String? = null,
    val operating_hours: String? = null,
    val location: String? = null,
    val bio: String? = null,
    val qualification: String? = null,
    val hpcsa_number: String? = null,
    val years_of_experience: Int? = null,
    val languages: String? = null,
    val verification_status: String? = null
)

/** One review row shown on the profile. */
data class DoctorReviewItem(
    val rating: Int,
    val comment: String? = null,
    val patientName: String? = null,
    val createdAt: String? = null
)

private val navy = Color(0xFF0B1828)
private val ink = Color(0xFF0F1F3D)
private val muted = Color(0xFF64748B)
private val teal = Color(0xFF0D9488)
private val tealSoft = Color(0xFFCCFBF1)
private val bg = Color(0xFFF4F7FA)
private val amber = Color(0xFFF59E0B)
private val red = Color(0xFFDC2626)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    onBack: () -> Unit,
    onBookAppointment: (doctorId: String) -> Unit,
    fetchDoctor: suspend () -> Result<DoctorProfile>,
    fetchReviews: suspend () -> Result<List<DoctorReviewItem>>
) {
    var doctor by remember { mutableStateOf<DoctorProfile?>(null) }
    var reviews by remember { mutableStateOf<List<DoctorReviewItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val docResult = fetchDoctor()
        val reviewResult = fetchReviews()
        isLoading = false
        docResult
            .onSuccess { doctor = it }
            .onFailure { loadError = it.message ?: "Could not load this doctor's profile." }
        reviewResult.onSuccess { reviews = it }
    }

    fun safeStart(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No app on the device can handle it — fail quietly.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Doctor Profile",
                        color = ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ink)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            doctor?.let { doc ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(tealSoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Consultation rate",
                            color = teal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            doc.hourly_rate?.let { "R %.2f/hr".format(it) } ?: "Rate on request",
                            color = teal,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onBookAppointment(doc.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = navy),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Book an Appointment",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        containerColor = bg
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = teal)
                }
            }

            doctor == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        loadError ?: "Doctor not found.",
                        color = red,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                val doc = doctor!!
                val averageRating =
                    if (reviews.isEmpty()) null else reviews.sumOf { it.rating } / reviews.size.toDouble()

                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item { ProfileHeader(doc, averageRating, reviews.size) }

                    item {
                        QuickStats(
                            rating = averageRating,
                            reviewCount = reviews.size,
                            yearsOfExperience = doc.years_of_experience,
                            rate = doc.hourly_rate
                        )
                    }

                    if (!doc.bio.isNullOrBlank()) {
                        item {
                            InfoCard(title = "About") {
                                Text(doc.bio, color = muted, fontSize = 13.5.sp, lineHeight = 20.sp)
                            }
                        }
                    }

                    item {
                        InfoCard(title = "Practice Details") {
                            InfoRow(
                                Icons.Outlined.LocationOn,
                                "Location",
                                doc.location ?: "Not provided"
                            )
                            InfoRow(
                                Icons.Outlined.Schedule,
                                "Operating hours",
                                doc.operating_hours ?: "Not provided"
                            )
                            InfoRow(
                                Icons.Outlined.Payments,
                                "Consultation rate",
                                doc.hourly_rate?.let { "R %.2f per hour".format(it) }
                                    ?: "On request"
                            )
                            if (!doc.languages.isNullOrBlank()) {
                                InfoRow(Icons.Outlined.Translate, "Languages", doc.languages)
                            }

                            if (!doc.location.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                ActionPill(
                                    icon = Icons.Outlined.Directions,
                                    label = "Get directions"
                                ) {
                                    safeStart(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("geo:0,0?q=${Uri.encode(doc.location)}")
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item {
                        InfoCard(title = "Credentials") {
                            InfoRow(
                                Icons.Outlined.WorkspacePremium,
                                "Qualification",
                                doc.qualification ?: "Not provided"
                            )
                            InfoRow(
                                Icons.Outlined.Badge,
                                "HPCSA number",
                                doc.hpcsa_number ?: "Not provided"
                            )
                            InfoRow(
                                Icons.Outlined.Verified,
                                "Verification",
                                when (doc.verification_status) {
                                    "approved" -> "Verified by eHealthy Connect"
                                    null -> "Not provided"
                                    else -> doc.verification_status.replaceFirstChar { it.uppercase() }
                                }
                            )
                            doc.years_of_experience?.let {
                                InfoRow(
                                    Icons.Outlined.WorkspacePremium,
                                    "Experience",
                                    "$it year${if (it == 1) "" else "s"}"
                                )
                            }
                        }
                    }

                    item {
                        InfoCard(title = "Contact") {
                            InfoRow(Icons.Outlined.Phone, "Phone", doc.phone ?: "Not provided")
                            InfoRow(Icons.Outlined.Email, "Email", doc.email ?: "Not provided")

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!doc.phone.isNullOrBlank()) {
                                    ActionPill(icon = Icons.Outlined.Phone, label = "Call") {
                                        safeStart(
                                            Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:${doc.phone}")
                                            )
                                        )
                                    }
                                }
                                if (!doc.email.isNullOrBlank()) {
                                    ActionPill(icon = Icons.Outlined.Email, label = "Email") {
                                        safeStart(
                                            Intent(
                                                Intent.ACTION_SENDTO,
                                                Uri.parse("mailto:${doc.email}")
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        InfoCard(
                            title = if (reviews.isEmpty()) "Patient Reviews"
                            else "Patient Reviews (${reviews.size})"
                        ) {
                            if (reviews.isEmpty()) {
                                Text(
                                    "No reviews yet — be the first to leave one after your visit.",
                                    color = muted,
                                    fontSize = 13.sp
                                )
                            } else {
                                reviews.take(5).forEach { ReviewRow(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(doc: DoctorProfile, rating: Double?, reviewCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(navy, Color(0xFF1A3A5C))))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(teal),
                contentAlignment = Alignment.Center
            ) {
                if (!doc.profile_image_url.isNullOrBlank()) {
                    AsyncImage(
                        model = doc.profile_image_url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Text(
                        "${doc.name?.firstOrNull() ?: ' '}${doc.surname?.firstOrNull() ?: ' '}"
                            .uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Dr. ${doc.name ?: ""} ${doc.surname ?: ""}".trim(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                doc.discipline ?: "General Practitioner",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.5.sp
            )

            if (doc.verification_status == "approved") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(teal.copy(alpha = 0.22f))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Verified,
                        contentDescription = null,
                        tint = Color(0xFF5EEAD4),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Verified doctor",
                        color = Color(0xFF5EEAD4),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (rating != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRow(rating)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "%.1f (%d)".format(rating, reviewCount),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStats(
    rating: Double?,
    reviewCount: Int,
    yearsOfExperience: Int?,
    rate: Double?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(
            value = rating?.let { "%.1f".format(it) } ?: "—",
            label = if (reviewCount == 1) "1 review" else "$reviewCount reviews",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            value = yearsOfExperience?.toString() ?: "—",
            label = "Years exp.",
            modifier = Modifier.weight(1f)
        )
        StatChip(
            value = rate?.let { "R%.0f".format(it) } ?: "—",
            label = "Per hour",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = teal, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = muted, fontSize = 11.5.sp)
            Spacer(modifier = Modifier.height(1.dp))
            Text(value, color = ink, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ActionPill(icon: ImageVector, label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = tealSoft),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = teal, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = teal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StarRow(rating: Double, size: Int = 16) {
    Row {
        repeat(5) { i ->
            Icon(
                if (i < kotlin.math.round(rating).toInt()) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = amber,
                modifier = Modifier.size(size.dp)
            )
        }
    }
}

@Composable
private fun ReviewRow(review: DoctorReviewItem) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                review.patientName ?: "Patient",
                color = ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            StarRow(review.rating.toDouble(), size = 13)
        }
        if (!review.comment.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(review.comment, color = muted, fontSize = 12.5.sp, lineHeight = 18.sp)
        }
        review.createdAt?.take(10)?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}