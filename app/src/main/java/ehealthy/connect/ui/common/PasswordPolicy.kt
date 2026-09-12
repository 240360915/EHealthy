package ehealthy.connect.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared complex-password policy for both doctor registration and password
 * reset, so the two flows can never drift apart.
 *
 * Rules: 8+ characters, at least one uppercase, one lowercase, one digit,
 * and one special (non-alphanumeric) character.
 */
data class PasswordRequirements(val label: String, val isMet: (String) -> Boolean)

val passwordRequirements = listOf(
    PasswordRequirements("At least 8 characters") { it.length >= 8 },
    PasswordRequirements("One uppercase letter (A-Z)") { pw -> pw.any { it.isUpperCase() } },
    PasswordRequirements("One lowercase letter (a-z)") { pw -> pw.any { it.isLowerCase() } },
    PasswordRequirements("One number (0-9)") { pw -> pw.any { it.isDigit() } },
    PasswordRequirements("One special character (e.g. !@#\$%)") { pw -> pw.any { !it.isLetterOrDigit() } }
)

fun isPasswordValid(password: String): Boolean =
    passwordRequirements.all { it.isMet(password) }

/**
 * Horizontal strength bar: empty and grey when blank, fills left-to-right
 * and shifts from red to green as more of the five requirements are met.
 */
@Composable
fun PasswordStrengthMeter(password: String, modifier: Modifier = Modifier) {
    val metCount = passwordRequirements.count { it.isMet(password) }
    val targetFraction =
        if (password.isEmpty()) 0f else metCount / passwordRequirements.size.toFloat()

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        label = "passwordStrengthFraction"
    )
    val animatedColor by animateColorAsState(
        targetValue = lerp(Color(0xFFD64545), Color(0xFF218B78), targetFraction),
        label = "passwordStrengthColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFE2E5EC))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(animatedColor)
        )
    }
}

/**
 * Live checklist shown under a password field — each requirement ticks
 * green as soon as it's satisfied.
 */
@Composable
fun PasswordRequirementsChecklist(
    password: String,
    metColor: Color = Color(0xFF218B78),
    unmetColor: Color = Color(0xFF9099A6)
) {
    Column {
        passwordRequirements.forEach { requirement ->
            val met = password.isNotEmpty() && requirement.isMet(password)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (met) Icons.Outlined.Check else Icons.Outlined.Close,
                    contentDescription = null,
                    tint = if (met) metColor else unmetColor,
                    modifier = Modifier
                        .height(14.dp)
                        .width(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = requirement.label,
                    color = if (met) metColor else unmetColor,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}