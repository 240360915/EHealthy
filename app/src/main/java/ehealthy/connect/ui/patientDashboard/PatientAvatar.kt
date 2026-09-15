package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

@Composable
fun PatientAvatar(
    avatarUrl: String?,
    name: String,
    size: Dp = 40.dp,
    onClick: (() -> Unit)? = null
) {
    val navy = Color(0xFF0F1F3D)

    val baseModifier = Modifier
        .size(size)
        .clip(CircleShape)
        .background(navy)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(modifier = baseModifier, contentAlignment = Alignment.Center) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.2).sp
            )
        }
    }
}

/**
 * Tap-to-act version: shows a bottom sheet with "View Photo" / "Change Photo"
 * options. Use this wherever the person should be able to both view the full
 * picture and replace it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditablePatientAvatar(
    avatarUrl: String?,
    name: String,
    size: Dp = 96.dp,
    isUploading: Boolean,
    onViewPhoto: () -> Unit,
    onChangePhoto: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    PatientAvatar(
        avatarUrl = avatarUrl,
        name = name,
        size = size,
        onClick = { showSheet = true }
    )

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                if (!avatarUrl.isNullOrBlank()) {
                    SheetOption(Icons.Outlined.Visibility, "View Photo") {
                        showSheet = false
                        onViewPhoto()
                    }
                }
                SheetOption(Icons.Outlined.CameraAlt, if (isUploading) "Uploading…" else "Change Photo") {
                    showSheet = false
                    onChangePhoto()
                }
            }
        }
    }
}

@Composable
private fun SheetOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF0F1F3D))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = Color(0xFF0F1F3D), fontSize = 15.sp)
    }
}

/**
 * Full-screen photo viewer — tap anywhere to dismiss.
 */
@Composable
fun PhotoViewerDialog(photoUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile picture",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color(0xFF0F1F3D))
            }
        }
    }
}