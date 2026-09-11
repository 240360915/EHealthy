package ehealthy.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Placeholder only — just enough so a successful doctor login/register has
 * somewhere to land without crashing (the way "patientDashboard" currently
 * does not, further up the nav graph). Swap this out once the real doctor
 * dashboard work starts.
 */
@Composable
fun DoctorDashboard(
    onLogOut: () -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val greyText = Color(0xFF4F555C)
    val navyButton = Color(0xFF293147)

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
            Text(
                text = "You're logged in",
                color = darkText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "The doctor dashboard hasn't been built yet — this is just a placeholder landing screen.",
                color = greyText,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onLogOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyButton)
            ) {
                Text("Log out", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}