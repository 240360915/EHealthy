package ehealthy.connect.consultation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoCall
import ehealthy.connect.data.ProfileRepository
import ehealthy.connect.data.DoctorLiveConsultationRepository
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.compose.ui.components.call.controls.actions.DefaultOnCallActionHandler
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideo
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private val consultationBackground =
    Color(0xFFF4F7FA)

private val consultationText =
    Color(0xFF0F1F3D)

private val consultationMuted =
    Color(0xFF64748B)

private val consultationTeal =
    Color(0xFF0D9488)

private val consultationRed =
    Color(0xFFDC2626)


/*
 * Public Stream API key.
 *
 * NEVER put STREAM_API_SECRET here.
 */
private const val CONSULTATION_STREAM_API_KEY =
    "67jj8rzevh48"


private sealed class ConsultationCallState {

    data object Loading :
        ConsultationCallState()


    data class Error(
        val message: String
    ) :
        ConsultationCallState()


    data object Waiting :
        ConsultationCallState()

    data object AwaitingPayment :
        ConsultationCallState()


    data object InCall :
        ConsultationCallState()
}


@Composable
fun ConsultationCallScreen(
    requestId: String,
    displayName: String,
    isDoctorView: Boolean,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var microphoneAllowed by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        microphoneAllowed = granted
    }
    LaunchedEffect(Unit) {
        if (!microphoneAllowed) microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }



    var screenState by remember {

        mutableStateOf<
                ConsultationCallState
                >(
            ConsultationCallState.Loading
        )
    }


    var call by remember {

        mutableStateOf<Call?>(
            null
        )
    }


    var retryKey by remember {

        mutableIntStateOf(
            0
        )
    }


    /*
     * ---------------------------------------------
     * CONNECT TO SECURE CONSULTATION
     * ---------------------------------------------
     */
    LaunchedEffect(
        requestId,
        retryKey,
        microphoneAllowed
    ) {

        if (!microphoneAllowed) {
            screenState = ConsultationCallState.Error(
                "Microphone access is required so the doctor and patient can hear each other. " +
                    "Allow microphone access in Android Settings, then try again."
            )
            return@LaunchedEffect
        }

        screenState =
            ConsultationCallState.Loading


        try {
            // A claimed consultation is still awaiting payment until
            // Supabase creates its paid consultation_sessions row.
            // Never treat a missing room (HTTP 409) as a failed call.
            val myDoctorId = if (isDoctorView) {
                ProfileRepository.doctorId()
            } else {
                null
            }

            var pendingCredentials: ConsultationStreamCredentials? = null

            while (pendingCredentials == null) {
                val request = ConsultationRepository.request(requestId)

                if (request == null) {
                    screenState = ConsultationCallState.Error(
                        "Consultation not found. Please return to your dashboard."
                    )
                    return@LaunchedEffect
                }

                if (request.status != "claimed") {
                    screenState = ConsultationCallState.Error(
                        when (request.status) {
                            "cancelled", "failed", "declined" ->
                                "This consultation was cancelled or its DemoPay payment failed."
                            "completed" -> "This consultation has already ended."
                            "pending" -> "The doctor has not accepted this consultation yet."
                            else -> "This consultation is no longer available."
                        }
                    )
                    return@LaunchedEffect
                }

                if (isDoctorView && request.claimed_doctor_id != myDoctorId) {
                    screenState = ConsultationCallState.Error(
                        "This consultation is assigned to another doctor."
                    )
                    return@LaunchedEffect
                }

                val session = ConsultationRepository.session(requestId)

                if (session == null) {
                    // No consultation_sessions row means no confirmed
                    // DemoPay yet. Patient must finish checkout; doctor waits.
                    if (!isDoctorView) {
                        screenState = ConsultationCallState.Error(
                            "Please return to Consult Doctor Now and finish " +
                                    "DemoPay before joining the video call."
                        )
                        return@LaunchedEffect
                    }
                    screenState = ConsultationCallState.AwaitingPayment
                    delay(3_000L)
                    continue
                }

                if (session.status == "closed" || session.status == "failed") {
                    screenState = ConsultationCallState.Error(
                        "This consultation video room is already closed."
                    )
                    return@LaunchedEffect
                }

                val tokenResult = ConsultationStreamRepository.getCredentials(requestId)
                val tokenError = tokenResult.exceptionOrNull()

                if (tokenError != null) {
                    val errorText = tokenError.message.orEmpty()
                    val roomIsStillPreparing =
                        errorText.contains("409") ||
                                errorText.contains("402") ||
                                errorText.contains("still being prepared", ignoreCase = true) ||
                                errorText.contains("DemoPay required", ignoreCase = true)

                    if (roomIsStillPreparing && isDoctorView) {
                        // Protect against the short race between the
                        // payment transaction and Stream room provisioning.
                        screenState = ConsultationCallState.AwaitingPayment
                        delay(3_000L)
                        continue
                    }

                    screenState = ConsultationCallState.Error(
                        if (roomIsStillPreparing) {
                            "Please finish DemoPay in Consult Doctor Now, " +
                                    "then try joining the video again."
                        } else {
                            consultationCallMessage(tokenError)
                        }
                    )
                    return@LaunchedEffect
                }

                pendingCredentials = tokenResult.getOrThrow()
            }

            val credentials = pendingCredentials
                ?: return@LaunchedEffect

            /*
             * Remove any old Stream client.
             */
            try {

                StreamVideo
                    .removeClient()

            } catch (
                _: Exception
            ) {

                // No previous client.
            }


            /*
             * Stream user identity comes from
             * the secure Edge Function.
             */
            val user =
                User(
                    id =
                        credentials.userId,

                    name =
                        displayName,

                    role =
                        "user"
                )


            val client =
                StreamVideoBuilder(

                    context =
                        context
                            .applicationContext,

                    apiKey =
                        CONSULTATION_STREAM_API_KEY,

                    geo =
                        GEO.GlobalEdgeNetwork,

                    user =
                        user,

                    token =
                        credentials.token
                )
                    .build()


            /*
             * IMPORTANT:
             *
             * We use the server-returned:
             *
             * callType
             * callId
             *
             * NOT requestId directly.
             */
            val streamCall =
                client.call(

                    type =
                        credentials.callType,

                    id =
                        credentials.callId
                )


            /*
             * IMPORTANT:
             *
             * create = false
             *
             * The Stream room must already have
             * been created by our secure server.
             */
            val joinResult =
                streamCall.join(
                    create =
                        false
                )


            if (
                joinResult.isFailure
            ) {

                /*
                 * Stream's error type is NOT Throwable.
                 *
                 * So we convert it to text instead of
                 * passing it to Log.e as a Throwable.
                 */
                val cause =
                    joinResult
                        .errorOrNull()


                android.util.Log.e(
                    "ConsultationCall",
                    "Stream consultation join failed: $cause"
                )


                screenState =
                    ConsultationCallState.Error(

                        cause
                            ?.message
                            ?: "Could not join the consultation."
                    )


                return@LaunchedEffect
            }


            // Publish local audio before the other participant arrives.
            streamCall.microphone.setEnabled(true)

            call =
                streamCall


            screenState =
                ConsultationCallState.Waiting


        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (
            e: Exception
        ) {

            screenState =
                ConsultationCallState.Error(
                    consultationCallMessage(
                        e
                    )
                )
        }
    }


    /*
     * ---------------------------------------------
     * WAIT FOR OTHER PERSON
     * ---------------------------------------------
     */
    val currentCall =
        call


    if (
        currentCall != null &&
        screenState is
                ConsultationCallState.Waiting
    ) {

        val remoteParticipants by
        currentCall
            .state
            .remoteParticipants
            .collectAsState()


        LaunchedEffect(
            remoteParticipants.size
        ) {

            if (
                remoteParticipants
                    .isNotEmpty()
            ) {

                screenState =
                    ConsultationCallState.InCall
            }
        }
    }


    /*
     * ---------------------------------------------
     * CLEANUP
     * ---------------------------------------------
     */
    DisposableEffect(
        Unit
    ) {

        onDispose {

            try {

                call
                    ?.leave()

            } catch (
                _: Exception
            ) {
            }


            try {

                StreamVideo
                    .removeClient()

            } catch (
                _: Exception
            ) {
            }
        }
    }

    fun finishConsultation(
        activeCall: Call
    ) {
        coroutineScope.launch {

            DoctorLiveConsultationRepository
                .endConsultation(requestId)
                .onFailure { error ->

                    android.util.Log.e(
                        "ConsultationCall",
                        "Could not mark consultation as completed",
                        error
                    )
                }

            try {
                activeCall.leave()
            } catch (error: Exception) {

                android.util.Log.e(
                    "ConsultationCall",
                    "Could not leave Stream call",
                    error
                )
            }

            try {
                StreamVideo.removeClient()
            } catch (_: Exception) {
            }

            onBack()
        }
    }


    VideoTheme {

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        consultationBackground
                    )
        ) {

            when (
                val state =
                    screenState
            ) {

                ConsultationCallState.Loading -> {

                    ConsultationLoading()
                }


                is ConsultationCallState.Error -> {

                    ConsultationCallError(
                        message =
                            state.message,

                        onRetry = {
                            retryKey++
                        },

                        onBack =
                            onBack
                    )
                }


                ConsultationCallState.AwaitingPayment -> {

                    ConsultationAwaitingPayment(
                        onBack = onBack
                    )
                }


                ConsultationCallState.Waiting -> {

                    ConsultationWaiting(
                        isDoctorView =
                            isDoctorView,
                        onBack =
                            onBack
                    )
                }


                ConsultationCallState.InCall -> {

                    currentCall
                        ?.let {
                                activeCall ->

                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                            ) {

                                ConsultationRecordingBanner()


                                CallContent(

                                    modifier =
                                        Modifier
                                            .fillMaxSize(),

                                    call =
                                        activeCall,

                                    onBackPressed = {
                                        finishConsultation(activeCall)
                                    },

                                    onCallAction = { action ->

                                        if (
                                            action is
                                                    io.getstream.video.android.core.call.state.LeaveCall
                                        ) {
                                            finishConsultation(activeCall)
                                        } else {
                                            // Preserve Stream's mute/unmute, camera and speaker actions.
                                            DefaultOnCallActionHandler.onCallAction(activeCall, action)
                                        }
                                    }
                                )
                            }
                        }
                }
            }
        }
    }
}


