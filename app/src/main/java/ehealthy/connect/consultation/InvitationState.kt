package ehealthy.connect.consultation

import java.time.Instant

data class InvitationState(val version: Int = 0, val closed: Boolean = false)
object InvitationRules {
    fun merge(old: InvitationState, version: Int, close: Boolean): InvitationState {
        if (version < old.version) return old
        return InvitationState(version, old.closed || close)
    }
    fun mayRing(state: InvitationState, version: Int, expiresAt: String, now: Long): Boolean =
        !state.closed && version >= state.version &&
            runCatching { Instant.parse(expiresAt).toEpochMilli() > now }.getOrDefault(false)
}
