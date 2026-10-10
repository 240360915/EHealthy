package ehealthy.connect.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Compatibility aliases for older screens.
 * The actual password rules now live only in PasswordPolicy.kt.
 */
fun isPasswordStrong(password: String): Boolean = isPasswordValid(password)

@Composable
fun PasswordStrengthChecklist(
    password: String,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(modifier = modifier) {
        PasswordRequirementsChecklist(password = password)
    }
}
