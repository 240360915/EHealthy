package ehealthy.connect.consultation

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ehealthy.connect.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

/** Explicit user opt-in. If killed/offline, server eligibility expires after two minutes. */
class DoctorAvailabilityService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    companion object { val status = MutableStateFlow("Unavailable") }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channel = "doctor_availability"
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(channel, "Doctor availability", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 4103, Intent(this, MainActivity::class.java)
            .putExtra("navigateTo", "doctorAvailability"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        startForeground(4103, NotificationCompat.Builder(this, channel).setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Available for consultations").setContentText("Tap to manage availability")
            .setContentIntent(open).setOngoing(true).build())
        if (job?.isActive == true) return START_NOT_STICKY
        job = scope.launch {
            val owner = ConsultationRepository.userId()
            try {
                while (owner != null && owner == ConsultationRepository.userId()) {
                    val presence = withTimeout(10000) { ConsultationRepository.heartbeat() }
                    if (!presence.is_available) break
                    status.value = "Available for general consultations"
                    delay(45000)
                }
                status.value = "Unavailable"
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                status.value = "Connection lost. Enable availability again when connected."
            } finally {
                withContext(NonCancellable) {
                    if (owner != null && owner == ConsultationRepository.userId())
                        runCatching { withTimeout(5000) { ConsultationRepository.availability(false) } }
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        status.value = "Availability expired. Open the app to enable it again."
        stopSelf()
    }
    override fun onDestroy() {
        scope.cancel()
        if (status.value.startsWith("Available")) status.value = "Unavailable"
        super.onDestroy()
    }
}
