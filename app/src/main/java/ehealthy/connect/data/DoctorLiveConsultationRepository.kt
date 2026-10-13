package ehealthy.connect.data

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
data class DoctorLiveConsultationRequest(
    val request_id: String,
    val patient_id: String,
    val patient_name: String = "Patient",
    val request_mode: String,
    val service: String,
    val price_minor: Int = 0,
    val currency: String = "ZAR",
    val request_status: String,
    val recipient_status: String,
    val created_at: String,
    val expires_at: String,
    val symptom_notes: String? = null,
    val urgency: String = "routine"
)

@Serializable
data class DoctorConsultationClaimResult(
    val request_id: String,
    val claim_result: String,
    val claimed_doctor_id: String? = null,
    val request_status: String? = null,
    val request_version: Int? = null
)

@Serializable
data class DoctorConsultationDeclineResult(
    val request_id: String,
    val result: String,
    val request_status: String? = null,
    val request_version: Int? = null
)

object DoctorLiveConsultationRepository {

    private val supabase =
        SupabaseClientProvider.client

    suspend fun getLiveRequests():
            Result<List<DoctorLiveConsultationRequest>> {

        return try {

            // Do not invoke this authenticated RPC with an anonymous/expired
            // session during app startup or after an account switch.
            supabase.auth.awaitInitialization()
            check(supabase.auth.currentUserOrNull() != null) {
                "Doctor sign-in is required to receive consultations."
            }

            val requests =
                supabase
                    .postgrest
                    .rpc(
                        "doctor_get_triaged_consultation_requests"
                    )
                    .decodeList<DoctorLiveConsultationRequest>()

            Result.success(requests)

        } catch (error: kotlinx.coroutines.CancellationException) {

            throw error

        } catch (error: Exception) {

            Result.failure(error)
        }
    }


    suspend fun acceptRequest(
        requestId: String
    ): Result<DoctorConsultationClaimResult> =
        runCatching {

            val parameters =
                buildJsonObject {

                    put(
                        "p_request_id",
                        requestId
                    )

                    put(
                        "p_idempotency_key",
                        UUID.randomUUID().toString()
                    )
                }


            supabase
                .postgrest
                .rpc(
                    function =
                        "claim_consultation_request",
                    parameters =
                        parameters
                )
                .decodeList<DoctorConsultationClaimResult>()
                .firstOrNull()
                ?: error(
                    "The consultation could not be claimed."
                )
        }


    suspend fun declineRequest(
        requestId: String
    ): Result<DoctorConsultationDeclineResult> =
        runCatching {

            val parameters =
                buildJsonObject {

                    put(
                        "p_request_id",
                        requestId
                    )
                }


            supabase
                .postgrest
                .rpc(
                    function =
                        "decline_consultation_request",
                    parameters =
                        parameters
                )
                .decodeList<DoctorConsultationDeclineResult>()
                .firstOrNull()
                ?: error(
                    "The consultation could not be declined."
                )
        }
    suspend fun sendHeartbeat(): Result<Unit> =
        runCatching {

            supabase
                .postgrest
                .rpc(
                    function = "doctor_heartbeat"
                )

            android.util.Log.d(
                "DoctorHeartbeat",
                "Heartbeat sent successfully"
            )

            Unit
        }.onFailure { error ->

            android.util.Log.e(
                "DoctorHeartbeat",
                "Heartbeat failed",
                error
            )
        }

    suspend fun endConsultation(
        requestId: String
    ): Result<Unit> =
        runCatching {

            val parameters =
                buildJsonObject {
                    put(
                        "p_request_id",
                        requestId
                    )
                }

            supabase
                .postgrest
                .rpc(
                    function = "end_consultation",
                    parameters = parameters
                )

            Unit
        }
}