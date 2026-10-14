package ehealthy.connect.consultation

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

/**
 * On-demand video contract returned by Supabase's stream-token Edge Function.
 * The backend returns five fields; Stream's public API key is configured in
 * the app and must not be expected in the credentials response.
 */
class ConsultationVideoContractTest {
    private val credentials = ConsultationStreamCredentials(
        token = "token",
        userId = "patient-auth",
        callId = "consultation-request",
        callType = "consultation",
        requestId = "request"
    )

    @Test
    fun acceptsServerConsultationIdentity() {
        credentials.validate(expectedRequestId = "request", expectedUserId = "patient-auth")
    }

    @Test
    fun decodesTheFiveFieldsReturnedByStreamToken() {
        val json = """
            {
              "token": "token",
              "userId": "patient-auth",
              "callId": "consultation-request",
              "callType": "consultation",
              "requestId": "request"
            }
        """.trimIndent()

        val decoded = Json.decodeFromString<ConsultationStreamCredentials>(json)
        decoded.validate(expectedRequestId = "request", expectedUserId = "patient-auth")
        assertEquals(credentials, decoded)
    }

    @Test
    fun rejectsAppointmentFallbackAndAccountOrRequestMismatch() {
        val invalidCredentials = listOf(
            credentials.copy(callType = "default"),
            credentials.copy(callId = "appointment-id"),
            credentials.copy(callId = "consultation-another-request"),
            credentials.copy(callId = ""),
            credentials.copy(requestId = "another-request"),
            credentials.copy(userId = "another-auth-user"),
            credentials.copy(userId = ""),
            credentials.copy(token = "")
        )

        for (invalid in invalidCredentials) {
            assertTrue(
                "Expected to reject invalid consultation credentials: $invalid",
                runCatching {
                    invalid.validate(expectedRequestId = "request", expectedUserId = "patient-auth")
                }.isFailure
            )
        }
    }

    @Test
    fun rejectsMissingOrMismatchedExpectedIdentity() {
        assertTrue(runCatching { credentials.validate("", "patient-auth") }.isFailure)
        assertTrue(runCatching { credentials.validate("request", "") }.isFailure)
        assertTrue(runCatching { credentials.validate("request", "another-auth-user") }.isFailure)
    }

    @Test
    fun losingClaimIsNeverAccepted() {
        assertFalse(isWinningConsultationClaim(ClaimResult("request", "already_claimed", 2), "doctor"))
        assertFalse(isWinningConsultationClaim(ClaimResult("request", "claimed", 2, "other"), "doctor"))
        assertTrue(isWinningConsultationClaim(ClaimResult("request", "claimed", 2, "doctor"), "doctor"))
    }
}
