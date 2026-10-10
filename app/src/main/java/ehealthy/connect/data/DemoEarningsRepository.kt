package ehealthy.connect.data

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Demo-only ledger; amounts are in cents to avoid floating-point errors. */
@Serializable
data class DemoEarning(
    val appointment_id: String,
    val appointment_date: String? = null,
    val payment_reference: String,
    val total_minor: Int,
    val doctor_minor: Int,
    val platform_minor: Int,
    val commission_bps: Int,
    val released_at: String
)

@Serializable
data class DemoReadyConsultation(
    val appointment_id: String,
    val patient_name: String? = null,
    val appointment_date: String,
    val appointment_time: String,
    val total_minor: Int
)

@Serializable
data class DemoReleaseResult(
    val appointment_id: String,
    val appointment_status: String,
    val total_minor: Int,
    val doctor_minor: Int,
    val platform_minor: Int,
    val commission_bps: Int
)

object DemoEarningsRepository {
    private val db get() = SupabaseClientProvider.client.postgrest

    suspend fun getMyEarnings(): Result<List<DemoEarning>> = safeRequest {
        db.rpc("doctor_get_my_demo_earnings")
            .decodeList<DemoEarning>()
    }

    suspend fun getReadyConsultations(): Result<List<DemoReadyConsultation>> = safeRequest {
        db.rpc("doctor_get_my_demo_ready_for_release")
            .decodeList<DemoReadyConsultation>()
    }

    /** The server validates doctor ownership, confirmed status and joined call.
     * It credits an appointment only once; NO real payout occurs.
     */
    suspend fun completeOnlineAppointment(appointmentId: String): Result<DemoReleaseResult> = safeRequest {
        require(appointmentId.isNotBlank()) { "Missing appointment ID." }
        db.rpc(
            "doctor_complete_online_demo_appointment",
            buildJsonObject { put("p_appointment_id", appointmentId) }
        ).decodeList<DemoReleaseResult>().firstOrNull()
            ?: error("The server returned no completion result.")
    }

    private suspend fun <T> safeRequest(action: suspend () -> T): Result<T> = try {
        Result.success(action())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}
