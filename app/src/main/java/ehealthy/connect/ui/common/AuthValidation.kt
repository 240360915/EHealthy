package ehealthy.connect.ui.common

private val emailPattern = Regex(
    pattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
    option = RegexOption.IGNORE_CASE
)

fun normalizeEmail(value: String): String = value.trim().lowercase()

fun isValidEmail(value: String): Boolean {
    val email = normalizeEmail(value)
    return email.isNotEmpty() && emailPattern.matches(email)
}

fun isValidSaIdNumber(value: String): Boolean =
    value.length == 13 && value.all(Char::isDigit)

fun isValidSaPhoneNumber(value: String): Boolean =
    value.length == 10 && value.all(Char::isDigit)

fun isValidSaPostalCode(value: String): Boolean =
    value.length == 4 && value.all(Char::isDigit)

fun digitsOnly(value: String, maxLength: Int): String =
    value.filter(Char::isDigit).take(maxLength)
