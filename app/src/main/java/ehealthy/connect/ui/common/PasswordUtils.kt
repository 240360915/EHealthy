package ehealthy.connect.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PasswordRequirement(val label: String, val satisfied: Boolean)

fun passwordRequirements(password: String): List<PasswordRequirement> = listOf(
    PasswordRequirement("At least 8 characters", password.length >= 8),
    PasswordRequirement("One uppercase letter (A–Z)", password.any { it.isUpperCase() }),
    PasswordRequirement("One lowercase letter (a–z)", password.any { it.isLowerCase() }),
    PasswordRequirement("One number (0–9)", password.any { it.isDigit() }),
    PasswordRequirement("One special character (!@#\$%^&*)", password.any { !it.isLetterOrDigit() })
)

fun isPasswordStrong(password: String): Boolean =
    passwordRequirements(password).all { it.satisfied }

@Composable
fun PasswordStrengthChecklist(password: String, modifier: Modifier = Modifier) {
    val green = Color(0xFF218B78)
    val grey = Color(0xFF9AA3AF)
    Column(modifier = modifier) {
        passwordRequirements(password).forEach { req ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(
                    imageVector = if (req.satisfied) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (req.satisfied) green else grey,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(req.label, fontSize = 12.sp, color = if (req.satisfied) green else grey)
            }
        }
    }
}