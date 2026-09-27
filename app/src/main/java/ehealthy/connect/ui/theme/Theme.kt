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
    secondary = DarkBlue,
    tertiary = Green,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color(0xFFCECDCD),
    onSurface = Color(0xFFE6E1E5),
    onSurfaceVariant = Color(0xFFA9A4AA)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal,
    secondary = Navy,
    tertiary = Green,
    background = LightBackground,
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF605D66)
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