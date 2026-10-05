package ehealthy.connect.consultation

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class InvitationRulesTest {
    private val expiry = "2026-09-30T12:00:00Z"
    private val before = Instant.parse(expiry).toEpochMilli() - 1
    @Test fun closeBeforeDelayedInvitationNeverRings() {
        val state = InvitationRules.merge(InvitationState(), 2, true)
        assertFalse(InvitationRules.mayRing(state, 1, expiry, before))
    }
    @Test fun terminalStateSurvivesDuplicateAndNewerInvitation() {
        val closed = InvitationRules.merge(InvitationState(1), 2, true)
        assertTrue(InvitationRules.merge(closed, 3, false).closed)
        assertEquals(closed, InvitationRules.merge(closed, 1, false))
    }
    @Test fun localDeclineCanCloseAtSameVersion() {
        assertTrue(InvitationRules.merge(InvitationState(1), 1, true).closed)
    }
    @Test fun staleCloseCannotCloseNewerState() {
        assertEquals(InvitationState(3), InvitationRules.merge(InvitationState(3), 2, true))
    }
    @Test fun expiryBoundaryAndMalformedExpiryFailClosed() {
        assertTrue(InvitationRules.mayRing(InvitationState(), 1, expiry, before))
        assertFalse(InvitationRules.mayRing(InvitationState(), 1, expiry, before + 1))
        assertFalse(InvitationRules.mayRing(InvitationState(), 1, "invalid", before))
    }
}
