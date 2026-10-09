package ehealthy.connect.ui.patientDashboard.FindDoctors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import ehealthy.connect.ui.patientDashboard.PatientColors
import ehealthy.connect.ui.patientDashboard.patientPressAnimation
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.ml.DoctorRecommendation
import ehealthy.connect.ml.DoctorRecommendationEngine
import androidx.compose.material3.Card
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
/**
 * This model intentionally keeps the Supabase database column names
 * because MainActivity currently decodes directly into DoctorListing.
 *
 * We can move this completely into PatientRepository later when we
 * clean MainActivity.
 */
@Suppress("PropertyName")
@Serializable
data class DoctorListing(
    val id: String,

    val name: String,

    val surname: String,

    val discipline: String? = null,

    val phone: String? = null,

    val hourly_rate: Double? = null,

    val profile_image_url: String? = null,

    val operating_hours: String? = null,

    val city: String? = null,

    val province: String? = null,

    val years_of_experience: Int? = null,

    val qualifications: String? = null,

    val language: String? = null,

    val verification_status: String? = null,

    val is_deactivated: Boolean? = false
) {

    val fullName: String
        get() = "Dr. $name $surname"

    val location: String
        get() =
            listOfNotNull(
                city?.takeIf { it.isNotBlank() },
                province?.takeIf { it.isNotBlank() }
            )
                .joinToString(", ")
                .ifBlank {
                    "Location not provided"
                }

    val specialty: String
        get() =
            discipline
                ?.takeIf { it.isNotBlank() }
                ?: "General Practitioner"
}

