
package ehealthy.connect.consultation

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import ehealthy.connect.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration.Companion.milliseconds

class DoctorAvailabilityService : Service() {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    private var job: Job? = null

    private var invitationJob: Job? = null
    companion object {
        val status = MutableStateFlow("Unavailable")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        val channel = "doctor_availability"

        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(
                        channel,
                        "Doctor availability",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }

        val open = PendingIntent.getActivity(
            this,
            4103,
            Intent(this, MainActivity::class.java)
                .putExtra("navigateTo", "doctorAvailability"),
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        startForeground(
            4103,
            NotificationCompat.Builder(this, channel)
                .setSmallIcon(
                    android.R.drawable.sym_call_incoming
                )
                .setContentTitle("Available for consultations")
                .setContentText("Monitoring doctor availability")
                .setContentIntent(open)
                .setOngoing(true)
                .build()
        )

        if (job?.isActive == true) {
            return START_STICKY
        }

        job = scope.launch {

            val owner = ConsultationRepository.userId()

            if (owner == null) {
                status.value = "Sign in to enable availability"
                stopSelf(startId)
                return@launch
            }

            var retryDelay = 5_000L

            while (
                isActive &&
                owner == ConsultationRepository.userId()
            ) {

                try {

                    // Refresh heartbeat on Supabase.
                    val presence = withTimeout(10_000L.milliseconds) {
                        ConsultationRepository.heartbeat()
                    }

                    // Respect the doctor's explicit OFF choice.
                    if (!presence.is_available) {
                        status.value = "Unavailable"
                        break
                    }

                    // Heartbeat succeeded.
                    status.value =
                        "Available for general consultations"

                    retryDelay = 5_000L

                    // Keep heartbeat fresh every 45 seconds.
                    delay(45_000L.milliseconds)

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    // Do not change availability to OFF.
                    Log.w(
                        "DoctorAvailability",
                        "Heartbeat failed. Retrying...",
                        e
                    )

                    status.value =
                        "Connection interrupted. Reconnecting..."

                    // Retry automatically.
                    delay(retryDelay.milliseconds)

                    // Gradually increase retry delay.
                    retryDelay = (retryDelay * 2)
                        .coerceAtMost(30_000L)
                }
            }

            stopSelf(startId)
        }


        // BACKUP: Check Supabase for incoming consultations.
        // This works even when an FCM push is not delivered.
        if (invitationJob?.isActive != true) {

            invitationJob = scope.launch {

                val owner = ConsultationRepository.userId()
                    ?: return@launch

                val shownInvitations =
                    mutableMapOf<String, Int>()

                while (
                    isActive &&
                    owner == ConsultationRepository.userId()
                ) {

                    try {

                        // Only check while our heartbeat is active.
                        if (
                            status.value.startsWith("Available")
                        ) {

                            val recipients = withTimeout(
                                8_000L.milliseconds
                            ) {
                                ConsultationRepository
                                    .invitations()
                                    .filter {
                                        it.status == "pending"
                                    }
                            }

                            val activeIds =
                                mutableSetOf<String>()

                            for (recipient in recipients) {

                                val request = withTimeout(
                                    8_000L.milliseconds
                                ) {
                                    ConsultationRepository.request(
                                        recipient.request_id
                                    )
                                }

                                if (
                                    request != null &&
                                    request.status == "pending"
                                ) {

                                    activeIds.add(request.id)

                                    // Avoid repeatedly displaying
                                    // the same invitation version.
                                    if (
                                        shownInvitations[request.id] !=
                                        request.version
                                    ) {

                                        ConsultationNotifications.show(
                                            this@DoctorAvailabilityService,
                                            owner,
                                            request
                                        )

                                        shownInvitations[request.id] =
                                            request.version
                                    }
                                }
                            }

                            // Stop ringing when another doctor
                            // accepts or the request is cancelled.
                            val finishedIds =
                                shownInvitations.keys
                                    .filter {
                                        it !in activeIds
                                    }

                            for (id in finishedIds) {

                                val version =
                                    shownInvitations.remove(id) ?: 1

                                ConsultationNotifications.close(
                                    this@DoctorAvailabilityService,
                                    owner,
                                    id,
                                    version
                                )
                            }
                        }

                    } catch (e: CancellationException) {

                        throw e

                    } catch (e: Exception) {

                        Log.w(
                            "DoctorAvailability",
                            "Backup invitation check failed",
                            e
                        )
                    }

                    // Check for new calls every 5 seconds.
                    delay(5_000L.milliseconds)
                }
            }
        }

        return START_STICKY

    }

    override fun onTimeout(
        startId: Int,
        fgsType: Int
    ) {
        status.value =
            "Android stopped background availability. " +
                    "Open the app to reconnect."

        stopSelf(startId)
    }

    override fun onDestroy() {

        scope.cancel()

        status.value =
            "Heartbeat service stopped"

        super.onDestroy()
    }
}
