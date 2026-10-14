package ehealthy.connect.ui.doctorDashboard

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ehealthy.connect.ui.theme.AdaptiveColors

object DoctorColors {

    // Main screen colours
    val Background get() = AdaptiveColors.background
    val Surface get() = AdaptiveColors.surface

    // Text
    val Ink get() = AdaptiveColors.ink
    val Muted get() = AdaptiveColors.muted

    // Borders
    val Border get() = AdaptiveColors.border

    // Teal - primary doctor colour
    val Teal = Color(0xFF0F8F86)
    val TealDark = Color(0xFF08766F)
    val TealSoft get() = AdaptiveColors.tealSoft

    // Blue
    val Blue = Color(0xFF2F6FED)
    val BlueSoft get() = AdaptiveColors.blueSoft

    // Purple
    val Purple = Color(0xFF7C5CE0)
    val PurpleSoft get() = AdaptiveColors.purpleSoft

    // Green
    val Green = Color(0xFF14966B)
    val GreenSoft get() = AdaptiveColors.greenSoft

    // Amber
    val Amber = Color(0xFFE99A0C)
    val AmberSoft get() = AdaptiveColors.amberSoft

    // Red
    val Red = Color(0xFFD84646)
    val RedSoft get() = AdaptiveColors.redSoft

    // Main Patient Care Hub gradient
    val PatientCareGradient = Brush.linearGradient(
        listOf(
            Color(0xFF08766F),
            Color(0xFF0F8F86),
            Color(0xFF2F6FED)
        )
    )

    // Used for more clinical/professional sections
    val ClinicalGradient = Brush.linearGradient(
        listOf(
            Color(0xFF123B60),
            Color(0xFF176B87),
            Color(0xFF0F8F86)
        )
    )
}


/*
 * Gives cards/buttons a small press animation.
 *
 * Instead of a button feeling completely static,
 * it slightly shrinks when the doctor presses it.
 */
@Composable
fun Modifier.doctorPressAnimation(
    interactionSource: MutableInteractionSource
): Modifier {

    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) {
            0.975f
        } else {
            1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "DoctorPressAnimation"
    )

    return scale(scale)
}


/*
 * Reusable animated container.
 *
 * We will use this for:
 * - patient cards
 * - clinical actions
 * - physical visit requests
 * - invoice actions
 */
@Composable
fun DoctorAnimatedClickableContainer(
    modifier: Modifier = Modifier,
    content: @Composable (MutableInteractionSource) -> Unit
) {

    val interactionSource = remember {
        MutableInteractionSource()
    }

    Box(
        modifier = modifier
            .doctorPressAnimation(interactionSource)
    ) {
        content(interactionSource)
    }
}