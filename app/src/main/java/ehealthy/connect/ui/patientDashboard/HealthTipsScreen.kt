package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CleanHands
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NoDrinks
import androidx.compose.material.icons.outlined.PhoneDisabled
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Sick
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TipCategory(val label: String) {
    ALL("All Tips"),
    HOME("At-Home Care"),
    DAILY("Daily Habits"),
    MENTAL("Mental Health"),
    PREVENTION("Prevention")
}

data class HealthTip(
    val title: String,
    val description: String,
    val category: TipCategory,
    val icon: ImageVector,
    val accent: Color
)

// Refined palette — softer, more cohesive than flat primary colors
private val tealDeep = Color(0xFF0D9488)
private val tealSoft = Color(0xFFCCFBF1)
private val amberDeep = Color(0xFFD97706)
private val amberSoft = Color(0xFFFEF3C7)
private val redDeep = Color(0xFFDC2626)
private val redSoft = Color(0xFFFEE2E2)
private val blueDeep = Color(0xFF2563EB)
private val blueSoft = Color(0xFFDBEAFE)
private val navy = Color(0xFF0B1828)
private val ink = Color(0xFF0F1F3D)
private val muted = Color(0xFF64748B)
private val bg = Color(0xFFF4F7FA)

private val healthTips = listOf(
    HealthTip(
        "Rest & Recovery",
        "Allow your immune system to fight illness. Aim for 7–9 hours of sleep and avoid overexerting yourself when sick.",
        TipCategory.HOME,
        Icons.Outlined.Bedtime,
        tealDeep
    ),
    HealthTip(
        "Stay Hydrated",
        "Drink plenty of fluids — especially water and clear broths — to prevent dehydration and support recovery.",
        TipCategory.HOME,
        Icons.Outlined.WaterDrop,
        blueDeep
    ),
    HealthTip(
        "Manage Symptoms",
        "Use OTC medication like paracetamol or ibuprofen for fever and pain. Always follow dosage instructions carefully.",
        TipCategory.HOME,
        Icons.Outlined.Medication,
        amberDeep
    ),
    HealthTip(
        "Hand Hygiene",
        "Wash hands frequently with soap and water for at least 20 seconds. Use hand sanitiser when soap isn't available.",
        TipCategory.HOME,
        Icons.Outlined.CleanHands,
        tealDeep
    ),
    HealthTip(
        "Cover Coughs & Sneezes",
        "Use a tissue or your elbow. Dispose of tissues immediately and wash your hands to prevent spreading germs.",
        TipCategory.HOME,
        Icons.Outlined.Sick,
        blueDeep
    ),
    HealthTip(
        "Avoid Smoking",
        "Smoking irritates the respiratory tract and slows recovery. Avoid smoky environments when you're unwell.",
        TipCategory.HOME,
        Icons.Outlined.NoDrinks,
        redDeep
    ),
    HealthTip(
        "Eat Nutritiously",
        "Add vegetables to every meal and swap refined grains for wholemeal options. A balanced diet supports your immune system.",
        TipCategory.DAILY,
        Icons.Outlined.Restaurant,
        tealDeep
    ),
    HealthTip(
        "Exercise Regularly",
        "Aim for 150 minutes of moderate movement per week. Walking, swimming, or cycling all count — find what you enjoy.",
        TipCategory.DAILY,
        Icons.Outlined.DirectionsRun,
        blueDeep
    ),
    HealthTip(
        "Daily Hydration",
        "Keep a reusable water bottle handy. Aim for 8–10 glasses of water daily. More if you exercise or it's hot.",
        TipCategory.DAILY,
        Icons.Outlined.WaterDrop,
        amberDeep
    ),
    HealthTip(
        "Consistent Sleep Schedule",
        "Go to bed and wake up at the same time daily — even on weekends. Consistency helps regulate your body clock.",
        TipCategory.DAILY,
        Icons.Outlined.EventAvailable,
        tealDeep
    ),
    HealthTip(
        "Manage Stress",
        "Try deep breathing, meditation, or journaling. Even 5 minutes of mindfulness daily can reduce anxiety significantly.",
        TipCategory.MENTAL,
        Icons.Outlined.SelfImprovement,
        blueDeep
    ),
    HealthTip(
        "Stay Connected",
        "Social connection is vital for mental wellbeing. Reach out to friends or family regularly, even just a quick message.",
        TipCategory.MENTAL,
        Icons.Outlined.FavoriteBorder,
        tealDeep
    ),
    HealthTip(
        "Limit Screen Time",
        "Especially before bed. Blue light disrupts sleep. Try a 30-minute screen-free wind-down routine each evening.",
        TipCategory.MENTAL,
        Icons.Outlined.PhoneDisabled,
        amberDeep
    ),
    HealthTip(
        "Keep Vaccinations Current",
        "Stay up to date with recommended vaccines for your age group. Prevention is always better than treatment.",
        TipCategory.PREVENTION,
        Icons.Outlined.Vaccines,
        tealDeep
    ),
    HealthTip(
        "Regular Check-ups",
        "Don't wait until you're sick. Schedule annual health screenings to catch issues early when they're easier to treat.",
        TipCategory.PREVENTION,
        Icons.Outlined.LocalHospital,
        blueDeep
    ),
    HealthTip(
        "Sun Protection",
        "Apply SPF 30+ sunscreen daily, even on cloudy days. Wear a hat and seek shade between 10am and 3pm.",
        TipCategory.PREVENTION,
        Icons.Outlined.WbSunny,
        amberDeep
    )
)

