package ehealthy.connect.ui.patientDashboard

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.Calendar
import kotlin.random.Random
import androidx.compose.material3.MaterialTheme
enum class TipCategory(val label: String) {
    ALL("All Tips"),
    HOME("General Health"),
    DAILY("Daily Habits"),
    MENTAL("Mental Health"),
    PREVENTION("Prevention")
}

// Palette
private val amberDeep = Color(0xFFD97706)
private val redDeep = Color(0xFFDC2626)
private val blueDeep = Color(0xFF2563EB)

@Composable
private fun colorForCategory(category: TipCategory): Color = when (category) {
    TipCategory.HOME -> MaterialTheme.colorScheme.primary
    TipCategory.DAILY -> blueDeep
    TipCategory.MENTAL -> amberDeep
    TipCategory.PREVENTION -> redDeep
    TipCategory.ALL -> MaterialTheme.colorScheme.primary
}

private val warningSigns = listOf(
    "Difficulty breathing or shortness of breath at rest",
    "Persistent chest pain, pressure, or tightness",
    "Dizziness, confusion, or inability to stay awake",
    "Severe muscle pain, weakness, or sudden numbness",
    "Fever that disappears and returns worse than before",
    "Signs of severe dehydration — dark urine, no urination for 8+ hours"
)

/** Changes once per calendar day, so "today's" featured tip is the same for
 * everyone on a given day but different tomorrow. */
private fun daySeed(): Long {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.YEAR) * 1000L + cal.get(Calendar.DAY_OF_YEAR)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTipsScreen(
    onBack: () -> Unit,
    onBookConsultation: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TipCategory.ALL) }
    var tips by remember { mutableStateOf<List<ExternalHealthTip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadTrigger) {
        isLoading = true
        loadError = null
        fetchCuratedHealthTips()
            .onSuccess { tips = it }
            .onFailure { loadError = it.message ?: "Couldn't load health tips right now." }
        isLoading = false
    }

    val todayTip = remember(tips) {
        if (tips.isEmpty()) null else tips[(daySeed() % tips.size).toInt()]
    }
    val restTips = remember(tips, todayTip) { tips.filterNot { it.id == todayTip?.id } }

    val groupedTips = remember(restTips, selectedCategory) {
        val categories = if (selectedCategory == TipCategory.ALL) {
            TipCategory.entries.filter { it != TipCategory.ALL }
        } else {
            listOf(selectedCategory)
        }
        categories.mapNotNull { category ->
            val items = restTips.filter { it.category == category }.shuffled(Random(daySeed()))
            if (items.isEmpty()) null else category to items
        }
    }

    var selectedTip by remember { mutableStateOf<ExternalHealthTip?>(null) }
    val navy = MaterialTheme.colorScheme.primary
    val tealDeep = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val bg = MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Health Tips", color = navy, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = navy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = bg
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = tealDeep) }
            }

            loadError != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(loadError ?: "", color = muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { reloadTrigger++ }, colors = ButtonDefaults.buttonColors(containerColor = navy)) {
                            Text("Try Again", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    modifier = Modifier.padding(paddingValues)
                ) {
                    todayTip?.let { tip ->
                        item { TipOfTheDayCard(tip, onClick = { selectedTip = tip }) }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TipCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = navy,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = muted
                                    ),
                                    border = null
                                )
                            }
                        }
                    }

                    groupedTips.forEach { (category, items) ->
                        item {
                            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                SectionLabel(category.label, colorForCategory(category))
                            }
                        }
                        items(items, key = { it.id }) { tip ->
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                ExternalTipCard(tip, onClick = { selectedTip = tip })
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            SectionLabel("When to seek help", tealDeep)
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
    }

    selectedTip?.let { tip ->
        TipDetailDialog(tip, onDismiss = { selectedTip = null })
    }
}

@Composable
private fun SectionLabel(text: String, accent: Color) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
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
private fun TipOfTheDayCard(tip: ExternalHealthTip, onClick: () -> Unit) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    val context = LocalContext.current
    val tealSoft = MaterialTheme.colorScheme.primaryContainer
    val tealDeep = MaterialTheme.colorScheme.primary
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = tealSoft),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            ) {
                Column {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(tip.imageUrl).crossfade(true).build(),
                        contentDescription = tip.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    )
                    Column(modifier = Modifier.padding(18.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.7f))
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
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(tip.title, color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(tip.teaser, color = muted, fontSize = 13.sp, lineHeight = 19.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalTipCard(tip: ExternalHealthTip, onClick: () -> Unit) {
    val context = LocalContext.current
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0x1A0B1828))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(tip.imageUrl).crossfade(true).build(),
                contentDescription = tip.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(tip.title, color = ink, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(tip.teaser, color = muted, fontSize = 12.5.sp, lineHeight = 17.sp, maxLines = 2)
            }
        }
    }
}

@Composable
private fun TipDetailDialog(tip: ExternalHealthTip, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val ink = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tealDeep = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(tip.imageUrl).crossfade(true).build(),
                        contentDescription = tip.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(22.dp)
                ) {
                    Text(tip.title, color = ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(tip.fullSummary, color = muted, fontSize = 13.5.sp, lineHeight = 20.sp)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Close", color = ink) }

                    tip.sourceUrl?.let { url ->
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = tealDeep),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("For More Info", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold) }
                    }
                }
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
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.LocalHospital, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(18.dp))
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
                        modifier = Modifier.padding(top = 7.dp).size(5.dp).clip(CircleShape).background(Color(0xFF6EE7B7))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(sign, color = Color.White.copy(alpha = 0.88f), fontSize = 13.5.sp, lineHeight = 19.sp)
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
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.WifiTethering, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Life-threatening emergency?", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Don't wait — call for help immediately.", color = Color.White.copy(alpha = 0.75f), fontSize = 12.5.sp)
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
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
private fun NoteBox(onBookConsultation: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tealDeep = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = tealDeep),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Book a Consultation", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}