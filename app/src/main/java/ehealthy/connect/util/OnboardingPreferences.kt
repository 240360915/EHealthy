package ehealthy.connect.util

import android.content.Context
import androidx.core.content.edit

/**
 * Stores whether this app installation has already completed onboarding.
 * This is device-local state and is intentionally separate from Supabase auth.
 */
object OnboardingPreferences {
    private const val PREFS_NAME = "ehealthy_onboarding"
    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    fun isComplete(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun markComplete(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putBoolean(KEY_ONBOARDING_COMPLETE, true)
            }
    }
}


