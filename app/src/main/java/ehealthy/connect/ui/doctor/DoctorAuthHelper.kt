package ehealthy.connect.ui.doctor

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Email/password auth for doctors, plus a Google sign-in shortcut for
 * doctors who already registered (see GoogleAuthHelper.kt — same
 * signInWithGoogle() the patient flow uses).
 */

data class DoctorRegistrationInfo(
    val practiceNumber: String,
    val practiceName: String,
    val hpcsaNumber: String,
    val discipline: String,
    val qualifications: String,
    val operatingHours: String,
    val consultationFee: String,     // -> "hourly_rate" (numeric column, parsed below)
    val medicalAidSchemes: String,   // -> "medical_aids"
    val name: String,
    val surname: String,
    val idNumber: String,
    val cellNumber: String,          // -> "phone"
    val email: String,
    val languagesSpoken: String,     // -> "language"
    val gender: String,
    val password: String
)

suspend fun signInDoctorWithEmail(email: String, password: String): Result<Unit> {
    return try {
        SupabaseClientProvider.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(friendlyAuthError(e)))
    }
}

/**
 * Creates the auth user, then writes the doctor's profile row.
 *
 * The row is built as a JsonObject (not a Map<String, Any?>) because the
 * insert mixes types — mostly strings, but hourly_rate is a Double — and
 * kotlinx.
 * serialization can't derive a serializer for a raw "Any" map.
 * buildJsonObject + put() handles heterogeneous types correctly.
 *
 * verification_status defaults to "pending", matching how the web app
 * flags new sign-ups for admin review before activation.
 *
 * This also assumes "Confirm email" is OFF in your Supabase Auth settings,
 * so a session exists immediately after sign-up. If email confirmation is
 * required instead, there's no signed-in user yet at this point, so the
 * profile row can't be inserted here — the result comes back as a failure
 * with a "check your email" message rather than a real error in that case.
 */
suspend fun registerDoctor(info: DoctorRegistrationInfo): Result<Unit> {
    return try {
        SupabaseClientProvider.client.auth.signUpWith(Email) {
            email = info.email
            password = info.password
        }

        val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
            ?: return Result.failure(
                Exception("Account created — please confirm your email, then log in.")
            )

        val hourlyRate = info.consultationFee.trim().toDoubleOrNull()

        val row = buildJsonObject {
            put("practice_number", info.practiceNumber)
            put("practice_name", info.practiceName)
            put("hpcsa_number", info.hpcsaNumber)
            put("discipline", info.discipline)
            put("qualifications", info.qualifications)
            put("operating_hours", info.operatingHours)
            put("medical_aids", info.medicalAidSchemes)
            put("name", info.name)
            put("surname", info.surname)
            put("id_number", info.idNumber)
            put("phone", info.cellNumber)
            put("email", info.email)
            put("language", info.languagesSpoken)
            put("gender", info.gender)
            put("user_id", userId)
            put("verification_status", "pending")
            if (hourlyRate != null) put("hourly_rate", hourlyRate)
        }

        SupabaseClientProvider.client.postgrest.from("doctors").insert(row)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(friendlyAuthError(e)))
    }
}

/** Step 1 of password reset: emails the doctor a 6-digit recovery code. */
suspend fun sendDoctorPasswordResetEmail(email: String): Result<Unit> {
    return try {
        SupabaseClientProvider.client.auth.resetPasswordForEmail(email = email)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(friendlyAuthError(e)))
    }
}

/**
 * Step 2: verifies the code from that email, then sets the new password.
 */
suspend fun verifyDoctorResetCodeAndSetPassword(
    email: String,
    code: String,
    newPassword: String
): Result<Unit> {
    return try {
        SupabaseClientProvider.client.auth.verifyEmailOtp(
            type = OtpType.Email.RECOVERY,
            email = email,
            token = code
        )
        SupabaseClientProvider.client.auth.updateUser {
            password = newPassword
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(friendlyAuthError(e)))
    }
}

private fun friendlyAuthError(e: Throwable): String {
    val raw = e.message ?: return "Something went wrong. Please try again."
    val firstLine = raw.substringBefore("\nURL:").trim()
    return when {
        firstLine.contains("invalid_credentials", ignoreCase = true) ->
            "Incorrect email or password."

        firstLine.contains("email_not_confirmed", ignoreCase = true) ->
            "Please confirm your email before logging in."

        firstLine.contains("user_not_found", ignoreCase = true) ->
            "No account found with that email."

        firstLine.contains("user_already_exists", ignoreCase = true) ->
            "An account with that email already exists."

        firstLine.contains("doctors_practice_number_key", ignoreCase = true) ->
            "That practice number is already registered. Please double-check it or use a different one."

        firstLine.contains("doctors_hpcsa_number_key", ignoreCase = true) ->
            "That HPCSA number is already registered."

        firstLine.contains("doctors_id_number_key", ignoreCase = true) ->
            "That ID number is already registered."

        firstLine.contains("doctors_email_key", ignoreCase = true) ->
            "That email is already registered as a doctor."

        firstLine.contains("duplicate key value violates unique constraint", ignoreCase = true) ->
            "Some of these details are already registered to another doctor account."

        else -> firstLine.substringBefore("(").trim()
            .ifBlank { "Something went wrong. Please try again." }
    }
}