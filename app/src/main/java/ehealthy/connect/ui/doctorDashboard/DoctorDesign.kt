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

object DoctorColors {

    // Main screen colours
    val Background = Color(0xFFF6F8FB)
    val Surface = Color.White

    // Text
    val Ink = Color(0xFF111827)
    val Muted = Color(0xFF667085)

    // Borders
    val Border = Color(0xFFE4E9F0)

    // Teal - primary doctor colour
    val Teal = Color(0xFF0F8F86)
    val TealDark = Color(0xFF08766F)
    val TealSoft = Color(0xFFE7F7F4)

    // Blue
    val Blue = Color(0xFF2F6FED)
    val BlueSoft = Color(0xFFEAF2FF)

    // Purple
    val Purple = Color(0xFF7C5CE0)
    val PurpleSoft = Color(0xFFF1EDFF)

    // Green
    val Green = Color(0xFF14966B)
    val GreenSoft = Color(0xFFE8F8F1)

    // Amber
    val Amber = Color(0xFFE99A0C)
    val AmberSoft = Color(0xFFFFF4DD)

    // Red
    val Red = Color(0xFFD84646)
    val RedSoft = Color(0xFFFFEEEE)

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