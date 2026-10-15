package ehealthy.connect.ui.call

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The Stream room is created securely by Supabase using the same
 *  consultation type/membership flow as working broadcasts.
 */
@Serializable
data class ScheduledStreamCredentials(
    val token: String,
    val userId: String,
    val appointmentId: String,
    val callType: String,
    val callId: String
) {
    fun validate(expectedAppointmentId: String, expectedUserId: String) {
        require(appointmentId == expectedAppointmentId) {
            "The server returned another appointment."
        }
        require(userId == expectedUserId) {
            "The video identity does not match the signed-in account."
        }
        require(callType == "consultation") {
            "Unexpected video room type."
        }
        require(callId == "scheduled-$expectedAppointmentId") {
            "Unexpected video room."
        }
        require(token.isNotBlank()) { "The secure video token is missing." }
    }
}

@Serializable
private data class ScheduledRoomRequest(val appointmentId: String)

private val scheduledRoomClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

object ScheduledStreamRepository {
    suspend fun getCredentials(
        appointmentId: String
    ): Result<ScheduledStreamCredentials> {
        return try {
            require(appointmentId.isNotBlank()) { "Appointment ID is required." }
            val accessToken = SupabaseClientProvider.client.auth.currentAccessTokenOrNull()
                ?: error("Please sign in again.")
            val expectedUserId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                ?: error("Patient or doctor identity is missing. Sign in again.")

            val result = scheduledRoomClient.post(
                "https://gqyhkccupbeudenvsdsf.supabase.co/functions/v1/scheduled-stream-call"
            ) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(ScheduledRoomRequest(appointmentId))
            }
            if (!result.status.isSuccess()) {
                val code = result.status.value
                val details = result.bodyAsText()
                throw IllegalStateException(
                    when (code) {
                        401 -> "Your session expired. Please sign in again."
                        402 -> "Finish DemoPay before opening the scheduled call."
                        403 -> "You are not authorized to join this doctor's appointment."
                        404 -> "The scheduled appointment was not found."
                        409 -> "This appointment has ended or is no longer confirmed."
                        else -> "Unable to prepare secure video ($code). $details"
                    }
                )
            }
            val credentials = result.body<ScheduledStreamCredentials>()
            credentials.validate(appointmentId, expectedUserId)
            Result.success(credentials)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
