/*
package ehealthy.connect.ui.patientDashboard
import java.util.Calendar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
    ,
    HealthTip("Monitor Your Temperature", "Check your temperature twice daily if unwell — a rising fever can signal your body needs more support.", TipCategory.HOME, Icons.Outlined.Sick, redDeep),
    HealthTip("Gargle Salt Water", "For a sore throat, gargle warm salt water a few times a day to ease irritation and reduce swelling.", TipCategory.HOME, Icons.Outlined.WaterDrop, tealDeep),
    HealthTip("Isolate When Contagious", "Stay home from work or school while symptomatic to protect others, especially with fever or a persistent cough.", TipCategory.HOME, Icons.Outlined.PhoneDisabled, amberDeep),
    HealthTip("Elevate Your Head at Night", "Prop yourself up with an extra pillow when congested — it eases breathing and helps you sleep better.", TipCategory.HOME, Icons.Outlined.Bedtime, blueDeep),
    HealthTip("Keep a Symptom Diary", "Note when symptoms started and how they've changed — it helps your doctor diagnose faster at your next visit.", TipCategory.HOME, Icons.Outlined.EventAvailable, tealDeep),
    HealthTip("Rest Your Voice", "If you're hoarse or losing your voice, avoid whispering or straining to talk — both irritate the vocal cords more than normal speech.", TipCategory.HOME, Icons.Outlined.Sick, amberDeep),

    HealthTip("Take the Stairs", "Small choices add up — a few flights a day builds cardiovascular fitness without needing extra time at the gym.", TipCategory.DAILY, Icons.Outlined.DirectionsRun, tealDeep),
    HealthTip("Limit Added Sugar", "Check food labels — added sugar hides in sauces, cereals, and drinks. Aim for under 25g a day where you can.", TipCategory.DAILY, Icons.Outlined.Restaurant, redDeep),
    HealthTip("Stretch Every Morning", "Five minutes of stretching after waking improves flexibility and can reduce stiffness and joint pain over time.", TipCategory.DAILY, Icons.Outlined.SelfImprovement, blueDeep),
    HealthTip("Get Morning Sunlight", "10–15 minutes of natural light early in the day helps regulate your sleep cycle and mood.", TipCategory.DAILY, Icons.Outlined.WbSunny, amberDeep),
    HealthTip("Floss Daily", "Brushing alone misses up to 40% of tooth surfaces — daily flossing prevents gum disease and cavities.", TipCategory.DAILY, Icons.Outlined.CleanHands, tealDeep),
    HealthTip("Eat Slowly and Mindfully", "It takes about 20 minutes for your brain to register fullness — eating slower helps prevent overeating.", TipCategory.DAILY, Icons.Outlined.Restaurant, blueDeep),
    HealthTip("Take Regular Screen Breaks", "Follow the 20-20-20 rule: every 20 minutes, look at something 20 feet away for 20 seconds to rest your eyes.", TipCategory.DAILY, Icons.Outlined.PhoneDisabled, amberDeep),
    HealthTip("Pack Healthy Snacks", "Keeping fruit or nuts on hand prevents reaching for vending-machine options when hunger strikes.", TipCategory.DAILY, Icons.Outlined.Restaurant, tealDeep),
    HealthTip("Practice Portion Control", "Using a smaller plate is a simple, effective way to naturally reduce portion sizes without feeling deprived.", TipCategory.DAILY, Icons.Outlined.Restaurant, amberDeep),

    HealthTip("Practice Gratitude", "Writing down three things you're grateful for each day is linked to lower stress and better sleep.", TipCategory.MENTAL, Icons.Outlined.FavoriteBorder, tealDeep),
    HealthTip("Set Boundaries", "Saying no to what drains you protects the energy you need for what matters most — it's not selfish, it's necessary.", TipCategory.MENTAL, Icons.Outlined.SelfImprovement, blueDeep),
    HealthTip("Take a Digital Detox Day", "One day a week with minimal screens can meaningfully reduce anxiety and improve focus.", TipCategory.MENTAL, Icons.Outlined.PhoneDisabled, amberDeep),
    HealthTip("Get Outside Daily", "Time in nature, even a short walk, is linked to lower cortisol levels and improved mood.", TipCategory.MENTAL, Icons.Outlined.DirectionsRun, tealDeep),
    HealthTip("Talk to Someone You Trust", "Naming what you're feeling out loud, to a friend or professional, often makes it more manageable.", TipCategory.MENTAL, Icons.Outlined.FavoriteBorder, blueDeep),
    HealthTip("Try the 4-7-8 Breathing Technique", "Inhale for 4 seconds, hold for 7, exhale for 8 — a simple way to calm your nervous system in under a minute.", TipCategory.MENTAL, Icons.Outlined.SelfImprovement, amberDeep),
    HealthTip("Celebrate Small Wins", "Acknowledging progress, however small, builds motivation and resilience over time.", TipCategory.MENTAL, Icons.Outlined.FavoriteBorder, tealDeep),

    HealthTip("Know Your Family Health History", "Understanding conditions that run in your family helps your doctor screen for risks earlier.", TipCategory.PREVENTION, Icons.Outlined.LocalHospital, blueDeep),
    HealthTip("Get Your Blood Pressure Checked", "High blood pressure often has no symptoms — regular checks catch it before it becomes serious.", TipCategory.PREVENTION, Icons.Outlined.Vaccines, redDeep),
    HealthTip("Practice Safe Food Handling", "Wash hands before cooking, keep raw meat separate, and refrigerate leftovers within two hours to avoid foodborne illness.", TipCategory.PREVENTION, Icons.Outlined.CleanHands, tealDeep),
    HealthTip("Stay Up to Date on Dental Checkups", "Twice-yearly visits catch issues early and are far cheaper and less painful than emergency treatment later.", TipCategory.PREVENTION, Icons.Outlined.LocalHospital, amberDeep),
    HealthTip("Wear Your Seatbelt", "It's the single most effective way to reduce serious injury or death in a car accident — every trip, every time.", TipCategory.PREVENTION, Icons.Outlined.WifiTethering, redDeep),
    HealthTip("Know Your Numbers", "Cholesterol and blood sugar levels are key early indicators — ask your doctor for a baseline reading at your next visit.", TipCategory.PREVENTION, Icons.Outlined.Vaccines, blueDeep),
    HealthTip("Protect Your Hearing", "Keep headphone volume below 60% and give your ears breaks during prolonged loud environments.", TipCategory.PREVENTION, Icons.Outlined.WifiTethering, tealDeep)
)

private val warningSigns = listOf(
    "Difficulty breathing or shortness of breath at rest",
    "Persistent chest pain, pressure, or tightness",
    "Dizziness, confusion, or inability to stay awake",
    "Severe muscle pain, weakness, or sudden numbness",
    "Fever that disappears and returns worse than before",
    "Signs of severe dehydration — dark urine, no urination for 8+ hours"
)
/** A stable seed that changes once per calendar day, so "today's" tip and
 * shuffle order are consistent for everyone that day but different tomorrow. */
