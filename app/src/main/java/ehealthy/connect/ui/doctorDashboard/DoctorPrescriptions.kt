package ehealthy.connect.ui.doctorDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/**
 * One patient this doctor can prescribe to.
 *
 * Currently scoped to patients who have a confirmed appointment.
 */
data class PrescriptionPatient(
    val id: String,
    val name: String
)


/**
 * One refill request submitted by a patient.
 *
 * id = original prescription ID.
 */
data class RefillRequest(
    val id: String,
    val patientId: String,
    val patientName: String,
    val medication: String,
    val requestedAt: String? = null
)


data class MedicationCategory(
    val name: String,
    val emoji: String,
    val items: List<String>
)


val medicationCatalog = listOf(

    MedicationCategory(
        "Pain Relief & Anti-Inflammatory",
        "💊",
        listOf(
            "Aspirin 100mg",
            "Ibuprofen 200mg",
            "Paracetamol 500mg",
            "Naproxen 250mg",
            "Diclofenac 50mg"
        )
    ),

    MedicationCategory(
        "Antibiotics",
        "🦠",
        listOf(
            "Amoxicillin 500mg",
            "Azithromycin 250mg",
            "Ciprofloxacin 500mg",
            "Doxycycline 100mg",
            "Metronidazole 400mg"
        )
    ),

    MedicationCategory(
        "Cardiovascular",
        "❤️",
        listOf(
            "Amlodipine 5mg",
            "Lisinopril 10mg",
            "Metoprolol 50mg",
            "Atorvastatin 20mg",
            "Warfarin 5mg"
        )
    ),

    MedicationCategory(
        "Respiratory",
        "🫁",
        listOf(
            "Albuterol 90mcg",
            "Montelukast 10mg",
            "Fluticasone 50mcg",
            "Prednisone 20mg"
        )
    ),

    MedicationCategory(
        "Gastrointestinal",
        "🧪",
        listOf(
            "Omeprazole 20mg",
            "Loperamide 2mg",
            "Metoclopramide 10mg",
            "Simethicone 125mg"
        )
    ),

    MedicationCategory(
        "Mental Health",
        "🧠",
        listOf(
            "Sertraline 50mg",
            "Escitalopram 10mg",
            "Alprazolam 0.5mg",
            "Lorazepam 1mg"
        )
    ),

    MedicationCategory(
        "Diabetes",
        "🩸",
        listOf(
            "Metformin 500mg",
            "Glipizide 5mg",
            "Insulin Glargine"
        )
    ),

    MedicationCategory(
        "Vitamins & Supplements",
        "🌿",
        listOf(
            "Vitamin D3",
            "Multivitamin",
            "Calcium 500mg",
            "Iron 65mg"
        )
    ),

    MedicationCategory(
        "Other",
        "🧴",
        listOf(
            "HCTZ 25mg",
            "Levothyroxine 50mcg",
            "Allopurinol 100mg",
            "Furosemide 40mg",
            "Diphenhydramine",
            "Cetirizine 10mg"
        )
    )
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorPrescriptions(

    patients: List<PrescriptionPatient>,

    refillRequests: List<RefillRequest> = emptyList(),

    isLoadingPatients: Boolean,

    isLoadingRefills: Boolean = false,

    isSaving: Boolean,

    saveError: String?,

    onBack: () -> Unit,

    onSave: (
        patientId: String,
        medications: List<String>
    ) -> Unit,

    onApproveRefill: (RefillRequest) -> Unit = {},

    onDeclineRefill: (RefillRequest) -> Unit = {}
) {

    val background =
        Color(0xFFF4F7FB)

    val navy =
        Color(0xFF0F1F3D)

    val accent =
        Color(0xFF2563EB)

    val muted =
        Color(0xFF64748B)

    val success =
        Color(0xFF059669)

    val successSoft =
        Color(0xFFECFDF5)

    val warning =
        Color(0xFFD97706)

    val warningSoft =
        Color(0xFFFFF7ED)

    val danger =
        Color(0xFFDC2626)

    val dangerSoft =
        Color(0xFFFEF2F2)


    var selectedPatientId by
    remember {
        mutableStateOf<String?>(
            null
        )
    }


    var patientDropdownExpanded by
    remember {
        mutableStateOf(
            false
        )
    }


    var selectedMedications by
    remember {
        mutableStateOf(
            listOf<String>()
        )
    }


    var search by
    remember {
        mutableStateOf("")
    }


    val selectedPatientName =
        patients
            .find {
                it.id ==
                        selectedPatientId
            }
            ?.name


    val query =
        search
            .trim()
            .lowercase()


    val filteredCatalog =
        remember(query) {

            if (
                query.isEmpty()
            ) {

                medicationCatalog

            } else {

                medicationCatalog
                    .map { category ->

                        category.copy(

                            items =
                                category.items
                                    .filter { medication ->

                                        medication
                                            .lowercase()
                                            .contains(
                                                query
                                            )
                                    }
                        )
                    }
                    .filter {
                        it.items
                            .isNotEmpty()
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
                                "Prescriptions",

                            color =
                                navy,

                            fontWeight =
                                FontWeight.ExtraBold,

                            fontSize =
                                18.sp
                        )

                        Text(
                            text =
                                "Medication & refill management",

                            color =
                                muted,

                            fontSize =
                                10.sp
                        )
                    }
                },


                navigationIcon = {

                    IconButton(
                        onClick =
                            onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.ArrowBack,

                            contentDescription =
                                "Back",

                            tint =
                                navy
                        )
                    }
                },


                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                Color.White
                        )
            )
        },


        containerColor =
            background

    ) { paddingValues ->


        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    ),

            contentPadding =
                PaddingValues(
                    horizontal = 18.dp,
                    vertical = 16.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    14.dp
                )

        ) {


            /*
             * ---------------------------------------
             * REFILL REQUEST HEADER
             * ---------------------------------------
             */
            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .background(
                                    warningSoft,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Refresh,

                            contentDescription =
                                null,

                            tint =
                                warning,

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
                                "Refill requests",

                            color =
                                navy,

                            fontWeight =
                                FontWeight.ExtraBold,

                            fontSize =
                                17.sp
                        )


                        Text(
                            text =
                                "Review requests submitted by your patients",

                            color =
                                muted,

                            fontSize =
                                11.sp
                        )
                    }


                    if (
                        refillRequests
                            .isNotEmpty()
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .background(
                                        warning,
                                        CircleShape
                                    )
                                    .padding(
                                        horizontal =
                                            10.dp,
                                        vertical =
                                            5.dp
                                    )
                        ) {

                            Text(
                                text =
                                    refillRequests
                                        .size
                                        .toString(),

                                color =
                                    Color.White,

                                fontWeight =
                                    FontWeight.Bold,

                                fontSize =
                                    11.sp
                            )
                        }
                    }
                }
            }


            /*
             * Loading refill requests
             */
            if (
                isLoadingRefills
            ) {

                item {

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        Color.White
                                ),

                        shape =
                            RoundedCornerShape(
                                18.dp
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        18.dp
                                    ),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        22.dp
                                    ),

                                strokeWidth =
                                    2.dp,

                                color =
                                    accent
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        12.dp
                                    )
                            )


                            Text(
                                text =
                                    "Loading refill requests…",

                                color =
                                    muted,

                                fontSize =
                                    13.sp
                            )
                        }
                    }
                }

            } else if (
                refillRequests
                    .isEmpty()
            ) {


                /*
                 * Empty refill state
                 */
                item {

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        Color.White
                                ),

                        shape =
                            RoundedCornerShape(
                                18.dp
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
                                            42.dp
                                        )
                                        .background(
                                            successSoft,
                                            CircleShape
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.CheckCircle,

                                    contentDescription =
                                        null,

                                    tint =
                                        success,

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


                            Column {

                                Text(
                                    text =
                                        "No refill requests",

                                    color =
                                        navy,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        13.sp
                                )


                                Text(
                                    text =
                                        "New requests from patients will appear here.",

                                    color =
                                        muted,

                                    fontSize =
                                        10.5.sp
                                )
                            }
                        }
                    }
                }

            } else {


                /*
                 * Refill request cards
                 */
                items(
                    refillRequests,
                    key = {
                        it.id
                    }
                ) { request ->


                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        Color.White
                                ),

                        shape =
                            RoundedCornerShape(
                                20.dp
                            ),

                        elevation =
                            CardDefaults
                                .cardElevation(
                                    defaultElevation =
                                        2.dp
                                )
                    ) {


                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        16.dp
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
                                                warningSoft,
                                                RoundedCornerShape(
                                                    14.dp
                                                )
                                            ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.Medication,

                                        contentDescription =
                                            null,

                                        tint =
                                            warning,

                                        modifier =
                                            Modifier
                                                .size(
                                                    23.dp
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
                                        Modifier
                                            .weight(
                                                1f
                                            )
                                ) {

                                    Text(
                                        text =
                                            request
                                                .medication,

                                        color =
                                            navy,

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
                                            request
                                                .patientName,

                                        color =
                                            muted,

                                        fontSize =
                                            11.5.sp
                                    )
                                }


                                Box(
                                    modifier =
                                        Modifier
                                            .background(
                                                warningSoft,
                                                RoundedCornerShape(
                                                    12.dp
                                                )
                                            )
                                            .padding(
                                                horizontal =
                                                    9.dp,
                                                vertical =
                                                    5.dp
                                            )
                                ) {

                                    Text(
                                        text =
                                            "REFILL",

                                        color =
                                            warning,

                                        fontSize =
                                            9.sp,

                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }


                            if (
                                !request
                                    .requestedAt
                                    .isNullOrBlank()
                            ) {

                                Spacer(
                                    modifier =
                                        Modifier.height(
                                            9.dp
                                        )
                                )


                                Text(
                                    text =
                                        "Requested ${formatRefillDate(request.requestedAt)}",

                                    color =
                                        muted,

                                    fontSize =
                                        10.5.sp
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
                                    onClick = {
                                        onDeclineRefill(
                                            request
                                        )
                                    },

                                    modifier =
                                        Modifier.weight(
                                            1f
                                        ),

                                    shape =
                                        RoundedCornerShape(
                                            14.dp
                                        )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.Close,

                                        contentDescription =
                                            null,

                                        tint =
                                            danger,

                                        modifier =
                                            Modifier.size(
                                                17.dp
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
                                            "Decline",

                                        color =
                                            danger,

                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }


                                Button(
                                    onClick = {
                                        onApproveRefill(
                                            request
                                        )
                                    },

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
                                                    success
                                            )
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.CheckCircle,

                                        contentDescription =
                                            null,

                                        modifier =
                                            Modifier.size(
                                                17.dp
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
                                            "Approve",

                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }


            /*
             * ---------------------------------------
             * NEW PRESCRIPTION SECTION
             * ---------------------------------------
             */
            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                Column {

                    Text(
                        text =
                            "New prescription",

                        color =
                            navy,

                        fontSize =
                            18.sp,

                        fontWeight =
                            FontWeight.ExtraBold
                    )


                    Text(
                        text =
                            "Select a patient and choose medication",

                        color =
                            muted,

                        fontSize =
                            11.sp
                    )
                }
            }


            /*
             * Patient picker
             */
            item {

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {


                    OutlinedTextField(
                        value =
                            selectedPatientName
                                ?: if (
                                    isLoadingPatients
                                ) {
                                    "Loading patients…"
                                } else {
                                    "Select a patient"
                                },

                        onValueChange = {},

                        readOnly =
                            true,

                        enabled =
                            !isLoadingPatients,

                        label = {
                            Text(
                                "Patient"
                            )
                        },

                        trailingIcon = {

                            Icon(
                                imageVector =
                                    Icons.Filled.ArrowDropDown,

                                contentDescription =
                                    "Select patient",

                                tint =
                                    muted
                            )
                        },

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(

                                    focusedBorderColor =
                                        accent,

                                    focusedContainerColor =
                                        Color.White,

                                    unfocusedContainerColor =
                                        Color.White,

                                    disabledContainerColor =
                                        Color.White,

                                    disabledBorderColor =
                                        Color(
                                            0xFFE2E5EC
                                        ),

                                    disabledTextColor =
                                        muted
                                ),

                        modifier =
                            Modifier.fillMaxWidth()
                    )


                    Box(
                        modifier =
                            Modifier
                                .matchParentSize()
                                .clickable(
                                    enabled =
                                        !isLoadingPatients
                                ) {

                                    patientDropdownExpanded =
                                        true
                                }
                    )


                    DropdownMenu(
                        expanded =
                            patientDropdownExpanded,

                        onDismissRequest = {

                            patientDropdownExpanded =
                                false
                        }
                    ) {


                        if (
                            patients.isEmpty() &&
                            !isLoadingPatients
                        ) {

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        "No confirmed patients yet"
                                    )
                                },

                                onClick = {

                                    patientDropdownExpanded =
                                        false
                                }
                            )
                        }


                        patients.forEach {
                                patient ->

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        patient.name
                                    )
                                },

                                onClick = {

                                    selectedPatientId =
                                        patient.id

                                    patientDropdownExpanded =
                                        false
                                }
                            )
                        }
                    }
                }
            }


            /*
             * Selected medication summary
             */
            item {

                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        ),

                    shape =
                        RoundedCornerShape(
                            18.dp
                        ),

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                15.dp
                            )
                    ) {

                        Text(
                            text =
                                "SELECTED MEDICATIONS",

                            color =
                                navy,

                            fontSize =
                                10.sp,

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
                            selectedMedications
                                .isEmpty()
                        ) {

                            Text(
                                text =
                                    "None selected",

                                color =
                                    muted,

                                fontSize =
                                    13.sp
                            )

                        } else {

                            selectedMedications
                                .forEach {

                                    Text(
                                        text =
                                            "• $it",

                                        color =
                                            navy,

                                        fontSize =
                                            13.sp
                                    )
                                }
                        }
                    }
                }
            }


            if (
                saveError != null
            ) {

                item {

                    Card(
                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        dangerSoft
                                ),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                saveError,

                            color =
                                danger,

                            fontSize =
                                12.sp,

                            modifier =
                                Modifier.padding(
                                    12.dp
                                )
                        )
                    }
                }
            }


            /*
             * Confirm prescription
             */
            item {

                Button(
                    onClick = {

                        val patientId =
                            selectedPatientId
                                ?: return@Button

                        onSave(
                            patientId,
                            selectedMedications
                        )
                    },

                    enabled =
                        !isSaving &&
                                selectedPatientId != null &&
                                selectedMedications.isNotEmpty(),

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                52.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    navy
                            )
                ) {

                    if (
                        isSaving
                    ) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    18.dp
                                ),

                            strokeWidth =
                                2.dp,

                            color =
                                Color.White
                        )

                    } else {

                        Text(
                            text =
                                "Confirm Prescription",

                            color =
                                Color.White,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            }


            /*
             * Medication search
             */
            item {

                OutlinedTextField(
                    value =
                        search,

                    onValueChange = {
                        search =
                            it
                    },

                    placeholder = {

                        Text(
                            "Search medications…"
                        )
                    },

                    singleLine =
                        true,

                    colors =
                        OutlinedTextFieldDefaults
                            .colors(

                                focusedBorderColor =
                                    accent,

                                focusedContainerColor =
                                    Color.White,

                                unfocusedContainerColor =
                                    Color.White
                            ),

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }


            /*
             * Medication catalogue
             */
            filteredCatalog
                .forEach {
                        category ->


                    item {

                        Text(
                            text =
                                "${category.emoji}  ${category.name}",

                            color =
                                navy,

                            fontSize =
                                13.sp,

                            fontWeight =
                                FontWeight.Bold,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 8.dp,
                                        bottom = 4.dp
                                    )
                        )
                    }


                    items(
                        category.items
                    ) { med ->


                        val checked =
                            selectedMedications
                                .contains(
                                    med
                                )


                        Card(
                            colors =
                                CardDefaults
                                    .cardColors(
                                        containerColor =
                                            Color.White
                                    ),

                            shape =
                                RoundedCornerShape(
                                    12.dp
                                ),

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        vertical = 2.dp
                                    )
                        ) {


                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {

                                            selectedMedications =
                                                if (
                                                    checked
                                                ) {

                                                    selectedMedications -
                                                            med

                                                } else {

                                                    selectedMedications +
                                                            med
                                                }
                                        }
                                        .padding(
                                            horizontal =
                                                12.dp,
                                            vertical =
                                                7.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {


                                Checkbox(
                                    checked =
                                        checked,

                                    onCheckedChange = {

                                        selectedMedications =
                                            if (
                                                checked
                                            ) {

                                                selectedMedications -
                                                        med

                                            } else {

                                                selectedMedications +
                                                        med
                                            }
                                    },

                                    colors =
                                        CheckboxDefaults
                                            .colors(
                                                checkedColor =
                                                    accent
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
                                        med,

                                    color =
                                        navy,

                                    fontSize =
                                        14.sp
                                )
                            }
                        }
                    }
                }


            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )
            }
        }
    }
}


private fun formatRefillDate(
    value: String?
): String {

    if (
        value.isNullOrBlank()
    ) {
        return "recently"
    }


    return try {

        val date =
            value
                .substringBefore(
                    "T"
                )

        val parts =
            date.split(
                "-"
            )


        if (
            parts.size != 3
        ) {
            value
        } else {

            val year =
                parts[0]

            val month =
                parts[1]
                    .toInt()

            val day =
                parts[2]
                    .toInt()


            val months =
                listOf(
                    "January",
                    "February",
                    "March",
                    "April",
                    "May",
                    "June",
                    "July",
                    "August",
                    "September",
                    "October",
                    "November",
                    "December"
                )


            "${months[month - 1]} $day, $year"
        }

    } catch (_: Exception) {

        value
    }
}