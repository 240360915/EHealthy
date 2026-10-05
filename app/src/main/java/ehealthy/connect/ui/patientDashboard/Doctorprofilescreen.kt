package ehealthy.connect.ui.patientDashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.round
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.outlined.StarBorder

@Suppress("PropertyName")
@Serializable
data class DoctorProfile(

    val id: String,

    val user_id: String? = null,

    val name: String? = null,

    val surname: String? = null,

    val discipline: String? = null,

    val email: String? = null,

    val phone: String? = null,

    val hourly_rate: Double? = null,

    val profile_image_url: String? = null,

    val operating_hours: String? = null,

    val city: String? = null,

    val province: String? = null,

    val bio: String? = null,

    @SerialName("qualifications")
    val qualification: String? = null,

    val hpcsa_number: String? = null,

    val years_of_experience: Int? = null,

    @SerialName("language")
    val languages: String? = null,

    val verification_status: String? = null,

    val is_deactivated: Boolean? = false
) {

    val fullName: String
        get() =
            listOfNotNull(
                name?.takeIf { it.isNotBlank() },
                surname?.takeIf { it.isNotBlank() }
            )
                .joinToString(" ")
                .let {
                    if (it.isBlank()) {
                        "Doctor"
                    } else {
                        "Dr. $it"
                    }
                }

    val specialty: String
        get() =
            discipline
                ?.takeIf { it.isNotBlank() }
                ?: "General Practitioner"

    val location: String
        get() =
            listOfNotNull(
                city?.takeIf { it.isNotBlank() },
                province?.takeIf { it.isNotBlank() }
            )
                .joinToString(", ")
                .ifBlank {
                    "Location not provided"
                }
}

data class DoctorReviewItem(
    val rating: Int,
    val comment: String? = null,
    val patientName: String? = null,
    val createdAt: String? = null
)

private val HeroStart =
    Color(0xFF0B3B60)

private val HeroEnd =
    Color(0xFF087F8C)

private val SuccessGreen =
    Color(0xFF15803D)

