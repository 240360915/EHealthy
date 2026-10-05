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

object PatientColors {
    val DoctorCard = Color(0xFFE8F7F5)
    val DoctorAccent = Color(0xFF0F766E)

    val AppointmentCard = Color(0xFFEDF4FF)
    val AppointmentAccent = Color(0xFF2563EB)

    val RecordsCard = Color(0xFFF4EEFF)
    val RecordsAccent = Color(0xFF7C3AED)

    val TipsCard = Color(0xFFFFF7E8)
    val TipsAccent = Color(0xFFF59E0B)

    val SuccessCard = Color(0xFFECFDF3)
    val SuccessAccent = Color(0xFF059669)

    val ReviewCard = Color(0xFFFFF8E7)
    val ReviewAccent = Color(0xFFD97706)

    val NeutralCard = Color(0xFFF8FAFC)

    val Primary = Color(0xFF087F8C)
    val PrimaryDark = Color(0xFF075985)

    val Blue = Color(0xFF2563EB)
    val BlueSoft = Color(0xFFEFF6FF)

    val Teal = Color(0xFF0F766E)
    val TealSoft = Color(0xFFECFDF5)

    val Green = Color(0xFF059669)
    val GreenSoft = Color(0xFFECFDF5)

    val Amber = Color(0xFFF59E0B)
    val AmberSoft = Color(0xFFFFFBEB)

    val Red = Color(0xFFDC2626)
    val RedSoft = Color(0xFFFEF2F2)

    val Purple = Color(0xFF7C3AED)
    val PurpleSoft = Color(0xFFF5F3FF)

    val TextPrimary = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF64748B)

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