private enum class DoctorSort(
    val label: String
) {

    RECOMMENDED(
        "Recommended"
    ),

    PRICE_LOW(
        "Price: Low to High"
    ),

    PRICE_HIGH(
        "Price: High to Low"
    ),

    NAME(
        "Doctor name"
    )
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun FindDoctorsScreen(
    onBack: () -> Unit,
    onSelectDoctor: (DoctorListing) -> Unit,
    onViewProfile: (DoctorListing) -> Unit,
    fetchDoctors: suspend () -> Result<List<DoctorListing>>
) {

    val scope =
        rememberCoroutineScope()

    var doctors by remember {
        mutableStateOf<List<DoctorListing>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var selectedSpecialty by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var selectedSort by remember {
        mutableStateOf(
            DoctorSort.RECOMMENDED
        )
    }

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }

    var recommendation by remember {
        mutableStateOf<DoctorRecommendation?>(
            null
        )
    }

    var recommendationLoading by remember {
        mutableStateOf(true)
    }

    var questionnaireHasSymptoms by remember {
        mutableStateOf(false)
    }

    var showRecommendedOnly by remember {
        mutableStateOf(false)
    }

    var preferredLanguage by remember {
        mutableStateOf<String?>(
            null
        )
    }


    suspend fun loadRecommendation() {

        recommendationLoading =
            true

        PatientRepository
            .getMyHealthProfile()
            .onSuccess { profile ->

                questionnaireHasSymptoms =
                    !profile.current_symptoms
                        .isNullOrBlank()

                preferredLanguage =
                    profile.preferred_language

                recommendation =
                    DoctorRecommendationEngine
                        .recommend(
                            profile
                        )
            }
            .onFailure {

                questionnaireHasSymptoms =
                    false

                recommendation =
                    null
            }

        recommendationLoading =
            false
    }


    suspend fun loadDoctors(
        refreshing: Boolean = false
    ) {

        if (refreshing) {

            isRefreshing = true

        } else {

            isLoading = true
        }

        errorMessage = null

        fetchDoctors()
            .onSuccess { result ->

                doctors =
                    result.filter {
                        it.is_deactivated != true &&
                                it.verification_status
                                    .equals(
                                        "approved",
                                        ignoreCase = true
                                    )
                    }
            }
            .onFailure { error ->

                errorMessage =
                    error.message
                        ?: "We couldn't load doctors right now."
            }

        isLoading = false

        isRefreshing = false
    }

    LaunchedEffect(Unit) {

        loadDoctors()

        loadRecommendation()
    }

    val specialties =
        remember(doctors) {

            doctors
                .map {
                    it.specialty
                }
                .distinct()
                .sorted()
        }

    val recommendedMatchCount =
        remember(
            doctors,
            recommendation
        ) {

            recommendation
                ?.let { result ->

                    doctors.count {
                            doctor ->

                        DoctorRecommendationEngine
                            .specialtyMatches(
                                doctorSpecialty =
                                    doctor.specialty,
                                recommendedSpecialty =
                                    result.specialty
                            )
                    }
                }
                ?: 0
        }


    val visibleDoctors =
        remember(
            doctors,
            searchQuery,
            selectedSpecialty,
            selectedSort,
            recommendation,
            showRecommendedOnly,
            preferredLanguage
        ) {

            val query =
                searchQuery
                    .trim()
                    .lowercase()

            val filtered =
                doctors.filter { doctor ->

                    val matchesSearch =

                        query.isBlank() ||

                                doctor.fullName
                                    .lowercase()
                                    .contains(
                                        query
                                    ) ||

                                doctor.specialty
                                    .lowercase()
                                    .contains(
                                        query
                                    ) ||

                                doctor.location
                                    .lowercase()
                                    .contains(
                                        query
                                    ) ||

                                doctor.language
                                    .orEmpty()
                                    .lowercase()
                                    .contains(
                                        query
                                    )

                    val matchesSpecialty =

                        selectedSpecialty == null ||

                                doctor.specialty ==
                                selectedSpecialty


                    val matchesRecommendation =

                        !showRecommendedOnly ||

                                recommendation
                                    ?.let { result ->

                                        DoctorRecommendationEngine
                                            .specialtyMatches(
                                                doctorSpecialty =
                                                    doctor.specialty,
                                                recommendedSpecialty =
                                                    result.specialty
                                            )
                                    }
                                ?: true


                    matchesSearch &&
                            matchesSpecialty &&
                            matchesRecommendation
                }


            when (
                selectedSort
            ) {

                DoctorSort.RECOMMENDED ->

                    filtered.sortedWith(

                        compareByDescending<DoctorListing> { doctor ->

                            recommendation
                                ?.let { result ->

                                    DoctorRecommendationEngine
                                        .specialtyMatches(
                                            doctorSpecialty =
                                                doctor.specialty,
                                            recommendedSpecialty =
                                                result.specialty
                                        )
                                }
                                ?: false
                        }
                            .thenByDescending { doctor ->

                                DoctorRecommendationEngine
                                    .languageMatches(
                                        doctorLanguages =
                                            doctor.language,
                                        preferredLanguage =
                                            preferredLanguage
                                    )
                            }
                            .thenByDescending {
                                it.years_of_experience
                                    ?: 0
                            }
                            .thenBy {
                                it.fullName
                            }
                    )


                DoctorSort.PRICE_LOW ->

                    filtered.sortedWith(

                        compareBy<DoctorListing> {
                            it.hourly_rate
                                ?: Double.MAX_VALUE
                        }
                            .thenBy {
                                it.fullName
                            }
                    )


                DoctorSort.PRICE_HIGH ->

                    filtered.sortedWith(

                        compareByDescending<DoctorListing> {
                            it.hourly_rate
                                ?: 0.0
                        }
                            .thenBy {
                                it.fullName
                            }
                    )


                DoctorSort.NAME ->

                    filtered.sortedBy {
                        it.fullName
                    }
            }
        }

    Scaffold(

        topBar = {

            TopAppBar(

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.ArrowBack,
                            contentDescription =
                                "Back"
                        )
                    }
                },

                title = {

                    Column {

                        Text(
                            text =
                                "Find your doctor",
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Choose the care that suits you",
                            fontSize =
                                11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }
                },

                actions = {

                    IconButton(

                        enabled =
                            !isRefreshing,

                        onClick = {

                            scope.launch {

                                loadDoctors(
                                    refreshing = true
                                )
                            }
                        }

                    ) {

                        if (
                            isRefreshing
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        22.dp
                                    ),
                                strokeWidth =
                                    2.dp
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Filled.Refresh,
                                contentDescription =
                                    "Refresh doctors"
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
        }

    ) { innerPadding ->

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .background
                    )
                    .padding(
                        innerPadding
                    )
        ) {

            DoctorDiscoveryHeader(
                doctorCount =
                    visibleDoctors.size
            )

            DoctorRecommendationCard(
                recommendation =
                    recommendation,
                isLoading =
                    recommendationLoading,
                questionnaireHasSymptoms =
                    questionnaireHasSymptoms,
                matchingDoctorCount =
                    recommendedMatchCount,
                showingRecommendedOnly =
                    showRecommendedOnly,
                onShowRecommended = {

                    selectedSpecialty =
                        null

                    searchQuery =
                        ""

                    selectedSort =
                        DoctorSort.RECOMMENDED

                    showRecommendedOnly =
                        true
                },
                onShowAll = {

                    showRecommendedOnly =
                        false
                }
            )

            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 16.dp
                    )
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                OutlinedTextField(

                    value =
                        searchQuery,

                    onValueChange = {
                        searchQuery = it
                    },

                    modifier =
                        Modifier.fillMaxWidth(),

                    placeholder = {

                        Text(
                            "Doctor, specialty, location..."
                        )
                    },

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Outlined.Search,
                            contentDescription =
                                null
                        )
                    },

                    singleLine =
                        true,

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    colors =
                        OutlinedTextFieldDefaults.colors(

                            focusedBorderColor =
                                PatientColors.DoctorAccent,

                            unfocusedBorderColor =
                                PatientColors.DoctorAccent
                                    .copy(
                                        alpha = 0.16f
                                    ),

                            focusedContainerColor =
                                PatientColors.DoctorCard
                                    .copy(
                                        alpha = 0.55f
                                    ),

                            unfocusedContainerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface,

                            focusedLeadingIconColor =
                                PatientColors.DoctorAccent,

                            cursorColor =
                                PatientColors.DoctorAccent
                        )
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
                            .fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement
                            .SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "Specialties",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            15.sp
                    )

                    Box {

                        TextButton(
                            onClick = {
                                sortMenuExpanded =
                                    true
                            }
                        ) {

                            Text(
                                text =
                                    selectedSort.label
                            )
                        }

                        DropdownMenu(

                            expanded =
                                sortMenuExpanded,

                            onDismissRequest = {
                                sortMenuExpanded =
                                    false
                            }

                        ) {

                            DoctorSort
                                .entries
                                .forEach { sort ->

                                    DropdownMenuItem(

                                        text = {

                                            Text(
                                                sort.label
                                            )
                                        },

                                        onClick = {

                                            selectedSort =
                                                sort

                                            sortMenuExpanded =
                                                false
                                        }
                                    )
                                }
                        }
                    }
                }
            }

            SpecialtyFilters(
                specialties =
                    specialties,
                selectedSpecialty =
                    selectedSpecialty,
                onSpecialtySelected = {
                    selectedSpecialty = it
                }
            )

            AnimatedVisibility(
                visible =
                    selectedSpecialty != null ||
                            searchQuery.isNotBlank() ||
                            showRecommendedOnly
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal =
                                    16.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "${visibleDoctors.size} result${
                                if (
                                    visibleDoctors.size ==
                                    1
                                ) {
                                    ""
                                } else {
                                    "s"
                                }
                            }",
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        fontWeight =
                            FontWeight.SemiBold,
                        fontSize =
                            13.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    TextButton(
                        onClick = {

                            searchQuery = ""

                            selectedSpecialty =
                                null

                            selectedSort =
                                DoctorSort
                                    .RECOMMENDED

                            showRecommendedOnly =
                                false
                        }
                    ) {

                        Text(
                            "Clear filters"
                        )
                    }
                }
            }

            when {

                isLoading -> {

                    LoadingDoctors()
                }

                errorMessage != null -> {

                    ErrorDoctors(
                        message =
                            errorMessage
                                ?: "Unable to load doctors.",
                        onRetry = {

                            scope.launch {

                                loadDoctors()
                            }
                        }
                    )
                }

                visibleDoctors
                    .isEmpty() -> {

                    EmptyDoctors(
                        hasFilters =
                            searchQuery
                                .isNotBlank() ||
                                    selectedSpecialty != null ||
                                    showRecommendedOnly,

                        onClear = {

                            searchQuery = ""

                            selectedSpecialty =
                                null

                            selectedSort =
                                DoctorSort
                                    .RECOMMENDED

                            showRecommendedOnly =
                                false
                        }
                    )
                }

                else -> {

                    LazyColumn(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentPadding =
                            PaddingValues(
                                start =
                                    16.dp,
                                end =
                                    16.dp,
                                top =
                                    8.dp,
                                bottom =
                                    28.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                14.dp
                            )

                    ) {

                        item {

                            Text(
                                text =
                                    if (
                                        showRecommendedOnly
                                    ) {
                                        "Recommended doctors"
                                    } else {
                                        "Available doctors"
                                    },
                                fontSize =
                                    17.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }

                        items(
                            items =
                                visibleDoctors,
                            key = {
                                it.id
                            }
                        ) { doctor ->

                            ModernDoctorCard(

                                doctor =
                                    doctor,

                                onProfile = {
                                    onViewProfile(
                                        doctor
                                    )
                                },

                                onBook = {
                                    onSelectDoctor(
                                        doctor
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorDiscoveryHeader(
    doctorCount: Int
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp
                )
                .clip(
                    RoundedCornerShape(
                        28.dp
                    )
                )
                .background(
                    PatientColors
                        .ConsultationGradient
                )
                .padding(
                    22.dp
                )
    ) {

        /*
         * Decorative circle
         */
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.TopEnd
                    )
                    .size(
                        105.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        Color.White.copy(
                            alpha = 0.08f
                        )
                    )
        )


        Column(
            modifier =
                Modifier.fillMaxWidth(
                    0.82f
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
                                34.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    11.dp
                                )
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.16f
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Search,
                        contentDescription =
                            null,
                        tint =
                            Color.White,
                        modifier =
                            Modifier.size(
                                18.dp
                            )
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
                        "FIND CARE",
                    color =
                        Color.White.copy(
                            alpha = 0.90f
                        ),
                    fontSize =
                        10.sp,
                    fontWeight =
                        FontWeight.Bold,
                    letterSpacing =
                        0.8.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        15.dp
                    )
            )


            Text(
                text =
                    "Find the right\ndoctor for you",
                color =
                    Color.White,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    24.sp,
                lineHeight =
                    28.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            Text(
                text =
                    "Browse approved healthcare professionals, compare their services and choose your preferred doctor.",
                color =
                    Color.White.copy(
                        alpha = 0.84f
                    ),
                fontSize =
                    12.5.sp,
                lineHeight =
                    18.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            Row(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                20.dp
                            )
                        )
                        .background(
                            Color.White.copy(
                                alpha = 0.15f
                            )
                        )
                        .padding(
                            horizontal = 11.dp,
                            vertical = 7.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Verified,
                    contentDescription =
                        null,
                    tint =
                        Color.White,
                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )


                Spacer(
                    modifier =
                        Modifier.width(
                            6.dp
                        )
                )


                Text(
                    text =
                        "$doctorCount approved doctor${
                            if (doctorCount == 1) {
                                ""
                            } else {
                                "s"
                            }
                        }",
                    color =
                        Color.White,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DoctorRecommendationCard(
    recommendation: DoctorRecommendation?,
    isLoading: Boolean,
    questionnaireHasSymptoms: Boolean,
    matchingDoctorCount: Int,
    showingRecommendedOnly: Boolean,
    onShowRecommended: () -> Unit,
    onShowAll: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal =
                        16.dp,
                    vertical =
                        10.dp
                ),
        shape =
            RoundedCornerShape(
                22.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors
                        .DoctorCard
            ),
        border =
            BorderStroke(
                1.dp,
                PatientColors
                    .DoctorAccent
                    .copy(
                        alpha =
                            0.14f
                    )
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
                                42.dp
                            )
                            .clip(
                                RoundedCornerShape(
                                    13.dp
                                )
                            )
                            .background(
                                PatientColors
                                    .DoctorAccent
                                    .copy(
                                        alpha =
                                            0.12f
                                    )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.HealthAndSafety,
                        contentDescription =
                            null,
                        tint =
                            PatientColors
                                .DoctorAccent,
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
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            "Smart doctor recommendation",
                        color =
                            PatientColors
                                .TextPrimary,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            14.sp
                    )

                    Text(
                        text =
                            "Decision Tree classification from your health questionnaire",
                        color =
                            PatientColors
                                .TextSecondary,
                        fontSize =
                            10.5.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            when {

                isLoading -> {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    18.dp
                                ),
                            strokeWidth =
                                2.dp,
                            color =
                                PatientColors
                                    .DoctorAccent
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )

                        Text(
                            text =
                                "Analysing your questionnaire...",
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                11.5.sp
                        )
                    }
                }


                !questionnaireHasSymptoms -> {

                    Text(
                        text =
                            "Add your current symptoms in Settings → Health profile to receive a personalised specialty recommendation.",
                        color =
                            PatientColors
                                .TextSecondary,
                        fontSize =
                            11.5.sp,
                        lineHeight =
                            16.sp
                    )
                }


                recommendation != null -> {

                    Text(
                        text =
                            recommendation.specialty,
                        color =
                            PatientColors
                                .DoctorAccent,
                        fontSize =
                            19.sp,
                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )


                    Text(
                        text =
                            "Model confidence: ${
                                (
                                        recommendation.confidence *
                                                100.0
                                        )
                                    .toInt()
                            }%",
                        color =
                            PatientColors
                                .TextSecondary,
                        fontSize =
                            10.5.sp,
                        fontWeight =
                            FontWeight.SemiBold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )


                    Text(
                        text =
                            recommendation.reason,
                        color =
                            PatientColors
                                .TextPrimary,
                        fontSize =
                            11.5.sp,
                        lineHeight =
                            16.sp
                    )


                    if (
                        recommendation
                            .matchedSignals
                            .isNotEmpty()
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    7.dp
                                )
                        )


                        Text(
                            text =
                                "Questionnaire signals: ${
                                    recommendation
                                        .matchedSignals
                                        .joinToString(
                                            ", "
                                        )
                                }",
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                10.sp,
                            lineHeight =
                                14.sp
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                11.dp
                            )
                    )


                    if (
                        matchingDoctorCount >
                        0
                    ) {

                        if (
                            showingRecommendedOnly
                        ) {

                            OutlinedButton(
                                onClick =
                                    onShowAll,
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    )
                            ) {

                                Text(
                                    "Show all approved doctors"
                                )
                            }

                        } else {

                            Button(
                                onClick =
                                    onShowRecommended,
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(
                                        14.dp
                                    ),
                                colors =
                                    ButtonDefaults
                                        .buttonColors(
                                            containerColor =
                                                PatientColors
                                                    .DoctorAccent
                                        )
                            ) {

                                Text(
                                    text =
                                        "Show $matchingDoctorCount recommended doctor${
                                            if (
                                                matchingDoctorCount ==
                                                1
                                            ) {
                                                ""
                                            } else {
                                                "s"
                                            }
                                        }",
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }

                    } else {

                        Text(
                            text =
                                "No exact approved specialist match is currently listed. You can still browse all approved doctors below.",
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                10.5.sp,
                            lineHeight =
                                15.sp
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .background(
                            Color.White
                                .copy(
                                    alpha =
                                        0.58f
                                )
                        )
                        .padding(
                            10.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Verified,
                    contentDescription =
                        null,
                    tint =
                        PatientColors
                            .DoctorAccent,
                    modifier =
                        Modifier.size(
                            15.dp
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
                        "This feature routes you to a healthcare specialty. It is not a medical diagnosis.",
                    color =
                        PatientColors
                            .TextSecondary,
                    fontSize =
                        9.8.sp,
                    lineHeight =
                        13.sp
                )
            }
        }
    }
}


@Composable
private fun SpecialtyFilters(
    specialties: List<String>,
    selectedSpecialty: String?,
    onSpecialtySelected: (String?) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 10.dp
                ),
        horizontalArrangement =
            Arrangement.spacedBy(
                9.dp
            )
    ) {

        FilterChip(
            selected =
                selectedSpecialty == null,

            onClick = {
                onSpecialtySelected(
                    null
                )
            },

            label = {

                Text(
                    text = "All",
                    fontWeight =
                        if (
                            selectedSpecialty == null
                        ) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                )
            },

            leadingIcon = {

                Icon(
                    imageVector =
                        Icons.Outlined.HealthAndSafety,
                    contentDescription =
                        null,
                    modifier =
                        Modifier.size(
                            16.dp
                        )
                )
            },

            colors =
                FilterChipDefaults
                    .filterChipColors(

                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surface,

                        labelColor =
                            PatientColors
                                .TextSecondary,

                        iconColor =
                            PatientColors
                                .TextSecondary,

                        selectedContainerColor =
                            PatientColors
                                .DoctorAccent,

                        selectedLabelColor =
                            Color.White,

                        selectedLeadingIconColor =
                            Color.White
                    ),

            shape =
                RoundedCornerShape(
                    18.dp
                )
        )


        specialties.forEach { specialty ->

            val selected =
                selectedSpecialty ==
                        specialty


            FilterChip(
                selected =
                    selected,

                onClick = {

                    onSpecialtySelected(
                        if (selected) {
                            null
                        } else {
                            specialty
                        }
                    )
                },

                label = {

                    Text(
                        text =
                            specialty,
                        fontWeight =
                            if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                        maxLines = 1
                    )
                },

                colors =
                    FilterChipDefaults
                        .filterChipColors(

                            containerColor =
                                PatientColors
                                    .DoctorCard,

                            labelColor =
                                PatientColors
                                    .DoctorAccent,

                            selectedContainerColor =
                                PatientColors
                                    .DoctorAccent,

                            selectedLabelColor =
                                Color.White
                        ),

                shape =
                    RoundedCornerShape(
                        18.dp
                    )
            )
        }
    }
}

