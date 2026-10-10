package ehealthy.connect.data

import ehealthy.connect.ui.patient.PatientRegistrationData
import ehealthy.connect.ui.common.isValidEmail
import ehealthy.connect.ui.common.normalizeEmail
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

object RegistrationRepository {
    suspend fun registerPatient(data: PatientRegistrationData) {
        val auth = SupabaseClientProvider.client.auth
        val email = normalizeEmail(data.email)
        require(isValidEmail(email)) { "Please enter a valid email address." }
        val existingSession = auth.currentUserOrNull()
        if (existingSession == null) {
            auth.signUpWith(Email) { this.email = email; password = data.password }
        } else {
            check(existingSession.email.equals(email, ignoreCase = true)) {
                "Sign out before registering a different email address."
            }
        }
        val user = auth.currentUserOrNull()
            ?: error("Check your email and confirm your account. Then sign in to complete your profile. You will need to re-enter your profile details.")
        check(user.email.equals(email, ignoreCase = true)) { "The signed-in email does not match this registration." }
        val userId = user.id
        val existing = SupabaseClientProvider.client.postgrest.from("patients")
            .select(columns = Columns.list("id")) { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<Map<String, String?>>()
        check(existing == null) { "This account already has a patient profile. Please sign in." }
        SupabaseClientProvider.client.postgrest.from("patients").insert(
            mapOf(
                "user_id" to userId,
                "name" to data.name,
                "surname" to data.surname,
                "title" to data.title,
                "date_of_birth" to data.dateOfBirth,
                "id_number" to data.idNumber,
                "phone" to data.phone,
                "languege" to data.language,
                "email" to email,
                "gender" to data.gender,
                "province" to data.province,
                "address1" to data.address1,
                "address2" to data.address2,
                "address3" to data.address3.ifBlank { null },
                "postal_code" to data.postalCode,
                "allergies" to data.allergies.ifBlank { null },
                "medication" to data.medication.ifBlank { null },
                "conditions" to data.conditions.ifBlank { null },
                "chronic" to data.chronic.ifBlank { null },
                "surgeries" to data.surgeries.ifBlank { null },
                "blood_group" to data.bloodGroup.ifBlank { null },
                "disability" to data.disability.ifBlank { null },
                "emergency_contact_name" to data.emergencyContactName.ifBlank { null },
                "emergency_contact_phone" to data.emergencyContactPhone.ifBlank { null },
                "emergency_contact_relationship" to data.emergencyContactRelationship.ifBlank { null }
            )
        )

    }
}
