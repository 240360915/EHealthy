package ehealthy.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreenTwo(
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {

    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val green = Color(0xFF218B78)
    val lightGreen = Color(0xFFDFF5F1)
    val lightBlue = Color(0xFFEFF1FF)
    val greyText = Color(0xFF4F555C)
    val navyButton = Color(0xFF293147)

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .padding(horizontal = 24.dp)
                .padding(paddingValues)
        ) {

            // ------------------------------------------------
            // TOP
            // ------------------------------------------------

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(lightGreen)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6CB4A6))
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "STEP 2 OF 4",
                        color = green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Skip",
                    color = darkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onSkip }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------------
            // ILLUSTRATION AREA (flexible — fills remaining space)
            // ------------------------------------------------

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(40.dp))
                    .background(lightBlue),
                contentAlignment = Alignment.Center
            ) {

                // Soft inner circle — search/magnifier motif

                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF7F7FF))
                        .border(1.dp, Color(0xFFE9E9F5), CircleShape),
                    contentAlignment = Alignment.Center
                ) {

                    // Magnifier ring

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .border(14.dp, Color(0xFF27A88E), CircleShape)
                    )

                    // Magnifier handle

                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 60.dp)
                            .align(Alignment.Center)
                            .offset(x = 55.dp, y = 55.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF087A66))
                    )

                    // Small doctor-pin dot inside ring

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6BE2C6))
                    )

                    // Floating dots for depth (same language as step 1)

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF55B5A4))
                    )

                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 30.dp, y = (-42).dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB8C1DA))
                    )
                }

                // Bottom pill — trust signal for this step

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 30.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFE6E6E6), RoundedCornerShape(30.dp))
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = green,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(9.dp))

                    Text(
                        text = "Rated & Verified Doctors",
                        color = darkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ------------------------------------------------
            // MAIN TITLE
            // ------------------------------------------------

            Text(
                text = "Find the right\ndoctor, fast.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = darkText,
                fontSize = 38.sp,
                lineHeight = 45.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ------------------------------------------------
            // DESCRIPTION
            // ------------------------------------------------

            Text(
                text = "Search by specialty or location and see\nverified doctors with real availability —\nno guesswork.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = greyText,
                fontSize = 19.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(27.dp))

            // ------------------------------------------------
            // PAGE INDICATORS (2nd dot active)
            // ------------------------------------------------

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD9E0F6))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(green)
                )

                Spacer(modifier = Modifier.width(10.dp))

                repeat(2) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD9E0F6))
                    )
                    if (it < 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ------------------------------------------------
            // JOURNEY PREVIEW ("Find Doctor" highlighted as current)
            // ------------------------------------------------

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(0xFFF0F2FF))
                    .padding(top = 14.dp, bottom = 10.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "JOURNEY PREVIEW",
                        color = Color(0xFF65708A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Tap to jump",
                        color = green,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {

                    JourneyItem(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                Icons.Outlined.PersonSearch,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(23.dp)
                            )
                        },
                        title = "Find Doctor",
                        step = "Step 2"
                    )

                    JourneyItem(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF65708A),
                                modifier = Modifier.size(23.dp)
                            )
                        },
                        title = "Book & Consult",
                        step = "Step 3"
                    )

                    JourneyItem(
                        modifier = Modifier.weight(1f),
                        icon = {
                            Icon(
                                Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = green,
                                modifier = Modifier.size(23.dp)
                            )
                        },
                        title = "Get Organized",
                        step = "Step 4"
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ------------------------------------------------
            // CONTINUE BUTTON
            // ------------------------------------------------

            Button(
                onClick = { onContinue },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                shape = RoundedCornerShape(35.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyButton)
            ) {
                Text(
                    text = "Continue  →",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = "Encrypted with HIPAA-compliant health grade security",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color(0xFF737A80),
                fontSize = 12.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}