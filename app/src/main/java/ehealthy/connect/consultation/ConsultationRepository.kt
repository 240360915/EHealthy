package ehealthy.connect.consultation

import ehealthy.connect.data.ProfileRepository
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ConsultationRow(
    val id: String,
    val patient_id: String,
    val mode: String,
    val service: String,
    val status: String,
    val price_minor: Int,
    val currency: String,
    val version: Int,
    val expires_at: String,
    val claimed_doctor_id: String? = null
)

@Serializable
data class ConsultationRecipient(
    val request_id: String,
    val doctor_id: String,
    val status: String
)

@Serializable
data class CreateResult(
    val request_id: String,
    val request_status: String,
    val request_mode: String,
    val recipient_count: Int,
    val amount_minor: Int,
    val payment_currency: String,
    val expires_at: String,
    val request_version: Int
)

@Serializable
data class ClaimResult(
    val request_id: String,
    val claim_result: String,
    val request_version: Int,
    val claimed_doctor_id: String? = null,
    val stream_call_id: String? = null
)

@Serializable
data class RequestResult(
    val request_id: String,
    val request_status: String,
    val request_version: Int
)

@Serializable
data class DoctorPresence(
    val doctor_id: String,
    val is_available: Boolean,
    val last_heartbeat: String? = null,
    val service_scope: List<String> = listOf("general")
)

@Serializable
data class ConsultationSession(
    val request_id: String,
    val stream_call_id: String,
    val status: String
)

object ConsultationRepository {

    private val db
        get() =
            SupabaseClientProvider
                .client
                .postgrest

    fun userId(): String? {

        return SupabaseClientProvider
            .client
            .auth
            .currentUserOrNull()
            ?.id
    }

    suspend fun privateDoctor(
        id: String
    ) =
        db
            .from("doctors")
            .select {

                filter {

                    eq(
                        "id",
                        id
                    )

                    eq(
                        "verification_status",
                        "approved"
                    )
                }
            }
            .decodeSingleOrNull<
                    ehealthy.connect.ui.patientDashboard.DoctorProfile
                    >()

    suspend fun register(
        installation: String,
        token: String
    ) {

        db.rpc(
            "register_device_token",
            buildJsonObject {

                put(
                    "p_installation_id",
                    installation
                )

                put(
                    "p_token",
                    token
                )

                put(
                    "p_platform",
                    "android"
                )
            }
        )
    }

    suspend fun unregister(
        installation: String
    ) {

        db.rpc(
            "unregister_device_token",
            buildJsonObject {

                put(
                    "p_installation_id",
                    installation
                )
            }
        )
    }

    /*
     * Turns the doctor's explicit opt-in availability
     * on or off on the SERVER.
     *
     * The Android client never writes doctor_presence directly.
     */
    suspend fun availability(
        available: Boolean
    ): DoctorPresence {

        return db
            .rpc(
                "set_doctor_availability",
                buildJsonObject {

                    put(
                        "p_is_available",
                        available
                    )

                    put(
                        "p_service_scope",
                        buildJsonArray {

                            add(
                                JsonPrimitive(
                                    "general"
                                )
                            )
                        }
                    )
                }
            )
            .decodeList<DoctorPresence>()
            .single()
    }

    /*
     * NEW:
     *
     * Reads the doctor's current availability directly
     * from the secure server RPC.
     *
     * This lets DoctorAvailabilityScreen show SERVER truth
     * instead of trusting DoctorAvailabilityService.status.
     */
    suspend fun presence(): DoctorPresence? {

        return db
            .rpc(
                "get_my_doctor_presence"
            )
            .decodeList<DoctorPresence>()
            .singleOrNull()
    }

    /*
     * Keeps an available doctor fresh.
     *
     * The foreground service calls this repeatedly.
     */
    suspend fun heartbeat(): DoctorPresence {

        return db
            .rpc(
                "doctor_heartbeat"
            )
            .decodeList<DoctorPresence>()
            .single()
    }

    suspend fun create(
        mode: String,
        doctorId: String?,
        key: String
    ): CreateResult {

        require(
            mode == "private" ||
                    mode == "broadcast"
        )

        require(
            (
                    mode == "private"
                    ) ==
                    !doctorId.isNullOrBlank()
        )

        return db
            .rpc(
                "create_consultation_request",
                buildJsonObject {

                    put(
                        "p_mode",
                        mode
                    )

                    put(
                        "p_target_doctor_id",
                        doctorId
                            ?.let {
                                JsonPrimitive(
                                    it
                                )
                            }
                            ?: JsonNull
                    )

                    put(
                        "p_service",
                        "general"
                    )

                    put(
                        "p_idempotency_key",
                        key
                    )
                }
            )
            .decodeList<CreateResult>()
            .single()
    }

