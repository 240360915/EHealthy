package ehealthy.connect.ui.patientDashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.patient.ConsultationMode
import ehealthy.connect.data.patient.ConsultationRequestResult
import ehealthy.connect.data.patient.ConsultationRequestRow
import ehealthy.connect.data.patient.PatientDoctorSummary
import ehealthy.connect.data.patient.PatientRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private enum class ConsultationScreenMode {
    PRIVATE,
    BROADCAST
}

private val ConsultationGreen =
    Color(0xFF15803D)

private val ConsultationAmber =
    Color(0xFFF59E0B)

private val ConsultationHeroStart =
    Color(0xFF0B3B60)

private val ConsultationHeroEnd =
    Color(0xFF087F8C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnDemandConsultationScreen(
    initialDoctorId: String? = null,
    onBack: () -> Unit,
    onChooseDoctor: () -> Unit,
    onConsultationReady: (
        requestId: String,
        doctorId: String
    ) -> Unit
) {

    val scope =
        rememberCoroutineScope()

    var selectedMode by remember {
        mutableStateOf(
            if (
                initialDoctorId.isNullOrBlank()
            ) {
                ConsultationScreenMode.BROADCAST
            } else {
                ConsultationScreenMode.PRIVATE
            }
        )
    }

    var selectedDoctorId by remember {
        mutableStateOf(
            initialDoctorId
        )
    }

    var selectedDoctor by remember {
        mutableStateOf<PatientDoctorSummary?>(
            null
        )
    }

    var loadingDoctor by remember {
        mutableStateOf(false)
    }

    var doctorError by remember {
        mutableStateOf<String?>(null)
    }

    var isCreating by remember {
        mutableStateOf(false)
    }

    var activeRequest by remember {
        mutableStateOf<ConsultationRequestResult?>(
            null
        )
    }

    var liveRequest by remember {
        mutableStateOf<ConsultationRequestRow?>(
            null
        )
    }

    var requestError by remember {
        mutableStateOf<String?>(null)
    }

    var cancelling by remember {
        mutableStateOf(false)
    }

    var secondsRemaining by remember {
        mutableIntStateOf(60)
    }

    var requestIdempotencyKey by remember {
        mutableStateOf(
            UUID.randomUUID()
                .toString()
        )
    }

    suspend fun loadSelectedDoctor() {

        val doctorId =
            selectedDoctorId

        if (
            doctorId.isNullOrBlank()
        ) {

            selectedDoctor =
                null

            return
        }

        loadingDoctor =
            true

        doctorError =
            null

        PatientRepository
            .getDoctor(
                doctorId
            )
            .onSuccess {

                selectedDoctor =
                    it
            }
            .onFailure { error ->

                selectedDoctor =
                    null

                doctorError =
                    error.message
                        ?: "Unable to load this doctor."
            }

        loadingDoctor =
            false
    }

    suspend fun refreshRequest() {

        val requestId =
            activeRequest
                ?.request_id
                ?: return

        PatientRepository
            .getMyConsultationRequests()
            .onSuccess { requests ->

                liveRequest =
                    requests.firstOrNull {
                        it.id ==
                                requestId
                    }
            }
    }

    fun resetRequest() {

        activeRequest =
            null

        liveRequest =
            null

        requestError =
            null

        secondsRemaining =
            60

        requestIdempotencyKey =
            UUID.randomUUID()
                .toString()
    }

    fun createRequest() {

        if (
            selectedMode ==
            ConsultationScreenMode.PRIVATE &&
            selectedDoctorId
                .isNullOrBlank()
        ) {

            requestError =
                "Choose a doctor for a private consultation."

            return
        }

        requestError =
            null

        isCreating =
            true

        scope.launch {

            PatientRepository
                .createConsultationRequest(
                    mode =
                        if (
                            selectedMode ==
                            ConsultationScreenMode.PRIVATE
                        ) {
                            ConsultationMode.PRIVATE
                        } else {
                            ConsultationMode.BROADCAST
                        },

                    targetDoctorId =
                        if (
                            selectedMode ==
                            ConsultationScreenMode.PRIVATE
                        ) {
                            selectedDoctorId
                        } else {
                            null
                        },

                    service =
                        "general",

                    idempotencyKey =
                        requestIdempotencyKey
                )
                .onSuccess { result ->

                    activeRequest =
                        result

                    secondsRemaining =
                        60

                    refreshRequest()
                }
                .onFailure { error ->

                    requestError =
                        friendlyConsultationError(
                            error.message,
                            selectedMode
                        )
                }

            isCreating =
                false
        }
    }

    fun cancelRequest() {

        val requestId =
            activeRequest
                ?.request_id
                ?: return

        cancelling =
            true

        scope.launch {

            PatientRepository
                .cancelConsultationRequest(
                    requestId
                )
                .onSuccess {

                    refreshRequest()
                }
                .onFailure { error ->

                    requestError =
                        error.message
                            ?: "Unable to cancel this consultation request."
                }

            cancelling =
                false
        }
    }

    LaunchedEffect(
        selectedDoctorId
    ) {

        loadSelectedDoctor()
    }

    /*
     * Poll the server while the request is active.
     *
     * Later we'll replace/add Supabase Realtime + FCM so the UI reacts
     * immediately, but polling keeps this patient flow functional now.
     */
    LaunchedEffect(
        activeRequest?.request_id
    ) {

        if (
            activeRequest == null
        ) {
            return@LaunchedEffect
        }

        while (true) {

            refreshRequest()

            val status =
                liveRequest
                    ?.status
                    ?.lowercase()

            if (
                status in listOf(
                    "claimed",
                    "cancelled",
                    "expired",
                    "declined",
                    "no_doctors",
                    "completed",
                    "failed"
                )
            ) {
                break
            }

            delay(
                1500
            )
        }
    }

    LaunchedEffect(
        activeRequest?.request_id
    ) {

        if (
            activeRequest == null
        ) {
            return@LaunchedEffect
        }

        secondsRemaining =
            60

        while (
            secondsRemaining > 0
        ) {

            val status =
                liveRequest
                    ?.status
                    ?.lowercase()

            if (
                status != null &&
                status != "pending"
            ) {
                break
            }

            delay(
                1000
            )

            secondsRemaining--
        }

        refreshRequest()
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
                                "Consult a doctor now",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "On-demand medical consultation",
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

        AnimatedContent(
            targetState =
                activeRequest != null,
            label =
                "consultation-state"
        ) { hasRequest ->

            if (
                hasRequest
            ) {

                ActiveConsultationRequestScreen(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),
                    request =
                        activeRequest!!,
                    liveRequest =
                        liveRequest,
                    secondsRemaining =
                        secondsRemaining,
                    cancelling =
                        cancelling,
                    error =
                        requestError,
                    onRefresh = {

                        scope.launch {
                            refreshRequest()
                        }
                    },
                    onCancel =
                        ::cancelRequest,
                    onTryAgain =
                        ::resetRequest,
                    onContinue = {

                        val request =
                            liveRequest

                        val doctorId =
                            request
                                ?.claimed_doctor_id

                        if (
                            request != null &&
                            !doctorId
                                .isNullOrBlank()
                        ) {

                            onConsultationReady(
                                request.id,
                                doctorId
                            )
                        }
                    }
                )

            } else {

                ConsultationSetupScreen(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                innerPadding
                            ),
                    selectedMode =
                        selectedMode,
                    selectedDoctor =
                        selectedDoctor,
                    loadingDoctor =
                        loadingDoctor,
                    doctorError =
                        doctorError,
                    isCreating =
                        isCreating,
                    requestError =
                        requestError,
                    onModeSelected = {

                        selectedMode =
                            it

                        requestError =
                            null
                    },
                    onChooseDoctor =
                        onChooseDoctor,
                    onCreate =
                        ::createRequest
                )
            }
        }
    }
}

