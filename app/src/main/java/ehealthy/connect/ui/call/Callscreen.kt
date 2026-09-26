package ehealthy.connect.ui.call

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import ehealthy.connect.util.fetchStreamToken
import io.getstream.video.android.compose.theme.VideoTheme
import io.getstream.video.android.compose.ui.components.call.activecall.CallContent
import io.getstream.video.android.core.Call
import io.getstream.video.android.core.GEO
import io.getstream.video.android.core.StreamVideoBuilder
import io.getstream.video.android.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val navy = Color(0xFF0B1828)
private val ink = Color(0xFF0F1F3D)
private val muted = Color(0xFF64748B)
private val teal = Color(0xFF0D9488)
private val red = Color(0xFFDC2626)
private val bg = Color(0xFFF4F7FA)

private const val STREAM_API_KEY = "67jj8rzevh48"

private sealed class CallScreenState {
    data object Loading : CallScreenState()
    data class Error(val message: String) : CallScreenState()
    data object Waiting : CallScreenState()
    data object OtherPartyNotAvailable : CallScreenState()
    data object InCall : CallScreenState()
    data object AppointmentCancelled : CallScreenState()
    data object NoAnswer : CallScreenState()
}

/**
 * Minimal shape of what this screen needs to know about the appointment's
 * live status — passed in via a polling fetch so we can detect the backend
 * cron job cancelling the appointment (no-show) while we're waiting.
 */
data class CallAppointmentStatus(
    val status: String?,
    val cancelledReason: String?
)

/**
 * Shared call screen for both patient and doctor. Set [isDoctorView] to true
 * when a doctor is joining — this swaps the waiting-room copy ("waiting for
 * the patient" instead of "waiting for the doctor"), hides the Reschedule
 * action (that's a patient-only action), and keeps cancellation messaging
 * generic rather than patient-specific refund wording.
 */
