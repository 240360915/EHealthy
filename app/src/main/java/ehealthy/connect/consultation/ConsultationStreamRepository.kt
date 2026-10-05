package ehealthy.connect.consultation

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


@Serializable
data class ConsultationStreamCredentials(
    val token: String,
    val userId: String,
    val callId: String,
    val callType: String,
    val requestId: String
)


@Serializable
private data class ConsultationStreamRequest(
    val consultationRequestId: String
)


private val consultationStreamHttpClient =
    HttpClient {

        install(ContentNegotiation) {

            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }


private const val CONSULTATION_STREAM_URL =
    "https://gqyhkccupbeudenvsdsf.supabase.co/functions/v1/stream-token"


object ConsultationStreamRepository {

    suspend fun getCredentials(
        requestId: String
    ): Result<ConsultationStreamCredentials> {

        return try {

            require(
                requestId.isNotBlank()
            ) {
                "Consultation request ID is required."
            }


            val accessToken =
                SupabaseClientProvider
                    .client
                    .auth
                    .currentAccessTokenOrNull()
                    ?: throw IllegalStateException(
                        "Please sign in again."
                    )


            val response =
                consultationStreamHttpClient
                    .post(
                        CONSULTATION_STREAM_URL
                    ) {

                        header(
                            HttpHeaders.Authorization,
                            "Bearer $accessToken"
                        )

                        contentType(
                            ContentType.Application.Json
                        )

                        setBody(
                            ConsultationStreamRequest(
                                consultationRequestId =
                                    requestId
                            )
                        )
                    }


            if (
                !response.status
                    .isSuccess()
            ) {

                val errorBody =
                    response
                        .bodyAsText()

                throw Exception(
                    "Consultation Stream request failed " +
                            "(${response.status.value}): $errorBody"
                )
            }


            val credentials =
                response
                    .body<
                            ConsultationStreamCredentials
                            >()


            require(
                credentials.requestId ==
                        requestId
            ) {
                "The server returned a different consultation."
            }


            require(
                credentials.token
                    .isNotBlank()
            ) {
                "Missing Stream token."
            }


            require(
                credentials.userId
                    .isNotBlank()
            ) {
                "Missing Stream user ID."
            }


            require(
                credentials.callId
                    .isNotBlank()
            ) {
                "Missing consultation call ID."
            }


            require(
                credentials.callType ==
                        "consultation"
            ) {
                "Unexpected Stream call type."
            }


            Result.success(
                credentials
            )


        } catch (
            e: CancellationException
        ) {

            throw e

        } catch (
            e: Exception
        ) {

            Result.failure(
                e
            )
        }
    }
}