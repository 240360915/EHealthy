package ehealthy.connect.consultation

import android.app.NotificationManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import ehealthy.connect.data.ProfileRepository
import ehealthy.connect.ui.theme.EHealthyTheme
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds


class IncomingConsultationActivity : ComponentActivity() {
    private fun openConsultationCall(
        requestId: String
    ) {

        val callIntent =
            android.content.Intent(
                this,
                ehealthy.connect.MainActivity::class.java
            ).apply {

                putExtra(
                    "navigateTo",
                    "consultationCall/$requestId/doctor"
                )
            }


        startActivity(
            callIntent
        )

        finish()
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        if (
            Build.VERSION.SDK_INT >= 27
        ) {

            setShowWhenLocked(
                true
            )

            setTurnScreenOn(
                true
            )
        }

        val requestId =
            intent.getStringExtra(
                "request_id"
            )
                ?: return finish()

        val owner =
            intent.getStringExtra(
                "owner"
            )
                ?: return finish()

        if (
            runCatching {
                UUID.fromString(
                    requestId
                )

                UUID.fromString(
                    owner
                )
            }.isFailure
        ) {

            finish()

            return
        }

        /*
         * Once this full screen is visible it controls the ringtone.
         * Stop the notification ringtone to prevent two sounds playing.
         */
        getSystemService(
            NotificationManager::class.java
        )
            .cancel(
                requestId,
                4101
            )

        setContent {

            EHealthyTheme {

                Surface(
                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    IncomingConsultationScreen(

                        requestId =
                            requestId,

                        owner =
                            owner,

                        onJoin = {

                            openConsultationCall(
                                requestId
                            )
                        },

                        onClose = {

                            finish()
                        }
                    )
                }
            }
        }
    }


}

private enum class IncomingScreenState {

    CHECKING,

    RINGING,

    ACCEPTED,

    CLOSED,

    CONNECTION_ERROR
}

