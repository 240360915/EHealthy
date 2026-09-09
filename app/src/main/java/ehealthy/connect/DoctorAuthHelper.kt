package ehealthy.connect

import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

/**
 * Email/password auth for doctors, plus a Google sign-in shortcut for
 * doctors who already registered (see GoogleAuthHelper.kt — same
 * signInWithGoogle() the patient flow uses).
 *
 * NOTE: signInWithGoogle() creates/uses a Supabase auth identity tied to
 * the Google account. If a doctor originally registered with email+password
 * and has never linked Google to that same Supabase user, Supabase will
 * treat the Google sign-in as a *different* user unless "automatic linking
 * by verified email" is enabled in your Supabase Auth settings. Worth
 * checking that setting so "log in with Google" actually resolves to the
 * doctor's existing row in "doctors" rather than a fresh, profile-less user.
 */

data class DoctorRegistrationInfo(
    val practiceNumber: String,
    val practiceName: String,
    val hpcsaNumber: String,
    val discipline: String,
    val qualifications: String,
    val operatingHours: String,
    val consultationFee: String,
    val medicalAidSchemes: String,
    val name: String,
    val surname: String,
    val idNumber: String,
    val cellNumber: String,
    val email: String,
    val languagesSpoken: String,
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
        Result.failure(e)
    }
}

/**
 * Creates the auth user, then writes the doctor's profile row.
 *
 * NOTE: the column names below are assumed — they mirror the "patients"
 * insert pattern elsewhere in this app. Adjust to match your actual
 * Supabase "doctors" table.
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

        SupabaseClientProvider.client.postgrest.from("doctors").insert(
            mapOf(
                "practice_number" to info.practiceNumber,
                "practice_name" to info.practiceName,
                "hpcsa_number" to info.hpcsaNumber,
                "discipline" to info.discipline,
                "qualifications" to info.qualifications,
                "operating_hours" to info.operatingHours,
                "consultation_fee" to info.consultationFee,
                "medical_aid_schemes" to info.medicalAidSchemes,
                "name" to info.name,
                "surname" to info.surname,
                "id_number" to info.idNumber,
                "cell_number" to info.cellNumber,
                "email" to info.email,
                "languages_spoken" to info.languagesSpoken,
                "gender" to info.gender,
                "user_id" to userId
            )
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/** Step 1 of password reset: emails the doctor a 6-digit recovery code. */
suspend fun sendDoctorPasswordResetEmail(email: String): Result<Unit> {
    return try {
        SupabaseClientProvider.client.auth.resetPasswordForEmail(email = email)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * Step 2: verifies the code from that email, then sets the new password.
 *
 * Uses the OTP-code recovery flow rather than the magic-link flow, because
 * the app has no deep link / intent-filter set up to catch a link redirect.
 * For the code to actually arrive by email, the "Reset password" email
 * template in the Supabase dashboard needs to include {{ .Token }} (the
 * 6-digit code) — not just {{ .ConfirmationURL }}, which is the default.
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
        Result.failure(e)
    }
}