private val WarningAmber =
    Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    onBack: () -> Unit,
    onBookAppointment: (doctorId: String) -> Unit,
    onRequestPrivate: (doctorId: String) -> Unit = {},
    onMessageDoctor: (doctorId: String) -> Unit,
    fetchDoctor: suspend () -> Result<DoctorProfile>,
    fetchReviews: suspend () -> Result<List<DoctorReviewItem>>
) {

    val context =
        LocalContext.current

    var doctor by remember {
        mutableStateOf<DoctorProfile?>(
            null
        )
    }

    var reviews by remember {
        mutableStateOf<List<DoctorReviewItem>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var showAllReviews by remember {
        mutableStateOf(false)
    }

    var showFullBio by remember {
        mutableStateOf(false)
    }

    suspend fun loadProfile(
        refresh: Boolean = false
    ) {

        if (refresh) {
            isRefreshing = true
        } else {
            isLoading = true
        }

        errorMessage = null

        val profileResult =
            fetchDoctor()

        val reviewResult =
            fetchReviews()

        profileResult
            .onSuccess {
                doctor = it
            }
            .onFailure {
                errorMessage =
                    it.message
                        ?: "We could not load this doctor."
            }

        reviewResult
            .onSuccess {
                reviews = it
            }

        isLoading = false

        isRefreshing = false
    }

    LaunchedEffect(Unit) {
        loadProfile()
    }

    fun openIntent(
        intent: Intent
    ) {

        runCatching {
            context.startActivity(
                intent
            )
        }
    }

    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text =
                            "Doctor profile",
                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                },

                actions = {

                    if (
                        doctor != null
                    ) {

                        IconButton(
                            enabled =
                                !isRefreshing,
                            onClick = {

                                isRefreshing = true
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Filled.Refresh,
                                contentDescription =
                                    "Refresh"
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                        )
            )
        },

        bottomBar = {

            doctor?.let { doc ->

                DoctorActionBar(

                    doctor =
                        doc,

                    onPrivate = {

                        onRequestPrivate(
                            doc.id
                        )
                    },

                    onBook = {

                        onBookAppointment(
                            doc.id
                        )
                    }
                )
            }
        }

    ) { innerPadding ->

        if (
            isRefreshing
        ) {

            LaunchedEffect(
                isRefreshing
            ) {

                if (
                    isRefreshing
                ) {

                    loadProfile(
                        refresh = true
                    )
                }
            }
        }

        when {

            isLoading -> {

                ProfileLoadingState(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            )
                )
            }

            doctor == null -> {

                ProfileErrorState(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),

                    message =
                        errorMessage
                            ?: "Doctor not found.",

                    onRetry = {
                        isRefreshing = true
                    }
                )
            }

            else -> {

                val currentDoctor =
                    doctor!!

                val averageRating =
                    if (
                        reviews.isEmpty()
                    ) {

                        null

                    } else {

                        reviews
                            .map {
                                it.rating
                            }
                            .average()
                    }

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),

                    contentPadding =
                        PaddingValues(
                            bottom =
                                32.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )

                ) {

                    item {

                        DoctorHero(
                            doctor =
                                currentDoctor,
                            averageRating =
                                averageRating,
                            reviewCount =
                                reviews.size
                        )
                    }

                    item {

                        DoctorQuickStats(

                            rating =
                                averageRating,

                            reviewCount =
                                reviews.size,

                            experience =
                                currentDoctor
                                    .years_of_experience,

                            rate =
                                currentDoctor
                                    .hourly_rate
                        )
                    }

                    item {

                        PatientChoiceCard()
                    }

                    if (
                        !currentDoctor
                            .bio
                            .isNullOrBlank()
                    ) {

                        item {

                            InformationCard(
                                title =
                                    "About ${
                                        currentDoctor
                                            .name
                                            ?: "this doctor"
                                    }"
                            ) {

                                Column(
                                    modifier =
                                        Modifier
                                            .animateContentSize()
                                ) {

                                    Text(
                                        text =
                                            currentDoctor
                                                .bio
                                                .orEmpty(),
                                        maxLines =
                                            if (
                                                showFullBio
                                            ) {
                                                Int.MAX_VALUE
                                            } else {
                                                4
                                            },
                                        overflow =
                                            TextOverflow
                                                .Ellipsis,
                                        style =
                                            MaterialTheme
                                                .typography
                                                .bodyMedium,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                    )

                                    if (
                                        currentDoctor
                                            .bio
                                            .orEmpty()
                                            .length > 180
                                    ) {

                                        TextButton(
                                            onClick = {

                                                showFullBio =
                                                    !showFullBio
                                            }
                                        ) {

                                            Text(
                                                if (
                                                    showFullBio
                                                ) {
                                                    "Show less"
                                                } else {
                                                    "Read more"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {

                        InformationCard(
                            title = "Practice details",
                            accent = PatientColors.DoctorAccent,
                            background = PatientColors.DoctorCard
                        ) {
                            ProfileInformationRow(
                                icon = Icons.Outlined.LocationOn,
                                title = "Location",
                                value = currentDoctor.location,
                                accent = PatientColors.DoctorAccent
                            )

                            ProfileInformationRow(
                                icon = Icons.Outlined.Schedule,
                                title = "Operating hours",
                                value =
                                    currentDoctor.operating_hours
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?: "Not provided",
                                accent = PatientColors.AppointmentAccent
                            )

                            ProfileInformationRow(
                                icon = Icons.Outlined.Payments,
                                title = "Private consultation",
                                value =
                                    currentDoctor.hourly_rate
                                        ?.let {
                                            "R %.2f per hour".format(it)
                                        }
                                        ?: "Rate unavailable",
                                accent = PatientColors.SuccessAccent
                            )

                            if (
                                !currentDoctor.languages
                                    .isNullOrBlank()
                            ) {

                                ProfileInformationRow(
                                    icon = Icons.Outlined.Translate,
                                    title = "Languages",
                                    value = currentDoctor.languages.orEmpty(),
                                    accent = PatientColors.Purple
                                )
                            }
                        }
                    }

                    item {

                        InformationCard(
                            title = "Professional credentials",
                            accent = PatientColors.Purple,
                            background = PatientColors.RecordsCard
                        ) {

                            ProfileInformationRow(
                                icon = Icons.Outlined.WorkspacePremium,
                                title = "Qualification",
                                value =
                                    currentDoctor.qualification
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?: "Not provided",
                                accent = PatientColors.Purple
                            )

                            ProfileInformationRow(
                                icon = Icons.Outlined.Badge,
                                title = "HPCSA number",
                                value =
                                    currentDoctor.hpcsa_number
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?: "Not provided",
                                accent = PatientColors.AppointmentAccent
                            )

                            ProfileInformationRow(
                                icon = Icons.Outlined.Verified,
                                title = "Verification",
                                value =
                                    if (
                                        currentDoctor.verification_status
                                            .equals(
                                                "approved",
                                                ignoreCase = true
                                            )
                                    ) {
                                        "Verified by eHealth Connect"
                                    } else {
                                        currentDoctor.verification_status
                                            ?.replaceFirstChar {
                                                it.uppercase()
                                            }
                                            ?: "Pending"
                                    },
                                accent =
                                    if (
                                        currentDoctor.verification_status
                                            .equals(
                                                "approved",
                                                ignoreCase = true
                                            )
                                    ) {
                                        PatientColors.SuccessAccent
                                    } else {
                                        PatientColors.ReviewAccent
                                    }
                            )

                            currentDoctor.years_of_experience
                                ?.let { years ->

                                    ProfileInformationRow(
                                        icon = Icons.Outlined.WorkspacePremium,
                                        title = "Experience",
                                        value =
                                            "$years year${
                                                if (years == 1) {
                                                    ""
                                                } else {
                                                    "s"
                                                }
                                            }",
                                        accent = PatientColors.ReviewAccent
                                    )
                                }


                            currentDoctor
                                .years_of_experience
                                ?.let { years ->

                                    ProfileInformationRow(
                                        icon =
                                            Icons.Outlined.WorkspacePremium,
                                        title =
                                            "Experience",
                                        value =
                                            "$years year${
                                                if (
                                                    years == 1
                                                ) {
                                                    ""
                                                } else {
                                                    "s"
                                                }
                                            }"
                                    )
                                }
                        }
                    }

                    item {
                        InformationCard(
                            title = "Contact doctor",
                            accent = PatientColors.Primary,
                            background = PatientColors.NeutralCard
                        ) {

                            Text(
                                text =
                                    "Message the doctor in the app, or use the available phone and email contact options.",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    11.5.sp,
                                lineHeight =
                                    17.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        15.dp
                                    )
                            )


                            /*
                             * Main messaging action
                             */
                            Card(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(
                                        18.dp
                                    ),
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            PatientColors.DoctorCard
                                    ),
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            PatientColors.DoctorAccent
                                                .copy(alpha = 0.10f)
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
                                                14.dp
                                            ),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Box(
                                        modifier =
                                            Modifier
                                                .size(
                                                    44.dp
                                                )
                                                .clip(
                                                    RoundedCornerShape(
                                                        13.dp
                                                    )
                                                )
                                                .background(
                                                    PatientColors.DoctorAccent
                                                        .copy(alpha = 0.12f)
                                                ),
                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Outlined.Chat,
                                            contentDescription =
                                                null,
                                            tint =
                                                PatientColors.DoctorAccent,
                                            modifier =
                                                Modifier.size(
                                                    21.dp
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
                                                "Message doctor",
                                            color =
                                                PatientColors.TextPrimary,
                                            fontWeight =
                                                FontWeight.Bold,
                                            fontSize =
                                                13.5.sp
                                        )


                                        Spacer(
                                            modifier =
                                                Modifier.height(
                                                    2.dp
                                                )
                                        )


                                        Text(
                                            text =
                                                "Continue the conversation in EHealthy",
                                            color =
                                                PatientColors.TextSecondary,
                                            fontSize =
                                                10.5.sp
                                        )
                                    }


                                    Button(
                                        onClick = {
                                            onMessageDoctor(
                                                currentDoctor.id
                                            )
                                        },
                                        shape =
                                            RoundedCornerShape(
                                                13.dp
                                            ),
                                        colors =
                                            ButtonDefaults.buttonColors(
                                                containerColor =
                                                    PatientColors.DoctorAccent,
                                                contentColor =
                                                    Color.White
                                            ),
                                        contentPadding =
                                            PaddingValues(
                                                horizontal = 13.dp,
                                                vertical = 9.dp
                                            )
                                    ) {

                                        Text(
                                            text =
                                                "Open",
                                            fontWeight =
                                                FontWeight.Bold,
                                            fontSize =
                                                11.sp
                                        )
                                    }
                                }
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        13.dp
                                    )
                            )


                            /*
                             * Phone + email
                             */
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        10.dp
                                    )
                            ) {

                                OutlinedButton(
                                    enabled =
                                        !currentDoctor.phone
                                            .isNullOrBlank(),

                                    onClick = {

                                        currentDoctor.phone
                                            ?.let { phone ->

                                                openIntent(
                                                    Intent(
                                                        Intent.ACTION_DIAL,
                                                        Uri.parse(
                                                            "tel:$phone"
                                                        )
                                                    )
                                                )
                                            }
                                    },

                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .height(
                                                49.dp
                                            ),

                                    shape =
                                        RoundedCornerShape(
                                            15.dp
                                        ),

                                    border =
                                        BorderStroke(
                                            width = 1.dp,
                                            color =
                                                PatientColors.SuccessAccent
                                                    .copy(
                                                        alpha =
                                                            if (
                                                                currentDoctor.phone
                                                                    .isNullOrBlank()
                                                            ) {
                                                                0.25f
                                                            } else {
                                                                0.65f
                                                            }
                                                    )
                                        )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.Phone,
                                        contentDescription =
                                            null,
                                        tint =
                                            PatientColors.SuccessAccent,
                                        modifier =
                                            Modifier.size(
                                                18.dp
                                            )
                                    )


                                    Spacer(
                                        modifier =
                                            Modifier.width(
                                                7.dp
                                            )
                                    )


                                    Text(
                                        text =
                                            "Call",
                                        color =
                                            PatientColors.SuccessAccent,
                                        fontWeight =
                                            FontWeight.Bold,
                                        fontSize =
                                            12.sp
                                    )
                                }


                                OutlinedButton(
                                    enabled =
                                        !currentDoctor.email
                                            .isNullOrBlank(),

                                    onClick = {

                                        currentDoctor.email
                                            ?.let { email ->

                                                openIntent(
                                                    Intent(
                                                        Intent.ACTION_SENDTO,
                                                        Uri.parse(
                                                            "mailto:$email"
                                                        )
                                                    )
                                                )
                                            }
                                    },

                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .height(
                                                49.dp
                                            ),

                                    shape =
                                        RoundedCornerShape(
                                            15.dp
                                        ),

                                    border =
                                        BorderStroke(
                                            width = 1.dp,
                                            color =
                                                PatientColors.AppointmentAccent
                                                    .copy(
                                                        alpha =
                                                            if (
                                                                currentDoctor.email
                                                                    .isNullOrBlank()
                                                            ) {
                                                                0.25f
                                                            } else {
                                                                0.65f
                                                            }
                                                    )
                                        )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.Email,
                                        contentDescription =
                                            null,
                                        tint =
                                            PatientColors.AppointmentAccent,
                                        modifier =
                                            Modifier.size(
                                                18.dp
                                            )
                                    )


                                    Spacer(
                                        modifier =
                                            Modifier.width(
                                                7.dp
                                            )
                                    )


                                    Text(
                                        text =
                                            "Email",
                                        color =
                                            PatientColors.AppointmentAccent,
                                        fontWeight =
                                            FontWeight.Bold,
                                        fontSize =
                                            12.sp
                                    )
                                }
                            }


                            if (
                                currentDoctor.phone.isNullOrBlank() &&
                                currentDoctor.email.isNullOrBlank()
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            10.dp
                                        )
                                )


                                Text(
                                    text =
                                        "Phone and email contact details are not currently available.",
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    color =
                                        PatientColors.TextSecondary,
                                    fontSize =
                                        10.5.sp,
                                    textAlign =
                                        TextAlign.Center
                                )
                            }
                        }
                    }

                    item {

                        ReviewsCard(

                            reviews =
                                reviews,

                            averageRating =
                                averageRating,

                            showAll =
                                showAllReviews,

                            onToggle = {

                                showAllReviews =
                                    !showAllReviews
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorHero(
    doctor: DoctorProfile,
    averageRating: Double?,
    reviewCount: Int
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "DoctorHeroAnimation"
        )

    val pulse by
    infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 1500,
                        easing =
                            FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label =
            "DoctorHeroPulse"
    )


    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    PatientColors
                        .ConsultationGradient
                )
                .padding(
                    horizontal = 20.dp,
                    vertical = 28.dp
                )
    ) {

        /*
         * Decorative background shapes
         */
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .size(135.dp)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(
                            alpha = 0.06f
                        )
                    )
        )


        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomStart
                    )
                    .size(85.dp)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(
                            alpha = 0.05f
                        )
                    )
        )


        Column(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            /*
             * Doctor image
             */
            Box(
                modifier =
                    Modifier
                        .size(118.dp)
                        .scale(pulse),
                contentAlignment =
                    Alignment.Center
            ) {

                /*
                 * Outer ring
                 */
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = 0.15f
                                )
                            )
                )


                Box(
                    modifier =
                        Modifier
                            .size(106.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = 0.18f
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    if (
                        !doctor.profile_image_url
                            .isNullOrBlank()
                    ) {

                        AsyncImage(
                            model =
                                doctor.profile_image_url,
                            contentDescription =
                                doctor.fullName,
                            contentScale =
                                ContentScale.Crop,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                        )

                    } else {

                        Text(
                            text =
                                initials(doctor),
                            color =
                                Color.White,
                            fontSize =
                                29.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    }
                }


                /*
                 * Verified indicator
                 */
                if (
                    doctor.verification_status
                        .equals(
                            "approved",
                            ignoreCase = true
                        )
                ) {

                    Box(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.BottomEnd
                                )
                                .size(31.dp)
                                .clip(CircleShape)
                                .background(
                                    Color.White
                                )
                                .padding(3.dp)
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        PatientColors
                                            .SuccessAccent
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Verified,
                                contentDescription =
                                    "Verified doctor",
                                tint =
                                    Color.White,
                                modifier =
                                    Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }


            Spacer(
                Modifier.height(15.dp)
            )


            Text(
                text =
                    doctor.fullName,
                color =
                    Color.White,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    23.sp,
                textAlign =
                    TextAlign.Center
            )


            Spacer(
                Modifier.height(5.dp)
            )


            Text(
                text =
                    doctor.specialty,
                color =
                    Color.White.copy(
                        alpha = 0.86f
                    ),
                fontSize =
                    14.sp,
                fontWeight =
                    FontWeight.Medium
            )


            Spacer(
                Modifier.height(14.dp)
            )


            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                if (
                    doctor.verification_status
                        .equals(
                            "approved",
                            ignoreCase = true
                        )
                ) {

                    HeroBadge(
                        icon =
                            Icons.Outlined.Verified,
                        text =
                            "Verified"
                    )
                }


                if (
                    averageRating != null
                ) {

                    HeroBadge(
                        icon =
                            Icons.Filled.Star,
                        text =
                            "%.1f • %d review%s"
                                .format(
                                    averageRating,
                                    reviewCount,
                                    if (
                                        reviewCount == 1
                                    ) {
                                        ""
                                    } else {
                                        "s"
                                    }
                                )
                    )
                }
            }


            doctor.location
                .takeIf {
                    it !=
                            "Location not provided"
                }
                ?.let { location ->

                    Spacer(
                        Modifier.height(10.dp)
                    )


                    Row(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    Color.White.copy(
                                        alpha = 0.10f
                                    )
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 6.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.LocationOn,
                            contentDescription =
                                null,
                            tint =
                                Color.White.copy(
                                    alpha = 0.90f
                                ),
                            modifier =
                                Modifier.size(15.dp)
                        )


                        Spacer(
                            Modifier.width(5.dp)
                        )


                        Text(
                            text =
                                location,
                            color =
                                Color.White.copy(
                                    alpha = 0.90f
                                ),
                            fontSize =
                                11.sp
                        )
                    }
                }
        }
    }
}

