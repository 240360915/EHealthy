package ehealthy.connect.ui.patientDashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ehealthy.connect.ui.theme.adaptiveColor

object PatientColors {
    val DoctorCard get() = adaptiveColor(Color(0xFFE8F7F5), Color(0xFF173E3D))
    val DoctorAccent get() = adaptiveColor(Color(0xFF0F766E), Color(0xFF54DFCF))

    val AppointmentCard get() = adaptiveColor(Color(0xFFEDF4FF), Color(0xFF1B3150))
    val AppointmentAccent get() = adaptiveColor(Color(0xFF2563EB), Color(0xFF93C5FD))

    val RecordsCard get() = adaptiveColor(Color(0xFFF4EEFF), Color(0xFF332747))
    val RecordsAccent get() = adaptiveColor(Color(0xFF7C3AED), Color(0xFFC4B5FD))

    val TipsCard get() = adaptiveColor(Color(0xFFFFF7E8), Color(0xFF413419))
    val TipsAccent get() = adaptiveColor(Color(0xFFF59E0B), Color(0xFFFCD34D))

    val SuccessCard get() = adaptiveColor(Color(0xFFECFDF3), Color(0xFF193D31))
    val SuccessAccent get() = adaptiveColor(Color(0xFF059669), Color(0xFF6EE7B7))

    val ReviewCard get() = adaptiveColor(Color(0xFFFFF8E7), Color(0xFF413419))
    val ReviewAccent get() = adaptiveColor(Color(0xFFD97706), Color(0xFFFCD34D))

    val NeutralCard get() = adaptiveColor(Color(0xFFF8FAFC), Color(0xFF1B2938))

    val Primary get() = adaptiveColor(Color(0xFF087F8C), Color(0xFF38B5C2))
    val PrimaryDark get() = adaptiveColor(Color(0xFF075985), Color(0xFF70BFF3))

    val Blue get() = adaptiveColor(Color(0xFF2563EB), Color(0xFF93C5FD))
    val BlueSoft get() = adaptiveColor(Color(0xFFEFF6FF), Color(0xFF1B3150))

    val Teal get() = adaptiveColor(Color(0xFF0F766E), Color(0xFF54DFCF))
    val TealSoft get() = adaptiveColor(Color(0xFFECFDF5), Color(0xFF173E3D))

    val Green get() = adaptiveColor(Color(0xFF059669), Color(0xFF6EE7B7))
    val GreenSoft get() = adaptiveColor(Color(0xFFECFDF5), Color(0xFF193D31))

    val Amber get() = adaptiveColor(Color(0xFFF59E0B), Color(0xFFFCD34D))
    val AmberSoft get() = adaptiveColor(Color(0xFFFFFBEB), Color(0xFF413419))

    val Red get() = adaptiveColor(Color(0xFFDC2626), Color(0xFFFCA5A5))
    val RedSoft get() = adaptiveColor(Color(0xFFFEF2F2), Color(0xFF46282C))

    val Purple get() = adaptiveColor(Color(0xFF7C3AED), Color(0xFFC4B5FD))
    val PurpleSoft get() = adaptiveColor(Color(0xFFF5F3FF), Color(0xFF332747))

    // Button fills stay dark enough for white labels in BOTH modes.
    // Icon/text accents above may be brighter on dark cards.
    val PrimaryFill = Color(0xFF087F8C)
    val DoctorAccentFill = Color(0xFF0F766E)
    val AppointmentAccentFill = Color(0xFF2563EB)
    val PurpleFill = Color(0xFF7C3AED)
    val RedFill = Color(0xFFDC2626)
    val TealFill = Color(0xFF0F766E)
    val BlueFill = Color(0xFF2563EB)

    val TextPrimary get() = adaptiveColor(Color(0xFF0F172A), Color(0xFFF1F5F9))
    val TextSecondary get() = adaptiveColor(Color(0xFF64748B), Color(0xFFB8C6D8))

    val HeroGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF075985),
            Color(0xFF087F8C),
            Color(0xFF0F766E)
        )
    )

    val ConsultationGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0F766E),
            Color(0xFF0891B2),
            Color(0xFF2563EB)
        )
    )
}


@Composable
fun Modifier.patientPressAnimation(
    interactionSource: MutableInteractionSource
): Modifier {

    val pressed by
    interactionSource.collectIsPressedAsState()

    val scale by
    animateFloatAsState(
        targetValue =
            if (pressed) 0.97f else 1f,
        animationSpec =
            spring(
                dampingRatio =
                    Spring.DampingRatioMediumBouncy,
                stiffness =
                    Spring.StiffnessMedium
            ),
        label = "PatientPressAnimation"
    )

    return scale(scale)
}


@Composable
fun PatientAnimatedClickableContainer(
    modifier: Modifier = Modifier,
    content: @Composable (
        MutableInteractionSource
    ) -> Unit
) {

    val interactionSource =
        remember {
            MutableInteractionSource()
        }

    Box(
        modifier =
            modifier.patientPressAnimation(
                interactionSource
            )
    ) {

        content(
            interactionSource
        )
    }
}

