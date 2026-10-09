package ehealthy.connect.data


import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

/**
 * Returns only appointments having an actual official prescription record.
 * Depends on the nullable prescriptions.appointment_id SQL migration.
 * RLS remains the source of truth for patient access.
 */
object PatientPrescriptionLinkRepository {

    @Serializable
    private data class PrescriptionLinkRow(val appointment_id: String? = null)

    suspend fun getMyLinkedAppointmentIds(): Result<Set<String>> = runCatching {
        val patientId = PatientRepository.getMyProfile().getOrThrow().id

        SupabaseClientProvider.client
            .from("prescriptions")
            .select(columns = Columns.list("appointment_id")) {
                filter {
                    eq("patient_id", patientId)
                    eq("is_official", true)
                }
            }
            .decodeList<PrescriptionLinkRow>()
            .mapNotNull { it.appointment_id?.takeIf(String::isNotBlank) }
            .toSet()
    }
}