@Composable
private fun IncomingConsultationScreen(
    requestId: String,
    owner: String,
    onJoin: () -> Unit,
    onClose: () -> Unit
) {

    val context =
        androidx.compose.ui.platform
            .LocalContext
            .current

    val lifecycleOwner =
        LocalLifecycleOwner.current

    val scope =
        rememberCoroutineScope()

    var request by remember {
        mutableStateOf<ConsultationRow?>(
            null
        )
    }

    var recipient by remember {
        mutableStateOf<ConsultationRecipient?>(
            null
        )
    }

    var screenState by remember {
        mutableStateOf(
            IncomingScreenState.CHECKING
        )
    }

    var busy by remember {
        mutableStateOf(false)
    }

    var foreground by remember {
        mutableStateOf(false)
    }

    var message by remember {
        mutableStateOf(
            "Checking consultation request..."
        )
    }

    var secondsRemaining by remember {
        mutableIntStateOf(60)
    }

    var acceptedDoctorId by remember {
        mutableStateOf<String?>(null)
    }

    val notificationChanges by
    ConsultationNotifications
        .changes
        .collectAsState()

    val localInvitationState =
        remember(
            notificationChanges,
            requestId,
            owner
        ) {

            ConsultationNotifications
                .state(
                    context,
                    owner,
                    requestId
                )
        }

    val locallyClosed =
        localInvitationState.closed

    suspend fun refreshRequest() {

        if (
            ConsultationRepository
                .userId() != owner
        ) {

            screenState =
                IncomingScreenState.CLOSED

            message =
                "This invitation belongs to another account."

            return
        }

        try {

            val currentRequest =
                withTimeout(
                    8_000.milliseconds
                ) {

                    ConsultationRepository
                        .request(
                            requestId
                        )
                }

            val currentRecipient =
                withTimeout(
                    8_000.milliseconds
                ) {

                    ConsultationRepository
                        .recipient(
                            requestId
                        )
                }

            request =
                currentRequest
            val currentDoctorId =
                ProfileRepository.doctorId()


            if (
                currentRequest != null &&
                currentRequest.status == "claimed" &&
                currentRequest.claimed_doctor_id ==
                currentDoctorId
            ) {

                screenState =
                    IncomingScreenState.ACCEPTED

                acceptedDoctorId =
                    currentDoctorId

                message =
                    "You accepted this consultation."

                ConsultationNotifications
                    .close(
                        context,
                        owner,
                        requestId,
                        currentRequest.version
                    )

                return
            }
            recipient =
                currentRecipient

            if (
                currentRequest == null ||
                currentRecipient == null
            ) {

                screenState =
                    IncomingScreenState.CLOSED

                message =
                    "This consultation invitation is no longer available."

                return
            }

            val mayRing =
                currentRequest.status ==
                        "pending" &&
                        currentRecipient.status ==
                        "pending" &&
                        InvitationRules
                            .mayRing(
                                ConsultationNotifications
                                    .state(
                                        context,
                                        owner,
                                        requestId
                                    ),
                                currentRequest.version,
                                currentRequest.expires_at,
                                System.currentTimeMillis()
                            )

            when {

                mayRing -> {

                    screenState =
                        IncomingScreenState.RINGING

                    message =
                        if (
                            currentRequest.mode ==
                            "private"
                        ) {

                            "A patient has requested a private consultation with you."

                        } else {

                            "A patient is requesting an available doctor."
                        }
                }

                currentRequest.status ==
                        "claimed" &&
                        currentRecipient.status ==
                        "claimed" &&
                        currentRequest.claimed_doctor_id == ProfileRepository.doctorId() -> {

                    screenState =
                        IncomingScreenState.ACCEPTED

                    acceptedDoctorId =
                        currentRequest
                            .claimed_doctor_id

                    message =
                        "You accepted this consultation."
                }

                currentRequest.status ==
                        "claimed" -> {

                    screenState =
                        IncomingScreenState.CLOSED

                    message =
                        "Another doctor accepted this consultation."
                }

                currentRequest.status ==
                        "cancelled" -> {

                    screenState =
                        IncomingScreenState.CLOSED

                    message =
                        "The patient cancelled this request."
                }

                currentRequest.status ==
                        "expired" -> {

                    screenState =
                        IncomingScreenState.CLOSED

                    message =
                        "This consultation request expired."
                }

                currentRecipient.status ==
                        "declined" -> {

                    screenState =
                        IncomingScreenState.CLOSED

                    message =
                        "You declined this consultation."
                }

                else -> {

                    screenState =
                        IncomingScreenState.CLOSED

                    message =
                        "This invitation is no longer available."
                }
            }

            if (
                screenState !=
                IncomingScreenState.RINGING
            ) {

                ConsultationNotifications
                    .close(
                        context,
                        owner,
                        requestId,
                        currentRequest.version
                    )
            }

        } catch (
            _: TimeoutCancellationException
        ) {

            screenState =
                IncomingScreenState.CONNECTION_ERROR

            message =
                "Connection lost. We couldn't confirm whether this invitation is still active."

        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (
            _: Exception
        ) {

            screenState =
                IncomingScreenState.CONNECTION_ERROR

            message =
                "We couldn't verify this invitation. Check your connection and retry."
        }
    }

    fun acceptConsultation() {

        if (
            busy
        ) {
            return
        }

        busy =
            true

        message =
            "Confirming your acceptance..."

        scope.launch {

            try {

                /*
                 * Stable idempotency key:
                 *
                 * If the doctor's network drops after the server accepts,
                 * tapping again safely returns the same claim result.
                 */
                val claimKey =
                    UUID.nameUUIDFromBytes(
                        "$owner/$requestId/claim"
                            .toByteArray()
                    )
                        .toString()

                val result =
                    withTimeout(
                        10_000.milliseconds
                    ) {

                        ConsultationRepository
                            .claim(
                                requestId,
                                claimKey
                            )
                    }

                ConsultationNotifications
                    .close(
                        context,
                        owner,
                        requestId,
                        result.request_version
                    )

                val doctorId =
                    ProfileRepository.doctorId()


                if (
                    isWinningConsultationClaim(
                        result,
                        doctorId
                    )
                ) {

                    /*
                     * The claim RPC already returned the authoritative
                     * server result.
                     *
                     * Do NOT immediately perform another network read.
                     */
                    acceptedDoctorId =
                        doctorId

                    screenState =
                        IncomingScreenState.ACCEPTED

                    message =
                        "You accepted this consultation."

                } else {

                    screenState =
                        IncomingScreenState.CLOSED

                    message =
                        if (
                            result.claim_result ==
                            "already_claimed"
                        ) {
                            "Another doctor accepted this consultation."
                        } else {
                            "This consultation is no longer available."
                        }
                }

            }
            catch (
                _: TimeoutCancellationException
            ) {

                screenState =
                    IncomingScreenState.CONNECTION_ERROR

                message =
                    "Acceptance was not confirmed. Tap Check status before trying again."

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                message =
                    consultationError(
                        e
                    )

                /*
                 * The server may actually have committed the claim
                 * before a network failure reached Android.
                 *
                 * Re-read server state instead of assuming failure.
                 */
                refreshRequest()

            } finally {

                busy =
                    false
            }
        }
    }

    fun declineConsultation() {

        if (
            busy
        ) {
            return
        }

        busy =
            true

        scope.launch {

            try {

                val result =
                    withTimeout(
                        10_000.milliseconds
                    ) {

                        ConsultationRepository
                            .decline(
                                requestId
                            )
                    }

                ConsultationNotifications
                    .close(
                        context,
                        owner,
                        requestId,
                        result.request_version
                    )

                screenState =
                    IncomingScreenState.CLOSED

                message =
                    "Consultation declined."

            } catch (
                _: TimeoutCancellationException
            ) {

                message =
                    "Decline was not confirmed. Check the request status before trying again."

                screenState =
                    IncomingScreenState.CONNECTION_ERROR

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                message =
                    consultationError(
                        e
                    )
            }

            busy =
                false
        }
    }

    /*
     * Keep checking server state while this screen is in the foreground.
     *
     * This catches:
     * - another doctor winning the broadcast race
     * - patient cancellation
     * - request expiry
     * - this doctor becoming the winner
     */
    LaunchedEffect(
        requestId,
        owner,
        lifecycleOwner
    ) {

        lifecycleOwner
            .lifecycle
            .repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                foreground =
                    true

                try {

                    withTimeout(
                        8_000.milliseconds
                    ) {

                        SupabaseClientProvider
                            .client
                            .auth
                            .awaitInitialization()
                    }

                    while (
                        isActive
                    ) {

                        refreshRequest()

                        if (
                            screenState ==
                            IncomingScreenState.CLOSED ||
                            screenState ==
                            IncomingScreenState.ACCEPTED
                        ) {
                            break
                        }

                        delay(
                            2_000.milliseconds
                        )
                    }

                } finally {

                    foreground =
                        false
                }
            }
    }

    /*
     * Countdown comes from the server's expires_at timestamp,
     * not a locally invented 60 second assumption.
     */
    LaunchedEffect(
        request?.expires_at
    ) {

        while (true) {

            val expiry =
                request
                    ?.expires_at
                    ?: break

            val remainingMillis =
                runCatching {

                    Instant
                        .parse(
                            expiry
                        )
                        .toEpochMilli() -
                            System.currentTimeMillis()

                }
                    .getOrDefault(
                        0L
                    )

            secondsRemaining =
                (
                        remainingMillis /
                                1_000L
                        )
                    .toInt()
                    .coerceAtLeast(
                        0
                    )

            if (
                remainingMillis <= 0L
            ) {

                refreshRequest()

                break
            }

            delay(
                1_000.milliseconds
            )
        }
    }

    val shouldRing =
        screenState ==
                IncomingScreenState.RINGING &&
                foreground &&
                !busy &&
                !locallyClosed

    /*
     * The visible incoming screen owns the ringtone.
     */
    DisposableEffect(
        shouldRing
    ) {

        val ringtone =
            if (
                shouldRing
            ) {

                runCatching {

                    RingtoneManager
                        .getRingtone(
                            context,
                            RingtoneManager
                                .getDefaultUri(
                                    RingtoneManager
                                        .TYPE_RINGTONE
                                )
                        )
                        .apply {

                            if (
                                Build.VERSION.SDK_INT >= 28
                            ) {

                                isLooping =
                                    true
                            }

                            play()
                        }

                }
                    .getOrNull()

            } else {

                null
            }

        onDispose {

            ringtone
                ?.stop()
        }
    }

    Scaffold(
        containerColor =
            MaterialTheme
                .colorScheme
                .background
    ) { innerPadding ->

        AnimatedContent(
            targetState =
                screenState,
            label =
                "incoming-consultation"
        ) { state ->

            when (
                state
            ) {

                IncomingScreenState.CHECKING -> {

                    CheckingInvitationScreen(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    innerPadding
                                )
                    )
                }

                IncomingScreenState.RINGING -> {

                    IncomingInvitationContent(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    innerPadding
                                ),
                        request =
                            request,
                        secondsRemaining =
                            secondsRemaining,
                        busy =
                            busy,
                        message =
                            message,
                        onAccept =
                            ::acceptConsultation,
                        onDecline =
                            ::declineConsultation,
                        onClose =
                            onClose
                    )
                }

                IncomingScreenState.ACCEPTED -> {

                    AcceptedConsultationScreen(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    innerPadding
                                ),
                        request =
                            request,
                        acceptedDoctorId =
                            acceptedDoctorId,
                        onJoin = onJoin,
                        onClose =
                            onClose
                    )
                }

                IncomingScreenState.CONNECTION_ERROR -> {

                    ConnectionErrorScreen(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    innerPadding
                                ),
                        message =
                            message,
                        busy =
                            busy,
                        onRetry = {

                            scope.launch {

                                busy =
                                    true

                                refreshRequest()

                                busy =
                                    false
                            }
                        },
                        onClose =
                            onClose
                    )
                }

                IncomingScreenState.CLOSED -> {

                    ClosedInvitationScreen(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    innerPadding
                                ),
                        message =
                            message,
                        onClose =
                            onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckingInvitationScreen(
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
                        14.dp
                    )
            )

            Text(
                text =
                    "Checking consultation...",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun IncomingInvitationContent(
    modifier: Modifier,
    request: ConsultationRow?,
    secondsRemaining: Int,
    busy: Boolean,
    message: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClose: () -> Unit
) {

    val progress =
        (secondsRemaining / 60f)
            .coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF041F33),
                        Color(0xFF073B5C),
                        Color(0xFF087F8C)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = 18.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /*
             * TOP SECURITY LABEL
             */
            Surface(
                color = Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.16f)
                )
            ) {

                Row(
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFF8FF3DA),
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(7.dp)
                    )

                    Text(
                        text = "Secure consultation request",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(
                modifier = Modifier.weight(0.6f)
            )


            /*
             * LARGE CALL ICON
             */
            Surface(
                modifier = Modifier
                    .size(126.dp)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.10f)
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Surface(
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.14f)
                    ) {

                        Box(
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Outlined.VideoCall,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(25.dp)
            )


            Text(
                text =
                    if (request?.mode == "private") {
                        "Private consultation"
                    } else {
                        "Incoming consultation"
                    },
                color = Color.White,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = message,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = 16.dp
                )
            )

            Spacer(
                modifier = Modifier.height(23.dp)
            )


            /*
             * DETAILS
             */
            request?.let { row ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    ConsultationInfoPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.VideoCall,
                        title = "TYPE",
                        value =
                            if (row.mode == "private") {
                                "Private"
                            } else {
                                "Broadcast"
                            }
                    )

                    ConsultationInfoPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Info,
                        title = "SERVICE",
                        value =
                            row.service.replaceFirstChar {
                                it.uppercase()
                            }
                    )

                    ConsultationInfoPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Payments,
                        title = "FEE",
                        value = consultationPrice(
                            row.price_minor,
                            row.currency
                        )
                    )
                }
            }


            Spacer(
                modifier = Modifier.height(24.dp)
            )


            /*
             * COUNTDOWN
             */
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            ) {

                Column(
                    modifier = Modifier.padding(17.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF8FF3DA),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(
                            modifier = Modifier.width(9.dp)
                        )

                        Text(
                            text = "Time remaining",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp
                        )

                        Spacer(
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${secondsRemaining}s",
                            color =
                                if (secondsRemaining <= 10) {
                                    Color(0xFFFFB4AB)
                                } else {
                                    Color.White
                                },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    LinearProgressIndicator(
                        progress = {
                            progress
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color =
                            if (secondsRemaining <= 10) {
                                Color(0xFFFF6B6B)
                            } else {
                                Color(0xFF6EE7C4)
                            },
                        trackColor =
                            Color.White.copy(alpha = 0.12f)
                    )
                }
            }


            Spacer(
                modifier = Modifier.weight(1f)
            )


            /*
             * ACTION BUTTONS
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceEvenly,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                ConsultationActionButton(
                    title = "Decline",
                    icon = Icons.Filled.Close,
                    containerColor = Color(0xFFD83B46),
                    enabled = !busy,
                    onClick = onDecline
                )


                ConsultationActionButton(
                    title =
                        if (busy) {
                            "Accepting"
                        } else {
                            "Accept"
                        },
                    icon = Icons.Filled.Call,
                    containerColor = Color(0xFF20A464),
                    enabled = !busy,
                    loading = busy,
                    onClick = onAccept
                )
            }


            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    horizontal = 12.dp
                )
            ) {

                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.size(14.dp)
                )

                Spacer(
                    modifier = Modifier.width(6.dp)
                )

                Text(
                    text =
                        "For broadcast requests, the server confirms exactly one doctor.",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 10.5.sp,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(
                onClick = onClose,
                enabled = !busy
            ) {

                Text(
                    text = "Dismiss",
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun ConsultationInfoPill(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.12f)
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 13.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF8FF3DA),
                modifier = Modifier.size(18.dp)
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = title,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = value,
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}


@Composable
private fun ConsultationActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    0.dp
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                disabledContainerColor =
                    containerColor.copy(alpha = 0.45f)
            )
        ) {

            if (loading) {

                CircularProgressIndicator(
                    modifier = Modifier.size(25.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )

            } else {

                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(29.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
@Composable
private fun AcceptedConsultationScreen(
    modifier: Modifier,
    request: ConsultationRow?,
    acceptedDoctorId: String?,
    onJoin: () -> Unit,
    onClose: () -> Unit
) {

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF062B37),
                        Color(0xFF075449),
                        Color(0xFF0B7A5A)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Surface(
                modifier = Modifier.size(118.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.13f),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.18f)
                )
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF8EF0BD),
                        modifier = Modifier.size(58.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Text(
                text = "Consultation secured",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "The server confirmed you as the doctor for this consultation.",
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center
            )

            request?.let { row ->

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    ConsultationInfoPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.VideoCall,
                        title = "MODE",
                        value =
                            if (row.mode == "private") {
                                "Private"
                            } else {
                                "Broadcast"
                            }
                    )

                    ConsultationInfoPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Info,
                        title = "SERVICE",
                        value =
                            row.service.replaceFirstChar {
                                it.uppercase()
                            }
                    )

                    ConsultationInfoPill(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Outlined.Payments,
                        title = "FEE",
                        value =
                            consultationPrice(
                                row.price_minor,
                                row.currency
                            )
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.Black.copy(alpha = 0.12f)
            ) {

                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color(0xFF8EF0BD),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(10.dp)
                    )

                    Text(
                        text =
                            "Your identity and room access will be checked again before the video connects.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = onJoin,
                enabled =
                    !acceptedDoctorId.isNullOrBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF075449)
                )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.VideoCall,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(9.dp)
                )

                Text(
                    text = "Join secure video",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            TextButton(
                onClick = onClose
            ) {

                Text(
                    text = "Return to dashboard",
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun ClosedInvitationScreen(
    modifier: Modifier,
    message: String,
    onClose: () -> Unit
) {

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF241B1C),
                        Color(0xFF3B2023),
                        Color(0xFF581F27)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.padding(30.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Surface(
                modifier = Modifier.size(105.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.09f)
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Close,
                        contentDescription = null,
                        tint = Color(0xFFFF9B9B),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Invitation closed",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = message,
                color = Color.White.copy(alpha = 0.68f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(
                modifier = Modifier.height(30.dp)
            )

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF581F27)
                )
            ) {

                Text(
                    text = "Close",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConnectionErrorScreen(
    modifier: Modifier,
    message: String,
    busy: Boolean,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF071F32),
                        Color(0xFF0A354E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.padding(30.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Surface(
                modifier = Modifier.size(106.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f)
            ) {

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = Color(0xFF8DD7F3),
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(25.dp)
            )

            Text(
                text = "Connection problem",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = message,
                color = Color.White.copy(alpha = 0.67f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )

            Spacer(
                modifier = Modifier.height(28.dp)
            )

            Button(
                onClick = onRetry,
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF073B5C)
                )
            ) {

                if (busy) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )
                }

                Icon(
                    imageVector =
                        Icons.Filled.Refresh,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = "Check status",
                    fontWeight = FontWeight.Bold
                )
            }

            TextButton(
                onClick = onClose,
                enabled = !busy
            ) {

                Text(
                    text = "Close",
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

fun consultationPrice(
    minor: Int,
    currency: String
): String {

    val amount =
        java.math.BigDecimal
            .valueOf(
                minor.toLong(),
                2
            )

    return if (
        currency.equals(
            "ZAR",
            ignoreCase =
                true
        )
    ) {

        "R ${amount.toPlainString()}"

    } else {

        "$currency ${amount.toPlainString()}"
    }
}