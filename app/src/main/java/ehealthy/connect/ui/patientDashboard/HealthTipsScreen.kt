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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.MaterialTheme
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

    var selectedCategory by remember {
        mutableStateOf(TipCategory.ALL)
    }

    var tips by remember {
        mutableStateOf<List<ExternalHealthTip>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    var reloadTrigger by remember {
        mutableIntStateOf(0)
    }

    var selectedTip by remember {
        mutableStateOf<ExternalHealthTip?>(null)
    }


    /*
     * Load health tips
     */
    LaunchedEffect(reloadTrigger) {

        isLoading = true
        loadError = null

        fetchCuratedHealthTips()
            .onSuccess {
                tips = it
            }
            .onFailure {
                loadError =
                    it.message
                        ?: "Couldn't load health tips right now."
            }

        isLoading = false
    }


    /*
     * Tip of the day
     */
    val todayTip =
        remember(tips) {

            if (tips.isEmpty()) {

                null

            } else {

                tips[
                    (daySeed() % tips.size)
                        .toInt()
                ]
            }
        }


    /*
     * Remaining tips
     */
    val restTips =
        remember(
            tips,
            todayTip
        ) {

            tips.filterNot {
                it.id == todayTip?.id
            }
        }


    /*
     * Group tips by selected category
     */
    val groupedTips =
        remember(
            restTips,
            selectedCategory
        ) {

            val categories =
                if (
                    selectedCategory ==
                    TipCategory.ALL
                ) {

                    TipCategory.entries
                        .filter {
                            it != TipCategory.ALL
                        }

                } else {

                    listOf(
                        selectedCategory
                    )
                }


            categories.mapNotNull { category ->

                val categoryTips =
                    restTips
                        .filter {
                            it.category == category
                        }
                        .shuffled(
                            Random(
                                daySeed()
                            )
                        )


                if (
                    categoryTips.isEmpty()
                ) {

                    null

                } else {

                    category to categoryTips
                }
            }
        }


    Scaffold(

        /*
         * Top bar
         */
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text =
                                "Health Tips",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                18.sp
                        )


                        Text(
                            text =
                                if (
                                    tips.isEmpty()
                                ) {
                                    "Simple guidance for healthier living"
                                } else {
                                    "${tips.size} wellness tip${
                                        if (tips.size == 1) {
                                            ""
                                        } else {
                                            "s"
                                        }
                                    }"
                                },
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        36.dp
                                    )
                                    .background(
                                        PatientColors.TipsCard,
                                        CircleShape
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription =
                                    "Back",
                                tint =
                                    PatientColors.TipsAccent,
                                modifier =
                                    Modifier.size(
                                        20.dp
                                    )
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface
                        )
            )
        },

        containerColor =
            MaterialTheme
                .colorScheme
                .background

    ) { paddingValues ->


        when {

            /*
             * Loading
             */
            isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        64.dp
                                    )
                                    .background(
                                        PatientColors.TipsCard,
                                        CircleShape
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        30.dp
                                    ),
                                color =
                                    PatientColors.TipsAccent,
                                strokeWidth =
                                    2.5.dp
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )


                        Text(
                            text =
                                "Loading health tips",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                14.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )


                        Text(
                            text =
                                "Preparing wellness guidance for you...",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }
                }
            }


            /*
             * Error
             */
            loadError != null -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                            .padding(
                                24.dp
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                22.dp
                            ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PatientColors.RedSoft
                            ),
                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation =
                                    0.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        22.dp
                                    ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(
                                            62.dp
                                        )
                                        .background(
                                            PatientColors.Red
                                                .copy(
                                                    alpha = 0.10f
                                                ),
                                            CircleShape
                                        ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.LocalHospital,
                                    contentDescription =
                                        null,
                                    tint =
                                        PatientColors.Red,
                                    modifier =
                                        Modifier.size(
                                            29.dp
                                        )
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        13.dp
                                    )
                            )


                            Text(
                                text =
                                    "Couldn't load health tips",
                                color =
                                    PatientColors.TextPrimary,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                fontSize =
                                    15.sp
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        5.dp
                                    )
                            )


                            Text(
                                text =
                                    loadError
                                        ?: "Something went wrong.",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    11.sp,
                                lineHeight =
                                    16.sp,
                                textAlign =
                                    TextAlign.Center
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        14.dp
                                    )
                            )


                            Button(
                                onClick = {
                                    reloadTrigger++
                                },
                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    ),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            PatientColors.TipsAccent,
                                        contentColor =
                                            Color.White
                                    )
                            ) {

                                Text(
                                    text =
                                        "Try again",
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }


            /*
             * Main content
             */
            else -> {

                LazyVerticalGrid(
                    columns =
                        GridCells.Fixed(1),

                    contentPadding =
                        PaddingValues(
                            top = 10.dp,
                            bottom = 24.dp
                        ),

                    modifier =
                        Modifier.padding(
                            paddingValues
                        )
                ) {

                    /*
                     * Wellness intro card
                     */
                    item {

                        Card(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = 20.dp,
                                        vertical = 6.dp
                                    ),

                            shape =
                                RoundedCornerShape(
                                    22.dp
                                ),

                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        PatientColors.TipsCard
                                ),

                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation =
                                        0.dp
                                )
                        ) {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            16.dp
                                        ),
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Box(
                                    modifier =
                                        Modifier
                                            .size(
                                                50.dp
                                            )
                                            .background(
                                                PatientColors.TipsAccent
                                                    .copy(
                                                        alpha = 0.12f
                                                    ),
                                                RoundedCornerShape(
                                                    15.dp
                                                )
                                            ),
                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.LocalHospital,
                                        contentDescription =
                                            null,
                                        tint =
                                            PatientColors.TipsAccent,
                                        modifier =
                                            Modifier.size(
                                                24.dp
                                            )
                                    )
                                }


                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            12.dp
                                        )
                                )


                                Column(
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        )
                                ) {

                                    Text(
                                        text =
                                            "Wellness library",
                                        color =
                                            PatientColors.TextPrimary,
                                        fontWeight =
                                            FontWeight.ExtraBold,
                                        fontSize =
                                            14.sp
                                    )


                                    Spacer(
                                        modifier =
                                            Modifier.height(
                                                2.dp
                                            )
                                    )


                                    Text(
                                        text =
                                            "Explore practical tips for everyday health, prevention and wellbeing.",
                                        color =
                                            PatientColors.TextSecondary,
                                        fontSize =
                                            10.5.sp,
                                        lineHeight =
                                            15.sp
                                    )
                                }
                            }
                        }
                    }


                    /*
                     * Tip of the day
                     */
                    todayTip?.let { tip ->

                        item {

                            TipOfTheDayCard(
                                tip =
                                    tip,
                                onClick = {
                                    selectedTip =
                                        tip
                                }
                            )
                        }
                    }


                    /*
                     * Category filters
                     *
                     * THIS is where the item { ... }
                     * from the previous message belongs.
                     */
                    item {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 8.dp,
                                        bottom = 4.dp
                                    )
                        ) {

                            Text(
                                text =
                                    "Explore by category",
                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            20.dp
                                    ),
                                color =
                                    PatientColors.TextPrimary,
                                fontSize =
                                    12.5.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )


                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(
                                            rememberScrollState()
                                        )
                                        .padding(
                                            horizontal =
                                                20.dp
                                        ),

                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {

                                TipCategory.entries
                                    .forEach { cat ->

                                        val accent =
                                            when (cat) {

                                                TipCategory.ALL ->
                                                    PatientColors.TipsAccent

                                                TipCategory.HOME ->
                                                    PatientColors.DoctorAccent

                                                TipCategory.DAILY ->
                                                    PatientColors.AppointmentAccent

                                                TipCategory.MENTAL ->
                                                    PatientColors.Purple

                                                TipCategory.PREVENTION ->
                                                    PatientColors.Red
                                            }


                                        val background =
                                            when (cat) {

                                                TipCategory.ALL ->
                                                    PatientColors.TipsCard

                                                TipCategory.HOME ->
                                                    PatientColors.DoctorCard

                                                TipCategory.DAILY ->
                                                    PatientColors.AppointmentCard

                                                TipCategory.MENTAL ->
                                                    PatientColors.PurpleSoft

                                                TipCategory.PREVENTION ->
                                                    PatientColors.RedSoft
                                            }


                                        val selected =
                                            selectedCategory ==
                                                    cat


                                        FilterChip(
                                            selected =
                                                selected,

                                            onClick = {
                                                selectedCategory =
                                                    cat
                                            },

                                            label = {

                                                Text(
                                                    text =
                                                        cat.label,
                                                    fontSize =
                                                        11.5.sp,
                                                    fontWeight =
                                                        if (
                                                            selected
                                                        ) {
                                                            FontWeight.Bold
                                                        } else {
                                                            FontWeight.Medium
                                                        }
                                                )
                                            },

                                            shape =
                                                RoundedCornerShape(
                                                    22.dp
                                                ),

                                            colors =
                                                FilterChipDefaults
                                                    .filterChipColors(

                                                        selectedContainerColor =
                                                            accent,

                                                        selectedLabelColor =
                                                            Color.White,

                                                        containerColor =
                                                            background,

                                                        labelColor =
                                                            accent
                                                    ),

                                            border =
                                                null
                                        )
                                    }
                            }
                        }
                    }


                    /*
                     * Tip sections
                     */
                    groupedTips.forEach {
                            (category, categoryTips) ->


                        item {

                            Box(
                                modifier =
                                    Modifier.padding(
                                        horizontal =
                                            20.dp,
                                        vertical =
                                            6.dp
                                    )
                            ) {

                                SectionLabel(
                                    text =
                                        category.label,
                                    accent =
                                        colorForCategory(
                                            category
                                        )
                                )
                            }
                        }


                        items(
                            categoryTips,
                            key = {
                                it.id
                            }
                        ) { tip ->

                            Box(
                                modifier =
                                    Modifier
                                        .animateItem()
                                        .padding(
                                            horizontal =
                                                20.dp,
                                            vertical =
                                                6.dp
                                        )
                            ) {

                                ExternalTipCard(
                                    tip =
                                        tip,
                                    onClick = {
                                        selectedTip =
                                            tip
                                    }
                                )
                            }
                        }
                    }


                    /*
                     * Safety guidance
                     */
                    item {

                        Column(
                            modifier =
                                Modifier.padding(
                                    horizontal =
                                        20.dp
                                )
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        8.dp
                                    )
                            )


                            SectionLabel(
                                text =
                                    "When to seek help",
                                accent =
                                    PatientColors.DoctorAccent
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        12.dp
                                    )
                            )


                            WarningSection()


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        20.dp
                                    )
                            )


                            EmergencyBanner()


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        20.dp
                                    )
                            )


                            NoteBox(
                                onBookConsultation =
                                    onBookConsultation
                            )
                        }
                    }
                }
            }
        }
    }


    /*
     * Tip details dialog
     */
    selectedTip?.let { tip ->

        TipDetailDialog(
            tip =
                tip,
            onDismiss = {
                selectedTip =
                    null
            }
        )
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
private fun TipOfTheDayCard(
    tip: ExternalHealthTip,
    onClick: () -> Unit
) {

    val visibleState =
        remember {
            MutableTransitionState(false)
                .apply {
                    targetState = true
                }
        }

    val context =
        LocalContext.current

    val categoryAccent =
        colorForCategory(
            tip.category
        )


    Box(
        modifier =
            Modifier.padding(
                horizontal = 20.dp,
                vertical = 8.dp
            )
    ) {

        AnimatedVisibility(
            visibleState =
                visibleState,
            enter =
                fadeIn() +
                        slideInVertically(
                            initialOffsetY = {
                                it / 3
                            }
                        )
        ) {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = onClick
                        ),

                shape =
                    RoundedCornerShape(
                        24.dp
                    ),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.TipsCard
                    ),

                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
            ) {

                Column {

                    /*
                     * Tip image
                     */
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(context)
                                    .data(
                                        tip.imageUrl
                                    )
                                    .crossfade(true)
                                    .build(),

                            contentDescription =
                                tip.title,

                            contentScale =
                                ContentScale.Crop,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(
                                        160.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 24.dp,
                                            topEnd = 24.dp
                                        )
                                    )
                        )


                        /*
                         * Tip of the day badge
                         */
                        Box(
                            modifier =
                                Modifier
                                    .align(
                                        Alignment.TopStart
                                    )
                                    .padding(
                                        12.dp
                                    )
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.92f
                                        ),
                                        RoundedCornerShape(
                                            20.dp
                                        )
                                    )
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 6.dp
                                    )
                        ) {

                            Text(
                                text =
                                    "TIP OF THE DAY",
                                color =
                                    PatientColors.TipsAccent,
                                fontSize =
                                    9.5.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing =
                                    0.5.sp
                            )
                        }


                        /*
                         * Category badge
                         */
                        Box(
                            modifier =
                                Modifier
                                    .align(
                                        Alignment.TopEnd
                                    )
                                    .padding(
                                        12.dp
                                    )
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.92f
                                        ),
                                        RoundedCornerShape(
                                            20.dp
                                        )
                                    )
                                    .padding(
                                        horizontal = 9.dp,
                                        vertical = 6.dp
                                    )
                        ) {

                            Text(
                                text =
                                    tip.category.label,
                                color =
                                    categoryAccent,
                                fontSize =
                                    9.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }


                    /*
                     * Tip information
                     */
                    Column(
                        modifier =
                            Modifier.padding(
                                17.dp
                            )
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(
                                            38.dp
                                        )
                                        .background(
                                            PatientColors.TipsAccent
                                                .copy(
                                                    alpha = 0.12f
                                                ),
                                            RoundedCornerShape(
                                                12.dp
                                            )
                                        ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.LocalHospital,
                                    contentDescription =
                                        null,
                                    tint =
                                        PatientColors.TipsAccent,
                                    modifier =
                                        Modifier.size(
                                            19.dp
                                        )
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        10.dp
                                    )
                            )


                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        "Today's wellness tip",
                                    color =
                                        PatientColors.TextSecondary,
                                    fontSize =
                                        9.5.sp,
                                    fontWeight =
                                        FontWeight.SemiBold
                                )


                                Text(
                                    text =
                                        tip.title,
                                    color =
                                        PatientColors.TextPrimary,
                                    fontSize =
                                        15.sp,
                                    fontWeight =
                                        FontWeight.ExtraBold,
                                    lineHeight =
                                        19.sp
                                )
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    10.dp
                                )
                        )


                        Text(
                            text =
                                tip.teaser,
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                11.5.sp,
                            lineHeight =
                                17.sp,
                            maxLines =
                                3
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )


                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Color.White.copy(
                                            alpha = 0.72f
                                        ),
                                        RoundedCornerShape(
                                            14.dp
                                        )
                                    )
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 10.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    "Tap to read the full tip",
                                modifier =
                                    Modifier.weight(1f),
                                color =
                                    PatientColors.TipsAccent,
                                fontSize =
                                    10.5.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )


                            Box(
                                modifier =
                                    Modifier
                                        .size(
                                            7.dp
                                        )
                                        .background(
                                            PatientColors.TipsAccent,
                                            CircleShape
                                        )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalTipCard(
    tip: ExternalHealthTip,
    onClick: () -> Unit
) {

    val context =
        LocalContext.current

    val accent =
        colorForCategory(
            tip.category
        )

    val background =
        when (tip.category) {

            TipCategory.HOME ->
                PatientColors.DoctorCard

            TipCategory.DAILY ->
                PatientColors.AppointmentCard

            TipCategory.MENTAL ->
                PatientColors.PurpleSoft

            TipCategory.PREVENTION ->
                PatientColors.RedSoft

            TipCategory.ALL ->
                PatientColors.TipsCard
        }


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),

        shape =
            RoundedCornerShape(
                18.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        13.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * Tip image
             */
            Box(
                modifier =
                    Modifier
                        .size(
                            72.dp
                        )
                        .clip(
                            RoundedCornerShape(
                                15.dp
                            )
                        )
                        .background(
                            background
                        )
            ) {

                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(context)
                            .data(
                                tip.imageUrl
                            )
                            .crossfade(true)
                            .build(),

                    contentDescription =
                        tip.title,

                    contentScale =
                        ContentScale.Crop,

                    modifier =
                        Modifier.fillMaxSize()
                )


                /*
                 * Category accent
                 */
                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomStart
                            )
                            .padding(
                                6.dp
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.92f
                                ),
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .padding(
                                horizontal = 6.dp,
                                vertical = 3.dp
                            )
                ) {

                    Text(
                        text =
                            tip.category.label,
                        color =
                            accent,
                        fontSize =
                            7.5.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.width(
                        12.dp
                    )
            )


            /*
             * Tip content
             */
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        tip.title,
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        13.5.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    lineHeight =
                        17.sp,
                    maxLines =
                        2
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )


                Text(
                    text =
                        tip.teaser,
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        10.5.sp,
                    lineHeight =
                        15.sp,
                    maxLines =
                        2
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    6.dp
                                )
                                .background(
                                    accent,
                                    CircleShape
                                )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                5.dp
                            )
                    )


                    Text(
                        text =
                            "Read more",
                        color =
                            accent,
                        fontSize =
                            9.5.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TipDetailDialog(
    tip: ExternalHealthTip,
    onDismiss: () -> Unit
) {

    val context =
        LocalContext.current

    val accent =
        colorForCategory(
            tip.category
        )

    val background =
        when (tip.category) {

            TipCategory.HOME ->
                PatientColors.DoctorCard

            TipCategory.DAILY ->
                PatientColors.AppointmentCard

            TipCategory.MENTAL ->
                PatientColors.PurpleSoft

            TipCategory.PREVENTION ->
                PatientColors.RedSoft

            TipCategory.ALL ->
                PatientColors.TipsCard
        }


    Dialog(
        onDismissRequest =
            onDismiss,

        properties =
            DialogProperties(
                usePlatformDefaultWidth =
                    false
            )
    ) {

        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        18.dp
                    )
                    .fillMaxHeight(
                        0.88f
                    ),

            shape =
                RoundedCornerShape(
                    28.dp
                ),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface
                ),

            elevation =
                CardDefaults.cardElevation(
                    defaultElevation =
                        5.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                /*
                 * Hero image
                 */
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    AsyncImage(
                        model =
                            ImageRequest
                                .Builder(context)
                                .data(
                                    tip.imageUrl
                                )
                                .crossfade(true)
                                .build(),

                        contentDescription =
                            tip.title,

                        contentScale =
                            ContentScale.Crop,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    190.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 28.dp,
                                        topEnd = 28.dp
                                    )
                                )
                    )


                    /*
                     * Category badge
                     */
                    Box(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.TopStart
                                )
                                .padding(
                                    13.dp
                                )
                                .background(
                                    Color.White.copy(
                                        alpha = 0.93f
                                    ),
                                    RoundedCornerShape(
                                        18.dp
                                    )
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 6.dp
                                )
                    ) {

                        Text(
                            text =
                                tip.category.label,
                            color =
                                accent,
                            fontSize =
                                9.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    /*
                     * Close button
                     */
                    IconButton(
                        onClick =
                            onDismiss,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.TopEnd
                                )
                                .padding(
                                    10.dp
                                )
                                .background(
                                    Color.Black.copy(
                                        alpha = 0.38f
                                    ),
                                    CircleShape
                                )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Close,
                            contentDescription =
                                "Close",
                            tint =
                                Color.White
                        )
                    }
                }


                /*
                 * Scrollable content
                 */
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .padding(
                                horizontal = 20.dp,
                                vertical = 18.dp
                            )
                ) {

                    /*
                     * Small accent row
                     */
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        38.dp
                                    )
                                    .background(
                                        background,
                                        RoundedCornerShape(
                                            12.dp
                                        )
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.LocalHospital,
                                contentDescription =
                                    null,
                                tint =
                                    accent,
                                modifier =
                                    Modifier.size(
                                        19.dp
                                    )
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.width(
                                    9.dp
                                )
                        )


                        Column {

                            Text(
                                text =
                                    "Health guidance",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    9.5.sp,
                                fontWeight =
                                    FontWeight.SemiBold
                            )


                            Text(
                                text =
                                    tip.category.label,
                                color =
                                    accent,
                                fontSize =
                                    10.5.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    Text(
                        text =
                            tip.title,
                        color =
                            PatientColors.TextPrimary,
                        fontSize =
                            20.sp,
                        fontWeight =
                            FontWeight.ExtraBold,
                        lineHeight =
                            25.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )


                    /*
                     * Summary
                     */
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    background,
                                    RoundedCornerShape(
                                        18.dp
                                    )
                                )
                                .padding(
                                    15.dp
                                )
                    ) {

                        Text(
                            text =
                                "What you should know",
                            color =
                                accent,
                            fontSize =
                                10.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    7.dp
                                )
                        )


                        Text(
                            text =
                                tip.fullSummary,
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                12.sp,
                            lineHeight =
                                19.sp
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                14.dp
                            )
                    )


                    /*
                     * General disclaimer
                     */
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.NeutralCard,
                                    RoundedCornerShape(
                                        15.dp
                                    )
                                )
                                .padding(
                                    12.dp
                                ),
                        verticalAlignment =
                            Alignment.Top
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Info,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.TextSecondary,
                            modifier =
                                Modifier.size(
                                    17.dp
                                )
                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        Text(
                            text =
                                "Health tips are general guidance and do not replace professional medical advice.",
                            modifier =
                                Modifier.weight(1f),
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.sp,
                            lineHeight =
                                14.sp
                        )
                    }
                }


                /*
                 * Bottom actions
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 20.dp,
                                vertical = 15.dp
                            ),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            9.dp
                        )
                ) {

                    OutlinedButton(
                        onClick =
                            onDismiss,

                        modifier =
                            Modifier
                                .weight(1f)
                                .height(
                                    50.dp
                                ),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            )
                    ) {

                        Text(
                            text =
                                "Close",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    tip.sourceUrl
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { url ->

                            Button(
                                onClick = {

                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            url.toUri()
                                        )
                                    )
                                },

                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(
                                            50.dp
                                        ),

                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    ),

                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            accent,
                                        contentColor =
                                            Color.White
                                    )
                            ) {

                                Text(
                                    text =
                                        "Learn more",
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun WarningSection() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.SuccessCard
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                44.dp
                            )
                            .background(
                                PatientColors.SuccessAccent
                                    .copy(alpha = 0.12f),
                                RoundedCornerShape(
                                    14.dp
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.LocalHospital,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.SuccessAccent,
                        modifier =
                            Modifier.size(
                                21.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Know the warning signs",
                        color =
                            PatientColors.TextPrimary,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            14.sp
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )


                    Text(
                        text =
                            "Seek medical care if you notice serious symptoms.",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp,
                        lineHeight =
                            15.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )


            warningSigns.forEachIndexed { index, sign ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 5.dp
                            ),
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    25.dp
                                )
                                .background(
                                    PatientColors.SuccessAccent
                                        .copy(alpha = 0.10f),
                                    CircleShape
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "${index + 1}",
                            color =
                                PatientColors.SuccessAccent,
                            fontSize =
                                9.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )


                    Text(
                        text =
                            sign,
                        modifier =
                            Modifier.weight(1f),
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp,
                        lineHeight =
                            15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyBanner() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.Red
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                46.dp
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.14f
                                ),
                                RoundedCornerShape(
                                    14.dp
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.WifiTethering,
                        contentDescription =
                            null,
                        tint =
                            Color.White,
                        modifier =
                            Modifier.size(
                                22.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Life-threatening emergency?",
                        color =
                            Color.White,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                2.dp
                            )
                    )


                    Text(
                        text =
                            "Don't wait — contact emergency services immediately.",
                        color =
                            Color.White.copy(
                                alpha = 0.78f
                            ),
                        fontSize =
                            10.5.sp,
                        lineHeight =
                            15.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(
                                alpha = 0.10f
                            ),
                            RoundedCornerShape(
                                16.dp
                            )
                        )
                        .padding(
                            vertical = 14.dp,
                            horizontal = 6.dp
                        ),

                horizontalArrangement =
                    Arrangement.SpaceEvenly
            ) {

                EmergencyNumber(
                    number =
                        "112",
                    label =
                        "Emergency\nfrom mobile"
                )


                EmergencyNumber(
                    number =
                        "10177",
                    label =
                        "Ambulance /\nFire"
                )


                EmergencyNumber(
                    number =
                        "10111",
                    label =
                        "Police"
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Text(
                text =
                    "Use emergency services for urgent or life-threatening situations.",
                modifier =
                    Modifier.fillMaxWidth(),
                color =
                    Color.White.copy(
                        alpha = 0.68f
                    ),
                fontSize =
                    9.5.sp,
                lineHeight =
                    13.sp,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmergencyNumber(
    number: String,
    label: String
) {

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier =
                Modifier
                    .background(
                        Color.White.copy(
                            alpha = 0.12f
                        ),
                        RoundedCornerShape(
                            12.dp
                        )
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 6.dp
                    )
        ) {

            Text(
                text =
                    number,
                color =
                    Color.White,
                fontSize =
                    17.sp,
                fontWeight =
                    FontWeight.Black
            )
        }


        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )


        Text(
            text =
                label,
            color =
                Color.White.copy(
                    alpha = 0.74f
                ),
            fontSize =
                8.5.sp,
            textAlign =
                TextAlign.Center,
            lineHeight =
                11.sp
        )
    }
}

@Composable
private fun NoteBox(
    onBookConsultation: () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                22.dp
            ),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.DoctorCard
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    17.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                44.dp
                            )
                            .background(
                                PatientColors.DoctorAccent
                                    .copy(alpha = 0.11f),
                                RoundedCornerShape(
                                    14.dp
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.LocalHospital,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.DoctorAccent,
                        modifier =
                            Modifier.size(
                                21.dp
                            )
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            11.dp
                        )
                )


                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Need professional advice?",
                        color =
                            PatientColors.TextPrimary,
                        fontSize =
                            14.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Text(
                        text =
                            "Speak to a healthcare professional",
                        color =
                            PatientColors.DoctorAccent,
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        13.dp
                    )
            )


            Text(
                text =
                    "If symptoms are severe, you are in a high-risk group, or your symptoms are not improving, consider speaking to a healthcare professional.",
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    10.5.sp,
                lineHeight =
                    16.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            Button(
                onClick =
                    onBookConsultation,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            52.dp
                        ),

                shape =
                    RoundedCornerShape(
                        15.dp
                    ),

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            PatientColors.DoctorAccent,
                        contentColor =
                            Color.White
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.LocalHospital,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            18.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            7.dp
                        )
                )


                Text(
                    text =
                        "Book a consultation",
                    fontWeight =
                        FontWeight.Bold,
                    fontSize =
                        13.sp
                )
            }
        }
    }
}