@Composable
private fun ConsultationAwaitingPayment(onBack: () -> Unit) {

    Box(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.VideoCall,
                contentDescription = null,
                tint = consultationTeal
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Waiting for patient payment",
                color = consultationText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "You have accepted the consultation. " +
                        "The patient is completing DemoPay. " +
                        "The video room will connect automatically after payment.",
                color = consultationMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(color = consultationTeal)

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(onClick = onBack) {
                Text("Leave waiting screen")
            }
        }
    }
}


@Composable
private fun ConsultationLoading() {

    Box(
        modifier =
            Modifier.fillMaxSize(),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                color =
                    consultationTeal
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    "Preparing secure consultation…",

                color =
                    consultationMuted,

                fontSize =
                    14.sp
            )
        }
    }
}


@Composable
private fun ConsultationWaiting(
    isDoctorView: Boolean,
    onBack: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    28.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.VideoCall,

                contentDescription =
                    null,

                tint =
                    consultationTeal,

                modifier =
                    Modifier.height(
                        52.dp
                    )
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
                        isDoctorView
                    ) {

                        "Waiting for the patient"

                    } else {

                        "Doctor accepted your request"
                    },

                color =
                    consultationText,

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
                        8.dp
                    )
            )


            Text(
                text =
                    if (
                        isDoctorView
                    ) {

                        "Your secure consultation room is ready. " +
                                "The call will start automatically " +
                                "when the patient joins."

                    } else {

                        "Your secure consultation room is ready. " +
                                "The call will start automatically " +
                                "when the doctor joins."
                    },

                color =
                    consultationMuted,

                fontSize =
                    13.sp,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            CircularProgressIndicator(
                color =
                    consultationTeal
            )


            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )


            OutlinedButton(
                onClick = {

                    onBack()
                }
            ) {

                Text(
                    "Leave waiting room"
                )
            }
        }
    }
}


