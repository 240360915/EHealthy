package ehealthy.connect.consultation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.google.firebase.messaging.FirebaseMessaging
import ehealthy.connect.util.SupabaseClientProvider
import ehealthy.connect.util.saveFcmToken
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

suspend fun firebaseToken(): String = suspendCancellableCoroutine { continuation ->
    FirebaseMessaging.getInstance().token.addOnCompleteListener {
        if (continuation.isActive) {
            if (it.isSuccessful) continuation.resume(it.result)
            else continuation.resumeWithException(it.exception ?: IllegalStateException("Token unavailable"))
        }
    }
}

@SuppressLint("StaticFieldLeak")
object ConsultationDevices {
    lateinit var context: Context
    private val mutex = Mutex()
    private var signingOut = false
    private fun installation(): String {
        val p = context.getSharedPreferences("consultation_device", Context.MODE_PRIVATE)
        return p.getString("installation", null) ?: UUID.randomUUID().toString().also {
            check(p.edit().putString("installation", it).commit())
        }
    }
    fun sync() {
        WorkManager.getInstance(context).enqueueUniqueWork("consultation-device", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<DeviceTokenWorker>().setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build())
    }
    suspend fun register() = mutex.withLock {

        if (signingOut) return@withLock

        val owner =
            ConsultationRepository.userId()
                ?: return@withLock

        val token =
            withTimeout(10000.milliseconds) {
                firebaseToken()
            }

        if (
            ConsultationRepository.userId() != owner
        ) {
            return@withLock
        }

        withTimeout(10000.milliseconds) {
            ConsultationRepository.register(
                installation(),
                token
            )
        }

        /*
         * TEMPORARILY DISABLED FOR TESTING.
         *
         * We are checking whether the old profile-token
         * registration is causing the consultation_id error.
         */
        // saveFcmToken(token)
    }
    @SuppressLint("StaticFieldLeak")
    suspend fun signOut() = mutex.withLock {
        signingOut = true
        try {
            context.stopService(Intent(context, DoctorAvailabilityService::class.java))
            runCatching { withTimeout(5000.milliseconds) { ConsultationRepository.availability(false) } }
            // Rotate the Firebase token even if unregister cannot reach the server.
            // Old queued messages are also rejected by recipient_user_id on this phone.
            runCatching { withTimeout(5000.milliseconds) { ConsultationRepository.unregister(installation()) } }
            runCatching { withTimeout(5000.milliseconds) { suspendCancellableCoroutine<Unit> { c ->
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                    if (c.isActive) { if (it.isSuccessful) c.resume(Unit) else c.resumeWithException(it.exception ?: Exception("Token reset failed")) }
                }
            } } }
            SupabaseClientProvider.client.auth.signOut()
            ConsultationNotifications.clearNotifications(context)
        } finally { signingOut = false }
    }
}

class DeviceTokenWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result = try {
        SupabaseClientProvider.client.auth.awaitInitialization()
        ConsultationDevices.register()
        Result.success()
    } catch (_: TimeoutCancellationException) { if (runAttemptCount < 5) Result.retry() else Result.failure()
    } catch (e: CancellationException) { throw e
    } catch (_: Exception) { if (runAttemptCount < 5) Result.retry() else Result.failure() }
}

class EHealthyApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        ConsultationDevices.context = applicationContext
        scope.launch {
            SupabaseClientProvider.client.auth.sessionStatus.collectLatest {
                if (ConsultationRepository.userId() != null) ConsultationDevices.sync()
                else {
                    stopService(Intent(this@EHealthyApplication, DoctorAvailabilityService::class.java))
                    ConsultationNotifications.clearNotifications(this@EHealthyApplication)
                }
            }
        }
    }
}