@Composable
private fun HeroBadge(
    icon: ImageVector,
    text: String
) {

    Row(
        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(
                        50
                    )
                )
                .background(
                    Color.White
                        .copy(
                            alpha =
                                0.15f
                        )
                )
                .padding(
                    horizontal =
                        10.dp,
                    vertical =
                        6.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            tint =
                Color.White,
            modifier =
                Modifier.size(
                    15.dp
                )
        )

        Spacer(
            modifier =
                Modifier.width(
                    5.dp
                )
        )

        Text(
            text =
                text,
            color =
                Color.White,
            fontSize =
                11.5.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun DoctorQuickStats(
    rating: Double?,
    reviewCount: Int,
    experience: Int?,
    rate: Double?
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        horizontalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        DoctorStat(
            modifier =
                Modifier.weight(1f),

            icon =
                Icons.Filled.Star,

            value =
                rating?.let {
                    "%.1f".format(it)
                } ?: "New",

            label =
                if (reviewCount == 1) {
                    "1 review"
                } else {
                    "$reviewCount reviews"
                },

            accent =
                PatientColors.ReviewAccent,

            background =
                PatientColors.ReviewCard
        )


        DoctorStat(
            modifier =
                Modifier.weight(1f),

            icon =
                Icons.Outlined.WorkspacePremium,

            value =
                experience
                    ?.toString()
                    ?: "—",

            label =
                "Years exp.",

            accent =
                PatientColors.DoctorAccent,

            background =
                PatientColors.DoctorCard
        )


        DoctorStat(
            modifier =
                Modifier.weight(1f),

            icon =
                Icons.Outlined.Payments,

            value =
                rate?.let {
                    "R%.0f".format(it)
                } ?: "—",

            label =
                "Per hour",

            accent =
                PatientColors.AppointmentAccent,

            background =
                PatientColors.AppointmentCard
        )
    }
}

@Composable
private fun DoctorStat(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    background: Color
) {

    Card(
        modifier =
            modifier,
        shape =
            RoundedCornerShape(
                19.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 14.dp,
                        horizontal = 8.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(34.dp)
                        .clip(
                            RoundedCornerShape(
                                11.dp
                            )
                        )
                        .background(
                            accent.copy(
                                alpha = 0.12f
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
                        Modifier.size(17.dp)
                )
            }


            Spacer(
                Modifier.height(8.dp)
            )


            Text(
                text =
                    value,
                color =
                    PatientColors.TextPrimary,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    17.sp,
                maxLines = 1
            )


            Spacer(
                Modifier.height(2.dp)
            )


            Text(
                text =
                    label,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PatientChoiceCard() {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(
                24.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.DoctorAccent
                        .copy(alpha = 0.10f)
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .background(
                                PatientColors.DoctorAccent
                                    .copy(
                                        alpha = 0.11f
                                    )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.VideoCall,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.DoctorAccent,
                        modifier =
                            Modifier.size(20.dp)
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(11.dp)
                )


                Column {

                    Text(
                        text =
                            "Choose how you want care",
                        color =
                            PatientColors.TextPrimary,
                        fontSize =
                            16.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            "Select the option that works best for you.",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            11.5.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(17.dp)
            )


            ChoiceExplanation(
                icon =
                    Icons.Outlined.Schedule,

                title =
                    "Scheduled appointment",

                description =
                    "Choose one of the doctor's available time slots and plan your consultation in advance.",

                accent =
                    PatientColors.AppointmentAccent,

                background =
                    PatientColors.AppointmentCard,

                badge =
                    "PLAN AHEAD"
            )


            Spacer(
                modifier =
                    Modifier.height(11.dp)
            )


            ChoiceExplanation(
                icon =
                    Icons.Outlined.VideoCall,

                title =
                    "Private consultation now",

                description =
                    "Send an immediate consultation request directly to this doctor when they are eligible and available.",

                accent =
                    PatientColors.DoctorAccent,

                background =
                    PatientColors.DoctorCard,

                badge =
                    "ON DEMAND"
            )
        }
    }
}

@Composable
private fun ChoiceExplanation(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color,
    background: Color,
    badge: String
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                18.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    accent.copy(
                        alpha = 0.10f
                    )
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
                        14.dp
                    ),
            verticalAlignment =
                Alignment.Top
        ) {

            Box(
                modifier =
                    Modifier
                        .size(43.dp)
                        .clip(
                            RoundedCornerShape(
                                13.dp
                            )
                        )
                        .background(
                            accent.copy(
                                alpha = 0.12f
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
                            21.dp
                        )
                )
            }


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            title,
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.5.sp,
                        modifier =
                            Modifier.weight(1f)
                    )


                    Box(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    accent.copy(
                                        alpha = 0.11f
                                    )
                                )
                                .padding(
                                    horizontal = 7.dp,
                                    vertical = 4.dp
                                )
                    ) {

                        Text(
                            text =
                                badge,
                            color =
                                accent,
                            fontSize =
                                8.sp,
                            fontWeight =
                                FontWeight.ExtraBold,
                            letterSpacing =
                                0.5.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Text(
                    text =
                        description,
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        11.5.sp,
                    lineHeight =
                        17.sp
                )
            }
        }
    }
}

@Composable
private fun InformationCard(
    title: String,
    accent: Color = PatientColors.DoctorAccent,
    background: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                ),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    background
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    accent.copy(
                        alpha = 0.10f
                    )
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .width(5.dp)
                            .height(24.dp)
                            .clip(
                                RoundedCornerShape(
                                    10.dp
                                )
                            )
                            .background(
                                accent
                            )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            9.dp
                        )
                )

                Text(
                    text =
                        title,
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        16.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            content()
        }
    }
}

