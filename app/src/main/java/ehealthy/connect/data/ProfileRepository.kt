package ehealthy.connect.data

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileIdentity(val id: String)

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

    suspend fun startupRoute(): String {
        val auth = SupabaseClientProvider.client.auth
        auth.awaitInitialization()
        val authId = auth.currentUserOrNull()?.id ?: return "onboarding1"
        val patient = find("patients", authId)
        val doctor = find("doctors", authId)
        return when {
            patient != null && doctor != null -> "choose"
            doctor != null -> "doctorDashboard"
            patient != null -> "patientDashboard"
            else -> "choose"
        }
    }
}
