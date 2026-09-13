package ehealthy.connect.ui.doctor

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

/** A file read from the device, ready to upload to Supabase Storage. */
data class DoctorFileUpload(
    val bytes: ByteArray,
    val extension: String,
    val contentType: String
)

/**
 * Every file collected on the Documents step of doctor registration.
 * Everything is nullable here — required-ness is enforced by
 * DoctorRegister.kt's step validation before onRegister ever fires.
 */
data class DoctorRegistrationFiles(
    val profilePhoto: DoctorFileUpload? = null,
    val idDocument: DoctorFileUpload? = null,
    val hpcsaCertificate: DoctorFileUpload? = null,
    val medicalDegree: DoctorFileUpload? = null,
    val specialistCertificate: DoctorFileUpload? = null,
    val practiceCertificate: DoctorFileUpload? = null,
    val proofOfAddress: DoctorFileUpload? = null
)

/**
 * Reads the bytes and mime type behind a content:// Uri (from a document or
 * photo picker) into a DoctorFileUpload. Returns null if it can't be read
 * rather than throwing, so a failed read on one field doesn't sink the
 * whole registration.
 */
suspend fun uriToDoctorFileUpload(context: Context, uri: Uri): DoctorFileUpload? =
    withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext null
            val contentType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val extension = MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(contentType) ?: "bin"
            DoctorFileUpload(bytes = bytes, extension = extension, contentType = contentType)
        } catch (e: Exception) {
            null
        }
    }

/**
 * Uploads one file to Supabase Storage under "{userId}/{fieldName}.{ext}".
 * Returns the public URL for the public profile-photo bucket, or the raw
 * storage path for the private documents bucket.
 */
private suspend fun uploadDoctorFile(
    bucket: String,
    userId: String,
    fieldName: String,
    file: DoctorFileUpload,
    public: Boolean
): String {
    val path = "$userId/$fieldName.${file.extension}"
    SupabaseClientProvider.client.storage.from(bucket).upload(path, file.bytes) {
        upsert = true
    }
    return if (public) {
        SupabaseClientProvider.client.storage.from(bucket).publicUrl(path)
    } else {
        path
    }
}

internal suspend fun uploadDoctorProfilePhoto(userId: String, file: DoctorFileUpload): String =
    uploadDoctorFile("profile-images", userId, "profile", file, public = true)

internal suspend fun uploadDoctorDocument(userId: String, fieldName: String, file: DoctorFileUpload): String =
    uploadDoctorFile("doctor-documents", userId, fieldName, file, public = false)

/**
 * Generates a temporary link (1 hour by default) to view a private document
 * — needed anywhere a doctor's uploaded documents get reviewed later, since
 * "doctor-documents" is private and what's stored in the doctors table is
 * a path, not a directly loadable URL.
 */
suspend fun getSignedDoctorDocumentUrl(path: String, expiresInSeconds: Int = 3600): Result<String> {
    return try {
        val url = SupabaseClientProvider.client.storage.from("doctor-documents")
            .createSignedUrl(path, expiresInSeconds.seconds)
        Result.success(url)
    } catch (e: Exception) {
        Result.failure(e)
    }
}