package ehealthy.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun JourneyItem(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    title: String,
    step: String
) {

    Column(
        modifier = modifier
            .height(90.dp)
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        icon()

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = title,
            color = Color(0xFF202633),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = step,
            color = Color(0xFF7C8187),
            fontSize = 11.sp
        )
    }
}
