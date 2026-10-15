package ehealthy.connect.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: a Map<String, Any?> failed before the PostgREST insert. */
class VisitReviewSerializationTest {
    @Test
    fun payloadSerializesEveryFieldWithoutAnySerializerLookup() {
        val json = Json.encodeToString(
            VisitReviewInsert(
                patient_id = "patient-profile-id",
                doctor_id = "doctor-id",
                appointment_id = "completed-visit-id",
                rating = 5,
                comment = "Excellent consultation"
            )
        )

        val row = Json.parseToJsonElement(json).jsonObject
        assertEquals("patient-profile-id", row["patient_id"]!!.jsonPrimitive.content)
        assertEquals("doctor-id", row["doctor_id"]!!.jsonPrimitive.content)
        assertEquals("completed-visit-id", row["appointment_id"]!!.jsonPrimitive.content)
        assertEquals(5, row["rating"]!!.jsonPrimitive.int)
        assertEquals("Excellent consultation", row["comment"]!!.jsonPrimitive.content)
        assertTrue(json.contains("\"rating\":5"))
    }
}
