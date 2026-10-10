package ehealthy.connect.ui.doctor

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ehealthy.connect.ui.common.AuthColors
import ehealthy.connect.ui.common.AuthErrorMessage
import ehealthy.connect.ui.common.AuthHeader
import ehealthy.connect.ui.common.AuthHeroIcon
import ehealthy.connect.ui.common.AuthInfoCard
import ehealthy.connect.ui.common.AuthPage
import ehealthy.connect.ui.common.AuthPrimaryButton
import ehealthy.connect.ui.common.AuthSecondaryButton
import ehealthy.connect.ui.common.AuthStatusPill

@Composable
fun DoctorVerificationScreen(
    status: String?,
    isRefreshing: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onSignOut: () -> Unit
) {
    val normalized = status?.trim()?.lowercase()
    val needsAttention = normalized in setOf("rejected", "declined")

    val accent: Color = if (needsAttention) AuthColors.Error else AuthColors.DoctorAccent
    val softBackground: Color = if (needsAttention) AuthColors.ErrorSoft else AuthColors.DoctorSoft

    val title = if (needsAttention) {
        "Verification needs attention"
    } else {
        "Your application is under review"
    }

    val subtitle = if (needsAttention) {
        "Your doctor profile was not approved. Contact support before changing or resubmitting verification documents."
    } else {
        "We've received your doctor profile and verification documents. Clinical features stay locked until your account is approved."
    }

    val statusLabel = when {
        normalized.isNullOrBlank() -> "Pending review"
        normalized == "pending" -> "Pending review"
        normalized == "approved" -> "Approved"
        normalized == "rejected" -> "Rejected"
        normalized == "declined" -> "Declined"
        else -> normalized.replaceFirstChar { it.uppercase() }
    }

    AuthPage {
        AuthHeroIcon(
            icon = if (needsAttention) Icons.Outlined.ErrorOutline else Icons.Outlined.VerifiedUser,
            accent = accent,
            background = softBackground
        )

        Spacer(modifier = Modifier.height(22.dp))

        AuthStatusPill(
            text = statusLabel,
            accent = accent,
            background = softBackground
        )

        Spacer(modifier = Modifier.height(18.dp))

        AuthHeader(title = title, subtitle = subtitle)

        Spacer(modifier = Modifier.height(24.dp))

        AuthInfoCard(
            text = if (needsAttention) {
                "Keep your account details as they are until support confirms what needs to be corrected."
            } else {
                "You can close the app and come back later. Your verification status is checked again when you return."
            },
            accent = accent,
            background = softBackground
        )

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            AuthErrorMessage(errorMessage, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(26.dp))

        AuthPrimaryButton(
            text = "Check verification status",
            onClick = onRefresh,
            isLoading = isRefreshing
        )

        Spacer(modifier = Modifier.height(12.dp))

        AuthSecondaryButton(
            text = "Sign out",
            onClick = onSignOut,
            enabled = !isRefreshing,
            textColor = AuthColors.TextSecondary
        )
    }
}
