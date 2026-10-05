package ehealthy.connect.consultation

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.util.UUID

object ConsultationNotifications {
    val changes = MutableStateFlow(0)
    private const val CHANNEL = "consultation_invitations"
    private fun prefs(c: Context) = c.getSharedPreferences("consultation_invitations", Context.MODE_PRIVATE)
    private fun key(owner: String, id: String) = "$owner/$id"
    @Synchronized fun state(c: Context, owner: String, id: String): InvitationState {
        val p = prefs(c); val k = key(owner, id)
        return InvitationState(p.getInt("$k/version", 0), p.getBoolean("$k/closed", false))
    }
    @Synchronized fun close(c: Context, owner: String, id: String, version: Int) {
        val old = state(c, owner, id)
        val next = InvitationRules.merge(old, version, true)
        prefs(c).edit().putInt("${key(owner,id)}/version", next.version)
            .putBoolean("${key(owner,id)}/closed", next.closed).commit()
        if (next.closed) c.getSystemService(NotificationManager::class.java).cancel(id, 4101)
        changes.value++
    }
    fun clearNotifications(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.id == 4101 }.forEach { manager.cancel(it.tag, it.id) }
        changes.value++
    }
    @Synchronized fun show(c: Context, owner: String, row: ConsultationRow) {
        if (ConsultationRepository.userId() != owner ||
            !InvitationRules.mayRing(state(c, owner, row.id), row.version, row.expires_at, System.currentTimeMillis())) return
        if (!NotificationManagerCompat.from(c).areNotificationsEnabled()) return
        val manager = c.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Consultation invitations", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE),
                    android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build())
            })
        val intent = Intent(c, IncomingConsultationActivity::class.java)
            .setData(Uri.parse("ehealthy://consultation/${row.id}"))
            .putExtra("request_id", row.id).putExtra("owner", owner)
        val pending = PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.sym_call_incoming).setContentTitle("Incoming consultation")
            .setContentText("Open to accept or decline").setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_CALL).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_MAX).setOnlyAlertOnce(true).setOngoing(true)
            .setTimeoutAfter((Instant.parse(row.expires_at).toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(1))
        if (Build.VERSION.SDK_INT < 34 || manager.canUseFullScreenIntent()) builder.setFullScreenIntent(pending, true)
        prefs(c).edit().putInt("${key(owner,row.id)}/version", row.version).commit()
        manager.notify(row.id, 4101, builder.build().apply { flags = flags or Notification.FLAG_INSISTENT })
    }
    fun receive(c: Context, data: Map<String, String>, highPriority: Boolean) {
        val id = data["request_id"] ?: return
        val owner = data["recipient_user_id"] ?: return
        if (runCatching { UUID.fromString(id); UUID.fromString(owner) }.isFailure) return
        val version = data["version"]?.toIntOrNull()?.takeIf { it > 0 } ?: return
        if (data["type"] == "consultation_closed") { close(c, owner, id, version); return }
        val expires = data["expires_at"] ?: return
        if (!InvitationRules.mayRing(state(c, owner, id), version, expires, System.currentTimeMillis())) return
        val request = OneTimeWorkRequestBuilder<ConsultationInviteWorker>()
            .setInputData(workDataOf("id" to id, "owner" to owner, "version" to version, "expires" to expires))
        if (highPriority) request.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        WorkManager.getInstance(c).enqueueUniqueWork("consultation/$owner/$id", ExistingWorkPolicy.KEEP, request.build())
    }
}

class ConsultationInviteWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val channel = "consultation_checks"
        if (Build.VERSION.SDK_INT >= 26) applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(channel, "Checking invitations", NotificationManager.IMPORTANCE_LOW))
        return ForegroundInfo(4102, NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(android.R.drawable.sym_call_incoming).setContentTitle("Checking consultation invitation").build())
    }
    override suspend fun doWork(): Result {
        val id = inputData.getString("id") ?: return Result.failure()
        val owner = inputData.getString("owner") ?: return Result.failure()
        val expires = inputData.getString("expires") ?: return Result.failure()
        var version = inputData.getInt("version", 1)
        var shown = false
        try {
            withTimeout(8000) { SupabaseClientProvider.client.auth.awaitInitialization() }
            while (ConsultationRepository.userId() == owner &&
                InvitationRules.mayRing(ConsultationNotifications.state(applicationContext, owner, id), version, expires, System.currentTimeMillis())) {
                val row = withTimeout(8000) { ConsultationRepository.request(id) } ?: break
                version = maxOf(version, row.version)
                val recipient = withTimeout(8000) { ConsultationRepository.recipient(id) }
                if (row.status != "pending" || recipient?.status != "pending") break
                if (!shown) { ConsultationNotifications.show(applicationContext, owner, row); shown = true }
                delay(2000)
            }
        } catch (_: TimeoutCancellationException) {
            // Once visible, stop the alert if server state cannot be verified.
            if (!shown && runAttemptCount < 2) return Result.retry()
        } catch (e: CancellationException) {
            applicationContext.getSystemService(NotificationManager::class.java).cancel(id, 4101)
            throw e
        } catch (_: Exception) {
            if (!shown && runAttemptCount < 2 &&
                InvitationRules.mayRing(ConsultationNotifications.state(applicationContext, owner, id), version, expires, System.currentTimeMillis())) return Result.retry()
        }
        ConsultationNotifications.close(applicationContext, owner, id, version)
        return Result.success()
    }
}
