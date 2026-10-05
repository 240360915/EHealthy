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
                        it.is_deactivated != true
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

    val visibleDoctors =
        remember(
            doctors,
            searchQuery,
            selectedSpecialty,
            selectedSort
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
                                    .contains(query) ||

                                doctor.specialty
                                    .lowercase()
                                    .contains(query) ||

                                doctor.location
                                    .lowercase()
                                    .contains(query) ||

                                doctor.language
                                    .orEmpty()
                                    .lowercase()
                                    .contains(query)

                    val matchesSpecialty =

                        selectedSpecialty == null ||

                                doctor.specialty ==
                                selectedSpecialty

                    matchesSearch &&
                            matchesSpecialty
                }

            when (selectedSort) {

                DoctorSort.RECOMMENDED ->

                    filtered.sortedWith(
                        compareByDescending<DoctorListing> {
                            it.years_of_experience ?: 0
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
                            it.hourly_rate ?: 0.0
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
                        OutlinedTextFieldDefaults
                            .colors(
                                focusedBorderColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
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
                            searchQuery.isNotBlank()
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
                                    selectedSpecialty != null,

                        onClear = {

                            searchQuery = ""

                            selectedSpecialty =
                                null

                            selectedSort =
                                DoctorSort
                                    .RECOMMENDED
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
                                    "Available doctors",
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
                        24.dp
                    )
                )
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme
                                .colorScheme
                                .primary,

                            MaterialTheme
                                .colorScheme
                                .tertiary
                        )
                    )
                )
                .padding(
                    20.dp
                )
    ) {

        Column {

            Text(
                text =
                    "Care that fits your day",
                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimary,
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    22.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    "Compare specialists, view their fees and choose who you want to see.",
                color =
                    MaterialTheme
                        .colorScheme
                        .onPrimary
                        .copy(
                            alpha = 0.88f
                        ),
                fontSize =
                    13.sp,
                lineHeight =
                    18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            AssistChip(

                onClick = {},

                label = {

                    Text(
                        "$doctorCount approved doctor${
                            if (
                                doctorCount == 1
                            ) {
                                ""
                            } else {
                                "s"
                            }
                        }"
                    )
                },

                leadingIcon = {

                    Icon(
                        imageVector =
                            Icons.Outlined.Verified,
                        contentDescription =
                            null,
                        modifier =
                            Modifier.size(
                                18.dp
                            )
                    )
                }
            )
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
                    bottom = 8.dp
                ),
        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
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
                    "All"
                )
            }
        )

        specialties
            .forEach { specialty ->

                FilterChip(

                    selected =
                        selectedSpecialty ==
                                specialty,

                    onClick = {

                        onSpecialtySelected(
                            if (
                                selectedSpecialty ==
                                specialty
                            ) {
                                null
                            } else {
                                specialty
                            }
                        )
                    },

                    label = {

                        Text(
                            specialty
                        )
                    }
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

    ElevatedCard(

        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(),

        colors =
            CardDefaults
                .elevatedCardColors(
                    containerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),

        elevation =
            CardDefaults
                .elevatedCardElevation(
                    defaultElevation =
                        2.dp
                )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick =
                                onProfile
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                DoctorAvatar(
                    doctor =
                        doctor
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
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                16.sp,
                            maxLines =
                                1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        if (
                            doctor.verification_status ==
                            "approved"
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
                                    MaterialTheme
                                        .colorScheme
                                        .primary,
                                modifier =
                                    Modifier.size(
                                        17.dp
                                    )
                            )
                        }
                    }

                    Text(
                        text =
                            doctor.specialty,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,
                        fontWeight =
                            FontWeight.SemiBold,
                        fontSize =
                            13.sp
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
                                        3.dp
                                    )
                            )

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
                                        MaterialTheme
                                            .colorScheme
                                            .tertiary,
                                    modifier =
                                        Modifier.size(
                                            14.dp
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
                                            if (
                                                years == 1
                                            ) {
                                                ""
                                            } else {
                                                "s"
                                            }
                                        } experience",
                                    fontSize =
                                        11.sp,
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
                                )
                            }
                        }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            DoctorInformationRow(
                icon =
                    Icons.Outlined.LocationOn,
                text =
                    doctor.location
            )

            if (
                !doctor
                    .operating_hours
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
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .primaryContainer
                                .copy(
                                    alpha =
                                        0.55f
                                )
                        )
                        .padding(
                            horizontal =
                                14.dp,
                            vertical =
                                12.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Payments,
                    contentDescription =
                        null,
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,
                    modifier =
                        Modifier.size(
                            19.dp
                        )
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Column {

                    Text(
                        text =
                            "Private consultation",
                        fontSize =
                            11.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Text(
                        text =
                            doctor.hourly_rate
                                ?.let {
                                    "R %.2f".format(
                                        it
                                    )
                                }
                                ?: "Rate unavailable",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            16.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                Spacer(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                Text(
                    text =
                        "per hour",
                    fontSize =
                        11.sp,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

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
                        Modifier.weight(
                            1f
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        )

                ) {

                    Text(
                        "View profile"
                    )
                }

                Button(

                    onClick =
                        onBook,

                    modifier =
                        Modifier.weight(
                            1f
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .primary
                            )

                ) {

                    Text(
                        text =
                            "Book",
                        fontWeight =
                            FontWeight.Bold
                    )
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
            Modifier
                .size(
                    68.dp
                )
                .clip(
                    CircleShape
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .primaryContainer
                ),
        contentAlignment =
            Alignment.Center
    ) {

        if (
            !doctor
                .profile_image_url
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
                fontSize =
                    19.sp,
                fontWeight =
                    FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
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

    Box(
        modifier =
            Modifier.fillMaxSize(),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator()

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    "Finding approved doctors...",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorDoctors(
    message: String,
    onRetry: () -> Unit
) {

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

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    "We couldn't load doctors",
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    message,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            Button(
                onClick =
                    onRetry
            ) {

                Icon(
                    imageVector =
                        Icons.Filled.Refresh,
                    contentDescription =
                        null
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Text(
                    "Try again"
                )
            }
        }
    }
}

@Composable
private fun EmptyDoctors(
    hasFilters: Boolean,
    onClear: () -> Unit
) {

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

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Search,
                contentDescription =
                    null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,
                modifier =
                    Modifier.size(
                        42.dp
                    )
            )

            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )

            Text(
                text =
                    if (
                        hasFilters
                    ) {
                        "No matching doctors"
                    } else {
                        "No doctors available"
                    },
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    18.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Text(
                text =
                    if (
                        hasFilters
                    ) {
                        "Try another specialty, name or location."
                    } else {
                        "Approved doctors will appear here when available."
                    },
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            if (
                hasFilters
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                TextButton(
                    onClick =
                        onClear
                ) {

                    Text(
                        "Clear filters"
                    )
                }
            }
        }
    }
}