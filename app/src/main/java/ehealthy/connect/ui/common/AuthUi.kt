package ehealthy.connect.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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

object AuthColors {
    val Background = Color(0xFFFAF9FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF5F6FA)
    val TextPrimary = Color(0xFF182033)
    val TextSecondary = Color(0xFF5D6470)
    val TextMuted = Color(0xFF8A919D)
    val Button = Color(0xFF293147)
    val Border = Color(0xFFE2E5EC)
    val Error = Color(0xFFD64545)
    val ErrorSoft = Color(0xFFFFEEEE)
    val Success = Color(0xFF218B78)
    val SuccessSoft = Color(0xFFDFF5F1)
    val Warning = Color(0xFF9A6500)
    val WarningSoft = Color(0xFFFFF4D8)
    val PatientAccent = Color(0xFF218B78)
    val PatientSoft = Color(0xFFDFF5F1)
    val DoctorAccent = Color(0xFF385A9E)
    val DoctorSoft = Color(0xFFEFF1FF)
}

@Composable
fun authTextFieldColors(accent: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        unfocusedBorderColor = AuthColors.Border,
        focusedTextColor = AuthColors.TextPrimary,
        unfocusedTextColor = AuthColors.TextPrimary,
        disabledTextColor = AuthColors.TextSecondary,
        cursorColor = accent,
        focusedLabelColor = accent,
        unfocusedLabelColor = AuthColors.TextSecondary,
        focusedContainerColor = AuthColors.Surface,
        unfocusedContainerColor = AuthColors.Surface,
        disabledContainerColor = AuthColors.SurfaceMuted,
        errorBorderColor = AuthColors.Error,
        errorLabelColor = AuthColors.Error,
        focusedPlaceholderColor = AuthColors.TextSecondary,
        unfocusedPlaceholderColor = AuthColors.TextSecondary
    )

@Composable
fun AuthPage(
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 28.dp,
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(containerColor = AuthColors.Background) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(AuthColors.Background)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(paddingValues)
                .padding(horizontal = horizontalPadding, vertical = 24.dp),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun AuthHeroIcon(
    icon: ImageVector,
    accent: Color,
    background: Color,
    contentDescription: String? = null
) {
    Box(
        modifier = Modifier
            .size(78.dp)
            .background(background, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = accent,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun AuthHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = AuthColors.TextPrimary,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = subtitle,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = AuthColors.TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuthColors.Button,
            disabledContainerColor = AuthColors.Button.copy(alpha = 0.42f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AuthSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = AuthColors.TextPrimary
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuthColors.Border)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun AuthStatusPill(
    text: String,
    accent: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(50.dp))
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, RoundedCornerShape(50.dp))
        )
        Spacer(modifier = Modifier.size(7.dp))
        Text(
            text = text,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AuthInfoCard(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = AuthColors.DoctorAccent,
    background: Color = AuthColors.DoctorSoft
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .border(1.dp, accent.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            color = AuthColors.TextSecondary,
            fontSize = 13.5.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun AuthErrorMessage(
    message: String?,
    modifier: Modifier = Modifier
) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            color = AuthColors.Error,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = modifier
        )
    }
}

@Composable
fun AuthSuccessMessage(
    message: String?,
    accent: Color,
    modifier: Modifier = Modifier
) {
    if (!message.isNullOrBlank()) {
        Text(
            text = message,
            color = accent,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = modifier
        )
    }
}