@Composable
fun CallScreen(
    appointmentId: String,
    displayName: String,
    isDoctorView: Boolean = false,
    fetchAppointmentStatus: suspend () -> Result<CallAppointmentStatus>,
    onJoined: suspend () -> Unit,
    onBack: () -> Unit,
    onReschedule: () -> Unit = onBack
) {
    var screenState by remember { mutableStateOf<CallScreenState>(CallScreenState.Loading) }
    var call by remember { mutableStateOf<Call?>(null) }
    var retryKey by remember { mutableIntStateOf(0) }
    var waitingSince by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ---- Set up Stream client + join the call ----
    LaunchedEffect(retryKey) {
        screenState = CallScreenState.Loading

        try {
            val tokenResult = fetchStreamToken()
            val tokenResponse = tokenResult.getOrElse {
                screenState = CallScreenState.Error(it.message ?: "Could not start the call.")
                return@LaunchedEffect
            }

            val user = User(id = tokenResponse.userId, name = displayName, role = "user")
            val client = StreamVideoBuilder(
                context = context.applicationContext,
                apiKey = STREAM_API_KEY,
                geo = GEO.GlobalEdgeNetwork,
                user = user,
                token = tokenResponse.token
            ).build()

            val streamCall = client.call(type = "default", id = appointmentId)
            val joinResult = streamCall.join(create = true)

            if (joinResult.isFailure) {
                val cause = joinResult.errorOrNull()
                android.util.Log.e("CallScreen", "Stream join failed: $cause")
                screenState = CallScreenState.Error(
                    "Could not join the call.\n\nDetails: ${cause?.message ?: cause.toString()}"
                )
                return@LaunchedEffect
            }

            call = streamCall
            onJoined()
            waitingSince = System.currentTimeMillis()
            screenState = CallScreenState.Waiting
        } catch (e: Exception) {
            screenState = CallScreenState.Error(e.message ?: "Something went wrong starting the call.")
        }
    }

    // ---- Watch for the other party actually joining ----
    val currentCall = call
    if (currentCall != null && screenState is CallScreenState.Waiting) {
        val remoteParticipants by currentCall.state.remoteParticipants.collectAsState()
        LaunchedEffect(remoteParticipants.size) {
            if (remoteParticipants.isNotEmpty()) {
                screenState = CallScreenState.InCall
            }
        }
    }

    // ---- Poll appointment status while waiting, to catch a no-show cancellation ----
    // ---- Doctor-only: if the patient doesn't answer within 30s, stop ringing ----
    // ---- Doctor-only: if the patient doesn't answer within 30s, stop ringing ----
    LaunchedEffect(waitingSince, retryKey) {
        val since = waitingSince ?: return@LaunchedEffect
        if (!isDoctorView) return@LaunchedEffect
        val remaining = 30_000 - (System.currentTimeMillis() - since)
        if (remaining > 0) delay(remaining.milliseconds)
        if (screenState is CallScreenState.Waiting) {
            call?.leave()
            screenState = CallScreenState.NoAnswer
        }
    }

    // ---- Poll appointment status while waiting, to catch a no-show cancellation ----
    LaunchedEffect(screenState) {
        while (screenState is CallScreenState.Waiting) {
            delay(15_000.milliseconds)
            val result = fetchAppointmentStatus()
            result.onSuccess { status ->
                if (status.status == "cancelled") {
                    screenState = if (status.cancelledReason == "doctor_no_show" || status.cancelledReason == "patient_no_show") {
                        CallScreenState.OtherPartyNotAvailable
                    } else {
                        CallScreenState.AppointmentCancelled
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            call?.leave()
        }
    }

    VideoTheme {
        Box(modifier = Modifier.fillMaxSize().background(bg)) {
            when (val state = screenState) {
                is CallScreenState.Loading -> LoadingState()
                is CallScreenState.Error -> ErrorState(state.message, onBack)
                is CallScreenState.Waiting -> WaitingState(isDoctorView)
                is CallScreenState.OtherPartyNotAvailable -> NotAvailableState(isDoctorView, onReschedule, onBack)
                is CallScreenState.AppointmentCancelled -> AppointmentCancelledState(onBack)
                is CallScreenState.NoAnswer -> NoAnswerState(
                    onRetry = { retryKey++ },
                    onBack = onBack
                )
                is CallScreenState.InCall -> {
                    currentCall?.let { activeCall ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            RecordingDisclaimerBanner()
                            CallContent(
                                modifier = Modifier.fillMaxSize(),
                                call = activeCall,
                                onBackPressed = onBack,
                                onCallAction = { action ->
                                    if (action is io.getstream.video.android.core.call.state.LeaveCall) {
                                        scope.launch {
                                            activeCall.end()
                                            onBack()
                                        }
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
private fun RecordingDisclaimerBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF7C2D12))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = Color.White, modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Recording this call is not allowed unless both parties verbally agree.",
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = teal)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connecting to your call…", color = muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun WaitingState(isDoctorView: Boolean) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = teal)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                if (isDoctorView) "Waiting for the patient to join…" else "Waiting for the doctor to join…",
                color = ink, fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You're connected. The call will start automatically once the other person joins.",
                color = muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NotAvailableState(isDoctorView: Boolean, onReschedule: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = red, modifier = Modifier.height(40.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (isDoctorView) "Appointment cancelled" else "Doctor not available",
                color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                if (isDoctorView)
                    "This appointment was automatically cancelled because one party didn't join in time."
                else
                    "The doctor didn't join within 5 minutes of the scheduled time. You've been fully refunded automatically.",
                color = muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (!isDoctorView) {
                Button(
                    onClick = onReschedule,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navy),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reschedule", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Dashboard", color = ink, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AppointmentCancelledState(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("This appointment was cancelled", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Dashboard", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
@Composable
private fun NoAnswerState(onRetry: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = red, modifier = Modifier.height(40.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("No answer", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The patient didn't join within 30 seconds. You can try calling again.",
                color = muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Dashboard", color = ink, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Couldn't start the call", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}