@Composable
private fun ProfileInformationRow(
    icon: ImageVector,
    title: String,
    value: String,
    accent: Color = PatientColors.DoctorAccent
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 7.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(39.dp)
                    .clip(
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .background(
                        accent.copy(
                            alpha = 0.10f
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
                        18.dp
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
                    PatientColors.TextSecondary,
                fontSize =
                    10.5.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            Text(
                text =
                    value,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    13.5.sp,
                fontWeight =
                    FontWeight.SemiBold,
                lineHeight =
                    18.sp
            )
        }
    }
}

@Composable
private fun ReviewsCard(
    reviews: List<DoctorReviewItem>,
    averageRating: Double?,
    showAll: Boolean,
    onToggle: () -> Unit
) {

    InformationCard(
        title =
            if (reviews.isEmpty()) {
                "Patient reviews"
            } else {
                "Patient reviews (${reviews.size})"
            },
        accent =
            PatientColors.ReviewAccent,
        background =
            PatientColors.ReviewCard
    ) {

        if (reviews.isEmpty()) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = 8.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                PatientColors.ReviewAccent
                                    .copy(alpha = 0.10f)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.StarBorder,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.ReviewAccent,
                        modifier =
                            Modifier.size(29.dp)
                    )
                }


                Spacer(
                    Modifier.height(12.dp)
                )


                Text(
                    text =
                        "No reviews yet",
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        14.sp
                )


                Spacer(
                    Modifier.height(4.dp)
                )


                Text(
                    text =
                        "Patient reviews will appear here after completed appointments.",
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        11.5.sp,
                    lineHeight =
                        17.sp,
                    textAlign =
                        TextAlign.Center
                )
            }

        } else {

            /*
             * Rating summary
             */
            averageRating?.let { rating ->

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White.copy(
                                    alpha = 0.58f
                                )
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
                                    15.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "%.1f".format(
                                        rating
                                    ),
                                color =
                                    PatientColors.TextPrimary,
                                fontSize =
                                    31.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )


                            Text(
                                text =
                                    "out of 5",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp
                            )
                        }


                        Spacer(
                            Modifier.width(18.dp)
                        )


                        Column {

                            RatingStars(
                                rating =
                                    rating,
                                iconSize =
                                    19
                            )


                            Spacer(
                                Modifier.height(5.dp)
                            )


                            Text(
                                text =
                                    "Based on ${reviews.size} review${
                                        if (reviews.size == 1) {
                                            ""
                                        } else {
                                            "s"
                                        }
                                    }",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.5.sp
                            )
                        }
                    }
                }


                Spacer(
                    Modifier.height(14.dp)
                )
            }


            val displayedReviews =
                if (showAll) {
                    reviews
                } else {
                    reviews.take(3)
                }


            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                displayedReviews.forEach { review ->

                    ReviewItem(
                        review = review
                    )
                }
            }


            if (reviews.size > 3) {

                Spacer(
                    Modifier.height(10.dp)
                )


                TextButton(
                    onClick =
                        onToggle,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        text =
                            if (showAll) {
                                "Show fewer reviews"
                            } else {
                                "Show all ${reviews.size} reviews"
                            },
                        color =
                            PatientColors.ReviewAccent,
                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        Modifier.width(5.dp)
                    )


                    Icon(
                        imageVector =
                            if (showAll) {
                                Icons.Filled.ExpandLess
                            } else {
                                Icons.Filled.ExpandMore
                            },
                        contentDescription =
                            null,
                        tint =
                            PatientColors.ReviewAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: DoctorReviewItem
) {

    val patientName =
        review.patientName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Patient"

    val initials =
        patientName
            .trim()
            .split(" ")
            .filter {
                it.isNotBlank()
            }
            .take(2)
            .joinToString("") {
                it.first()
                    .uppercase()
            }
            .ifBlank {
                "P"
            }


    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                17.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White.copy(
                        alpha = 0.60f
                    )
            ),
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.ReviewAccent
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
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(39.dp)
                            .clip(CircleShape)
                            .background(
                                PatientColors.ReviewAccent
                                    .copy(alpha = 0.12f)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            initials,
                        color =
                            PatientColors.ReviewAccent,
                        fontSize =
                            11.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }


                Spacer(
                    Modifier.width(10.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            patientName,
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.sp
                    )


                    review.createdAt
                        ?.takeIf {
                            it.length >= 10
                        }
                        ?.take(10)
                        ?.let { date ->

                            Spacer(
                                Modifier.height(2.dp)
                            )


                            Text(
                                text =
                                    date,
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp
                            )
                        }
                }


                RatingStars(
                    rating =
                        review.rating
                            .toDouble(),
                    iconSize =
                        14
                )
            }


            if (
                !review.comment
                    .isNullOrBlank()
            ) {

                Spacer(
                    Modifier.height(11.dp)
                )


                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    13.dp
                                )
                            )
                            .background(
                                PatientColors.ReviewAccent
                                    .copy(alpha = 0.055f)
                            )
                            .padding(
                                11.dp
                            )
                ) {

                    Text(
                        text =
                            review.comment
                                .orEmpty(),
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            11.5.sp,
                        lineHeight =
                            17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingStars(
    rating: Double,
    iconSize: Int = 17
) {

    Row {

        repeat(
            5
        ) { index ->

            Icon(
                imageVector =
                    if (
                        index <
                        round(
                            rating
                        )
                            .toInt()
                    ) {
                        Icons.Filled.Star
                    } else {
                        Icons.Filled.StarBorder
                    },
                contentDescription =
                    null,
                tint =
                    WarningAmber,
                modifier =
                    Modifier.size(
                        iconSize.dp
                    )
            )
        }
    }
}

@Composable
private fun DoctorActionBar(
    doctor: DoctorProfile,
    onPrivate: () -> Unit,
    onBook: () -> Unit
) {

    val isApproved =
        doctor.verification_status
            .equals(
                "approved",
                ignoreCase = true
            )

    val isActive =
        doctor.is_deactivated != true

    val canPrivateConsult =
        isApproved &&
                isActive &&
                (doctor.hourly_rate ?: 0.0) > 0.0

    val canBook =
        isApproved &&
                isActive


    Surface(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                topStart = 26.dp,
                topEnd = 26.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .surface,
        tonalElevation =
            6.dp,
        shadowElevation =
            14.dp
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 13.dp,
                        bottom = 12.dp
                    )
        ) {

            /*
             * Fee + doctor status
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
                            .size(42.dp)
                            .clip(
                                RoundedCornerShape(
                                    13.dp
                                )
                            )
                            .background(
                                PatientColors
                                    .AppointmentCard
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Payments,
                        contentDescription =
                            null,
                        tint =
                            PatientColors
                                .AppointmentAccent,
                        modifier =
                            Modifier.size(
                                21.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(10.dp)
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Private consultation",
                        color =
                            PatientColors
                                .TextSecondary,
                        fontSize =
                            10.5.sp
                    )


                    Text(
                        text =
                            doctor.hourly_rate
                                ?.let {
                                    "R %.2f".format(it)
                                }
                                ?: "Rate unavailable",
                        color =
                            PatientColors
                                .TextPrimary,
                        fontSize =
                            17.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )
                }


                if (isApproved) {

                    Row(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .background(
                                    PatientColors
                                        .SuccessCard
                                )
                                .padding(
                                    horizontal = 9.dp,
                                    vertical = 6.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Verified,
                            contentDescription =
                                null,
                            tint =
                                PatientColors
                                    .SuccessAccent,
                            modifier =
                                Modifier.size(
                                    15.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(4.dp)
                        )


                        Text(
                            text =
                                "Verified",
                            color =
                                PatientColors
                                    .SuccessAccent,
                            fontSize =
                                10.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            /*
             * Main actions
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                /*
                 * Private consultation
                 */
                FilledTonalButton(
                    enabled =
                        canPrivateConsult,
                    onClick =
                        onPrivate,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor =
                                PatientColors
                                    .DoctorCard,
                            contentColor =
                                PatientColors
                                    .DoctorAccent
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.VideoCall,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                19.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(7.dp)
                    )


                    Text(
                        text =
                            "Private now",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            12.sp
                    )
                }


                /*
                 * Scheduled booking
                 */
                Button(
                    enabled =
                        canBook,
                    onClick =
                        onBook,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PatientColors
                                    .AppointmentAccent,
                            contentColor =
                                Color.White
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Schedule,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                19.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(7.dp)
                    )


                    Text(
                        text =
                            "Book",
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            12.5.sp
                    )
                }
            }


            if (!canBook) {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                Text(
                    text =
                        "Appointments are currently unavailable for this doctor.",
                    modifier =
                        Modifier.fillMaxWidth(),
                    color =
                        PatientColors
                            .TextSecondary,
                    fontSize =
                        10.5.sp,
                    textAlign =
                        TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProfileLoadingState(
    modifier: Modifier
) {

    Box(
        modifier =
            modifier,
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator()

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    "Loading doctor profile...",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileErrorState(
    modifier: Modifier,
    message: String,
    onRetry: () -> Unit
) {

    Box(
        modifier =
            modifier
                .padding(
                    24.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "Doctor unavailable",
                fontSize =
                    19.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    message,
                textAlign =
                    TextAlign.Center,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Button(
                onClick =
                    onRetry
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.Refresh,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Text(
                    "Try again"
                )
            }
        }
    }
}

private fun initials(
    doctor: DoctorProfile
): String {

    val first =
        doctor
            .name
            ?.firstOrNull()
            ?.uppercaseChar()
            ?: 'D'

    val second =
        doctor
            .surname
            ?.firstOrNull()
            ?.uppercaseChar()
            ?: 'R'

    return "$first$second"
}