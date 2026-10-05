package ehealthy.connect.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00201C),
    primaryContainer = Color(0xFF004D43),
    onPrimaryContainer = Color(0xFFA6F5EA),
    secondary = DarkBlue,
    onSecondary = Color(0xFFE6E1E5),
    secondaryContainer = Color(0xFF2B3B5C),
    onSecondaryContainer = Color(0xFFB9D4FF),
    tertiary = Green,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2A2A2E),
    onBackground = Color(0xFFCECDCD),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFA9A4AA),
    outline = Color(0xFF8D8B92),
    error = Color(0xFFEF5350)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Navy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF2563EB),
    tertiary = Green,
    background = LightBackground,
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F4F8),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF605D66),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFDC2626)
)

@Composable
fun EHealthyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color pulls from wallpaper on Android 12+ and overrides everything below —
    // turned off so the app's own palette (and dark-mode toggle) actually control the UI.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}