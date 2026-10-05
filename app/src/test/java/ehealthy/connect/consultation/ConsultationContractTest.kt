package ehealthy.connect.consultation

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.Assert.*
import org.junit.Test

class ConsultationContractTest {
    private val json = Json { ignoreUnknownKeys = true }
    @Test fun claimLoserAllowsNullDoctorAndStreamFields() {
        val row = json.decodeFromString<ClaimResult>("""{"request_id":"request","claim_result":"already_claimed","claimed_doctor_id":null,"request_version":2,"stream_call_id":null}""")
        assertEquals("already_claimed", row.claim_result)
        assertNull(row.stream_call_id)
    }
    @Test fun creationDecodesServerPriceAndNoDoctorsState() {
        val row = json.decodeFromString<CreateResult>("""{"request_id":"request","request_status":"no_doctors","request_mode":"private","recipient_count":0,"amount_minor":23750,"payment_currency":"ZAR","expires_at":"2026-09-30T12:00:00+00:00","request_version":2}""")
        assertEquals("no_doctors", row.request_status)
        assertEquals("ZAR 237.50", consultationPrice(row.amount_minor, row.payment_currency))
    }
    @Test fun heartbeatDoesNotRequireScopeAbsentFromRpcResponse() {
        val row = json.decodeFromString<DoctorPresence>("""{"doctor_id":"doctor","is_available":false,"last_heartbeat":"2026-09-30T12:00:00+00:00"}""")
        assertFalse(row.is_available)
    }
    @Test fun declineAcceptsAdditionalResultField() {
        val row = json.decodeFromString<RequestResult>("""{"request_id":"request","result":"declined","request_status":"pending","request_version":1}""")
        assertEquals(1, row.request_version)
        assertEquals("pending", row.request_status)
    }
}
