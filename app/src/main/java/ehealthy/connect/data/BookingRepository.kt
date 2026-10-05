package ehealthy.connect.data

import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class PublishedSlot(val id: String, val doctor_id: String, val date: String,
                         val time: String, val is_booked: Boolean = false)

object BookingRepository {
    const val BACKEND_REQUIRED = "Booking is a prototype. No reservation or payment was made. Secure booking must be enabled on the server first."

    suspend fun availableTimes(doctorId: String, date: String): Result<Set<String>> = try {
        val slots = SupabaseClientProvider.client.postgrest.from("time_slots").select {
            filter { eq("doctor_id", doctorId); eq("date", date); eq("is_booked", false) }
        }.decodeList<PublishedSlot>()
        Result.success(slots.map { it.time.take(5) }.toSortedSet())
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException("Availability could not be loaded. Try again; database access may need updating."))
    }
    // TODO: reserve_slot(slot_id, appointment_type, reason, idempotency_key) must atomically
    // create the appointment and claim the slot. Never fall back to client-side inserts.
}
