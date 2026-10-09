package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.serialization.Serializable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Verified
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Serializable
data class PrescriptionItem(
    val medication: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val duration: String = "",
    val instructions: String? = null
)

@Serializable
data class Prescription(
    val id: String,
    val patient_id: String,
    val medication: String? = null,
    val created_at: String? = null,
    val doctor_id: String? = null,
    val refill_status: String? = null,
    val refill_requested_at: String? = null,

    val prescription_reference: String? = null,
    val patient_name: String? = null,
    val doctor_name: String? = null,
    val doctor_discipline: String? = null,
    val doctor_practice_number: String? = null,
    val doctor_hpcsa_number: String? = null,
    val prescription_items: List<PrescriptionItem>? = null,
    val general_instructions: String? = null,
    val doctor_signature_data: String? = null,
    val doctor_stamp_data: String? = null,
    val is_official: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientPrescriptionsScreen(
    prescriptions: List<Prescription>,
    isLoading: Boolean = false,
    onBack: () -> Unit = {},
    doctorNames: Map<String, String> = emptyMap(),
    doctorPhotos: Map<String, String> = emptyMap(),
    onRequestRefill: (Prescription) -> Unit = {}
) {

    val context =
        LocalContext.current

    var pendingPdfPrescription by remember {
        mutableStateOf<Prescription?>(
            null
        )
    }

    var pendingPdfDoctorName by remember {
        mutableStateOf<String?>(
            null
        )
    }

    val createPdfLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "application/pdf"
                )
        ) { uri ->

            val prescription =
                pendingPdfPrescription

            val fallbackDoctorName =
                pendingPdfDoctorName

            if (
                uri != null &&
                prescription != null
            ) {

                runCatching {

                    context
                        .contentResolver
                        .openOutputStream(
                            uri
                        )
                        ?.use { output ->

                            PrescriptionPdfExporter.writePrescriptionPdf(
                                context = context,
                                prescription = prescription,
                                fallbackDoctorName = fallbackDoctorName,
                                output = output
                            )
                        }
                        ?: error(
                            "Could not open the selected file."
                        )
                }.onSuccess {

                    Toast
                        .makeText(
                            context,
                            "Prescription PDF saved.",
                            Toast.LENGTH_LONG
                        )
                        .show()

                }.onFailure {

                    Toast
                        .makeText(
                            context,
                            it.message
                                ?: "Could not save prescription PDF.",
                            Toast.LENGTH_LONG
                        )
                        .show()
                }
            }

            pendingPdfPrescription =
                null

            pendingPdfDoctorName =
                null
        }

    val sortedPrescriptions =
        remember(prescriptions) {

            prescriptions.sortedByDescending {
                it.created_at ?: ""
            }
        }


    Scaffold(
        topBar = {

            TopAppBar(
                title = {

                    Column {

                        Text(
                            text = "My Prescriptions",
                            color = PatientColors.TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )

                        Text(
                            text =
                                if (sortedPrescriptions.isEmpty()) {
                                    "Your prescribed medication"
                                } else {
                                    "${sortedPrescriptions.size} prescription${
                                        if (sortedPrescriptions.size == 1) "" else "s"
                                    }"
                                },
                            color = PatientColors.TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .background(
                                        PatientColors.PurpleSoft,
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
                                    PatientColors.Purple,
                                modifier =
                                    Modifier.size(20.dp)
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
            )
        },

        containerColor =
            MaterialTheme.colorScheme.background

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
                            .padding(paddingValues),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Card(
                        shape =
                            RoundedCornerShape(24.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PatientColors.PurpleSoft
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
                                    vertical = 26.dp
                                ),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(62.dp)
                                        .background(
                                            PatientColors.Purple
                                                .copy(alpha = 0.10f),
                                            CircleShape
                                        ),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                CircularProgressIndicator(
                                    modifier =
                                        Modifier.size(30.dp),
                                    color =
                                        PatientColors.Purple,
                                    strokeWidth =
                                        2.5.dp
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(12.dp)
                            )


                            Text(
                                text =
                                    "Loading prescriptions",
                                color =
                                    PatientColors.TextPrimary,
                                fontSize =
                                    14.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )


                            Text(
                                text =
                                    "Getting your medication records...",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.5.sp
                            )
                        }
                    }
                }
            }


            /*
             * Empty state
             */
            sortedPrescriptions.isEmpty() -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(24.dp),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PatientColors.PurpleSoft
                            ),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 0.dp
                            )
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Box(
                                modifier =
                                    Modifier
                                        .size(70.dp)
                                        .background(
                                            PatientColors.Purple
                                                .copy(alpha = 0.10f),
                                            CircleShape
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
                                        PatientColors.Purple,
                                    modifier =
                                        Modifier.size(34.dp)
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(14.dp)
                            )


                            Text(
                                text =
                                    "No prescriptions yet",
                                color =
                                    PatientColors.TextPrimary,
                                fontSize =
                                    15.sp,
                                fontWeight =
                                    FontWeight.ExtraBold
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )


                            Text(
                                text =
                                    "Prescriptions issued by your doctors will appear here after a consultation.",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.5.sp,
                                lineHeight =
                                    15.sp,
                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }
                }
            }


            /*
             * Main content
             */
            else -> {

                LazyColumn(
                    modifier =
                        Modifier.padding(
                            paddingValues
                        ),

                    contentPadding =
                        PaddingValues(
                            horizontal = 18.dp,
                            vertical = 12.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            12.dp
                        )
                ) {

                    /*
                     * Intro card
                     */
                    item {

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
                                        PatientColors.PurpleSoft
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
                                                PatientColors.Purple
                                                    .copy(
                                                        alpha = 0.11f
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
                                            Icons.Outlined.Medication,
                                        contentDescription =
                                            null,
                                        tint =
                                            PatientColors.Purple,
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
                                            "Medication history",
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
                                            "Review medications prescribed by your healthcare providers and request refills when available.",
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
                     * Section label
                     */
                    item {

                        Column {

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        4.dp
                                    )
                            )


                            Text(
                                text =
                                    "YOUR PRESCRIPTIONS",
                                color =
                                    PatientColors.TextSecondary,
                                fontSize =
                                    10.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                letterSpacing =
                                    0.7.sp
                            )
                        }
                    }


                    /*
                     * Prescription cards
                     */
                    items(
                        sortedPrescriptions,
                        key = {
                            it.id
                        }
                    ) { prescription ->

                        PrescriptionCard(
                            prescription =
                                prescription,

                            doctorName =
                                prescription.doctor_id
                                    ?.let {
                                        doctorNames[it]
                                    },

                            doctorPhoto =
                                prescription.doctor_id
                                    ?.let {
                                        doctorPhotos[it]
                                    },

                            onRequestRefill = {
                                onRequestRefill(
                                    prescription
                                )
                            },

                            onDownloadPdf = {

                                val hasOfficialPdf =
                                    prescription.is_official &&
                                            !prescription.doctor_signature_data
                                                .isNullOrBlank() &&
                                            !prescription.doctor_stamp_data
                                                .isNullOrBlank()

                                if (
                                    !hasOfficialPdf
                                ) {

                                    Toast
                                        .makeText(
                                            context,
                                            "This older prescription does not have the official signed PDF details.",
                                            Toast.LENGTH_LONG
                                        )
                                        .show()

                                } else {

                                    pendingPdfPrescription =
                                        prescription

                                    pendingPdfDoctorName =
                                        prescription.doctor_name
                                            ?: prescription.doctor_id
                                                ?.let {
                                                    doctorNames[
                                                        it
                                                    ]
                                                }

                                    val reference =
                                        prescription
                                            .prescription_reference
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: prescription.id
                                                .take(
                                                    8
                                                )

                                    createPdfLauncher.launch(
                                        "EHealthy_Prescription_$reference.pdf"
                                    )
                                }
                            }
                        )
                    }


                    item {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    12.dp
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrescriptionCard(
    prescription: Prescription,
    doctorName: String?,
    doctorPhoto: String?,
    onRequestRefill: () -> Unit,
    onDownloadPdf: () -> Unit
) {

    val dateLabel =
        remember(prescription.created_at) {
            formatPrescriptionDate(
                prescription.created_at
            )
        }

    val refillStatus =
        prescription.refill_status
            ?.lowercase()
            ?.trim()


    Card(
        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(22.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            ),

        border =
            BorderStroke(
                width = 1.dp,
                color =
                    PatientColors.Purple
                        .copy(alpha = 0.10f)
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            PatientColors.Purple
                        )
            )


            Column(
                modifier =
                    Modifier.padding(17.dp)
            ) {

                /*
                 * Medication + doctor
                 */
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(52.dp)
                                .clip(
                                    RoundedCornerShape(
                                        16.dp
                                    )
                                )
                                .background(
                                    PatientColors.PurpleSoft
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        if (
                            !doctorPhoto
                                .isNullOrBlank()
                        ) {

                            AsyncImage(
                                model =
                                    doctorPhoto,

                                contentDescription =
                                    doctorName,

                                contentScale =
                                    ContentScale.Crop,

                                modifier =
                                    Modifier
                                        .size(52.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                16.dp
                                            )
                                        )
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Medication,

                                contentDescription =
                                    null,

                                tint =
                                    PatientColors.Purple,

                                modifier =
                                    Modifier.size(
                                        25.dp
                                    )
                            )
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.width(12.dp)
                    )


                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text =
                                prescription.prescription_items
                                    ?.mapNotNull {
                                        it.medication
                                            .takeIf {
                                                    name ->
                                                name.isNotBlank()
                                            }
                                    }
                                    ?.takeIf {
                                        it.isNotEmpty()
                                    }
                                    ?.joinToString(
                                        ", "
                                    )
                                    ?: prescription.medication
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }
                                    ?: "Medication not specified",

                            color =
                                PatientColors.TextPrimary,

                            fontSize =
                                15.sp,

                            fontWeight =
                                FontWeight.ExtraBold
                        )


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )


                        Text(
                            text =
                                doctorName
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: "Prescribed by your doctor",

                            color =
                                PatientColors.TextSecondary,

                            fontSize =
                                11.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )


                /*
                 * Prescription date
                 */
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
                                PatientColors.NeutralCard
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 10.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .background(
                                    PatientColors
                                        .AppointmentAccent
                                        .copy(alpha = 0.10f),
                                    CircleShape
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
                                    16.dp
                                )
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.width(9.dp)
                    )


                    Column {

                        Text(
                            text =
                                "PRESCRIBED",

                            color =
                                PatientColors.TextSecondary,

                            fontSize =
                                8.5.sp,

                            fontWeight =
                                FontWeight.Bold
                        )


                        Text(
                            text =
                                dateLabel,

                            color =
                                PatientColors.TextPrimary,

                            fontSize =
                                11.sp,

                            fontWeight =
                                FontWeight.SemiBold
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )


                /*
                 * Official signed prescription PDF
                 */
                val officialPdfReady =
                    prescription.is_official &&
                            !prescription.doctor_signature_data
                                .isNullOrBlank() &&
                            !prescription.doctor_stamp_data
                                .isNullOrBlank()


                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    15.dp
                                )
                            )
                            .background(
                                if (
                                    officialPdfReady
                                ) {
                                    PatientColors.SuccessCard
                                } else {
                                    PatientColors.NeutralCard
                                }
                            )
                            .padding(
                                horizontal =
                                    13.dp,
                                vertical =
                                    12.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            if (
                                officialPdfReady
                            ) {
                                Icons.Outlined.Verified
                            } else {
                                Icons.Outlined.Medication
                            },
                        contentDescription =
                            null,
                        tint =
                            if (
                                officialPdfReady
                            ) {
                                PatientColors.SuccessAccent
                            } else {
                                PatientColors.TextSecondary
                            },
                        modifier =
                            Modifier.size(
                                20.dp
                            )
                    )


                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
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
                                if (
                                    officialPdfReady
                                ) {
                                    "Official prescription"
                                } else {
                                    "Legacy prescription"
                                },
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                11.5.sp
                        )


                        Text(
                            text =
                                if (
                                    officialPdfReady
                                ) {
                                    "Includes EHealthy branding, doctor signature and stamp."
                                } else {
                                    "This prescription was created before signed PDF support."
                                },
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                9.5.sp,
                            lineHeight =
                                13.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )


                TextButton(
                    onClick =
                        onDownloadPdf,
                    enabled =
                        officialPdfReady,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .background(
                                PatientColors.PurpleSoft
                            )
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Download,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.Purple,
                        modifier =
                            Modifier.size(
                                17.dp
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
                            "Download signed PDF",
                        color =
                            PatientColors.Purple,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            12.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                /*
                 * Refill status
                 */
                when (refillStatus) {

                    /*
                     * Waiting for doctor
                     */
                    "requested" -> {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(
                                            15.dp
                                        )
                                    )
                                    .background(
                                        PatientColors.TipsCard
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
                                        .size(34.dp)
                                        .background(
                                            PatientColors.TipsAccent
                                                .copy(
                                                    alpha = 0.12f
                                                ),
                                            CircleShape
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
                                        PatientColors.TipsAccent,

                                    modifier =
                                        Modifier.size(
                                            17.dp
                                        )
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.width(10.dp)
                            )


                            Column {

                                Text(
                                    text =
                                        "Refill requested",

                                    color =
                                        PatientColors.TipsAccent,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        11.5.sp
                                )


                                Text(
                                    text =
                                        "Waiting for your doctor to review your request.",

                                    color =
                                        PatientColors.TextSecondary,

                                    fontSize =
                                        9.5.sp,

                                    lineHeight =
                                        13.sp
                                )
                            }
                        }
                    }


                    /*
                     * Doctor approved
                     */
                    "approved" -> {

                        Column {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(
                                            RoundedCornerShape(
                                                15.dp
                                            )
                                        )
                                        .background(
                                            PatientColors.SuccessCard
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
                                            .size(34.dp)
                                            .background(
                                                PatientColors.SuccessAccent
                                                    .copy(
                                                        alpha = 0.12f
                                                    ),
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
                                            PatientColors.SuccessAccent,

                                        modifier =
                                            Modifier.size(
                                                17.dp
                                            )
                                    )
                                }


                                Spacer(
                                    modifier =
                                        Modifier.width(10.dp)
                                )


                                Column {

                                    Text(
                                        text =
                                            "Refill approved",

                                        color =
                                            PatientColors.SuccessAccent,

                                        fontWeight =
                                            FontWeight.Bold,

                                        fontSize =
                                            11.5.sp
                                    )


                                    Text(
                                        text =
                                            "Your doctor approved your refill request.",

                                        color =
                                            PatientColors.TextSecondary,

                                        fontSize =
                                            9.5.sp,

                                        lineHeight =
                                            13.sp
                                    )
                                }
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )


                            TextButton(
                                onClick =
                                    onRequestRefill,

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.Refresh,

                                    contentDescription =
                                        null,

                                    tint =
                                        PatientColors.SuccessAccent,

                                    modifier =
                                        Modifier.size(
                                            16.dp
                                        )
                                )


                                Spacer(
                                    modifier =
                                        Modifier.width(6.dp)
                                )


                                Text(
                                    text =
                                        "Request another refill",

                                    color =
                                        PatientColors.SuccessAccent,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        11.5.sp
                                )
                            }
                        }
                    }


                    /*
                     * Doctor declined
                     */
                    "declined" -> {

                        Column {

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(
                                            RoundedCornerShape(
                                                15.dp
                                            )
                                        )
                                        .background(
                                            PatientColors.RedSoft
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
                                            .size(34.dp)
                                            .background(
                                                PatientColors.Red
                                                    .copy(
                                                        alpha = 0.12f
                                                    ),
                                                CircleShape
                                            ),

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Icon(
                                        imageVector =
                                            Icons.Outlined.Close,

                                        contentDescription =
                                            null,

                                        tint =
                                            PatientColors.Red,

                                        modifier =
                                            Modifier.size(
                                                17.dp
                                            )
                                    )
                                }


                                Spacer(
                                    modifier =
                                        Modifier.width(10.dp)
                                )


                                Column {

                                    Text(
                                        text =
                                            "Refill declined",

                                        color =
                                            PatientColors.Red,

                                        fontWeight =
                                            FontWeight.Bold,

                                        fontSize =
                                            11.5.sp
                                    )


                                    Text(
                                        text =
                                            "Your doctor did not approve this refill request.",

                                        color =
                                            PatientColors.TextSecondary,

                                        fontSize =
                                            9.5.sp,

                                        lineHeight =
                                            13.sp
                                    )
                                }
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )


                            TextButton(
                                onClick =
                                    onRequestRefill,

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.Refresh,

                                    contentDescription =
                                        null,

                                    tint =
                                        PatientColors.Purple,

                                    modifier =
                                        Modifier.size(
                                            16.dp
                                        )
                                )


                                Spacer(
                                    modifier =
                                        Modifier.width(6.dp)
                                )


                                Text(
                                    text =
                                        "Request again",

                                    color =
                                        PatientColors.Purple,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize =
                                        11.5.sp
                                )
                            }
                        }
                    }


                    /*
                     * No refill request yet
                     */
                    else -> {

                        TextButton(
                            onClick =
                                onRequestRefill,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(
                                        RoundedCornerShape(
                                            14.dp
                                        )
                                    )
                                    .background(
                                        PatientColors.SuccessCard
                                    )
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Refresh,

                                contentDescription =
                                    null,

                                tint =
                                    PatientColors.SuccessAccent,

                                modifier =
                                    Modifier.size(
                                        17.dp
                                    )
                            )


                            Spacer(
                                modifier =
                                    Modifier.width(7.dp)
                            )


                            Text(
                                text =
                                    "Request refill",

                                color =
                                    PatientColors.SuccessAccent,

                                fontSize =
                                    12.sp,

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

private fun formatPrescriptionDate(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return "Date unknown"
    return try {
        // created_at from Supabase looks like "2026-01-15T09:30:00+00:00" — take the date part
        val datePart = isoTimestamp.substringBefore("T")
        val (year, month, day) = datePart.split("-")
        val months = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        "Prescribed ${months[month.toInt() - 1]} ${day.toInt()}, $year"
    } catch (e: Exception) {
        isoTimestamp
    }
}
