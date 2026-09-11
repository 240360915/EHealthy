package ehealthy.connect

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object SupabaseAuthRest {
    private const val URL = "https://gqyhkccupbeudenvsdsf.supabase.co"
    private const val ANON_KEY = "YOUR_ANON_KEY"

    private val http = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    @Serializable
    data class PasswordLogin(val email: String, val password: String)
    @Serializable
    data class OtpRequest(val email: String)
    @Serializable
    data class OtpVerify(val email: String, val token: String, val type: String = "email")
    @Serializable
    data class AuthResult(val access_token: String? = null, val error: String? = null)

    suspend fun signInWithPassword(email: String, password: String): Result<AuthResult> =
        runCatching {
            http.post("$URL/auth/v1/token?grant_type=password") {
                header("apikey", ANON_KEY)
                contentType(ContentType.Application.Json)
                setBody(PasswordLogin(email, password))
            }.body()
        }

    suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        http.post("$URL/auth/v1/otp") {
            header("apikey", ANON_KEY)
            contentType(ContentType.Application.Json)
            setBody(OtpRequest(email))
        }
        Unit
    }

    suspend fun verifyOtp(email: String, token: String): Result<AuthResult> = runCatching {
        http.post("$URL/auth/v1/verify") {
            header("apikey", ANON_KEY)
            contentType(ContentType.Application.Json)
            setBody(OtpVerify(email, token))
        }.body()
    }
}