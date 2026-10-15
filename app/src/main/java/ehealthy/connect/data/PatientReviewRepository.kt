package ehealthy.connect.data

import ehealthy.connect.data.patient.PatientAppointment
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from

object PatientReviewRepository {

    private val supabase
        get() = SupabaseClientProvider.client

    suspend fun hasReviewed(
        appointmentId: String
    ): Result<Boolean> = runCatching {

        PatientRepository
            .getMyReviews()
            .getOrThrow()
            .any { item ->

                item.review.appointment_id ==
                        appointmentId
            }
    }

    suspend fun submitReview(
        appointment: PatientAppointment,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {

        require(rating in 1..5) {
            "Rating must be between 1 and 5."
        }

        // My Visits has already loaded this exact patient-owned appointment.
        // Re-fetching the entire appointment list on Submit could wrongly
        // report it missing even though it is visible as completed.
        val patientId = PatientRepository.getMyProfile().getOrThrow().id
        require(appointment.id.isNotBlank() && appointment.patient_id == patientId) {
            "This appointment is not linked to your patient account. Refresh My Visits."
        }

        check(appointment.status.equals("completed", ignoreCase = true)) {
            "Only completed appointments can be reviewed."
        }

        val doctorId = appointment.doctor_id
            ?.takeIf { it.isNotBlank() }
            ?: error("The doctor for this appointment could not be found.")
        val appointmentId = appointment.id

        val alreadyReviewed =
            hasReviewed(
                appointmentId
            )
                .getOrThrow()

        if (alreadyReviewed) {

            error(
                "You have already reviewed this appointment."
            )
        }

        // patientId above is patients.id (never auth.users.id).

        val cleanedComment =
            comment
                .trim()
                .takeIf {
                    it.isNotBlank()
                }

        supabase
            .from("reviews")
            .insert(
                mapOf(
                    "patient_id" to patientId,
                    "doctor_id" to doctorId,
                    "appointment_id" to appointmentId,
                    "rating" to rating,
                    "comment" to cleanedComment
                )
            )

        Unit
    }
}