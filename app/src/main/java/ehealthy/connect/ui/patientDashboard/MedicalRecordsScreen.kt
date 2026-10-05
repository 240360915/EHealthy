package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
@Serializable
data class MedicalRecord(
    val id: String,
    val record_type: String? = null,
    val medications: List<String>? = null,
    val notes: String? = null,
    val created_at: String,
    val doctor_id: String? = null
)

data class MedicalRecordDisplay(
    val record: MedicalRecord,
    val doctorName: String,
    val doctorPhoto: String? = null
)

enum class RecordSort(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordsScreen(
    onBack: () -> Unit,
    fetchRecords: suspend () -> Result<List<MedicalRecordDisplay>>
) {

    var records by remember {
        mutableStateOf<List<MedicalRecordDisplay>>(
            emptyList()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    var selectedType by remember {
        mutableStateOf<String?>(null)
    }

    var sortOrder by remember {
        mutableStateOf(
            RecordSort.NEWEST
        )
    }


    LaunchedEffect(Unit) {

        val result =
            fetchRecords()

        isLoading =
            false

        result
            .onSuccess {
                records = it
            }
            .onFailure {
                loadError =
                    it.message
                        ?: "Failed to load medical records."
            }
    }


    val recordTypes =
        remember(records) {

            records
                .mapNotNull {
                    it.record.record_type
                }
                .distinct()
                .sorted()
        }


    val displayedRecords =
        remember(
            records,
            selectedType,
            sortOrder
        ) {

            val filtered =
                if (selectedType == null) {

                    records

                } else {

                    records.filter {
                        it.record.record_type ==
                                selectedType
                    }
                }


            when (sortOrder) {

                RecordSort.NEWEST ->
                    filtered.sortedByDescending {
                        it.record.created_at
                    }

                RecordSort.OLDEST ->
                    filtered.sortedBy {
                        it.record.created_at
                    }
            }
        }


    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text =
                                "Medical Records",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                18.sp
                        )


                        Text(
                            text =
                                if (records.isEmpty()) {
                                    "Your health history"
                                } else {
                                    "${records.size} medical record${
                                        if (records.size == 1) {
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
                                        PatientColors.RecordsCard,
                                        CircleShape
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Filled.ArrowBack,
                                contentDescription =
                                    "Back",
                                tint =
                                    PatientColors.RecordsAccent,
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


        Column(
            modifier =
                Modifier.padding(
                    paddingValues
                )
        ) {

            /*
             * Records header
             */
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 10.dp
                        ),
                shape =
                    RoundedCornerShape(
                        22.dp
                    ),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            PatientColors.RecordsCard
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
                                    48.dp
                                )
                                .background(
                                    PatientColors.RecordsAccent
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
                                Icons.Outlined.Description,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.RecordsAccent,
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
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Your health history",
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
                                "Records from your consultations are kept together here.",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp,
                            lineHeight =
                                15.sp
                        )
                    }


                    if (
                        records.isNotEmpty()
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .background(
                                        PatientColors.RecordsAccent
                                            .copy(
                                                alpha = 0.10f
                                            ),
                                        CircleShape
                                    )
                                    .padding(
                                        horizontal =
                                            10.dp,
                                        vertical =
                                            6.dp
                                    )
                        ) {

                            Text(
                                text =
                                    records.size
                                        .toString(),
                                color =
                                    PatientColors.RecordsAccent,
                                fontSize =
                                    11.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }


            /*
             * Filters
             */
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 6.dp
                    )
            ) {

                Text(
                    text =
                        "Record type",
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
                        Modifier.horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    FilterChip(
                        selected =
                            selectedType == null,

                        onClick = {
                            selectedType =
                                null
                        },

                        label = {

                            Text(
                                text =
                                    "All Records",
                                fontSize =
                                    11.5.sp,
                                fontWeight =
                                    if (
                                        selectedType == null
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
                                        PatientColors.RecordsAccent,

                                    selectedLabelColor =
                                        androidx.compose.ui.graphics.Color.White,

                                    containerColor =
                                        PatientColors.RecordsCard,

                                    labelColor =
                                        PatientColors.RecordsAccent
                                ),

                        border =
                            null
                    )


                    recordTypes
                        .forEach { type ->

                            val selected =
                                selectedType ==
                                        type


                            FilterChip(
                                selected =
                                    selected,

                                onClick = {
                                    selectedType =
                                        type
                                },

                                label = {

                                    Text(
                                        text =
                                            type.replaceFirstChar {
                                                it.uppercase()
                                            },
                                        fontSize =
                                            11.5.sp,
                                        fontWeight =
                                            if (selected) {
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
                                                PatientColors.RecordsAccent,

                                            selectedLabelColor =
                                                androidx.compose.ui.graphics.Color.White,

                                            containerColor =
                                                PatientColors.RecordsCard,

                                            labelColor =
                                                PatientColors.RecordsAccent
                                        ),

                                border =
                                    null
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
                 * Sort
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                "Sort records",
                            color =
                                PatientColors.TextPrimary,
                            fontSize =
                                12.5.sp,
                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            text =
                                "Choose how your history is ordered",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                9.5.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    RecordSort.entries
                        .forEach { order ->

                            val selected =
                                sortOrder ==
                                        order


                            FilterChip(
                                selected =
                                    selected,

                                onClick = {
                                    sortOrder =
                                        order
                                },

                                label = {

                                    Text(
                                        text =
                                            order.label,
                                        fontSize =
                                            11.sp,
                                        fontWeight =
                                            if (selected) {
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
                                                PatientColors.AppointmentAccent,

                                            selectedLabelColor =
                                                androidx.compose.ui.graphics.Color.White,

                                            containerColor =
                                                PatientColors.AppointmentCard,

                                            labelColor =
                                                PatientColors.AppointmentAccent
                                        ),

                                border =
                                    null
                            )
                        }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )


            /*
             * Results
             */
            when{
                isLoading -> {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    40.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Card(
                            shape =
                                RoundedCornerShape(
                                    24.dp
                                ),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        PatientColors.RecordsCard
                                ),
                            elevation =
                                CardDefaults.cardElevation(
                                    defaultElevation = 0.dp
                                )
                        ) {

                            Column(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 32.dp,
                                        vertical = 27.dp
                                    ),
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
                                                PatientColors.RecordsAccent
                                                    .copy(alpha = 0.10f),
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
                                            PatientColors.RecordsAccent,
                                        strokeWidth =
                                            2.5.dp
                                    )
                                }


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            14.dp
                                        )
                                )


                                Text(
                                    text =
                                        "Loading your records",
                                    color =
                                        PatientColors.TextPrimary,
                                    fontSize =
                                        14.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            4.dp
                                        )
                                )


                                Text(
                                    text =
                                        "Getting your latest medical history...",
                                    color =
                                        PatientColors.TextSecondary,
                                    fontSize =
                                        10.5.sp,
                                    textAlign =
                                        TextAlign.Center
                                )
                            }
                        }
                    }
                }


                loadError != null -> {

                    EmptyState(
                        icon =
                            Icons.Outlined.SearchOff,

                        title =
                            "Couldn't load your records",

                        message =
                            loadError
                                ?: "Something went wrong while loading your medical records.",

                        accent =
                            PatientColors.Red,

                        background =
                            PatientColors.RedSoft
                    )
                }


                displayedRecords.isEmpty() -> {

                    EmptyState(
                        icon =
                            Icons.Outlined.Description,

                        title =
                            if (records.isEmpty()) {
                                "No medical records yet"
                            } else {
                                "No matching records"
                            },

                        message =
                            if (records.isEmpty()) {
                                "Records created during your consultations will appear here."
                            } else {
                                "Try choosing another record type to see more of your history."
                            },

                        accent =
                            PatientColors.RecordsAccent,

                        background =
                            PatientColors.RecordsCard
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordCard(
    item: MedicalRecordDisplay
) {

    val record =
        item.record

    val dateLabel =
        remember(
            record.created_at
        ) {
            formatRecordDate(
                record.created_at
            )
        }


    val recordType =
        record.record_type
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "Medical record"


    val medications =
        record.medications
            .orEmpty()


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
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.RecordsAccent
                        .copy(alpha = 0.10f)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
    ) {

        Column {

            /*
             * Purple accent strip
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            4.dp
                        )
                        .background(
                            PatientColors.RecordsAccent
                        )
            )


            Column(
                modifier =
                    Modifier.padding(
                        16.dp
                    )
            ) {

                /*
                 * Record type + date
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .background(
                                    PatientColors.RecordsCard,
                                    RoundedCornerShape(
                                        20.dp
                                    )
                                )
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 5.dp
                                )
                    ) {

                        Text(
                            text =
                                recordType.uppercase(),
                            color =
                                PatientColors.RecordsAccent,
                            fontSize =
                                9.5.sp,
                            fontWeight =
                                FontWeight.ExtraBold,
                            letterSpacing =
                                0.4.sp
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.weight(1f)
                    )


                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        27.dp
                                    )
                                    .background(
                                        PatientColors.AppointmentCard,
                                        RoundedCornerShape(
                                            8.dp
                                        )
                                    ),
                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.CalendarMonth,
                                contentDescription =
                                    null,
                                tint =
                                    PatientColors.AppointmentAccent,
                                modifier =
                                    Modifier.size(
                                        14.dp
                                    )
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.width(
                                    6.dp
                                )
                        )


                        Text(
                            text =
                                dateLabel,
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp,
                            fontWeight =
                                FontWeight.Medium
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
                 * Doctor section
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(
                                    48.dp
                                )
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    PatientColors.DoctorCard
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        if (
                            !item.doctorPhoto
                                .isNullOrBlank()
                        ) {

                            AsyncImage(
                                model =
                                    item.doctorPhoto,
                                contentDescription =
                                    "Doctor profile image",
                                contentScale =
                                    ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .size(
                                            48.dp
                                        )
                                        .clip(
                                            CircleShape
                                        )
                            )

                        } else {

                            Box(
                                modifier =
                                    Modifier
                                        .size(
                                            39.dp
                                        )
                                        .background(
                                            PatientColors.DoctorAccent,
                                            CircleShape
                                        ),
                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.MedicalServices,
                                    contentDescription =
                                        null,
                                    tint =
                                        MaterialTheme.colorScheme.surface,
                                    modifier =
                                        Modifier.size(
                                            20.dp
                                        )
                                )
                            }
                        }
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
                                "Recorded by",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                9.5.sp
                        )


                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )


                        Text(
                            text =
                                item.doctorName,
                            color =
                                PatientColors.TextPrimary,
                            fontSize =
                                13.5.sp,
                            fontWeight =
                                FontWeight.ExtraBold
                        )
                    }


                    Box(
                        modifier =
                            Modifier
                                .size(
                                    37.dp
                                )
                                .background(
                                    PatientColors.RecordsCard,
                                    RoundedCornerShape(
                                        11.dp
                                    )
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Description,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.RecordsAccent,
                            modifier =
                                Modifier.size(
                                    18.dp
                                )
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
                 * Medications
                 */
                Text(
                    text =
                        "Medications",
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        11.5.sp,
                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            7.dp
                        )
                )


                if (
                    medications.isEmpty()
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    PatientColors.NeutralCard,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                                .padding(
                                    11.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.MedicalServices,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.TextSecondary,
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
                                "No medications listed",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }

                } else {

                    FlowRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                7.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                7.dp
                            )
                    ) {

                        medications
                            .forEach { medication ->

                                Box(
                                    modifier =
                                        Modifier
                                            .background(
                                                PatientColors.DoctorCard,
                                                RoundedCornerShape(
                                                    20.dp
                                                )
                                            )
                                            .padding(
                                                horizontal =
                                                    11.dp,
                                                vertical =
                                                    6.dp
                                            )
                                ) {

                                    Text(
                                        text =
                                            medication,
                                        color =
                                            PatientColors.DoctorAccent,
                                        fontSize =
                                            10.5.sp,
                                        fontWeight =
                                            FontWeight.SemiBold
                                    )
                                }
                            }
                    }
                }


                /*
                 * Notes
                 */
                record.notes
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?.let { notes ->

                        Spacer(
                            modifier =
                                Modifier.height(
                                    14.dp
                                )
                        )


                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        PatientColors.RecordsCard,
                                        RoundedCornerShape(
                                            15.dp
                                        )
                                    )
                                    .padding(
                                        12.dp
                                    )
                        ) {

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.Description,
                                    contentDescription =
                                        null,
                                    tint =
                                        PatientColors.RecordsAccent,
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
                                        "Clinical notes",
                                    color =
                                        PatientColors.RecordsAccent,
                                    fontSize =
                                        10.sp,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(
                                        6.dp
                                    )
                            )


                            Text(
                                text =
                                    notes,
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.5.sp,
                                lineHeight =
                                    16.sp
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    accent: androidx.compose.ui.graphics.Color,
    background: androidx.compose.ui.graphics.Color
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 24.dp,
                    vertical = 42.dp
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
                            78.dp
                        )
                        .background(
                            background,
                            CircleShape
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(
                                56.dp
                            )
                            .background(
                                accent.copy(
                                    alpha = 0.10f
                                ),
                                CircleShape
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            icon,
                        contentDescription =
                            null,
                        tint =
                            accent,
                        modifier =
                            Modifier.size(
                                29.dp
                            )
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
                    title,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    15.sp,
                fontWeight =
                    FontWeight.ExtraBold,
                textAlign =
                    TextAlign.Center
            )


            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )


            Text(
                text =
                    message,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    11.sp,
                lineHeight =
                    16.sp,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

private fun formatRecordDate(isoTimestamp: String): String {
    return try {
        // created_at from Supabase looks like "2026-01-15T09:30:00+00:00" — take the date part
        val datePart = isoTimestamp.substringBefore("T")
        val (year, month, day) = datePart.split("-")
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        "${months[month.toInt() - 1]} ${day.toInt()}, $year"
    } catch (e: Exception) {
        isoTimestamp
    }
}