@Composable
private fun ConsultationSetupScreen(
    modifier: Modifier,
    selectedMode: ConsultationScreenMode,
    selectedDoctor: PatientDoctorSummary?,
    loadingDoctor: Boolean,
    doctorError: String?,
    isCreating: Boolean,
    requestError: String?,
    onModeSelected: (ConsultationScreenMode) -> Unit,
    onChooseDoctor: () -> Unit,
    onCreate: () -> Unit
) {

    LazyColumn(
        modifier =
            modifier,
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

            ConsultationHero()
        }

        item {

            Column(
                modifier =
                    Modifier.padding(
                        horizontal =
                            16.dp
                    )
            ) {

                Text(
                    text =
                        "How should we find your doctor?",
                    fontSize =
                        18.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                Text(
                    text =
                        "Choose one doctor privately or send the request to all eligible available doctors.",
                    fontSize =
                        12.5.sp,
                    lineHeight =
                        18.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        item {

            ConsultationModeCard(
                icon =
                    Icons.Outlined.Person,
                title =
                    "Private doctor",
                description =
                    "Only the doctor you select receives the request.",
                selected =
                    selectedMode ==
                            ConsultationScreenMode.PRIVATE,
                onClick = {

                    onModeSelected(
                        ConsultationScreenMode.PRIVATE
                    )
                }
            )
        }

        item {

            ConsultationModeCard(
                icon =
                    Icons.Outlined.VideoCall,
                title =
                    "Broadcast to available doctors",
                description =
                    "Eligible doctors who are currently available receive the request. The first valid doctor to accept gets the consultation.",
                selected =
                    selectedMode ==
                            ConsultationScreenMode.BROADCAST,
                onClick = {

                    onModeSelected(
                        ConsultationScreenMode.BROADCAST
                    )
                }
            )
        }

        if (
            selectedMode ==
            ConsultationScreenMode.PRIVATE
        ) {

            item {

                PrivateDoctorSection(
                    doctor =
                        selectedDoctor,
                    loading =
                        loadingDoctor,
                    error =
                        doctorError,
                    onChooseDoctor =
                        onChooseDoctor
                )
            }
        }

        item {

            HowItWorksCard(
                mode =
                    selectedMode
            )
        }

        item {

            PricingNotice(
                mode =
                    selectedMode,
                doctor =
                    selectedDoctor
            )
        }

        if (
            requestError != null
        ) {

            item {

                ConsultationErrorCard(
                    message =
                        requestError
                )
            }
        }

        item {

            Button(
                onClick =
                    onCreate,
                enabled =
                    !isCreating &&
                            (
                                    selectedMode ==
                                            ConsultationScreenMode.BROADCAST ||
                                            selectedDoctor != null
                                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            56.dp
                        )
                        .padding(
                            horizontal =
                                16.dp
                        ),
                shape =
                    RoundedCornerShape(
                        16.dp
                    )
            ) {

                if (
                    isCreating
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                20.dp
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
                                9.dp
                            )
                    )

                    Text(
                        "Creating request..."
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Outlined.VideoCall,
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
                            if (
                                selectedMode ==
                                ConsultationScreenMode.PRIVATE
                            ) {
                                "Request this doctor"
                            } else {
                                "Find available doctor"
                            },
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsultationHero() {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                )
                .background(
                    brush =
                        Brush.linearGradient(
                            listOf(
                                ConsultationHeroStart,
                                ConsultationHeroEnd
                            )
                        ),
                    shape =
                        RoundedCornerShape(
                            24.dp
                        )
                )
                .padding(
                    21.dp
                )
    ) {

        Column {

            Surface(
                shape =
                    CircleShape,
                color =
                    Color.White.copy(
                        alpha =
                            0.16f
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.VideoCall,
                    contentDescription =
                        null,
                    tint =
                        Color.White,
                    modifier =
                        Modifier
                            .padding(
                                11.dp
                            )
                            .size(
                                28.dp
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            Text(
                text =
                    "Need a doctor now?",
                color =
                    Color.White,
                fontSize =
                    23.sp,
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
                    "Request an on-demand consultation without waiting for a future appointment slot.",
                color =
                    Color.White.copy(
                        alpha =
                            0.88f
                    ),
                fontSize =
                    13.sp,
                lineHeight =
                    18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        13.dp
                    )
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Info,
                    contentDescription =
                        null,
                    tint =
                        Color.White,
                    modifier =
                        Modifier.size(
                            16.dp
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
                        "Not an emergency service",
                    color =
                        Color.White,
                    fontWeight =
                        FontWeight.SemiBold,
                    fontSize =
                        11.sp
                )
            }
        }
    }
}

@Composable
private fun ConsultationModeCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                )
                .clickable(
                    onClick =
                        onClick
                ),
        shape =
            RoundedCornerShape(
                19.dp
            ),
        colors =
            CardDefaults
                .elevatedCardColors(
                    containerColor =
                        if (
                            selected
                        ) {
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                        } else {
                            MaterialTheme
                                .colorScheme
                                .surface
                        }
                )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    17.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Surface(
                shape =
                    CircleShape,
                color =
                    if (
                        selected
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
            ) {

                Icon(
                    imageVector =
                        icon,
                    contentDescription =
                        null,
                    tint =
                        if (
                            selected
                        ) {
                            MaterialTheme
                                .colorScheme
                                .onPrimary
                        } else {
                            MaterialTheme
                                .colorScheme
                                .primary
                        },
                    modifier =
                        Modifier.padding(
                            10.dp
                        )
                )
            }

            Spacer(
                modifier =
                    Modifier.width(
                        13.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Text(
                    text =
                        title,
                    fontSize =
                        15.5.sp,
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
                        description,
                    fontSize =
                        11.5.sp,
                    lineHeight =
                        16.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Surface(
                modifier =
                    Modifier.size(
                        24.dp
                    ),
                shape =
                    CircleShape,
                color =
                    if (
                        selected
                    ) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .outlineVariant
                    }
            ) {

                if (
                    selected
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription =
                            "Selected",
                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimary,
                        modifier =
                            Modifier.padding(
                                3.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivateDoctorSection(
    doctor: PatientDoctorSummary?,
    loading: Boolean,
    error: String?,
    onChooseDoctor: () -> Unit
) {

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                19.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Text(
                text =
                    "Selected doctor",
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    15.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            when {

                loading -> {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    22.dp
                                ),
                            strokeWidth =
                                2.dp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    10.dp
                                )
                        )

                        Text(
                            "Loading doctor..."
                        )
                    }
                }

                doctor != null -> {

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
                                    .primaryContainer
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Person,
                                contentDescription =
                                    null,
                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                modifier =
                                    Modifier.padding(
                                        11.dp
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
                                Modifier.weight(
                                    1f
                                )
                        ) {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    text =
                                        doctor.fullName,
                                    fontWeight =
                                        FontWeight.Bold,
                                    maxLines =
                                        1,
                                    overflow =
                                        TextOverflow.Ellipsis
                                )

                                if (
                                    doctor
                                        .verification_status ==
                                    "approved"
                                ) {

                                    Spacer(
                                        modifier =
                                            Modifier.width(
                                                4.dp
                                            )
                                    )

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.Verified,
                                        contentDescription =
                                            "Verified",
                                        tint =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        modifier =
                                            Modifier.size(
                                                16.dp
                                            )
                                    )
                                }
                            }

                            Text(
                                text =
                                    doctor.discipline
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                        ?: "General Practitioner",
                                fontSize =
                                    11.5.sp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            )

                            doctor.hourly_rate
                                ?.let { rate ->

                                    Text(
                                        text =
                                            "R %.2f private fee"
                                                .format(
                                                    rate
                                                ),
                                        fontSize =
                                            11.5.sp,
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )
                                }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )

                    OutlinedButton(
                        onClick =
                            onChooseDoctor,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Choose another doctor"
                        )
                    }
                }

                else -> {

                    Text(
                        text =
                            error
                                ?: "No doctor selected.",
                        fontSize =
                            12.5.sp,
                        color =
                            if (
                                error != null
                            ) {
                                MaterialTheme
                                    .colorScheme
                                    .error
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )

                    Button(
                        onClick =
                            onChooseDoctor,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Choose doctor"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksCard(
    mode: ConsultationScreenMode
) {

    ElevatedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                19.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Text(
                text =
                    "How it works",
                fontSize =
                    15.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(
                        13.dp
                    )
            )

            if (
                mode ==
                ConsultationScreenMode.PRIVATE
            ) {

                ProcessStep(
                    number =
                        "1",
                    title =
                        "Request selected doctor",
                    description =
                        "The server checks whether that doctor is approved, active and currently available."
                )

                ProcessStep(
                    number =
                        "2",
                    title =
                        "Doctor receives invitation",
                    description =
                        "The doctor can accept or decline the consultation."
                )

                ProcessStep(
                    number =
                        "3",
                    title =
                        "Start consultation",
                    description =
                        "If accepted, only you and that doctor enter the consultation."
                )

            } else {

                ProcessStep(
                    number =
                        "1",
                    title =
                        "Find eligible doctors",
                    description =
                        "Only approved, active and recently available doctors are invited."
                )

                ProcessStep(
                    number =
                        "2",
                    title =
                        "Doctors receive the request",
                    description =
                        "Multiple eligible doctors may receive the invitation."
                )

                ProcessStep(
                    number =
                        "3",
                    title =
                        "First valid acceptance wins",
                    description =
                        "The database allows only one doctor to claim the consultation. Other invitations are closed."
                )
            }
        }
    }
}

@Composable
private fun ProcessStep(
    number: String,
    title: String,
    description: String
) {

    Row(
        modifier =
            Modifier.padding(
                vertical =
                    6.dp
            ),
        verticalAlignment =
            Alignment.Top
    ) {

        Surface(
            shape =
                CircleShape,
            color =
                MaterialTheme
                    .colorScheme
                    .primary
        ) {

            Box(
                modifier =
                    Modifier.size(
                        28.dp
                    ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        number,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onPrimary,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(
                    10.dp
                )
        )

        Column {

            Text(
                text =
                    title,
                fontWeight =
                    FontWeight.SemiBold,
                fontSize =
                    12.5.sp
            )

            Text(
                text =
                    description,
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
}

@Composable
private fun PricingNotice(
    mode: ConsultationScreenMode,
    doctor: PatientDoctorSummary?
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .secondaryContainer
                .copy(
                    alpha =
                        0.55f
                )
    ) {

        Row(
            modifier =
                Modifier.padding(
                    14.dp
                ),
            verticalAlignment =
                Alignment.Top
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Payments,
                contentDescription =
                    null,
                modifier =
                    Modifier.size(
                        19.dp
                    ),
                tint =
                    MaterialTheme
                        .colorScheme
                        .secondary
            )

            Spacer(
                modifier =
                    Modifier.width(
                        9.dp
                    )
            )

            Column {

                Text(
                    text =
                        if (
                            mode ==
                            ConsultationScreenMode.PRIVATE
                        ) {
                            "Private consultation pricing"
                        } else {
                            "Broadcast consultation pricing"
                        },
                    fontWeight =
                        FontWeight.SemiBold,
                    fontSize =
                        12.5.sp
                )

                Text(
                    text =
                        if (
                            mode ==
                            ConsultationScreenMode.PRIVATE
                        ) {

                            doctor
                                ?.hourly_rate
                                ?.let {
                                    "This doctor's configured rate is R %.2f. The server confirms and snapshots the actual fee when the request is created."
                                        .format(
                                            it
                                        )
                                }
                                ?: "The doctor's configured rate will be confirmed by the server."

                        } else {

                            "The standard broadcast tariff is controlled by the server. The app does not invent or hard-code a price."
                        },
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
    }
}

@Composable
private fun ConsultationErrorCard(
    message: String
) {

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp
                ),
        shape =
            RoundedCornerShape(
                14.dp
            ),
        color =
            MaterialTheme
                .colorScheme
                .errorContainer
    ) {

        Text(
            text =
                message,
            color =
                MaterialTheme
                    .colorScheme
                    .onErrorContainer,
            modifier =
                Modifier.padding(
                    13.dp
                ),
            fontSize =
                12.5.sp
        )
    }
}

@Composable
private fun ActiveConsultationRequestScreen(
    modifier: Modifier,
    request: ConsultationRequestResult,
    liveRequest: ConsultationRequestRow?,
    secondsRemaining: Int,
    cancelling: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    onTryAgain: () -> Unit,
    onContinue: () -> Unit
) {

    val status =
        liveRequest
            ?.status
            ?.lowercase()
            ?: request
                .request_status
                .lowercase()

    when (status) {

        "claimed" -> {

            ConsultationClaimedScreen(
                modifier =
                    modifier,
                request =
                    request,
                liveRequest =
                    liveRequest,
                onContinue =
                    onContinue
            )
        }

        "cancelled",
        "expired",
        "declined",
        "no_doctors",
        "failed" -> {

            ConsultationFinishedScreen(
                modifier =
                    modifier,
                status =
                    status,
                error =
                    error,
                onTryAgain =
                    onTryAgain
            )
        }

        else -> {

            WaitingForDoctorScreen(
                modifier =
                    modifier,
                request =
                    request,
                secondsRemaining =
                    secondsRemaining,
                cancelling =
                    cancelling,
                error =
                    error,
                onRefresh =
                    onRefresh,
                onCancel =
                    onCancel
            )
        }
    }
}

@Composable
private fun WaitingForDoctorScreen(
    modifier: Modifier,
    request: ConsultationRequestResult,
    secondsRemaining: Int,
    cancelling: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCancel: () -> Unit
) {

    Box(
        modifier =
            modifier
                .padding(
                    20.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        ElevatedCard(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    26.dp
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

                CircularProgressIndicator(
                    modifier =
                        Modifier.size(
                            54.dp
                        ),
                    strokeWidth =
                        5.dp
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

                Text(
                    text =
                        if (
                            request
                                .request_mode ==
                            "broadcast"
                        ) {
                            "Finding an available doctor..."
                        } else {
                            "Waiting for doctor..."
                        },
                    fontSize =
                        20.sp,
                    fontWeight =
                        FontWeight.Bold,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )

                Text(
                    text =
                        if (
                            request
                                .request_mode ==
                            "broadcast"
                        ) {
                            "${request.recipient_count} eligible doctor${
                                if (
                                    request
                                        .recipient_count ==
                                    1
                                ) {
                                    ""
                                } else {
                                    "s"
                                }
                            } received this request."
                        } else {
                            "Your selected doctor has been invited."
                        },
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                LinearProgressIndicator(
                    progress = {
                        secondsRemaining
                            .coerceIn(
                                0,
                                60
                            ) / 60f
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                Text(
                    text =
                        if (
                            secondsRemaining > 0
                        ) {
                            "$secondsRemaining seconds remaining"
                        } else {
                            "Checking final status..."
                        },
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
                            16.dp
                        )
                )

                PriceRow(
                    amountMinor =
                        request.amount_minor,
                    currency =
                        request.payment_currency
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

                error?.let {

                    ConsultationErrorCard(
                        message =
                            it
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                10.dp
                            )
                    )
                }

                OutlinedButton(
                    onClick =
                        onRefresh,
                    modifier =
                        Modifier.fillMaxWidth()
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
                                7.dp
                            )
                    )

                    Text(
                        "Check status"
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )

                TextButton(
                    onClick =
                        onCancel,
                    enabled =
                        !cancelling
                ) {

                    if (
                        cancelling
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    17.dp
                                ),
                            strokeWidth =
                                2.dp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    7.dp
                                )
                        )
                    }

                    Text(
                        text =
                            if (
                                cancelling
                            ) {
                                "Cancelling..."
                            } else {
                                "Cancel request"
                            },
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsultationClaimedScreen(
    modifier: Modifier,
    request: ConsultationRequestResult,
    liveRequest: ConsultationRequestRow?,
    onContinue: () -> Unit
) {

    Box(
        modifier =
            modifier
                .padding(
                    20.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {

        ElevatedCard(
            modifier =
                Modifier.fillMaxWidth(),
            shape =
                RoundedCornerShape(
                    26.dp
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

                Surface(
                    shape =
                        CircleShape,
                    color =
                        ConsultationGreen.copy(
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
                            ConsultationGreen,
                        modifier =
                            Modifier
                                .padding(
                                    16.dp
                                )
                                .size(
                                    44.dp
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
                        "Doctor accepted",
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
                        "A doctor has securely claimed your consultation. Other broadcast invitations are now closed.",
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

                PriceRow(
                    amountMinor =
                        request.amount_minor,
                    currency =
                        request.payment_currency
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )

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
                                            0.55f
                                    ),
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .padding(
                                13.dp
                            ),
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Lock,
                        contentDescription =
                            null,
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
                            "The server selected exactly one winning doctor. The consultation room will only allow the patient and that doctor.",
                        fontSize =
                            11.sp,
                        lineHeight =
                            15.sp
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )

                Button(
                    onClick =
                        onContinue,
                    enabled =
                        !liveRequest
                            ?.claimed_doctor_id
                            .isNullOrBlank(),
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

                    Icon(
                        imageVector =
                            Icons.Outlined.VideoCall,
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
                            "Continue to consultation",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsultationFinishedScreen(
    modifier: Modifier,
    status: String,
    error: String?,
    onTryAgain: () -> Unit
) {

    val title =
        when (status) {

            "no_doctors" ->
                "No doctors available"

            "expired" ->
                "Request expired"

            "declined" ->
                "Request declined"

            "cancelled" ->
                "Request cancelled"

            else ->
                "Consultation unavailable"
        }

    val message =
        when (status) {

            "no_doctors" ->
                "There are currently no eligible available doctors for this request."

            "expired" ->
                "No doctor accepted before the request expired."

            "declined" ->
                "The invited doctor or doctors could not accept this consultation."

            "cancelled" ->
                "You cancelled this consultation request."

            else ->
                "The consultation could not be started."
        }

    Box(
        modifier =
            modifier
                .padding(
                    22.dp
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

                Surface(
                    shape =
                        CircleShape,
                    color =
                        MaterialTheme
                            .colorScheme
                            .errorContainer
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Close,
                        contentDescription =
                            null,
                        tint =
                            MaterialTheme
                                .colorScheme
                                .error,
                        modifier =
                            Modifier
                                .padding(
                                    14.dp
                                )
                                .size(
                                    38.dp
                                )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                Text(
                    text =
                        title,
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
                        error
                            ?: message,
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

                Button(
                    onClick =
                        onTryAgain,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Try again"
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRow(
    amountMinor: Int,
    currency: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                        .copy(
                            alpha =
                                0.45f
                        ),
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .padding(
                    14.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                Icons.Outlined.Payments,
            contentDescription =
                null,
            tint =
                MaterialTheme
                    .colorScheme
                    .primary
        )

        Spacer(
            modifier =
                Modifier.width(
                    9.dp
                )
        )

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    "Consultation price",
                fontSize =
                    10.5.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Text(
                text =
                    formatConsultationPrice(
                        amountMinor,
                        currency
                    ),
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    18.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}

private fun formatConsultationPrice(
    amountMinor: Int,
    currency: String
): String {

    val amount =
        amountMinor / 100.0

    return if (
        currency.equals(
            "ZAR",
            ignoreCase = true
        )
    ) {

        "R %.2f".format(
            amount
        )

    } else {

        "$currency %.2f".format(
            amount
        )
    }
}

private fun friendlyConsultationError(
    message: String?,
    mode: ConsultationScreenMode
): String {

    val value =
        message
            .orEmpty()

    return when {

        value.contains(
            "price",
            ignoreCase = true
        ) &&
                mode ==
                ConsultationScreenMode.BROADCAST ->

            "The standard broadcast consultation price has not been configured on the server yet."

        value.contains(
            "no doctors",
            ignoreCase = true
        ) ->

            "No eligible doctors are available right now. Please try again shortly."

        value.contains(
            "available",
            ignoreCase = true
        ) &&
                mode ==
                ConsultationScreenMode.PRIVATE ->

            "This doctor is not currently available for an on-demand consultation."

        value.contains(
            "approved",
            ignoreCase = true
        ) ->

            "This doctor is temporarily unavailable for consultations."

        value.contains(
            "active request",
            ignoreCase = true
        ) ->

            "You already have an active consultation request."

        value.contains(
            "Patient profile",
            ignoreCase = true
        ) ->

            "Your patient profile could not be found. Please sign in again."

        value.isNotBlank() ->
            value

        else ->
            "We couldn't create the consultation request. Please try again."
    }
}

