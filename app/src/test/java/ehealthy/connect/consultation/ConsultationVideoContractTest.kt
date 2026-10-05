package ehealthy.connect.consultation

import org.junit.Assert.*
import org.junit.Test

class ConsultationVideoContractTest {
    private val credentials = ConsultationStreamCredentials(
        "token", "patient-auth", "consultation-request", "consultation", "request", "public-key"
    )

    @Test fun acceptsServerConsultationIdentity() {
        credentials.validate("request", "patient-auth")
    }

    @Test fun rejectsAppointmentFallbackAndAccountOrRequestMismatch() {
        for (invalid in listOf(
            credentials.copy(callType = "default"),
            credentials.copy(callId = "appointment-id"),
            credentials.copy(requestId = "another-request"),
            credentials.copy(userId = "another-auth-user"),
            credentials.copy(token = ""),
            credentials.copy(apiKey = "")
        )) {
            assertTrue(runCatching { invalid.validate("request", "patient-auth") }.isFailure)
        }
    }

    @Test fun losingClaimIsNeverAccepted() {
        assertFalse(isWinningConsultationClaim(ClaimResult("request", "already_claimed", 2), "doctor"))
        assertFalse(isWinningConsultationClaim(ClaimResult("request", "claimed", 2, "other"), "doctor"))
        assertTrue(isWinningConsultationClaim(ClaimResult("request", "claimed", 2, "doctor"), "doctor"))
    }
}