    suspend fun claim(
        id: String,
        key: String
    ): ClaimResult {

        return db
            .rpc(
                "claim_consultation_request",
                buildJsonObject {

                    put(
                        "p_request_id",
                        id
                    )

                    put(
                        "p_idempotency_key",
                        key
                    )
                }
            )
            .decodeList<ClaimResult>()
            .single()
    }

    suspend fun decline(
        id: String
    ): RequestResult {

        return db
            .rpc(
                "decline_consultation_request",
                buildJsonObject {

                    put(
                        "p_request_id",
                        id
                    )
                }
            )
            .decodeList<RequestResult>()
            .single()
    }

    suspend fun cancel(
        id: String
    ): RequestResult {

        return db
            .rpc(
                "cancel_consultation_request",
                buildJsonObject {

                    put(
                        "p_request_id",
                        id
                    )
                }
            )
            .decodeList<RequestResult>()
            .single()
    }

    suspend fun request(
        id: String
    ): ConsultationRow? {

        return db
            .from(
                "consultation_requests"
            )
            .select {

                filter {

                    eq(
                        "id",
                        id
                    )
                }
            }
            .decodeSingleOrNull<
                    ConsultationRow
                    >()
    }

    suspend fun recipient(
        id: String
    ): ConsultationRecipient? {

        val doctorId =
            ProfileRepository
                .doctorId()

        return db
            .from(
                "consultation_recipients"
            )
            .select {

                filter {

                    eq(
                        "request_id",
                        id
                    )

                    eq(
                        "doctor_id",
                        doctorId
                    )
                }
            }
            .decodeSingleOrNull<
                    ConsultationRecipient
                    >()
    }

    suspend fun patientRequests():
            List<ConsultationRow> {

        val patientId =
            ProfileRepository
                .patientId()

        return db
            .from(
                "consultation_requests"
            )
            .select {

                filter {

                    eq(
                        "patient_id",
                        patientId
                    )
                }
            }
            .decodeList<
                    ConsultationRow
                    >()
            .sortedByDescending {
                it.expires_at
            }
            .take(
                20
            )
    }

    suspend fun invitations():
            List<ConsultationRecipient> {

        val doctorId =
            ProfileRepository
                .doctorId()

        return db
            .from(
                "consultation_recipients"
            )
            .select {

                filter {

                    eq(
                        "doctor_id",
                        doctorId
                    )

                    isIn(
                        "status",
                        listOf(
                            "pending",
                            "claimed"
                        )
                    )
                }
            }
            .decodeList<
                    ConsultationRecipient
                    >()
    }

    suspend fun session(
        id: String
    ): ConsultationSession? {

        return db
            .from(
                "consultation_sessions"
            )
            .select {

                filter {

                    eq(
                        "request_id",
                        id
                    )
                }
            }
            .decodeSingleOrNull<
                    ConsultationSession
                    >()
    }
}

fun consultationError(
    error: Throwable
): String {

    val message =
        error.message
            .orEmpty()

    return when {

        message.contains(
            "No standard broadcast consultation price",
            ignoreCase = true
        ) ->

            "Broadcast is not available yet: the standard price has not been configured."

        message.contains(
            "already has an active",
            ignoreCase = true
        ) ->

            "You already have an active request. Refresh to view it."

        message.contains(
            "already occupied",
            ignoreCase = true
        ) ->

            "You are already assigned to another consultation."

        message.contains(
            "no longer available",
            ignoreCase = true
        ) ->

            "You are no longer available. Check availability and connection."

        message.contains(
            "Only approved",
            ignoreCase = true
        ) ->

            "Only approved doctors can enable availability."

        message.contains(
            "claimed consultation cannot",
            ignoreCase = true
        ) ->

            "This request has been accepted and cannot be cancelled here."

        else ->

            "The request could not be completed. Check your connection and retry."
    }
}
/** A losing broadcast response must never show the accepted handoff. */
fun isWinningConsultationClaim(result: ClaimResult, doctorId: String): Boolean =
    result.claim_result == "claimed" && result.claimed_doctor_id == doctorId
