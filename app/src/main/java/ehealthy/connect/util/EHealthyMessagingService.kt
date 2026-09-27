package ehealthy.connect.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ehealthy.connect.MainActivity
import ehealthy.connect.R
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EHealthyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save immediately when Firebase issues/rotates a token — don't wait
        // for the person to open the app again.
        CoroutineScope(Dispatchers.IO).launch {
            saveFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (message.data["type"] == "incoming_call") {
            showIncomingCall(message.data)
            return
        }

        val title = message.notification?.title ?: "eHealthy Connect"
        val body = message.notification?.body ?: "You have an update."
        showNotification(title, body)
    }

    private fun showIncomingCall(data: Map<String, String>) {
        val channelId = "incoming_calls"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Incoming appointment calls",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("appointmentId", data["appointmentId"])
            putExtra("role", data["role"])
            putExtra("otherPartyName", data["otherPartyName"])
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Appointment starting now")
            .setContentText(data["otherPartyName"] ?: "Your appointment is starting")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(9999, notification)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "appointments"
        val manager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Appointment reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // swap for your app icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

/**
 * Saves the current device's FCM token against the logged-in person's row —
 * whichever table they actually belong to (patient or doctor), since this
 * app serves both roles but shares one FirebaseMessagingService.
 * Safe to call repeatedly — no-ops if nobody is logged in yet, or if
 * somehow neither table has a matching row.
 */
suspend fun saveFcmToken(token: String) {
    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id ?: return
    try {
        val patientRow = SupabaseClientProvider.client.postgrest
            .from("patients")
            .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("id")) {
                filter { eq("user_id", userId) }
            }
            .decodeSingleOrNull<Map<String, String?>>()

        if (patientRow != null) {
            SupabaseClientProvider.client.postgrest.from("patients")
                .update({ set("fcm_token", token) }) {
                    filter { eq("user_id", userId) }
                }
            return
        }

        val doctorRow = SupabaseClientProvider.client.postgrest
            .from("doctors")
            .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("id")) {
                filter { eq("user_id", userId) }
            }
            .decodeSingleOrNull<Map<String, String?>>()

        if (doctorRow != null) {
            SupabaseClientProvider.client.postgrest.from("doctors")
                .update({ set("fcm_token", token) }) {
                    filter { eq("user_id", userId) }
                }
        }
    } catch (e: Exception) {
        android.util.Log.e("FCM", "Failed to save token", e)
    }
}

