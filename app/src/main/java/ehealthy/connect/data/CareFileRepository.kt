package ehealthy.connect.data

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Existing patients.id is the patient's file ID. No duplicate patient table is created. */
@Serializable
data class DoctorCareFile(
    val appointment_id: String,
    val patient_id: String,
    val patient_name: String = "Patient",
    val visit_date: String? = null,
    val visit_time: String? = null,
    val visit_reason: String? = null,
    val visit_status: String? = null,
    val gender: String? = null,
    val date_of_birth: String? = null,
    val blood_group: String? = null,
    val allergies: String? = null,
    val chronic_conditions: String? = null,
    val current_medication: String? = null,
    val surgeries: String? = null,
    val current_symptoms: String? = null,
    val symptom_duration: String? = null,
    val symptom_severity: Int? = null,
    val consultation_summary: String? = null,
    val care_plan: String? = null,
    val note_updated_at: String? = null
)

@Serializable
data class PatientCareNote(
    val appointment_id: String,
    val visit_date: String? = null,
    val doctor_name: String? = null,
    val consultation_summary: String = "",
    val care_plan: String = "",
    val updated_at: String? = null
)

object CareFileRepository {
    private val client get() = SupabaseClientProvider.client

    /** Server checks that the authenticated doctor owns this confirmed/completed appointment. */
    suspend fun getDoctorCareFile(appointmentId: String): Result<DoctorCareFile> = try {
        require(appointmentId.isNotBlank()) { "Appointment ID is required." }
        val rows = client.postgrest.rpc(
            "doctor_get_patient_care_file",
            buildJsonObject { put("p_appointment_id", appointmentId) }
        ).decodeList<DoctorCareFile>()
        Result.success(rows.singleOrNull() ?: error("The patient file is unavailable or access is denied."))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    /** Doctor-authored content is explicitly patient-visible. One note per appointment. */
    suspend fun saveDoctorCareNote(
        appointmentId: String,
        summary: String,
        plan: String
    ): Result<Unit> = try {
        require(summary.trim().isNotBlank()) { "Please enter a consultation summary." }
        require(summary.length <= 5000 && plan.length <= 5000) { "Each field must be under 5000 characters." }
        client.postgrest.rpc(
            "doctor_save_patient_care_note",
            buildJsonObject {
                put("p_appointment_id", appointmentId)
                put("p_consultation_summary", summary.trim())
                put("p_care_plan", plan.trim())
            }
        )
        Result.success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }

    /** Server derives the patient ID from auth.uid(); the app cannot request another patient's notes. */
    suspend fun getMyCareNotes(): Result<List<PatientCareNote>> = try {
        val notes = client.postgrest.rpc("patient_get_my_care_notes")
            .decodeList<PatientCareNote>()
        Result.success(notes)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
