package ehealthy.connect.data

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
        appointmentId: String,
        rating: Int,
        comment: String
    ): Result<Unit> = runCatching {

        require(
            rating in 1..5
        ) {
            "Rating must be between 1 and 5."
        }

        val appointment =
            PatientRepository
                .getMyAppointments()
                .getOrThrow()
                .firstOrNull {
                    it.id == appointmentId
                }
                ?: error(
                    "Appointment not found."
                )

        if (
            !appointment.status.equals(
                "completed",
                ignoreCase = true
            )
        ) {

            error(
                "Only completed appointments can be reviewed."
            )
        }

        val doctorId =
            appointment.doctor_id
                ?: error(
                    "The doctor for this appointment could not be found."
                )

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

        /*
         * IMPORTANT:
         *
         * reviews.patient_id uses patients.id,
         * NOT auth.users.id.
         */
        val patientId =
            PatientRepository
                .getMyProfile()
                .getOrThrow()
                .id

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