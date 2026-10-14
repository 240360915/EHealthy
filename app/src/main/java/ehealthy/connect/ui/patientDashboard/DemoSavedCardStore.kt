package ehealthy.connect.ui.patientDashboard

import android.content.Context

/**
 * DemoPay stores only a local, non-sensitive test-card profile.
 * Never store card PANs, CVVs or bank credentials here or in Supabase.
 */
internal data class DemoSavedCard(
    val label: String,
    val lastFour: String,
    val expiry: String
)

internal object DemoSavedCardStore {
    private const val PREFS = "ehealthy_demo_test_cards_v1"

    // DemoPay is a local simulation and never sends a number to a payment gateway.
    // Accept any invented 16-digit number, not only a hard-coded test number.
    // This function does NOT validate or authorize a real bank card.
    fun isValidDemoNumber(number: String): Boolean =
        number.length == 16 && number.all { it in '0'..'9' }

    private fun baseKey(accountId: String) = "card_" + accountId

    fun load(context: Context, accountId: String): DemoSavedCard? {
        if (accountId.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val base = baseKey(accountId)
        val lastFour = prefs.getString(base + "_last4", null)
        val label = prefs.getString(base + "_label", null)
        val expiry = prefs.getString(base + "_expiry", null)
        return if (
            !lastFour.isNullOrBlank() && lastFour.length == 4 &&
            !label.isNullOrBlank() && !expiry.isNullOrBlank()
        ) DemoSavedCard(label, lastFour, expiry) else null
    }

    fun save(context: Context, accountId: String, label: String, number: String, expiry: String) {
        require(accountId.isNotBlank()) { "Sign in to save a demo card." }
        require(isValidDemoNumber(number)) { "Enter exactly 16 digits for a fictional demo card." }
        require(expiry.length == 4) { "Invalid sandbox expiry." }
        val base = baseKey(accountId)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(base + "_last4", number.takeLast(4))
            .putString(base + "_label", label.take(40))
            .putString(base + "_expiry", expiry)
            .apply()
    }

    fun remove(context: Context, accountId: String) {
        if (accountId.isBlank()) return
        val base = baseKey(accountId)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(base + "_last4")
            .remove(base + "_label")
            .remove(base + "_expiry")
            .apply()
    }
}
