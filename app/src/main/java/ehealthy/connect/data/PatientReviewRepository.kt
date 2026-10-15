package ehealthy.connect.data

import ehealthy.connect.data.patient.PatientAppointment
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

/**
 * Visit reviews use the appointment which My Visits already loaded for
 * the signed-in patient. Never search a second, broader appointment list.
 *
 * The database enforces ownership, completed status and one review per visit.
 */
@Serializable
internal data class VisitReviewInsert(
    val patient_id: String,
    val doctor_id: String,
    val appointment_id: String,
    val rating: Int,
    val comment: String?
)

object PatientReviewRepository {

    private val supabase
        get() = SupabaseClientProvider.client

    @Serializable
    private data class ReviewId(val id: String)

    suspend fun hasReviewed(appointment: PatientAppointment): Result<Boolean> = runCatching {
        val patientId = appointment.patient_id
            ?.takeIf { it.isNotBlank() }
            ?: error("This visit does not have a patient record. Refresh My Visits.")

        require(appointment.id.isNotBlank()) {
            "This visit does not have an appointment ID. Refresh My Visits."
        }

        // Query only this visit's reviews; do not reload the patient profile,
        // all reviews, or unrelated doctor profiles just to check duplicates.
        supabase.from("reviews")
            .select(columns = Columns.list("id")) {
                filter {
                    eq("patient_id", patientId)
                    eq("appointment_id", appointment.id)
                }
            }
            .decodeList<ReviewId>()
            .isNotEmpty()
    }

    suspend fun submitReview(
        appointment: PatientAppointment,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {
        require(rating in 1..5) { "Please choose a rating between 1 and 5." }

        val appointmentId = appointment.id.takeIf { it.isNotBlank() }
            ?: error("This visit does not have an appointment ID. Refresh My Visits.")
        val patientId = appointment.patient_id?.takeIf { it.isNotBlank() }
            ?: error("This visit does not have a patient record. Refresh My Visits.")
        val doctorId = appointment.doctor_id?.takeIf { it.isNotBlank() }
            ?: error("This visit does not have a doctor record. Refresh My Visits.")

        check(appointment.status.equals("completed", ignoreCase = true)) {
            "Only completed appointments can be reviewed."
        }

        check(!hasReviewed(appointment).getOrThrow()) {
            "You have already reviewed this appointment."
        }

        // Server-side RLS independently verifies the active user's patient
        // profile, appointment ownership, the doctor and completed status.
        // A @Serializable request with concrete field types is required here.
        // mapOf(String, String, String, Int, String?) infers Map<String, Any?>,
        // which fails before the POST with "Serializer for class 'Any' is not found".
        supabase.from("reviews").insert(
            VisitReviewInsert(
                patient_id = patientId,
                doctor_id = doctorId,
                appointment_id = appointmentId,
                rating = rating,
                comment = comment.trim().takeIf { it.isNotBlank() }
            )
        )
        Unit
    }
}
