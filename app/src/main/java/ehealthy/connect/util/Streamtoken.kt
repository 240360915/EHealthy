package ehealthy.connect.util

import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class StreamTokenResponse(val token: String, val userId: String)

private val streamHttpClient = HttpClient {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

/**
 * Calls the stream-token Edge Function using the current Supabase session,
 * returning a Stream Video auth token for the logged-in user.
 */
suspend fun fetchStreamToken(): Result<StreamTokenResponse> {
    return try {
        val accessToken = SupabaseClientProvider.client.auth.currentAccessTokenOrNull()
            ?: throw Exception("Not logged in")

        val httpResponse = streamHttpClient.post(
            "https://gqyhkccupbeudenvsdsf.supabase.co/functions/v1/stream-token"
        ) {
            header("Authorization", "Bearer $accessToken")
        }

        if (!httpResponse.status.isSuccess()) {
            val errorBody = httpResponse.bodyAsText()
            return Result.failure(Exception("Stream token request failed (${httpResponse.status.value}): $errorBody"))
        }

        Result.success(httpResponse.body<StreamTokenResponse>())
    } catch (e: Exception) {
        Result.failure(e)
    }
}