package ehealthy.connect.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Tiny app-wide dark/light mode holder. `isDarkMode` is a Compose-observable
 * state, so wrapping EHealthyTheme(darkTheme = ThemeManager.isDarkMode) in
 * MainActivity will recompose the whole app the moment it changes.
 *
 * Call ThemeManager.init(context) once, e.g. at the top of
 * MainActivity.onCreate, before setContent { ... }.
 */
object ThemeManager {
    private const val PREFS_NAME = "ehealthy_prefs"
    private const val KEY_DARK_MODE = "dark_mode_enabled"

    var isDarkMode by mutableStateOf(false)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        isDarkMode = p.getBoolean(KEY_DARK_MODE, false)
    }
    fun updateDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        prefs?.edit()?.putBoolean(KEY_DARK_MODE, enabled)?.apply()
    }

}
