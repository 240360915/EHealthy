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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    data object WaitingForDoctor : CallScreenState()
    data object DoctorNotAvailable : CallScreenState()
    data object InCall : CallScreenState()
    data object AppointmentCancelled : CallScreenState()
}

/**
 * Minimal shape of what this screen needs to know about the appointment's
 * live status — passed in via a polling fetch so we can detect the backend
 * cron job cancelling the appointment (doctor no-show) while we're waiting.
 */
data class CallAppointmentStatus(
    val status: String?,
    val cancelledReason: String?
)

@Composable
fun CallScreen(
    appointmentId: String,
    displayName: String,
    fetchAppointmentStatus: suspend () -> Result<CallAppointmentStatus>,
    onMarkPatientJoined: suspend () -> Unit,
    onBack: () -> Unit,
    onReschedule: () -> Unit
) {
    val context = LocalContext.current

    var screenState by remember { mutableStateOf<CallScreenState>(CallScreenState.Loading) }
    var call by remember { mutableStateOf<Call?>(null) }

    // ---- Set up Stream client + join the call ----
    LaunchedEffect(Unit) {
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
            onMarkPatientJoined()
            screenState = CallScreenState.WaitingForDoctor
        } catch (e: Exception) {
            screenState = CallScreenState.Error(e.message ?: "Something went wrong starting the call.")
        }
    }

    // ---- Watch for the doctor actually joining ----
    val currentCall = call
    if (currentCall != null && screenState is CallScreenState.WaitingForDoctor) {
        val remoteParticipants by currentCall.state.remoteParticipants.collectAsState()
        LaunchedEffect(remoteParticipants.size) {
            if (remoteParticipants.isNotEmpty()) {
                screenState = CallScreenState.InCall
            }
        }
    }

    // ---- Poll appointment status while waiting, to catch a doctor-no-show cancellation ----
    LaunchedEffect(screenState) {
        while (screenState is CallScreenState.WaitingForDoctor) {
            delay(15_000.milliseconds)
            val result = fetchAppointmentStatus()
            result.onSuccess { status ->
                if (status.status == "cancelled") {
                    screenState = if (status.cancelledReason == "doctor_no_show") {
                        CallScreenState.DoctorNotAvailable
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
                is CallScreenState.WaitingForDoctor -> WaitingForDoctorState()
                is CallScreenState.DoctorNotAvailable -> DoctorNotAvailableState(onReschedule, onBack)
                is CallScreenState.AppointmentCancelled -> AppointmentCancelledState(onBack)
                is CallScreenState.InCall -> {
                    currentCall?.let { activeCall ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            RecordingDisclaimerBanner()
                            CallContent(
                                modifier = Modifier.fillMaxSize(),
                                call = activeCall,
                                onBackPressed = onBack
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
private fun WaitingForDoctorState() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = teal)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Waiting for the doctor to join…", color = ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You're connected. The call will start automatically once your doctor joins.",
                color = muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DoctorNotAvailableState(onReschedule: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.WifiOff, contentDescription = null, tint = red, modifier = Modifier.height(40.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Doctor not available", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The doctor didn't join within 5 minutes of the scheduled time. You've been fully refunded automatically.",
                color = muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onReschedule,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navy),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reschedule", color = Color.White, fontWeight = FontWeight.SemiBold)
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