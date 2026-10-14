package ehealthy.connect.ui.patientDashboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DemoPay is fictional. These checks test input length and digits only:
 * they do not make any bank authorization, verification, or payment.
 */
class DemoSavedCardNumberTest {
    @Test
    fun acceptsAnyInventedSixteenDigitNumber() {
        assertTrue(DemoSavedCardStore.isValidDemoNumber("1234567890123456"))
        assertTrue(DemoSavedCardStore.isValidDemoNumber("9876543210123456"))
        assertTrue(DemoSavedCardStore.isValidDemoNumber("0000000000000000"))
    }

    @Test
    fun rejectsEmptyIncompleteAndExtraDigits() {
        assertFalse(DemoSavedCardStore.isValidDemoNumber(""))
        assertFalse(DemoSavedCardStore.isValidDemoNumber("123456789012345"))
        assertFalse(DemoSavedCardStore.isValidDemoNumber("12345678901234567"))
    }

    @Test
    fun rejectsLettersSpacesAndSymbols() {
        assertFalse(DemoSavedCardStore.isValidDemoNumber("123456789012345a"))
        assertFalse(DemoSavedCardStore.isValidDemoNumber("1234 5678 9012 3456"))
        assertFalse(DemoSavedCardStore.isValidDemoNumber("123456789012345!"))
    }
}