@Composable
private fun ConsultationCallError(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    28.dp
                ),

        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Info,

                contentDescription =
                    null,

                tint =
                    consultationRed,

                modifier =
                    Modifier.height(
                        44.dp
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Text(
                text =
                    "Could not join consultation",

                color =
                    consultationText,

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

                color =
                    consultationMuted,

                fontSize =
                    13.sp,

                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        24.dp
                    )
            )


            Button(
                onClick =
                    onRetry
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Refresh,

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


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            OutlinedButton(
                onClick =
                    onBack
            ) {

                Text(
                    "Go back"
                )
            }
        }
    }
}


@Composable
private fun ConsultationRecordingBanner() {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color(
                        0xFF7C2D12
                    )
                )
                .statusBarsPadding()
                .padding(
                    horizontal =
                        16.dp,

                    vertical =
                        8.dp
                ),

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
                Modifier.height(
                    16.dp
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
                "Recording this consultation is not allowed " +
                        "unless both parties verbally agree.",

            color =
                Color.White,

            fontSize =
                11.5.sp,

            fontWeight =
                FontWeight.Medium
        )
    }
}


private fun consultationCallMessage(
    error: Throwable
): String {

    val message =
        error.message
            .orEmpty()


    return when {

        message.contains("402") ||
                message.contains("DemoPay required", ignoreCase = true) ->
            "Waiting for the patient to finish DemoPay before joining. Try again shortly."

        message.contains(
            "403"
        ) ->

            "You are not authorized to join this consultation."


        message.contains(
            "not a participant",
            ignoreCase =
                true
        ) ->

            "You are not authorized to join this consultation."


        message.contains(
            "409"
        ) ->

            "The video room is still being prepared. Try again shortly."


        message.contains(
            "still being prepared",
            ignoreCase =
                true
        ) ->

            "The video room is still being prepared. Try again."


        message.contains(
            "401"
        ) ->

            "Your session expired. Please sign in again."


        else ->

            message.ifBlank {

                "The secure video consultation could not be started."
            }
    }
}