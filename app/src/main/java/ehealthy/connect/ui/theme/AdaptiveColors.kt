package ehealthy.connect.ui.theme

import androidx.compose.ui.graphics.Color
import ehealthy.connect.util.ThemeManager

/**
 * Returns an app-palette token that follows EHealthy's persisted light/dark toggle.
 *
 * Reading ThemeManager.isDarkMode (Compose snapshot state) in UI makes its
 * dependent composables recompose immediately when mode changes. This is
 * deliberately not @Composable: existing dashboard color tokens are used by
 * ordinary helper functions as well as composables.
 */
fun adaptiveColor(light: Color, dark: Color): Color =
    if (ThemeManager.isDarkMode) dark else light

object AdaptiveColors {
    val background get() = adaptiveColor(Color(0xFFF7F9FC), Color(0xFF0F1724))
    val surface get() = adaptiveColor(Color.White, Color(0xFF182437))
    val ink get() = adaptiveColor(Color(0xFF171B27), Color(0xFFF1F5F9))
    val muted get() = adaptiveColor(Color(0xFF6C7280), Color(0xFFB8C6D8))
    val border get() = adaptiveColor(Color(0xFFE3E8EF), Color(0xFF35445A))
    val tealSoft get() = adaptiveColor(Color(0xFFE8F7F5), Color(0xFF102826))
    val blueSoft get() = adaptiveColor(Color(0xFFEAF2FF), Color(0xFF15253F))
    val purpleSoft get() = adaptiveColor(Color(0xFFF4EEFF), Color(0xFF261D38))
    val greenSoft get() = adaptiveColor(Color(0xFFE9FAF2), Color(0xFF142F25))
    val amberSoft get() = adaptiveColor(Color(0xFFFFF4DD), Color(0xFF312713))
    val redSoft get() = adaptiveColor(Color(0xFFFFEEEE), Color(0xFF391A20))
    val accentTeal get() = adaptiveColor(Color(0xFF119E95), Color(0xFF54DFCF))
    val accentBlue get() = adaptiveColor(Color(0xFF2F6FED), Color(0xFF93C5FD))
    val accentPurple get() = adaptiveColor(Color(0xFF8B5CF6), Color(0xFFC4B5FD))
    val accentGreen get() = adaptiveColor(Color(0xFF18A572), Color(0xFF6EE7B7))
    val accentAmber get() = adaptiveColor(Color(0xFFF59E0B), Color(0xFFFCD34D))
    val accentRed get() = adaptiveColor(Color(0xFFE53935), Color(0xFFFCA5A5))
}
