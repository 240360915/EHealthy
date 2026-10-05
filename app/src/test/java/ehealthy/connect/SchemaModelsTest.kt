package ehealthy.connect

import ehealthy.connect.ui.patientDashboard.DoctorProfile
import ehealthy.connect.ui.patientDashboard.FindDoctors.DoctorListing
import ehealthy.connect.data.PublishedSlot
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SchemaModelsTest {
    private val json = Json { ignoreUnknownKeys = true }
    @Test fun doctorUsesActualSchemaNamesAndIgnoresNumericLegacyLocation() {
        val doctor = json.decodeFromString<DoctorProfile>("""{"id":"profile-id","user_id":"different-auth-id",
            "qualifications":"MBChB","language":"English","city":"Mbombela","province":"Mpumalanga","location":-25.47}""")
        assertEquals("MBChB", doctor.qualification)
        assertEquals("English", doctor.languages)
        assertEquals("Mbombela, Mpumalanga", doctor.location)
        assertNotEquals(doctor.id, doctor.user_id)
    }
    @Test fun listingUsesCityAndProvinceAndRetainsDeactivation() {
        val doctor = json.decodeFromString<DoctorListing>("""{"id":"d","name":"A","surname":"B",
            "city":"Mbombela","province":null,"is_deactivated":true,"location":123.0}""")
        assertEquals("Mbombela", doctor.location)
        assertEquals(true, doctor.is_deactivated)
    }
    @Test fun publishedSlotRetainsIdentityAndBookingState() {
        val slot = json.decodeFromString<PublishedSlot>("""{"id":"slot-id","doctor_id":"profile-id",
            "date":"2027-01-02","time":"09:30:00","is_booked":true}""")
        assertEquals("profile-id", slot.doctor_id)
        assertTrue(slot.is_booked)
    }
}
