package ehealthy.connect.ui.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ehealthy.connect.util.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.util.Log

@Serializable
data class DoctorProfileSummary(
    val name: String? = null,
    val surname: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("verification_status") val verificationStatus: String? = null
)

/**
 * Still just enough to prove sign-up → storage → dashboard works end to
 * end (photo + name + pending-review status). The real dashboard is a
 * separate piece of work.
 */
@Composable
fun DoctorDashboard(
    onLogOut: () -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val greyText = Color(0xFF4F555C)
    val blue = Color(0xFF385A9E)
    val navyButton = Color(0xFF293147)

    var profile by remember { mutableStateOf<DoctorProfileSummary?>(null) }

    LaunchedEffect(Unit) {
        val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: return@LaunchedEffect
        try {
            profile = SupabaseClientProvider.client.postgrest.from("doctors")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<DoctorProfileSummary>()
            Log.d("DoctorDashboard", "Fetched profile: $profile")
        } catch (e: Exception) {
            Log.e("DoctorDashboard", "Failed to fetch doctor profile for userId=$userId", e)
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(horizontal = 28.dp)
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF1FF)),
                contentAlignment = Alignment.Center
            ) {
                if (profile?.profileImageUrl != null) {
                    AsyncImage(
                        model = profile?.profileImageUrl,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onLoading = { Log.d("DoctorDashboard", "Loading photo: ${profile?.profileImageUrl}") },
                        onSuccess = { Log.d("DoctorDashboard", "Photo loaded OK") },
                        onError = { state ->
                            Log.e("DoctorDashboard", "Photo FAILED to load", state.result.throwable)
                        }
                    )
                } else {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = blue, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = listOfNotNull(profile?.name, profile?.surname)
                    .joinToString(" ")
                    .ifBlank { "You're logged in" },
                color = darkText, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )

            if (profile?.verificationStatus == "pending") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Your account is pending administrator verification.",
                    color = greyText, fontSize = 13.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "The full doctor dashboard hasn't been built yet — this is just a landing screen.",
                color = greyText, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onLogOut,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyButton)
            ) {
                Text("Log out", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}