@Composable
private fun ModernDoctorCard(
    doctor: DoctorListing,
    onProfile: () -> Unit,
    onBook: () -> Unit
) {

    val interactionSource =
        remember {
            MutableInteractionSource()
        }


    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .patientPressAnimation(
                    interactionSource
                ),
        shape =
            RoundedCornerShape(
                25.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    PatientColors.DoctorCard
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 5.dp
            ),
        border =
            BorderStroke(
                width =
                    1.dp,
                color =
                    PatientColors
                        .DoctorAccent
                        .copy(
                            alpha = 0.10f
                        )
            )
    ) {

        Column {

            /*
             * Teal accent strip
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            4.dp
                        )
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    PatientColors
                                        .DoctorAccent,
                                    PatientColors
                                        .Primary,
                                    PatientColors
                                        .AppointmentAccent
                                )
                            )
                        )
            )


            Column(
                modifier =
                    Modifier.padding(
                        17.dp
                    )
            ) {

                /*
                 * Doctor header
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource =
                                    interactionSource,
                                indication =
                                    null,
                                onClick =
                                    onProfile
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    DoctorAvatar(
                        doctor = doctor
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                14.dp
                            )
                    )


                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    doctor.fullName,
                                color =
                                    PatientColors
                                        .TextPrimary,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                fontSize =
                                    16.sp,
                                maxLines =
                                    1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )


                            if (
                                doctor.verification_status
                                    .equals(
                                        "approved",
                                        ignoreCase =
                                            true
                                    )
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            5.dp
                                        )
                                )


                                Icon(
                                    imageVector =
                                        Icons.Outlined.Verified,
                                    contentDescription =
                                        "Verified doctor",
                                    tint =
                                        PatientColors
                                            .DoctorAccent,
                                    modifier =
                                        Modifier.size(
                                            17.dp
                                        )
                                )
                            }
                        }


                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )


                        Text(
                            text =
                                doctor.specialty,
                            color =
                                PatientColors
                                    .DoctorAccent,
                            fontWeight =
                                FontWeight.SemiBold,
                            fontSize =
                                12.5.sp
                        )


                        doctor
                            .years_of_experience
                            ?.takeIf {
                                it > 0
                            }
                            ?.let { years ->

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            6.dp
                                        )
                                )


                                Row(
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Box(
                                        modifier =
                                            Modifier
                                                .clip(
                                                    RoundedCornerShape(
                                                        20.dp
                                                    )
                                                )
                                                .background(
                                                    PatientColors
                                                        .ReviewCard
                                                )
                                                .padding(
                                                    horizontal =
                                                        8.dp,
                                                    vertical =
                                                        4.dp
                                                )
                                    ) {

                                        Row(
                                            verticalAlignment =
                                                Alignment.CenterVertically
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Filled.Star,
                                                contentDescription =
                                                    null,
                                                tint =
                                                    PatientColors
                                                        .ReviewAccent,
                                                modifier =
                                                    Modifier.size(
                                                        13.dp
                                                    )
                                            )


                                            Spacer(
                                                modifier =
                                                    Modifier.width(
                                                        4.dp
                                                    )
                                            )


                                            Text(
                                                text =
                                                    "$years year${
                                                        if (years == 1) {
                                                            ""
                                                        } else {
                                                            "s"
                                                        }
                                                    } experience",
                                                color =
                                                    PatientColors
                                                        .ReviewAccent,
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
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            15.dp
                        )
                )


                /*
                 * Doctor information
                 */
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.55f
                                )
                            )
                            .padding(
                                12.dp
                            )
                ) {

                    DoctorInformationRow(
                        icon =
                            Icons.Outlined.LocationOn,
                        text =
                            doctor.location
                    )


                    if (
                        !doctor.operating_hours
                            .isNullOrBlank()
                    ) {

                        DoctorInformationRow(
                            icon =
                                Icons.Outlined.Schedule,
                            text =
                                doctor.operating_hours
                                    ?: ""
                        )
                    }


                    if (
                        !doctor.language
                            .isNullOrBlank()
                    ) {

                        DoctorInformationRow(
                            icon =
                                Icons.Outlined.Verified,
                            text =
                                "Languages: ${doctor.language}"
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )


                /*
                 * Consultation price
                 */
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .background(
                                PatientColors
                                    .AppointmentCard
                            )
                            .padding(
                                horizontal = 13.dp,
                                vertical = 12.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    38.dp
                                )
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .background(
                                    PatientColors
                                        .AppointmentAccent
                                        .copy(
                                            alpha = 0.11f
                                        )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Payments,
                            contentDescription =
                                null,
                            tint =
                                PatientColors
                                    .AppointmentAccent,
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


                    Column {

                        Text(
                            text =
                                "Private consultation",
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                10.5.sp
                        )


                        Text(
                            text =
                                doctor.hourly_rate
                                    ?.let {
                                        "R %.2f"
                                            .format(it)
                                    }
                                    ?: "Rate unavailable",
                            color =
                                PatientColors
                                    .AppointmentAccent,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                16.sp
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )


                    if (
                        doctor.hourly_rate != null
                    ) {

                        Text(
                            text =
                                "per hour",
                            color =
                                PatientColors
                                    .TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            15.dp
                        )
                )


                /*
                 * Actions
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    OutlinedButton(
                        onClick =
                            onProfile,
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .height(
                                    48.dp
                                ),
                        shape =
                            RoundedCornerShape(
                                15.dp
                            ),
                        border =
                            BorderStroke(
                                width =
                                    1.dp,
                                color =
                                    PatientColors
                                        .DoctorAccent
                            )
                    ) {

                        Text(
                            text =
                                "View profile",
                            color =
                                PatientColors
                                    .DoctorAccent,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                12.sp
                        )
                    }


                    Button(
                        onClick =
                            onBook,
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .height(
                                    48.dp
                                ),
                        shape =
                            RoundedCornerShape(
                                15.dp
                            ),
                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        PatientColors
                                            .DoctorAccent,
                                    contentColor =
                                        Color.White
                                )
                    ) {

                        Text(
                            text =
                                "Book appointment",
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                11.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DoctorAvatar(
    doctor: DoctorListing
) {

    Box(
        modifier =
            Modifier.size(
                72.dp
            ),
        contentAlignment =
            Alignment.Center
    ) {

        /*
         * Soft outer ring
         */
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(
                        CircleShape
                    )
                    .background(
                        PatientColors
                            .DoctorAccent
                            .copy(
                                alpha = 0.10f
                            )
                    )
        )


        Box(
            modifier =
                Modifier
                    .size(
                        64.dp
                    )
                    .clip(
                        CircleShape
                    )
                    .background(
                        PatientColors
                            .DoctorCard
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            if (
                !doctor.profile_image_url
                    .isNullOrBlank()
            ) {

                AsyncImage(
                    model =
                        doctor.profile_image_url,
                    contentDescription =
                        doctor.fullName,
                    contentScale =
                        ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxSize()
                )

            } else {

                Text(
                    text =
                        buildString {

                            append(
                                doctor.name
                                    .firstOrNull()
                                    ?.uppercaseChar()
                                    ?: 'D'
                            )

                            append(
                                doctor.surname
                                    .firstOrNull()
                                    ?.uppercaseChar()
                                    ?: 'R'
                            )
                        },
                    color =
                        PatientColors
                            .DoctorAccent,
                    fontSize =
                        19.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )
            }
        }


        /*
         * Verified badge
         */
        if (
            doctor.verification_status
                .equals(
                    "approved",
                    ignoreCase = true
                )
        ) {

            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomEnd
                        )
                        .size(
                            24.dp
                        )
                        .clip(
                            CircleShape
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surface
                        )
                        .padding(
                            2.dp
                        )
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(
                                CircleShape
                            )
                            .background(
                                PatientColors
                                    .SuccessAccent
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Verified,
                        contentDescription =
                            "Approved doctor",
                        tint =
                            Color.White,
                        modifier =
                            Modifier.size(
                                13.dp
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorInformationRow(
    icon:
    ImageVector,
    text: String
) {

    Row(
        modifier =
            Modifier.padding(
                vertical = 3.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                null,
            tint =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier.size(
                    16.dp
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
                text,
            fontSize =
                12.5.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            maxLines =
                2,
            overflow =
                TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingDoctors() {

    val infinite =
        rememberInfiniteTransition(
            label = "DoctorLoading"
        )

    val shimmer by
    infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        900,
                        easing = FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "DoctorLoadingPulse"
    )

    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        items(3) {

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .alpha(shimmer),
                shape =
                    RoundedCornerShape(
                        24.dp
                    ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.DoctorCard
                    ),
                border =
                    BorderStroke(
                        1.dp,
                        PatientColors.DoctorAccent
                            .copy(alpha = 0.08f)
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
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        PatientColors.DoctorAccent
                                            .copy(alpha = 0.10f)
                                    )
                        )

                        Spacer(
                            Modifier.width(
                                14.dp
                            )
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(0.70f)
                                        .height(16.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                8.dp
                                            )
                                        )
                                        .background(
                                            PatientColors.DoctorAccent
                                                .copy(alpha = 0.12f)
                                        )
                            )

                            Spacer(
                                Modifier.height(
                                    9.dp
                                )
                            )

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth(0.45f)
                                        .height(11.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                8.dp
                                            )
                                        )
                                        .background(
                                            PatientColors.DoctorAccent
                                                .copy(alpha = 0.08f)
                                        )
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(
                            16.dp
                        )
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(
                                    RoundedCornerShape(
                                        15.dp
                                    )
                                )
                                .background(
                                    Color.White.copy(
                                        alpha = 0.45f
                                    )
                                )
                    )

                    Spacer(
                        Modifier.height(
                            12.dp
                        )
                    )

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(
                                    RoundedCornerShape(
                                        15.dp
                                    )
                                )
                                .background(
                                    PatientColors.AppointmentCard
                                )
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorDoctors(
    message: String,
    onRetry: () -> Unit
) {

    val friendlyMessage =
        when {
            message.contains(
                "network",
                ignoreCase = true
            ) ||
                    message.contains(
                        "internet",
                        ignoreCase = true
                    ) ->
                "We couldn't connect to the doctor directory. Check your internet connection and try again."

            else ->
                "We couldn't load the doctor directory right now. Please try again."
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
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
                    26.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        PatientColors.RedSoft
                ),
            border =
                BorderStroke(
                    1.dp,
                    PatientColors.Red
                        .copy(alpha = 0.12f)
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            24.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                DoctorErrorIllustration()

                Spacer(
                    Modifier.height(
                        16.dp
                    )
                )

                Text(
                    text =
                        "Couldn't load doctors",
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        18.sp,
                    fontWeight =
                        FontWeight.ExtraBold,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    Modifier.height(
                        7.dp
                    )
                )

                Text(
                    text =
                        friendlyMessage,
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        12.5.sp,
                    lineHeight =
                        18.sp,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    Modifier.height(
                        20.dp
                    )
                )

                Button(
                    onClick =
                        onRetry,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                49.dp
                            ),
                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PatientColors.Red
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Filled.Refresh,
                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.width(
                            8.dp
                        )
                    )

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
@Composable
private fun DoctorErrorIllustration() {

    Canvas(
        modifier =
            Modifier.size(
                82.dp
            )
    ) {

        val center =
            Offset(
                size.width / 2f,
                size.height / 2f
            )

        drawCircle(
            color =
                PatientColors.Red,
            radius =
                size.minDimension * 0.34f,
            center =
                center,
            style =
                Stroke(
                    width =
                        4.dp.toPx()
                )
        )

        drawLine(
            color =
                PatientColors.Red,
            start =
                Offset(
                    center.x,
                    center.y -
                            15.dp.toPx()
                ),
            end =
                Offset(
                    center.x,
                    center.y +
                            5.dp.toPx()
                ),
            strokeWidth =
                5.dp.toPx(),
            cap =
                StrokeCap.Round
        )

        drawCircle(
            color =
                PatientColors.Red,
            radius =
                3.dp.toPx(),
            center =
                Offset(
                    center.x,
                    center.y +
                            16.dp.toPx()
                )
        )
    }
}

@Composable
private fun EmptyDoctors(
    hasFilters: Boolean,
    onClear: () -> Unit
) {

    val infinite =
        rememberInfiniteTransition(
            label = "EmptyDoctors"
        )

    val pulse by
    infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        1300,
                        easing = FastOutSlowInEasing
                    ),
                repeatMode =
                    RepeatMode.Reverse
            ),
        label = "EmptyDoctorsPulse"
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
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
                    26.dp
                ),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        PatientColors.DoctorCard
                ),
            border =
                BorderStroke(
                    1.dp,
                    PatientColors.DoctorAccent
                        .copy(alpha = 0.10f)
                )
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            24.dp
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                100.dp
                            )
                            .scale(
                                pulse
                            )
                            .clip(
                                CircleShape
                            )
                            .background(
                                PatientColors.DoctorAccent
                                    .copy(alpha = 0.08f)
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Search,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.DoctorAccent,
                        modifier =
                            Modifier.size(
                                42.dp
                            )
                    )
                }

                Spacer(
                    Modifier.height(
                        18.dp
                    )
                )

                Text(
                    text =
                        if (hasFilters) {
                            "No matching doctors"
                        } else {
                            "No doctors available"
                        },
                    color =
                        PatientColors.TextPrimary,
                    fontWeight =
                        FontWeight.ExtraBold,
                    fontSize =
                        18.sp,
                    textAlign =
                        TextAlign.Center
                )

                Spacer(
                    Modifier.height(
                        7.dp
                    )
                )

                Text(
                    text =
                        if (hasFilters) {
                            "Try another doctor name, specialty or location."
                        } else {
                            "Approved doctors will appear here when they become available."
                        },
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        12.5.sp,
                    lineHeight =
                        18.sp,
                    textAlign =
                        TextAlign.Center
                )

                if (hasFilters) {

                    Spacer(
                        Modifier.height(
                            19.dp
                        )
                    )

                    OutlinedButton(
                        onClick =
                            onClear,
                        modifier =
                            Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                15.dp
                            ),
                        border =
                            BorderStroke(
                                1.dp,
                                PatientColors.DoctorAccent
                            )
                    ) {

                        Text(
                            text =
                                "Clear filters",
                            color =
                                PatientColors.DoctorAccent,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
