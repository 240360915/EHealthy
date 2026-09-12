package ehealthy.connect.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate

data class ParsedSaId(
    val dateOfBirth: LocalDate,
    val gender: String   // "Male" or "Female"
)

/**
 * Parses a 13-digit South African ID number.
 * Format: YYMMDD SSSS C A Z
 *   YYMMDD = date of birth
 *   SSSS   = 0000-4999 female, 5000-9999 male
 *   Z      = Luhn check digit
 *
 * Returns null if the number is malformed, the date is invalid,
 * or the checksum doesn't match (catches typos).
 */
@RequiresApi(Build.VERSION_CODES.O)
fun parseSaIdNumber(id: String): ParsedSaId? {
    if (id.length != 13 || !id.all { it.isDigit() }) return null
    if (!isValidLuhn(id)) return null

    val yy = id.substring(0, 2).toInt()
    val mm = id.substring(2, 4).toInt()
    val dd = id.substring(4, 6).toInt()
    val genderDigits = id.substring(6, 10).toInt()

    // Heuristic: if YY is greater than the current two-digit year,
    // assume the person was born in the 1900s; otherwise 2000s.
    // (Ambiguous only for someone exactly 100 years old on the boundary year.)
    val currentYearTwoDigit = LocalDate.now().year % 100
    val century = if (yy > currentYearTwoDigit) 1900 else 2000
    val fullYear = century + yy

    val dateOfBirth = try {
        LocalDate.of(fullYear, mm, dd)
    } catch (e: Exception) {
        return null   // invalid calendar date, e.g. 02/30
    }

    val gender = if (genderDigits < 5000) "Female" else "Male"

    return ParsedSaId(dateOfBirth, gender)
}

private fun isValidLuhn(id: String): Boolean {
    val digits = id.map { it.toString().toInt() }
    var sum = 0
    for (i in 0..11) {
        var d = digits[i]
        if (i % 2 == 1) {   // even position (1-indexed) → double it
            d *= 2
            if (d > 9) d -= 9
        }
        sum += d
    }
    val checkDigit = (10 - (sum % 10)) % 10
    return checkDigit == digits[12]
}

