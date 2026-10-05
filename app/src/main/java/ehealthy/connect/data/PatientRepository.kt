package ehealthy.connect.data.patient

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * All patient-side database access should go through this repository.
 *
 * Architecture:
 *
 * auth.users.id
 *      ↓
 * patients.user_id
 *      ↓
 * patients.id
 *
 * Relationships such as:
 * appointments.patient_id
 * reviews.patient_id
 * prescriptions.patient_id
 *
 * must use patients.id, NOT auth.uid().
 *
 * Secure state-changing operations use the Supabase RPC functions
 * we created instead of direct table UPDATE/INSERT calls.
 */
object PatientRepository {

    private val supabase
        get() = SupabaseClientProvider.client

    // -------------------------------------------------------------------------
    // PATIENT PROFILE
    // -------------------------------------------------------------------------

    suspend fun getMyProfile(): Result<PatientProfile> = runCatching {

        supabase.auth.awaitInitialization()

        val userId = requireUserId()

        supabase
            .from("patients")
            .select(
                columns = Columns.list(
                    "id",
                    "user_id",
                    "name",
                    "surname",
                    "email",
                    "phone",
                    "profile_image_url"
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeSingleOrNull<PatientProfile>()
            ?: error("Patient profile was not found for this account.")
    }

    // -------------------------------------------------------------------------
    // APPOINTMENTS
    // -------------------------------------------------------------------------

    suspend fun getMyAppointments(): Result<List<PatientAppointment>> =
        runCatching {

            val patientId = requirePatientProfileId()

            supabase
                .from("appointments")
                .select(
                    columns = Columns.list(
                        "id",
                        "doctor_id",
                        "patient_id",
                        "patient_name",
                        "reason",
                        "date",
                        "time",
                        "status",
                        "appointment_type",
                        "slot_id",
                        "price_minor",
                        "currency",
                        "amount_paid",
                        "payment_method",
                        "created_at",
                        "rescheduled_at",
                        "completion_code",
                        "cancelled_reason",
                        "cancelled_at"
                    )
                ) {
                    filter {
                        eq("patient_id", patientId)
                    }
                }
                .decodeList<PatientAppointment>()
                .sortedWith(
                    compareBy<PatientAppointment> {
                        it.date ?: "9999-12-31"
                    }.thenBy {
                        it.time ?: "23:59:59"
                    }
                )
        }

    /**
     * Securely books a real published time slot.
     *
     * The database function:
     * - identifies the patient from auth.uid()
     * - locks the slot
     * - checks doctor availability
     * - gets the server-side price
     * - creates the appointment
     * - marks the slot booked
     */
    suspend fun reserveSlot(
        slotId: String,
        appointmentType: String,
        reason: String,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<ReserveSlotResult> = runCatching {

        require(
            appointmentType == "online" ||
                    appointmentType == "in_person"
        ) {
            "Appointment type must be online or in_person."
        }

        val parameters = buildJsonObject {

            put(
                "p_slot_id",
                slotId
            )

            put(
                "p_appointment_type",
                appointmentType
            )

            put(
                "p_reason",
                reason.trim()
            )

            put(
                "p_idempotency_key",
                idempotencyKey
            )
        }

        supabase.postgrest
            .rpc(
                function = "reserve_slot",
                parameters = parameters
            )
            .decodeList<ReserveSlotResult>()
            .firstOrNull()
            ?: error(
                "The booking server returned no appointment."
            )
    }

    /**
     * Patient cancellation must go through the protected database function.
     *
     * Do not directly UPDATE appointments from Android.
     */
    suspend fun cancelAppointment(
        appointmentId: String,
        reason: String,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<CancelAppointmentResult> = runCatching {

        val parameters = buildJsonObject {

            put(
                "p_appointment_id",
                appointmentId
            )

            put(
                "p_reason",
                reason.trim()
            )

            put(
                "p_idempotency_key",
                idempotencyKey
            )
        }

        supabase.postgrest
            .rpc(
                function = "cancel_appointment",
                parameters = parameters
            )
            .decodeList<CancelAppointmentResult>()
            .firstOrNull()
            ?: error(
                "The cancellation server returned no result."
            )
    }

    /**
     * Reschedules the appointment transactionally.
     *
     * Supabase releases the previous slot and claims the new slot together.
     */
    suspend fun rescheduleAppointment(
        appointmentId: String,
        newSlotId: String,
        reason: String,
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<RescheduleAppointmentResult> = runCatching {

        val parameters = buildJsonObject {

            put(
                "p_appointment_id",
                appointmentId
            )

            put(
                "p_new_slot_id",
                newSlotId
            )

            put(
                "p_reason",
                reason.trim()
            )

            put(
                "p_idempotency_key",
                idempotencyKey
            )
        }

        supabase.postgrest
            .rpc(
                function = "reschedule_appointment",
                parameters = parameters
            )
            .decodeList<RescheduleAppointmentResult>()
            .firstOrNull()
            ?: error(
                "The rescheduling server returned no result."
            )
    }


    /**
     * Securely requests the completion code for an in-person appointment.
     *
     * The database function:
     * - identifies the logged-in patient
     * - verifies appointment ownership
     * - checks that the appointment is confirmed
     * - checks that it is an in-person appointment
     * - generates or returns the existing completion code
     */
    suspend fun requestCompletionCode(
        appointmentId: String
    ): Result<CompletionCodeResult> = runCatching {

        require(appointmentId.isNotBlank()) {
            "Appointment ID is required."
        }

        val parameters = buildJsonObject {

            put(
                "p_appointment_id",
                appointmentId
            )
        }

        supabase.postgrest
            .rpc(
                function = "request_completion_code",
                parameters = parameters
            )
            .decodeList<CompletionCodeResult>()
            .firstOrNull()
            ?: error(
                "The server returned no completion code."
            )
    }

    // -------------------------------------------------------------------------
    // DOCTORS
    // -------------------------------------------------------------------------

    suspend fun getApprovedDoctors():
            Result<List<PatientDoctorSummary>> =
        runCatching {

            supabase
                .from("doctors")
                .select(
                    columns = Columns.list(
                        "id",
                        "name",
                        "surname",
                        "discipline",
                        "phone",
                        "hourly_rate",
                        "profile_image_url",
                        "operating_hours",
                        "city",
                        "province",
                        "qualifications",
                        "language",
                        "verification_status",
                        "is_deactivated"
                    )
                ) {
                    filter {

                        eq(
                            "verification_status",
                            "approved"
                        )

                        eq(
                            "is_deactivated",
                            false
                        )
                    }
                }
                .decodeList<PatientDoctorSummary>()
                .sortedWith(
                    compareBy<PatientDoctorSummary> {
                        it.discipline.orEmpty()
                    }
                        .thenBy {
                            it.name
                        }
                        .thenBy {
                            it.surname
                        }
                )
        }

    suspend fun getDoctor(
        doctorId: String
    ): Result<PatientDoctorSummary> =
        runCatching {

            supabase
                .from("doctors")
                .select(
                    columns = Columns.list(
                        "id",
                        "name",
                        "surname",
                        "discipline",
                        "phone",
                        "hourly_rate",
                        "profile_image_url",
                        "operating_hours",
                        "city",
                        "province",
                        "qualifications",
                        "language",
                        "verification_status",
                        "is_deactivated"
                    )
                ) {
                    filter {

                        eq(
                            "id",
                            doctorId
                        )

                        eq(
                            "verification_status",
                            "approved"
                        )

                        eq(
                            "is_deactivated",
                            false
                        )
                    }
                }
                .decodeSingleOrNull<PatientDoctorSummary>()
                ?: error(
                    "Doctor is not available."
                )
        }

    // -------------------------------------------------------------------------
    // DOCTOR AVAILABILITY
    // -------------------------------------------------------------------------

    /**
     * Reads real doctor-published time slots from Supabase.
     *
     * There are NO hard-coded appointment times here.
     */
    suspend fun getAvailableSlots(
        doctorId: String,
        date: String? = null
    ): Result<List<AvailableSlot>> =
        runCatching {

            supabase
                .from("time_slots")
                .select(
                    columns = Columns.list(
                        "id",
                        "doctor_id",
                        "date",
                        "time",
                        "is_booked"
                    )
                ) {
                    filter {

                        eq(
                            "doctor_id",
                            doctorId
                        )

                        eq(
                            "is_booked",
                            false
                        )

                        if (!date.isNullOrBlank()) {

                            eq(
                                "date",
                                date
                            )
                        }
                    }
                }
                .decodeList<AvailableSlot>()
                .sortedWith(
                    compareBy<AvailableSlot> {
                        it.date
                    }.thenBy {
                        it.time
                    }
                )
        }

    // -------------------------------------------------------------------------
    // REVIEWS
    // -------------------------------------------------------------------------

    suspend fun getMyReviews():
            Result<List<PatientReviewDisplay>> =
        runCatching {

            val patientId =
                requirePatientProfileId()

            val reviews = supabase
                .from("reviews")
                .select(
                    columns = Columns.list(
                        "id",
                        "patient_id",
                        "doctor_id",
                        "appointment_id",
                        "rating",
                        "comment",
                        "created_at"
                    )
                ) {
                    filter {

                        eq(
                            "patient_id",
                            patientId
                        )
                    }
                }
                .decodeList<PatientReview>()

            val doctorIds =
                reviews
                    .map {
                        it.doctor_id
                    }
                    .distinct()

            val doctorNames =
                if (doctorIds.isEmpty()) {

                    emptyMap()

                } else {

                    supabase
                        .from("doctors")
                        .select(
                            columns = Columns.list(
                                "id",
                                "name",
                                "surname"
                            )
                        ) {
                            filter {

                                isIn(
                                    "id",
                                    doctorIds
                                )
                            }
                        }
                        .decodeList<DoctorName>()
                        .associate { doctor ->

                            doctor.id to
                                    "Dr. ${
                                        doctor.name.orEmpty()
                                    } ${
                                        doctor.surname.orEmpty()
                                    }".trim()
                        }
                }

            reviews
                .sortedByDescending {
                    it.created_at
                }
                .map { review ->

                    PatientReviewDisplay(
                        review = review,
                        doctorName =
                            doctorNames[
                                review.doctor_id
                            ] ?: "Doctor"
                    )
                }
        }

    // -------------------------------------------------------------------------
    // ON-DEMAND CONSULTATIONS
    // -------------------------------------------------------------------------

    /**
     * Creates:
     *
     * PRIVATE:
     * Rings one selected available doctor.
     * Price comes from that doctor's hourly_rate.
     *
     * BROADCAST:
     * Rings every eligible available doctor.
     * First doctor whose database claim succeeds wins.
     *
     * Broadcast requires an enabled standard tariff in
     * consultation_prices.
     */
    suspend fun createConsultationRequest(
        mode: ConsultationMode,
        targetDoctorId: String? = null,
        service: String = "general",
        idempotencyKey: String =
            UUID.randomUUID().toString()
    ): Result<ConsultationRequestResult> =
        runCatching {

            if (
                mode == ConsultationMode.PRIVATE
            ) {

                require(
                    !targetDoctorId.isNullOrBlank()
                ) {
                    "Select a doctor for a private consultation."
                }
            }

            val parameters =
                buildJsonObject {

                    put(
                        "p_mode",
                        mode.databaseValue
                    )

                    if (
                        mode ==
                        ConsultationMode.PRIVATE &&
                        !targetDoctorId.isNullOrBlank()
                    ) {

                        put(
                            "p_target_doctor_id",
                            targetDoctorId
                        )

                    } else {

                        put(
                            "p_target_doctor_id",
                            JsonNull
                        )
                    }

                    put(
                        "p_service",
                        service
                            .trim()
                            .ifBlank {
                                "general"
                            }
                    )

                    put(
                        "p_idempotency_key",
                        idempotencyKey
                    )
                }

            supabase.postgrest
                .rpc(
                    function =
                        "create_consultation_request",
                    parameters =
                        parameters
                )
                .decodeList<ConsultationRequestResult>()
                .firstOrNull()
                ?: error(
                    "The consultation server returned no request."
                )
        }

    suspend fun cancelConsultationRequest(
        requestId: String
    ): Result<ConsultationCancelResult> =
        runCatching {

            val parameters =
                buildJsonObject {

                    put(
                        "p_request_id",
                        requestId
                    )
                }

            supabase.postgrest
                .rpc(
                    function =
                        "cancel_consultation_request",
                    parameters =
                        parameters
                )
                .decodeList<ConsultationCancelResult>()
                .firstOrNull()
                ?: error(
                    "The consultation server returned no cancellation result."
                )
        }

    suspend fun getMyConsultationRequests():
            Result<List<ConsultationRequestRow>> =
        runCatching {

            val patientId =
                requirePatientProfileId()

            supabase
                .from("consultation_requests")
                .select(
                    columns = Columns.list(
                        "id",
                        "patient_id",
                        "mode",
                        "service",
                        "target_doctor_id",
                        "claimed_doctor_id",
                        "status",
                        "price_minor",
                        "currency",
                        "version",
                        "created_at",
                        "expires_at",
                        "claimed_at",
                        "cancelled_at"
                    )
                ) {
                    filter {

                        eq(
                            "patient_id",
                            patientId
                        )
                    }
                }
                .decodeList<ConsultationRequestRow>()
                .sortedByDescending {
                    it.created_at
                }
        }

    // -------------------------------------------------------------------------
    // PATIENT AVATAR
    // -------------------------------------------------------------------------

    suspend fun uploadAvatar(
        bytes: ByteArray
    ): Result<String> =
        runCatching {

            supabase.auth.awaitInitialization()

            val userId =
                requireUserId()

            /*
             * Matches the Storage policy we configured:
             *
             * patient-avatars/{auth.uid()}/avatar.jpg
             */
            val path =
                "$userId/avatar.jpg"

            supabase.storage
                .from("patient-avatars")
                .upload(
                    path,
                    bytes
                ) {
                    upsert = true
                }

            val publicUrl =
                supabase.storage
                    .from("patient-avatars")
                    .publicUrl(path)

            /*
             * RLS checks patients.user_id = auth.uid().
             */
            supabase
                .from("patients")
                .update(
                    {
                        set(
                            "profile_image_url",
                            publicUrl
                        )
                    }
                ) {
                    filter {

                        eq(
                            "user_id",
                            userId
                        )
                    }
                }

            publicUrl
        }

    // -------------------------------------------------------------------------
    // AUTH
    // -------------------------------------------------------------------------

    suspend fun signOut():
            Result<Unit> =
        runCatching {

            supabase.auth.signOut()
        }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    private suspend fun requirePatientProfileId():
            String {

        return getMyProfile()
            .getOrThrow()
            .id
    }

    private fun requireUserId():
            String {

        return supabase.auth
            .currentSessionOrNull()
            ?.user
            ?.id
            ?: error(
                "Please sign in again."
            )
    }
}

// =============================================================================
// DATA MODELS
// =============================================================================

@Serializable
data class PatientProfile(

    val id: String,

    val user_id: String? = null,

    val name: String? = null,

    val surname: String? = null,

    val email: String? = null,

    val phone: String? = null,

    val profile_image_url: String? = null
) {

    val displayName: String
        get() =
            listOfNotNull(
                name?.takeIf {
                    it.isNotBlank()
                },
                surname?.takeIf {
                    it.isNotBlank()
                }
            )
                .joinToString(" ")
                .ifBlank {
                    "Patient"
                }
}

@Serializable
data class PatientAppointment(

    val id: String,

    val doctor_id: String? = null,

    val patient_id: String? = null,

    val patient_name: String? = null,

    val reason: String? = null,

    val date: String? = null,

    val time: String? = null,

    val status: String? = null,

    val appointment_type: String =
        "in_person",

    val slot_id: String? = null,

    val price_minor: Int? = null,

    val currency: String? = "ZAR",

    val amount_paid: Double? = null,

    val payment_method: String? = null,

    val created_at: String? = null,

    val rescheduled_at: String? = null,

    val completion_code: String? = null,

    val cancelled_reason: String? = null,

    val cancelled_at: String? = null
)

@Serializable
data class PatientReview(

    val id: String,

    val patient_id: String,

    val doctor_id: String,

    val appointment_id: String? = null,

    val rating: Int,

    val comment: String? = null,

    val created_at: String
)

data class PatientReviewDisplay(

    val review: PatientReview,

    val doctorName: String
)

@Serializable
private data class DoctorName(

    val id: String,

    val name: String? = null,

    val surname: String? = null
)

@Serializable
data class PatientDoctorSummary(

    val id: String,

    val name: String,

    val surname: String,

    val discipline: String? = null,

    val phone: String? = null,

    val hourly_rate: Double? = null,

    val profile_image_url: String? = null,

    val operating_hours: String? = null,

    val city: String? = null,

    val province: String? = null,

    val qualifications: String? = null,

    val language: String? = null,

    val verification_status: String? = null,

    val is_deactivated: Boolean? = false
) {

    val fullName: String
        get() =
            "Dr. $name $surname"
                .trim()

    val displayLocation: String
        get() =
            listOfNotNull(
                city?.takeIf {
                    it.isNotBlank()
                },
                province?.takeIf {
                    it.isNotBlank()
                }
            )
                .joinToString(", ")
                .ifBlank {
                    "Location not provided"
                }
}

@Serializable
data class AvailableSlot(

    val id: String,

    val doctor_id: String,

    val date: String,

    val time: String,

    val is_booked: Boolean = false
)

// =============================================================================
// BOOKING RPC RESPONSES
// =============================================================================

@Serializable
data class ReserveSlotResult(

    val appointment_id: String,

    val reserved_slot_id: String,

    val reserved_doctor_id: String,

    val appointment_date: String,

    val appointment_time: String,

    val appointment_status: String,

    val amount_minor: Int,

    val payment_currency: String
)

@Serializable
data class CancelAppointmentResult(

    val appointment_id: String,

    val appointment_status: String,

    val released_slot_id: String? = null,

    val payment_action_required: Boolean
)

@Serializable
data class RescheduleAppointmentResult(

    val appointment_id: String,

    val new_slot_id: String,

    val doctor_id: String,

    val appointment_date: String,

    val appointment_time: String,

    val appointment_status: String
)

@Serializable
data class CompletionCodeResult(

    val appointment_id: String,

    val completion_code: String,

    val requested_at: String
)
// =============================================================================
// CONSULTATION MODE
// =============================================================================

enum class ConsultationMode(
    val databaseValue: String
) {

    PRIVATE(
        "private"
    ),

    BROADCAST(
        "broadcast"
    )
}

// =============================================================================
// CONSULTATION RPC RESPONSES
// =============================================================================

@Serializable
data class ConsultationRequestResult(

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
data class ConsultationCancelResult(

    val request_id: String,

    val request_status: String,

    val request_version: Int
)

@Serializable
data class ConsultationRequestRow(

    val id: String,

    val patient_id: String,

    val mode: String,

    val service: String,

    val target_doctor_id: String? = null,

    val claimed_doctor_id: String? = null,

    val status: String,

    val price_minor: Int,

    val currency: String,

    val version: Int,

    val created_at: String,

    val expires_at: String,

    val claimed_at: String? = null,

    val cancelled_at: String? = null
)