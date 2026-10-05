package ehealthy.connect.ui.patientDashboard.myVisits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.PatientReviewRepository
import ehealthy.connect.data.patient.PatientAppointment
import ehealthy.connect.data.patient.PatientDoctorSummary
import ehealthy.connect.data.patient.PatientRepository

import kotlinx.coroutines.launch

private val ReviewGold =
    Color(0xFFF59E0B)

private val ReviewGreen =
    Color(0xFF15803D)

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun RateVisitScreen(
    appointment: PatientAppointment,
    onBack: () -> Unit,
    onSubmitted: () -> Unit
) {

    val scope =
        rememberCoroutineScope()

    var doctor by remember {
        mutableStateOf<PatientDoctorSummary?>(
            null
        )
    }

    var rating by rememberSaveable {
        mutableIntStateOf(0)
    }

    var comment by rememberSaveable {
        mutableStateOf("")
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSubmitting by remember {
        mutableStateOf(false)
    }

    var alreadyReviewed by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var submitted by remember {
        mutableStateOf(false)
    }

    suspend fun loadScreen() {

        isLoading = true
        errorMessage = null

        val doctorId =
            appointment.doctor_id

        if (
            !doctorId.isNullOrBlank()
        ) {

            PatientRepository
                .getDoctor(
                    doctorId
                )
                .onSuccess {
                    doctor = it
                }
        }

        PatientReviewRepository
            .hasReviewed(
                appointment.id
            )
            .onSuccess {
                alreadyReviewed = it
            }
            .onFailure {

                errorMessage =
                    it.message
                        ?: "Unable to check your previous review."
            }

        isLoading = false
    }

    fun submitReview() {

        if (rating == 0) {

            errorMessage =
                "Choose a star rating first."

            return
        }

        isSubmitting = true
        errorMessage = null

        scope.launch {

            PatientReviewRepository
                .submitReview(
                    appointmentId =
                        appointment.id,
                    rating =
                        rating,
                    comment =
                        comment
                )
                .onSuccess {

                    submitted = true
                }
                .onFailure {

                    errorMessage =
                        friendlyReviewError(
                            it.message
                        )
                }

            isSubmitting = false
        }
    }

    LaunchedEffect(
        appointment.id
    ) {

        loadScreen()
    }

    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        topBar = {

            TopAppBar(

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                },

                title = {

                    Column {

                        Text(
                            text =
                                "Rate your visit",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Share your experience",
                            fontSize =
                                11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
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
        }

    ) { innerPadding ->

        when {

            isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            submitted -> {

                ReviewSuccessScreen(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),
                    onDone =
                        onSubmitted
                )
            }

            alreadyReviewed -> {

                AlreadyReviewedScreen(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),
                    onDone =
                        onBack
                )
            }

            else -> {

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            )
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .imePadding()
                            .padding(
                                horizontal =
                                    16.dp,
                                vertical =
                                    14.dp
                            )
                ) {

                    VisitReviewCard(
                        appointment =
                            appointment,
                        doctor =
                            doctor
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )

                    RatingCard(
                        rating =
                            rating,
                        onRatingChange = {

                            rating = it
                            errorMessage = null
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )

                    CommentCard(
                        comment =
                            comment,
                        onCommentChange = {

                            if (
                                it.length <= 500
                            ) {

                                comment = it
                            }
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )

                    ReviewPrivacyNotice()

                    if (
                        errorMessage != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )

                        Surface(
                            modifier =
                                Modifier.fillMaxWidth(),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .errorContainer,
                            shape =
                                RoundedCornerShape(
                                    14.dp
                                )
                        ) {

                            Text(
                                text =
                                    errorMessage.orEmpty(),
                                modifier =
                                    Modifier.padding(
                                        13.dp
                                    ),
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onErrorContainer,
                                fontSize =
                                    12.5.sp
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                18.dp
                            )
                    )

                    Button(
                        onClick =
                            ::submitReview,
                        enabled =
                            !isSubmitting &&
                                    rating > 0 &&
                                    appointment.status
                                        .equals(
                                            "completed",
                                            ignoreCase =
                                                true
                                        ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    54.dp
                                ),
                        shape =
                            RoundedCornerShape(
                                15.dp
                            )
                    ) {

                        if (
                            isSubmitting
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        19.dp
                                    ),
                                strokeWidth =
                                    2.dp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onPrimary
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(
                                        8.dp
                                    )
                            )

                            Text(
                                "Submitting..."
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Outlined
                                        .RateReview,
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
                                text =
                                    "Submit review",
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    if (
                        !appointment.status
                            .equals(
                                "completed",
                                ignoreCase =
                                    true
                            )
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    8.dp
                                )
                        )

                        Text(
                            text =
                                "Reviews can only be submitted after the appointment has been completed.",
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .error,
                            fontSize =
                                11.5.sp,
                            textAlign =
                                TextAlign.Center,
                            modifier =
                                Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                30.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun VisitReviewCard(
    appointment: PatientAppointment,
    doctor: PatientDoctorSummary?
) {

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults
                .elevatedCardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                            .copy(
                                alpha =
                                    0.45f
                            )
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

                Surface(
                    shape =
                        CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Person,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimary,
                        modifier =
                            Modifier.padding(
                                10.dp
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
                        )
                )

                Column {

                    Text(
                        text =
                            doctor
                                ?.fullName
                                ?: "Doctor",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            16.sp
                    )

                    Text(
                        text =
                            doctor
                                ?.discipline
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Medical consultation",
                        fontSize =
                            11.5.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            ReviewDetailRow(
                label =
                    "Date",
                value =
                    appointment.date
                        ?: "Not available"
            )

            ReviewDetailRow(
                label =
                    "Time",
                value =
                    appointment.time
                        ?.take(
                            5
                        )
                        ?: "Not available"
            )

            ReviewDetailRow(
                label =
                    "Visit",
                value =
                    when (
                        appointment
                            .appointment_type
                    ) {

                        "online" ->
                            "Online"

                        "in_person" ->
                            "In person"

                        else ->
                            "Medical visit"
                    }
            )

            appointment.reason
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    ReviewDetailRow(
                        label =
                            "Reason",
                        value =
                            it
                    )
                }
        }
    }
}

@Composable
private fun ReviewDetailRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        3.dp
                )
    ) {

        Text(
            text =
                label,
            modifier =
                Modifier.width(
                    56.dp
                ),
            fontSize =
                11.5.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Text(
            text =
                value,
            modifier =
                Modifier.weight(
                    1f
                ),
            fontSize =
                12.5.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}

@Composable
private fun RatingCard(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    20.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "How was your experience?",
                fontSize =
                    18.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    ratingDescription(
                        rating
                    ),
                fontSize =
                    12.5.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            Row(
                horizontalArrangement =
                    Arrangement.Center,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                repeat(
                    5
                ) { index ->

                    val star =
                        index + 1

                    Icon(
                        imageVector =
                            if (
                                star <= rating
                            ) {
                                Icons.Filled.Star
                            } else {
                                Icons.Filled.StarBorder
                            },
                        contentDescription =
                            "$star star",
                        tint =
                            if (
                                star <= rating
                            ) {
                                ReviewGold
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .outline
                            },
                        modifier =
                            Modifier
                                .size(
                                    46.dp
                                )
                                .clickable {

                                    onRatingChange(
                                        star
                                    )
                                }
                                .padding(
                                    5.dp
                                )
                    )
                }
            }

            if (
                rating > 0
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        "$rating / 5",
                    color =
                        ReviewGold,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: String,
    onCommentChange: (String) -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    18.dp
                )
        ) {

            Text(
                text =
                    "Tell us more",
                fontSize =
                    16.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        3.dp
                    )
            )

            Text(
                text =
                    "Your comment is optional.",
                fontSize =
                    11.5.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            OutlinedTextField(
                value =
                    comment,
                onValueChange =
                    onCommentChange,
                modifier =
                    Modifier.fillMaxWidth(),
                placeholder = {

                    Text(
                        "What went well? Was the doctor helpful and professional?"
                    )
                },
                minLines =
                    4,
                maxLines =
                    7,
                supportingText = {

                    Text(
                        text =
                            "${comment.length}/500",
                        modifier =
                            Modifier.fillMaxWidth(),
                        textAlign =
                            TextAlign.End
                    )
                }
            )
        }
    }
}

@Composable
private fun ReviewPrivacyNotice() {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme
                        .colorScheme
                        .secondaryContainer
                        .copy(
                            alpha =
                                0.5f
                        ),
                    RoundedCornerShape(
                        15.dp
                    )
                )
                .padding(
                    14.dp
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
                MaterialTheme
                    .colorScheme
                    .secondary,
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

        Text(
            text =
                "Do not include passwords, banking information, identity numbers or detailed private medical information in a public review.",
            fontSize =
                11.sp,
            lineHeight =
                15.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun ReviewSuccessScreen(
    modifier: Modifier,
    onDone: () -> Unit
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

        ElevatedCard(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    25.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        26.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Surface(
                    shape =
                        CircleShape,
                    color =
                        ReviewGreen
                            .copy(
                                alpha =
                                    0.12f
                            )
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            null,
                        tint =
                            ReviewGreen,
                        modifier =
                            Modifier
                                .padding(
                                    16.dp
                                )
                                .size(
                                    42.dp
                                )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            16.dp
                        )
                )

                Text(
                    text =
                        "Thank you",
                    fontSize =
                        22.sp,
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
                        "Your review has been submitted successfully.",
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
                            20.dp
                        )
                )

                Button(
                    onClick =
                        onDone,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Back to visits"
                    )
                }
            }
        }
    }
}

@Composable
private fun AlreadyReviewedScreen(
    modifier: Modifier,
    onDone: () -> Unit
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

        ElevatedCard(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    24.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(
                        24.dp
                    ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.CheckCircle,
                    contentDescription =
                        null,
                    tint =
                        ReviewGreen,
                    modifier =
                        Modifier.size(
                            52.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                Text(
                    text =
                        "Already reviewed",
                    fontSize =
                        20.sp,
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
                        "You have already submitted a review for this appointment.",
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
                            18.dp
                        )
                )

                TextButton(
                    onClick =
                        onDone
                ) {

                    Text(
                        "Back to visits"
                    )
                }
            }
        }
    }
}

private fun ratingDescription(
    rating: Int
): String {

    return when (rating) {

        1 ->
            "Very poor"

        2 ->
            "Could be better"

        3 ->
            "Good"

        4 ->
            "Very good"

        5 ->
            "Excellent"

        else ->
            "Tap a star to rate the visit"
    }
}

private fun friendlyReviewError(
    message: String?
): String {

    val value =
        message.orEmpty()

    return when {

        value.contains(
            "already",
            ignoreCase = true
        ) ->
            "You have already reviewed this appointment."

        value.contains(
            "completed",
            ignoreCase = true
        ) ->
            "You can only review a completed appointment."

        value.contains(
            "not found",
            ignoreCase = true
        ) ->
            "This appointment could not be found."

        value.contains(
            "permission",
            ignoreCase = true
        ) ->
            "You do not have permission to review this appointment."

        value.isNotBlank() ->
            value

        else ->
            "We couldn't submit your review. Please try again."
    }
}