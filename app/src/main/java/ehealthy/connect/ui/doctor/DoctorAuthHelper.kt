package ehealthy.connect.ui.doctor

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class DoctorRegistrationInfo(
    val practiceNumber: String,
    val practiceName: String,
    val hpcsaNumber: String,
    val discipline: String,
    val qualifications: String,
    val operatingHours: String,
    val consultationFee: String,     // -> "hourly_rate" (numeric column, parsed below)
    val medicalAidSchemes: String,   // -> "medical_aids"
    val title: String,               // NOTE: no matching column, not persisted
    val name: String,
    val surname: String,
    val idNumber: String,
    val cellNumber: String,          // -> "phone"
    val email: String,
    val languagesSpoken: String,     // -> "language"
    val gender: String,
    val province: String,
    val city: String,
    val addressLine1: String,
    val addressLine2: String,
    val addressLine3: String,
    val postalCode: String,
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
 * Creates the auth user, uploads any provided documents/photo to Storage,
 * then writes the doctor's profile row with everything — including the
 * resulting file URLs/paths — in one insert.
 *
 * If a file upload fails partway through, the exception propagates and the
 * doctors row never gets inserted — files already uploaded before the
 * failure stay in Storage, orphaned under that user's folder. Harmless,
 * but worth knowing if storage usage looks odd while testing.
 *
 * "title" is collected in the UI but intentionally left out of this insert
 * — the "doctors" table has no matching column yet.
 */
suspend fun registerDoctor(
    info: DoctorRegistrationInfo,
    files: DoctorRegistrationFiles = DoctorRegistrationFiles()
): Result<Unit> {
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

        val profilePhotoUrl = files.profilePhoto?.let { uploadDoctorProfilePhoto(userId, it) }
        val idDocumentPath = files.idDocument?.let { uploadDoctorDocument(userId, "id_document", it) }
        val hpcsaCertPath = files.hpcsaCertificate?.let { uploadDoctorDocument(userId, "hpcsa_certificate", it) }
        val medicalDegreePath = files.medicalDegree?.let { uploadDoctorDocument(userId, "medical_degree", it) }
        val specialistCertPath = files.specialistCertificate?.let { uploadDoctorDocument(userId, "specialist_certificate", it) }
        val practiceCertPath = files.practiceCertificate?.let { uploadDoctorDocument(userId, "practice_certificate", it) }
        val proofOfAddressPath = files.proofOfAddress?.let { uploadDoctorDocument(userId, "proof_of_address", it) }

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
            put("province", info.province)
            put("city", info.city)
            put("address_line1", info.addressLine1)
            put("address_line2", info.addressLine2)
            put("address_line3", info.addressLine3)
            put("postal_code", info.postalCode)
            put("user_id", userId)
            put("verification_status", "pending")
            if (hourlyRate != null) put("hourly_rate", hourlyRate)
            if (profilePhotoUrl != null) put("profile_image_url", profilePhotoUrl)
            if (idDocumentPath != null) put("id_document_url", idDocumentPath)
            if (hpcsaCertPath != null) put("hpcsa_certificate_url", hpcsaCertPath)
            if (medicalDegreePath != null) put("medical_degree_url", medicalDegreePath)
            if (specialistCertPath != null) put("specialist_certificate_url", specialistCertPath)
            if (practiceCertPath != null) put("practice_certificate_url", practiceCertPath)
            if (proofOfAddressPath != null) put("proof_of_address_url", proofOfAddressPath)
        }

        SupabaseClientProvider.client.postgrest.from("doctors").insert(row)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(friendlyAuthError(e)))
    }
}

suspend fun sendDoctorPasswordResetEmail(email: String): Result<Unit> {
    return try {
        SupabaseClientProvider.client.auth.resetPasswordForEmail(email = email)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(friendlyAuthError(e)))
    }
}

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