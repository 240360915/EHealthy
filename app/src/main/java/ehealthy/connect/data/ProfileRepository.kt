package ehealthy.connect.data

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileIdentity(val id: String)

@Serializable
private data class DoctorStartupProfile(
    val id: String,
    @SerialName("verification_status") val verificationStatus: String? = null
)

/** Authentication IDs identify accounts; relational IDs identify profiles. Never substitute one. */
object ProfileRepository {
    suspend fun patientId(): String = requireProfile("patients")
    suspend fun doctorId(): String = requireProfile("doctors")

    private suspend fun requireProfile(table: String): String {
        val authId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            ?: error("Please sign in again.")
        return find(table, authId) ?: error("Your profile is missing or inaccessible. Please complete registration or contact support.")
    }

    private suspend fun find(table: String, authId: String): String? =
        SupabaseClientProvider.client.postgrest.from(table)
            .select(columns = Columns.list("id")) { filter { eq("user_id", authId) } }
            .decodeSingleOrNull<ProfileIdentity>()?.id

    private suspend fun findDoctor(authId: String): DoctorStartupProfile? =
        SupabaseClientProvider.client.postgrest.from("doctors")
            .select(columns = Columns.list("id", "verification_status")) {
                filter { eq("user_id", authId) }
            }
            .decodeSingleOrNull<DoctorStartupProfile>()

    suspend fun doctorVerificationStatus(): String? {
        val auth = SupabaseClientProvider.client.auth
        auth.awaitInitialization()
        val authId = auth.currentUserOrNull()?.id ?: return null
        return findDoctor(authId)?.verificationStatus
    }

    suspend fun doctorHomeRoute(): String {
        val auth = SupabaseClientProvider.client.auth
        auth.awaitInitialization()
        val authId = auth.currentUserOrNull()?.id ?: return "doctorLogin"
        val doctor = findDoctor(authId) ?: return "doctorRegister"
        return if (doctor.verificationStatus.equals("approved", ignoreCase = true)) {
            "doctorDashboard"
        } else {
            "doctorVerificationPending"
        }
    }

    suspend fun startupRoute(onboardingComplete: Boolean): String {
        val auth = SupabaseClientProvider.client.auth
        auth.awaitInitialization()
        val authId = auth.currentUserOrNull()?.id
            ?: return if (onboardingComplete) "choose" else "onboarding1"
        val patient = find("patients", authId)
        val doctor = findDoctor(authId)
        return when {
            patient != null && doctor != null -> "choose"
            doctor != null && doctor.verificationStatus.equals("approved", ignoreCase = true) ->
                "doctorDashboard"
            doctor != null -> "doctorVerificationPending"
            patient != null -> "patientDashboard"
            else -> "incompleteProfile"
        }
    }
}