private fun daySeed(): Long {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
}

private fun tipOfTheDay(): HealthTip {
    val index = (daySeed() % healthTips.size).toInt()
    return healthTips[index]
}

private fun dailyShuffledTips(): List<HealthTip> {
    return healthTips.shuffled(kotlin.random.Random(daySeed()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTipsScreen(
    onBack: () -> Unit,
    onBookConsultation: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TipCategory.ALL) }

    val todayTip = remember { tipOfTheDay() }
// Excludes today's featured tip from the grid below so it isn't shown twice.
    val restOfTips = remember { dailyShuffledTips().filterNot { it == todayTip } }

    val filteredTips = remember(selectedCategory, restOfTips) {
        if (selectedCategory == TipCategory.ALL) restOfTips
        else restOfTips.filter { it.category == selectedCategory }
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
            item { TipOfTheDayCard(todayTip) }

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

            items(filteredTips, key = { it.title }) { tip ->
                Box(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
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
private fun TipOfTheDayCard(tip: HealthTip) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val (iconBg, iconTint) = when (tip.accent) {
        tealDeep -> tealSoft to tealDeep
        amberDeep -> amberSoft to amberDeep
        redDeep -> redSoft to redDeep
        else -> blueSoft to blueDeep
    }

    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = tealSoft),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "TIP OF THE DAY",
                                color = tealDeep,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tip.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tip.title, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(tip.description, color = muted, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
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
}*/

package ehealthy.connect.ui.patientDashboard

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleanHands
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

enum class TipCategory(val label: String) {
    ALL("All Tips"),
    HOME("At-Home Care"),
    DAILY("Daily Habits"),
    MENTAL("Mental Health"),
    PREVENTION("Prevention")
}

data class HealthTip(
    val id: String,
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

private const val TIPS_PREFS_NAME = "health_tips_prefs"
private const val SEEN_TIP_IDS_KEY = "seen_tip_ids"

private val healthTips = listOf(
    HealthTip(
        "rest-recovery",
        "Rest & Recovery",
        "Allow your immune system to fight illness. Aim for 7–9 hours of sleep and avoid overexerting yourself when sick.",
        TipCategory.HOME,
        Icons.Outlined.Bedtime,
        tealDeep
    ),
    HealthTip(
        "stay-hydrated-sick",
        "Stay Hydrated",
        "Drink plenty of fluids — especially water and clear broths — to prevent dehydration and support recovery.",
        TipCategory.HOME,
        Icons.Outlined.WaterDrop,
        blueDeep
    ),
    HealthTip(
        "manage-symptoms",
        "Manage Symptoms",
        "Use OTC medication like paracetamol or ibuprofen for fever and pain. Always follow dosage instructions carefully.",
        TipCategory.HOME,
        Icons.Outlined.Medication,
        amberDeep
    ),
    HealthTip(
        "hand-hygiene",
        "Hand Hygiene",
        "Wash hands frequently with soap and water for at least 20 seconds. Use hand sanitiser when soap isn't available.",
        TipCategory.HOME,
        Icons.Outlined.CleanHands,
        tealDeep
    ),
    HealthTip(
        "cover-coughs",
        "Cover Coughs & Sneezes",
        "Use a tissue or your elbow. Dispose of tissues immediately and wash your hands to prevent spreading germs.",
        TipCategory.HOME,
        Icons.Outlined.Sick,
        blueDeep
    ),
    HealthTip(
        "avoid-smoking",
        "Avoid Smoking",
        "Smoking irritates the respiratory tract and slows recovery. Avoid smoky environments when you're unwell.",
        TipCategory.HOME,
        Icons.Outlined.NoDrinks,
        redDeep
    ),
    HealthTip(
        "eat-nutritiously",
        "Eat Nutritiously",
        "Add vegetables to every meal and swap refined grains for wholemeal options. A balanced diet supports your immune system.",
        TipCategory.DAILY,
        Icons.Outlined.Restaurant,
        tealDeep
    ),
    HealthTip(
        "exercise-regularly",
        "Exercise Regularly",
        "Aim for 150 minutes of moderate movement per week. Walking, swimming, or cycling all count — find what you enjoy.",
        TipCategory.DAILY,
        Icons.AutoMirrored.Outlined.DirectionsRun,
        blueDeep
    ),
    HealthTip(
        "daily-hydration",
        "Daily Hydration",
        "Keep a reusable water bottle handy. Aim for 8–10 glasses of water daily. More if you exercise or it's hot.",
        TipCategory.DAILY,
        Icons.Outlined.WaterDrop,
        amberDeep
    ),
    HealthTip(
        "sleep-schedule",
        "Consistent Sleep Schedule",
        "Go to bed and wake up at the same time daily — even on weekends. Consistency helps regulate your body clock.",
        TipCategory.DAILY,
        Icons.Outlined.EventAvailable,
        tealDeep
    ),
    HealthTip(
        "manage-stress",
        "Manage Stress",
        "Try deep breathing, meditation, or journaling. Even 5 minutes of mindfulness daily can reduce anxiety significantly.",
        TipCategory.MENTAL,
        Icons.Outlined.SelfImprovement,
        blueDeep
    ),
    HealthTip(
        "stay-connected",
        "Stay Connected",
        "Social connection is vital for mental wellbeing. Reach out to friends or family regularly, even just a quick message.",
        TipCategory.MENTAL,
        Icons.Outlined.FavoriteBorder,
        tealDeep
    ),
    HealthTip(
        "limit-screen-time",
        "Limit Screen Time",
        "Especially before bed. Blue light disrupts sleep. Try a 30-minute screen-free wind-down routine each evening.",
        TipCategory.MENTAL,
        Icons.Outlined.PhoneDisabled,
        amberDeep
    ),
    HealthTip(
        "vaccinations",
        "Keep Vaccinations Current",
        "Stay up to date with recommended vaccines for your age group. Prevention is always better than treatment.",
        TipCategory.PREVENTION,
        Icons.Outlined.Vaccines,
        tealDeep
    ),
    HealthTip(
        "regular-checkups",
        "Regular Check-ups",
        "Don't wait until you're sick. Schedule annual health screenings to catch issues early when they're easier to treat.",
        TipCategory.PREVENTION,
        Icons.Outlined.LocalHospital,
        blueDeep
    ),
    HealthTip(
        "sun-protection",
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

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTipsScreen(
    onBack: () -> Unit,
    onBookConsultation: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(TIPS_PREFS_NAME, Context.MODE_PRIVATE) }

    var seenIds by remember {
        mutableStateOf(prefs.getStringSet(SEEN_TIP_IDS_KEY, emptySet())?.toSet() ?: emptySet())
    }

    fun markSeen(tipId: String) {
        if (tipId !in seenIds) {
            val updated = seenIds + tipId
            seenIds = updated
            prefs.edit().putStringSet(SEEN_TIP_IDS_KEY, updated).apply()
        }
    }

    // A new tip featured each day, deterministic from the date — no state to store.
    val today = remember { LocalDate.now() }
    val tipOfTheDay = remember(today) { healthTips[today.dayOfYear % healthTips.size] }

    var selectedCategory by remember { mutableStateOf(TipCategory.ALL) }


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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = navy)
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
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    TipOfTheDayCard(tip = tipOfTheDay, onClick = { markSeen(tipOfTheDay.id) })
                }
            }

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

            item {
                // Crossfades + gently slides the whole tip list whenever the
                // category changes, instead of an abrupt swap.
                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 8 }) togetherWith
                                fadeOut(tween(150))
                    },
                    label = "tipCategoryTransition"
                ) { category ->
                    val tipsForCategory = (
                            if (category == TipCategory.ALL) healthTips
                            else healthTips.filter { it.category == category }
                            ).sortedBy { it.id in seenIds }
                    val allSeen = tipsForCategory.isNotEmpty() && tipsForCategory.all { it.id in seenIds }

                    Column {
                        if (allSeen) {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                                AllCaughtUpBanner()
                            }
                        }
                        tipsForCategory.forEachIndexed { index, tip ->
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                AnimatedTipCard(
                                    tip = tip,
                                    isSeen = tip.id in seenIds,
                                    index = index,
                                    onClick = { markSeen(tip.id) }
                                )
                            }
                        }
                    }
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
private fun TipOfTheDayCard(tip: HealthTip, onClick: () -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488)),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
        ) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "TIP OF THE DAY",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(tip.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        tip.description,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AllCaughtUpBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = tealSoft),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = tealDeep, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "You've read every tip here — a great refresher never hurts though!",
                color = tealDeep,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
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
private fun AnimatedTipCard(tip: HealthTip, isSeen: Boolean, index: Int, onClick: () -> Unit) {
    val visibleState = remember(tip.id) { MutableTransitionState(false) }
    LaunchedEffect(tip.id) {
        kotlinx.coroutines.delay((index * 30L).coerceAtMost(240L).milliseconds)
        visibleState.targetState = true
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(300)) + expandVertically(tween(300))
    ) {
        TipCard(tip = tip, isSeen = isSeen, onClick = onClick)
    }
}

@Composable
private fun TipCard(tip: HealthTip, isSeen: Boolean, onClick: () -> Unit) {
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
            .clickable(onClick = onClick)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tip.title, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                    if (!isSeen) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tealSoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("NEW", color = tealDeep, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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