package ehealthy.connect.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.common.AuthColors
import ehealthy.connect.ui.common.AuthPage

@Composable
fun ChooseRoleScreen(
    onPatientSelected: () -> Unit,
    onDoctorSelected: () -> Unit,
    onTermsSelected: () -> Unit,
    onPrivacySelected: () -> Unit
) {
    AuthPage(horizontalPadding = 24.dp) {
        Text(
            text = "e-Health Connect",
            color = AuthColors.PatientAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "How are you using\ne-Health Connect?",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = AuthColors.TextPrimary,
            fontSize = 31.sp,
            lineHeight = 37.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Choose your account type so we can take you to the right sign-in and setup flow.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = AuthColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(34.dp))

        RoleCard(
            eyebrow = "PATIENT",
            title = "Manage my healthcare",
            subtitle = "Book appointments, consult doctors, and keep track of your health information.",
            iconBg = AuthColors.PatientSoft,
            iconTint = AuthColors.PatientAccent,
            icon = Icons.Outlined.Person,
            onClick = onPatientSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        RoleCard(
            eyebrow = "DOCTOR",
            title = "Provide care",
            subtitle = "Manage consultations, appointments, prescriptions, and your professional profile.",
            iconBg = AuthColors.DoctorSoft,
            iconTint = AuthColors.DoctorAccent,
            icon = Icons.Outlined.MedicalServices,
            onClick = onDoctorSelected
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "By continuing, you agree to our",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = AuthColors.TextMuted,
            fontSize = 12.5.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Terms of Use",
                color = AuthColors.TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onTermsSelected() }
            )
            Text("  •  ", color = AuthColors.TextMuted, fontSize = 12.5.sp)
            Text(
                text = "Privacy Policy",
                color = AuthColors.TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onPrivacySelected() }
            )
        }
    }
}

@Composable
private fun RoleCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    iconBg: Color,
    iconTint: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuthColors.Surface, RoundedCornerShape(22.dp))
            .border(1.dp, AuthColors.Border, RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 19.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(27.dp)
            )
        }

        Spacer(modifier = Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow,
                color = iconTint,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                color = AuthColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
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
