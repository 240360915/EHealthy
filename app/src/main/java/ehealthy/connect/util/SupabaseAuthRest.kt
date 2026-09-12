package ehealthy.connect.util

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
    private const val ANON_KEY =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdxeWhrY2N1cGJldWRlbnZzZHNmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU1MDgyMjgsImV4cCI6MjA5MTA4NDIyOH0.C6dtGH1277KKiMBpXtWSxRY9JQrfbbo7eYKIgomoap8"

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