private val warningSigns = listOf(
    "Difficulty breathing or shortness of breath at rest",
    "Persistent chest pain, pressure, or tightness",
    "Dizziness, confusion, or inability to stay awake",
    "Severe muscle pain, weakness, or sudden numbness",
    "Fever that disappears and returns worse than before",
    "Signs of severe dehydration — dark urine, no urination for 8+ hours"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTipsScreen(
    onBack: () -> Unit,
    onBookConsultation: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TipCategory.ALL) }

    val filteredTips = remember(selectedCategory) {
        if (selectedCategory == TipCategory.ALL) healthTips
        else healthTips.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Health Tips",
                        color = navy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = navy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = bg
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.padding(paddingValues)
        ) {
            item { HeroHeader() }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TipCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    cat.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = navy,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = muted
                            ),
                            border = null
                        )
                    }
                }
            }

            items(filteredTips) { tip ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    TipCard(tip)
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionLabel("When to seek help")
                    Spacer(modifier = Modifier.height(12.dp))
                    WarningSection()
                    Spacer(modifier = Modifier.height(20.dp))
                    EmergencyBanner()
                    Spacer(modifier = Modifier.height(20.dp))
                    NoteBox(onBookConsultation)
                }
            }
        }
    }
}

@Composable
private fun HeroHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(navy, Color(0xFF1A3A5C))
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    "WELLNESS GUIDE",
                    color = Color(0xFF6EE7B7),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Simple habits,\nstronger health.",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Practical, doctor-informed advice to help you stay well between consultations.",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.5.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(tealDeep)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text.uppercase(),
            color = muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun TipCard(tip: HealthTip) {
    val (iconBg, iconTint) = when (tip.accent) {
        tealDeep -> tealSoft to tealDeep
        amberDeep -> amberSoft to amberDeep
        redDeep -> redSoft to redDeep
        else -> blueSoft to blueDeep
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color(0x1A0B1828)
            )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tip.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tip.title, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(tip.description, color = muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun WarningSection() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.LocalHospital,
                        contentDescription = null,
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Seek immediate medical care if you notice:",
                    color = Color.White, fontSize = 14.5.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            warningSigns.forEach { sign ->
                Row(modifier = Modifier.padding(bottom = 10.dp)) {
                    Box(
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6EE7B7))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        sign,
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.WifiTethering,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        "Life-threatening emergency?",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Don't wait — call for help immediately.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.5.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EmergencyNumber("112", "Any emergency\n(cell phone)")
                EmergencyNumber("10177", "Ambulance /\nFire")
                EmergencyNumber("10111", "Police")
            }
        }
    }
}

@Composable
private fun EmergencyNumber(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(number, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )
    }
}

@Composable
private fun NoteBox(onBookConsultation: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                "If symptoms are severe, you belong to a high-risk group (elderly, pregnant, or have underlying conditions), or symptoms don't improve after a few days — consult a healthcare professional.",
                color = muted, fontSize = 13.5.sp, lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBookConsultation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tealDeep),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Book a Consultation",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}