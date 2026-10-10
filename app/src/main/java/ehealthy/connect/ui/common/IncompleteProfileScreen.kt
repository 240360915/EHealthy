package ehealthy.connect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun IncompleteProfileScreen(
    email: String,
    onContinueAsPatient: () -> Unit,
    onContinueAsDoctor: () -> Unit,
    onSignOut: () -> Unit
) {
    AuthPage {
        AuthHeroIcon(
            icon = Icons.Outlined.PersonOutline,
            accent = AuthColors.TextPrimary,
            background = AuthColors.SurfaceMuted
        )

        Spacer(modifier = Modifier.height(24.dp))

        AuthHeader(
            title = "Finish your account setup",
            subtitle = "Your sign-in account exists, but your profile setup was interrupted. Choose the profile you were creating to continue."
        )

        if (email.isNotBlank()) {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .background(AuthColors.Surface, RoundedCornerShape(14.dp))
                    .border(1.dp, AuthColors.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            ) {
                Text(
                    text = email,
                    color = AuthColors.TextSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        ProfileChoiceCard(
            title = "Continue patient setup",
            subtitle = "Finish your patient profile using this account.",
            icon = Icons.Outlined.Person,
            accent = AuthColors.PatientAccent,
            soft = AuthColors.PatientSoft,
            onClick = onContinueAsPatient
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileChoiceCard(
            title = "Continue doctor setup",
            subtitle = "Finish your doctor application and verification details.",
            icon = Icons.Outlined.MedicalServices,
            accent = AuthColors.DoctorAccent,
            soft = AuthColors.DoctorSoft,
            onClick = onContinueAsDoctor
        )

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Sign out instead",
            color = AuthColors.TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onSignOut() }
        )
    }
}

@Composable
private fun ProfileChoiceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    soft: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthColors.Surface, RoundedCornerShape(20.dp))
            .border(1.dp, AuthColors.Border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(soft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(25.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AuthColors.TextPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = AuthColors.TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = AuthColors.TextMuted,
            modifier = Modifier.size(21.dp)
        )
    }
}
