package ehealthy.connect.ui.call

import androidx.compose.runtime.Composable
import ehealthy.connect.consultation.ConsultationCallScreen

/**
 * Scheduled appointment status returned by the existing doctor/patient
 * Supabase callbacks. Kept so MainActivity's call routes remain unchanged.
 */
data class CallAppointmentStatus(
    val status: String?,
    val cancelledReason: String?,
    val callEndedAt: String? = null
)

/**
 * Scheduled calls now use the EXACT working broadcast Stream screen:
 * camera/microphone permissions, participant rendering, bottom controls,
 * speaker routing and video hang-up. Only the appointment-specific server
 * credentials and status callbacks differ.
 *
 * The broadcast route continues to call ConsultationCallScreen normally.
 */
@Composable
fun CallScreen(
    appointmentId: String,
    displayName: String,
    isDoctorView: Boolean = false,
    fetchAppointmentStatus: suspend () -> Result<CallAppointmentStatus>,
    onJoined: suspend () -> Result<Unit>,
    onEndCall: suspend () -> Result<Unit>,
    onBack: () -> Unit,
    onReschedule: () -> Unit = onBack
) {
    ConsultationCallScreen(
        requestId = appointmentId,
        displayName = displayName,
        isDoctorView = isDoctorView,
        onBack = onBack,
        scheduledAppointmentId = appointmentId,
        scheduledFetchStatus = fetchAppointmentStatus,
        scheduledOnJoined = onJoined,
        scheduledOnEnd = onEndCall
    )
}
