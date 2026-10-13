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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.doctor.DoctorPatientCareRepository
import ehealthy.connect.data.patient.PatientHealthProfile
import ehealthy.connect.data.patient.PhysicalVisitInvoice
import ehealthy.connect.data.patient.PhysicalVisitRequest
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import kotlin.math.roundToInt


private enum class PatientCareTab(
    val title: String
) {
    OVERVIEW("Overview"),
    HEALTH("Health"),
    PHYSICAL_VISITS("Physical visits")
}


@Composable
fun DoctorPatientCareHub(
    appointmentId: String,
    patientName: String,
    onBack: () -> Unit
) {

    var selectedTab by remember {
        mutableStateOf(PatientCareTab.OVERVIEW)
    }

    var healthProfile by remember {
        mutableStateOf<PatientHealthProfile?>(null)
    }

    var physicalRequests by remember {
        mutableStateOf<List<PhysicalVisitRequest>>(emptyList())
    }

    var invoices by remember {
        mutableStateOf<List<PhysicalVisitInvoice>>(emptyList())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var refreshTrigger by remember {
        mutableStateOf(0)
    }


    LaunchedEffect(
        appointmentId,
        refreshTrigger
    ) {

        if (appointmentId.isBlank()) {
            errorMessage =
                "Appointment information is missing."

            isLoading =
                false

            return@LaunchedEffect
        }

        isLoading =
            true

        errorMessage =
            null


        DoctorPatientCareRepository
            .getPatientHealthProfile(
                appointmentId
            )
            .onSuccess {

                healthProfile =
                    it
            }
            .onFailure {

                errorMessage =
                    it.message
                        ?: "Could not load patient health information."
            }


        DoctorPatientCareRepository
            .getPhysicalVisitRequests(
                appointmentId
            )
            .onSuccess {

                physicalRequests =
                    it
            }


        DoctorPatientCareRepository
            .getPhysicalVisitInvoices(
                appointmentId
            )
            .onSuccess {

                invoices =
                    it
            }


        isLoading =
            false
    }


    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    DoctorColors.Background
                )
    ) {

        PatientCareTopBar(
            patientName =
                patientName,
            onBack =
                onBack,
            onRefresh = {
                refreshTrigger++
            }
        )


        PatientCareHero(
            patientName =
                patientName,
            profile =
                healthProfile,
            requestCount =
                physicalRequests.count {
                    it.status.equals(
                        "pending",
                        ignoreCase = true
                    )
                },
            invoiceCount =
                invoices.count {
                    it.status.equals(
                        "issued",
                        ignoreCase = true
                    ) ||
                            it.status.equals(
                                "pending",
                                ignoreCase = true
                            )
                }
        )


        PatientCareTabs(
            selectedTab =
                selectedTab,
            onSelected = {
                selectedTab =
                    it
            }
        )


        when {

            isLoading -> {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    contentAlignment =
                        Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color =
                            DoctorColors.Teal
                    )
                }
            }


            errorMessage != null &&
                    healthProfile == null -> {

                PatientCareError(
                    message =
                        errorMessage
                            ?: "Something went wrong.",
                    onRetry = {
                        refreshTrigger++
                    }
                )
            }


            else -> {

                when (selectedTab) {

                    PatientCareTab.OVERVIEW -> {

                        PatientOverviewContent(
                            profile =
                                healthProfile,
                            physicalRequests =
                                physicalRequests,
                            invoices =
                                invoices
                        )
                    }


                    PatientCareTab.HEALTH -> {

                        PatientHealthContent(
                            profile =
                                healthProfile
                        )
                    }


                    PatientCareTab.PHYSICAL_VISITS -> {

                        PhysicalVisitContent(
                            requests = physicalRequests,
                            invoices = invoices,
                            onRequestUpdated = {
                                refreshTrigger++
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PatientCareTopBar(
    patientName: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color.White
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        IconButton(
            onClick =
                onBack
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.ArrowBack,
                contentDescription =
                    "Back",
                tint =
                    DoctorColors.Ink
            )
        }


        Column(
            modifier =
                Modifier.weight(1f)
        ) {

            Text(
                text =
                    "Patient Care Hub",
                color =
                    DoctorColors.Ink,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    17.sp
            )

            Text(
                text =
                    patientName.ifBlank {
                        "Patient"
                    },
                color =
                    DoctorColors.Muted,
                fontSize =
                    11.sp
            )
        }


        IconButton(
            onClick =
                onRefresh
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Refresh,
                contentDescription =
                    "Refresh",
                tint =
                    DoctorColors.Teal
            )
        }
    }
}


@Composable
private fun PatientCareHero(
    patientName: String,
    profile: PatientHealthProfile?,
    requestCount: Int,
    invoiceCount: Int
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    DoctorColors.PatientCareGradient
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 22.dp
                )
    ) {

        Column {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(58.dp)
                            .background(
                                Color.White.copy(
                                    alpha = 0.18f
                                ),
                                CircleShape
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            initials(
                                patientName
                            ),
                        color =
                            Color.White,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            20.sp
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            14.dp
                        )
                )


                Column {

                    Text(
                        text =
                            patientName.ifBlank {
                                profile?.patientName
                                    ?: "Patient"
                            },
                        color =
                            Color.White,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            21.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )

                    Text(
                        text =
                            "Clinical patient workspace",
                        color =
                            Color.White.copy(
                                alpha = 0.82f
                            ),
                        fontSize =
                            11.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        9.dp
                    )
            ) {

                HeroMetric(
                    title =
                        "Blood group",
                    value =
                        cleanValue(
                            profile?.blood_group
                        ),
                    modifier =
                        Modifier.weight(1f)
                )


                HeroMetric(
                    title =
                        "Requests",
                    value =
                        requestCount.toString(),
                    modifier =
                        Modifier.weight(1f)
                )


                HeroMetric(
                    title =
                        "Invoices",
                    value =
                        invoiceCount.toString(),
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
private fun HeroMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier
                .background(
                    Color.White.copy(
                        alpha = 0.14f
                    ),
                    RoundedCornerShape(
                        16.dp
                    )
                )
                .padding(
                    12.dp
                )
    ) {

        Text(
            text =
                value,
            color =
                Color.White,
            fontWeight =
                FontWeight.ExtraBold,
            fontSize =
                16.sp
        )

        Text(
            text =
                title,
            color =
                Color.White.copy(
                    alpha = 0.78f
                ),
            fontSize =
                9.sp
        )
    }
}


@Composable
private fun PatientCareTabs(
    selectedTab: PatientCareTab,
    onSelected: (PatientCareTab) -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Color.White
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 10.dp
                ),
        horizontalArrangement =
            Arrangement.spacedBy(
                6.dp
            )
    ) {

        PatientCareTab.entries.forEach {
                tab ->

            val selected =
                tab == selectedTab


            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .background(
                            if (selected) {
                                DoctorColors.TealSoft
                            } else {
                                Color.Transparent
                            },
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .clickable {
                            onSelected(
                                tab
                            )
                        }
                        .padding(
                            vertical = 10.dp
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        tab.title,
                    color =
                        if (selected) {
                            DoctorColors.Teal
                        } else {
                            DoctorColors.Muted
                        },
                    fontWeight =
                        if (selected) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        },
                    fontSize =
                        11.sp,
                    textAlign =
                        TextAlign.Center
                )
            }
        }
    }
}


@Composable
private fun PatientOverviewContent(
    profile: PatientHealthProfile?,
    physicalRequests: List<PhysicalVisitRequest>,
    invoices: List<PhysicalVisitInvoice>
) {

    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                16.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        item {

            SectionTitle(
                title =
                    "Clinical snapshot",
                subtitle =
                    "Important information before consultation"
            )
        }


        item {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                SnapshotCard(
                    title =
                        "Allergies",
                    value =
                        cleanValue(
                            profile?.allergies
                        ),
                    background =
                        DoctorColors.RedSoft,
                    modifier =
                        Modifier.weight(1f)
                )


                SnapshotCard(
                    title =
                        "Chronic",
                    value =
                        cleanValue(
                            profile?.chronic
                        ),
                    background =
                        DoctorColors.AmberSoft,
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }


        item {

            SnapshotCard(
                title =
                    "Current medication",
                value =
                    cleanValue(
                        profile?.medication
                    ),
                background =
                    DoctorColors.BlueSoft,
                modifier =
                    Modifier.fillMaxWidth()
            )
        }


        item {

            SectionTitle(
                title =
                    "Current concern",
                subtitle =
                    "Patient-reported symptoms"
            )
        }


        item {

            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    ),
                shape =
                    RoundedCornerShape(
                        20.dp
                    ),
                elevation =
                    CardDefaults.cardElevation(
                        defaultElevation =
                            0.dp
                    ),
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(
                            16.dp
                        )
                ) {

                    CareInfoRow(
                        label =
                            "Symptoms",
                        value =
                            cleanValue(
                                profile?.current_symptoms
                            )
                    )

                    CareInfoRow(
                        label =
                            "Duration",
                        value =
                            cleanValue(
                                profile?.symptom_duration
                            )
                    )

                    CareInfoRow(
                        label =
                            "Severity",
                        value =
                            profile
                                ?.symptom_severity
                                ?.let {
                                    "$it / 10"
                                }
                                ?: "Not provided"
                    )
                }
            }
        }


        item {

            SectionTitle(
                title =
                    "Care workflow",
                subtitle =
                    "Physical follow-up and billing activity"
            )
        }


        item {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                WorkflowMetricCard(
                    value =
                        physicalRequests.size
                            .toString(),
                    title =
                        "Physical requests",
                    color =
                        DoctorColors.Purple,
                    background =
                        DoctorColors.PurpleSoft,
                    modifier =
                        Modifier.weight(1f)
                )


                WorkflowMetricCard(
                    value =
                        invoices.size
                            .toString(),
                    title =
                        "Invoices",
                    color =
                        DoctorColors.Green,
                    background =
                        DoctorColors.GreenSoft,
                    modifier =
                        Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
private fun PatientHealthContent(
    profile: PatientHealthProfile?
) {

    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                16.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                14.dp
            )
    ) {

        item {

            HealthSectionCard(
                title =
                    "Medical background",
                icon =
                    Icons.Outlined.HealthAndSafety,
                rows =
                    listOf(
                        "Blood group" to
                                cleanValue(
                                    profile?.blood_group
                                ),
                        "Allergies" to
                                cleanValue(
                                    profile?.allergies
                                ),
                        "Chronic conditions" to
                                cleanValue(
                                    profile?.chronic
                                ),
                        "Medication" to
                                cleanValue(
                                    profile?.medication
                                ),
                        "Previous surgeries" to
                                cleanValue(
                                    profile?.surgeries
                                ),
                        "Disability" to
                                cleanValue(
                                    profile?.disability
                                )
                    )
            )
        }


        item {

            HealthSectionCard(
                title =
                    "Current health concern",
                icon =
                    Icons.Outlined.MedicalServices,
                rows =
                    listOf(
                        "Symptoms" to
                                cleanValue(
                                    profile?.current_symptoms
                                ),
                        "Duration" to
                                cleanValue(
                                    profile?.symptom_duration
                                ),
                        "Severity" to
                                (
                                        profile
                                            ?.symptom_severity
                                            ?.let {
                                                "$it / 10"
                                            }
                                            ?: "Not provided"
                                        ),
                        "Family history" to
                                cleanValue(
                                    profile
                                        ?.family_medical_history
                                )
                    )
            )
        }


        item {

            HealthSectionCard(
                title =
                    "Lifestyle",
                icon =
                    Icons.Outlined.HealthAndSafety,
                rows =
                    listOf(
                        "Preferred language" to
                                cleanValue(
                                    profile
                                        ?.preferred_language
                                ),
                        "Smoking status" to
                                cleanValue(
                                    profile
                                        ?.smoking_status
                                ),
                        "Alcohol use" to
                                cleanValue(
                                    profile
                                        ?.alcohol_use
                                ),
                        "Height" to
                                (
                                        profile
                                            ?.height_cm
                                            ?.let {
                                                "$it cm"
                                            }
                                            ?: "Not provided"
                                        ),
                        "Weight" to
                                (
                                        profile
                                            ?.weight_kg
                                            ?.let {
                                                "$it kg"
                                            }
                                            ?: "Not provided"
                                        )
                    )
            )
        }


        item {

            HealthSectionCard(
                title =
                    "Emergency contact",
                icon =
                    Icons.Outlined.HealthAndSafety,
                rows =
                    listOf(
                        "Name" to
                                cleanValue(
                                    profile
                                        ?.emergency_contact_name
                                ),
                        "Relationship" to
                                cleanValue(
                                    profile
                                        ?.emergency_contact_relationship
                                ),
                        "Phone" to
                                cleanValue(
                                    profile
                                        ?.emergency_contact_phone
                                )
                    )
            )
        }
    }
}


@Composable
private fun PhysicalVisitContent(
    requests: List<PhysicalVisitRequest>,
    invoices: List<PhysicalVisitInvoice>,
    onRequestUpdated: () -> Unit
){

    LazyColumn(
        modifier =
            Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                16.dp
            ),
        verticalArrangement =
            Arrangement.spacedBy(
                12.dp
            )
    ) {

        item {

            SectionTitle(
                title =
                    "Physical follow-up",
                subtitle =
                    "Requests created after online consultations"
            )
        }


        if (
            requests.isEmpty()
        ) {

            item {

                EmptyPhysicalVisitCard()
            }

        } else {

            requests.forEach {
                    request ->

                item(
                    key =
                        request.id
                ) {

                    PhysicalRequestCard(
                        request = request,

                        hasInvoice =
                            invoices.any {
                                it.request_id == request.id
                            },

                        onRequestUpdated =
                            onRequestUpdated
                    )
                }
            }
        }


        if (
            invoices.isNotEmpty()
        ) {

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )

                SectionTitle(
                    title =
                        "Invoices",
                    subtitle =
                        "Physical visit billing"
                )
            }


            invoices.forEach {
                    invoice ->

                item(
                    key =
                        invoice.id
                ) {

                    PhysicalInvoiceCard(
                        invoice =
                            invoice
                    )
                }
            }
        }
    }
}


@Composable
private fun PhysicalRequestCard(
    request: PhysicalVisitRequest,
    hasInvoice: Boolean,
    onRequestUpdated: () -> Unit
) {

    val scope =
        rememberCoroutineScope()

    var isWorking by remember {
        mutableStateOf(false)
    }

    var actionError by remember {
        mutableStateOf<String?>(null)
    }

    var showInvoiceDialog by remember {
        mutableStateOf(false)
    }

    var showDeclineDialog by remember {
        mutableStateOf(false)
    }

    val isPending =
        request.status.equals(
            "pending",
            ignoreCase = true
        )
    val isAccepted =
        request.status.equals(
            "accepted",
            ignoreCase = true
        )



    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
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
                                42.dp
                            )
                            .background(
                                DoctorColors.PurpleSoft,
                                RoundedCornerShape(
                                    13.dp
                                )
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
                            DoctorColors.Purple
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
                            "Physical visit request",
                        color =
                            DoctorColors.Ink,
                        fontWeight =
                            FontWeight.ExtraBold,
                        fontSize =
                            14.sp
                    )

                    Text(
                        text =
                            request.status
                                .replaceFirstChar {
                                    it.uppercase()
                                },
                        color =
                            when {
                                request.status.equals(
                                    "accepted",
                                    ignoreCase = true
                                ) -> DoctorColors.Green

                                request.status.equals(
                                    "declined",
                                    ignoreCase = true
                                ) -> DoctorColors.Red

                                else ->
                                    DoctorColors.Amber
                            },
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.Bold
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
                    "Reason",
                color =
                    DoctorColors.Muted,
                fontSize =
                    10.sp,
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
                    request.reason,
                color =
                    DoctorColors.Ink,
                fontSize =
                    13.sp
            )


            request
                .doctor_response_note
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )

                    Text(
                        text =
                            "Doctor note",
                        color =
                            DoctorColors.Muted,
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            it,
                        color =
                            DoctorColors.Ink,
                        fontSize =
                            12.sp
                    )
                }


            if (actionError != null) {

                Spacer(
                    modifier =
                        Modifier.height(
                            10.dp
                        )
                )

                Text(
                    text =
                        actionError
                            ?: "",
                    color =
                        DoctorColors.Red,
                    fontSize =
                        10.sp
                )
            }


            if (isPending) {

                Spacer(
                    modifier =
                        Modifier.height(
                            15.dp
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

                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = !isWorking,
                        onClick = {
                            showDeclineDialog = true
                        }
                    ) {

                        Text(
                            text = "Decline",
                            color = DoctorColors.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }


                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .background(
                                    DoctorColors.Teal,
                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )
                                .clickable(
                                    enabled =
                                        !isWorking
                                ) {

                                    scope.launch {

                                        isWorking =
                                            true

                                        actionError =
                                            null


                                        DoctorPatientCareRepository
                                            .acceptPhysicalVisitRequest(
                                                request.id
                                            )
                                            .onSuccess {

                                                onRequestUpdated()
                                            }
                                            .onFailure {

                                                actionError =
                                                    it.message
                                                        ?: "Could not accept request."
                                            }


                                        isWorking =
                                            false
                                    }
                                }
                                .padding(
                                    vertical =
                                        12.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        if (isWorking) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(
                                        18.dp
                                    ),
                                color =
                                    Color.White,
                                strokeWidth =
                                    2.dp
                            )

                        } else {

                            Text(
                                text =
                                    "Accept request",
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
            if (
                isAccepted &&
                !hasInvoice
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            14.dp
                        )
                )

                Button(
                    onClick = {
                        showInvoiceDialog =
                            true
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                DoctorColors.Teal
                        )
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.ReceiptLong,
                        contentDescription =
                            null
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                7.dp
                            )
                    )

                    Text(
                        text =
                            "Create physical visit invoice",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            11.sp
                    )
                }
            }
            if (
                isAccepted &&
                hasInvoice
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            13.dp
                        )
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                DoctorColors.GreenSoft,
                                RoundedCornerShape(
                                    14.dp
                                )
                            )
                            .padding(
                                12.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.ReceiptLong,
                        contentDescription =
                            null,
                        tint =
                            DoctorColors.Green
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )

                    Column {

                        Text(
                            text =
                                "Invoice issued",
                            color =
                                DoctorColors.Green,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                11.sp
                        )

                        Text(
                            text =
                                "Waiting for the patient's decision.",
                            color =
                                DoctorColors.Muted,
                            fontSize =
                                9.sp
                        )
                    }
                }
            }
        }
    }
    if (showInvoiceDialog) {

        PhysicalVisitInvoiceDialog(
            requestId =
                request.id,

            onDismiss = {
                showInvoiceDialog =
                    false
            },

            onInvoiceIssued = {

                showInvoiceDialog =
                    false

                onRequestUpdated()
            }
        )
    }

    if (showDeclineDialog) {

        DeclinePhysicalVisitDialog(
            requestId = request.id,

            onDismiss = {
                showDeclineDialog = false
            },

            onDeclined = {
                showDeclineDialog = false
                onRequestUpdated()
            }
        )
    }
}

@Composable
private fun DeclinePhysicalVisitDialog(
    requestId: String,
    onDismiss: () -> Unit,
    onDeclined: () -> Unit
) {

    val scope = rememberCoroutineScope()

    var reason by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }


    AlertDialog(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },

        title = {

            Text(
                text = "Decline physical visit",
                color = DoctorColors.Ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        },

        text = {

            Column {

                Text(
                    text = "Give the patient a short clinical reason for declining the physical follow-up.",
                    color = DoctorColors.Muted,
                    fontSize = 10.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )


                OutlinedTextField(
                    value = reason,

                    onValueChange = {
                        reason = it
                        errorMessage = null
                    },

                    label = {
                        Text("Reason")
                    },

                    placeholder = {
                        Text(
                            "e.g. Physical examination is not required at this stage."
                        )
                    },

                    modifier = Modifier.fillMaxWidth(),

                    minLines = 3,

                    maxLines = 5
                )


                if (errorMessage != null) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = errorMessage ?: "",
                        color = DoctorColors.Red,
                        fontSize = 10.sp
                    )
                }
            }
        },

        confirmButton = {

            Button(
                enabled = !isSaving,

                onClick = {

                    if (reason.trim().length < 5) {

                        errorMessage =
                            "Please provide a short reason."

                    } else {

                        scope.launch {

                            isSaving = true
                            errorMessage = null


                            DoctorPatientCareRepository
                                .declinePhysicalVisitRequest(
                                    requestId = requestId,
                                    doctorNote = reason.trim()
                                )
                                .onSuccess {

                                    onDeclined()
                                }
                                .onFailure {

                                    errorMessage =
                                        it.message
                                            ?: "Could not decline request."
                                }


                            isSaving = false
                        }
                    }
                },

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            DoctorColors.Red
                    )
            ) {

                if (isSaving) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(17.dp),
                        color =
                            Color.White,
                        strokeWidth =
                            2.dp
                    )

                } else {

                    Text(
                        text = "Decline request",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },

        dismissButton = {

            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {

                Text(
                    text = "Cancel",
                    color = DoctorColors.Muted
                )
            }
        },

        containerColor = Color.White,

        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun PhysicalVisitInvoiceDialog(
    requestId: String,
    onDismiss: () -> Unit,
    onInvoiceIssued: () -> Unit
) {

    val scope =
        rememberCoroutineScope()


    var service by remember {
        mutableStateOf("")
    }

    var amount by remember {
        mutableStateOf("")
    }

    var date by remember {
        mutableStateOf("")
    }

    var time by remember {
        mutableStateOf("")
    }

    var location by remember {
        mutableStateOf("")
    }

    var notes by remember {
        mutableStateOf("")
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }


    AlertDialog(
        onDismissRequest = {

            if (!isSaving) {
                onDismiss()
            }
        },

        title = {

            Column {

                Text(
                    text =
                        "Create physical visit invoice",
                    color =
                        DoctorColors.Ink,
                    fontWeight =
                        FontWeight.ExtraBold,
                    fontSize =
                        18.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                Text(
                    text =
                        "The patient must review and accept this invoice before the physical visit.",
                    color =
                        DoctorColors.Muted,
                    fontSize =
                        10.sp
                )
            }
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                OutlinedTextField(
                    value =
                        service,
                    onValueChange = {
                        service =
                            it

                        errorMessage =
                            null
                    },
                    label = {
                        Text(
                            "Service"
                        )
                    },
                    placeholder = {
                        Text(
                            "e.g. Physical examination"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true
                )


                OutlinedTextField(
                    value =
                        amount,
                    onValueChange = { newValue ->

                        if (
                            newValue.all {
                                it.isDigit() ||
                                        it == '.'
                            }
                        ) {

                            amount =
                                newValue
                        }

                        errorMessage =
                            null
                    },
                    label = {
                        Text(
                            "Amount (R)"
                        )
                    },
                    placeholder = {
                        Text(
                            "650.00"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    singleLine =
                        true
                )


                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        )
                ) {

                    OutlinedTextField(
                        value =
                            date,
                        onValueChange = {
                            date =
                                it

                            errorMessage =
                                null
                        },
                        label = {
                            Text(
                                "Date"
                            )
                        },
                        placeholder = {
                            Text(
                                "2026-10-15"
                            )
                        },
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        singleLine =
                            true
                    )


                    OutlinedTextField(
                        value =
                            time,
                        onValueChange = {
                            time =
                                it

                            errorMessage =
                                null
                        },
                        label = {
                            Text(
                                "Time"
                            )
                        },
                        placeholder = {
                            Text(
                                "10:30"
                            )
                        },
                        modifier =
                            Modifier.weight(
                                1f
                            ),
                        singleLine =
                            true
                    )
                }


                OutlinedTextField(
                    value =
                        location,
                    onValueChange = {
                        location =
                            it

                        errorMessage =
                            null
                    },
                    label = {
                        Text(
                            "Location"
                        )
                    },
                    placeholder = {
                        Text(
                            "e.g. Riverside Medical Centre"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )


                OutlinedTextField(
                    value =
                        notes,
                    onValueChange = {
                        notes =
                            it
                    },
                    label = {
                        Text(
                            "Additional notes"
                        )
                    },
                    placeholder = {
                        Text(
                            "Optional instructions for the patient"
                        )
                    },
                    modifier =
                        Modifier.fillMaxWidth(),
                    minLines =
                        2,
                    maxLines =
                        4
                )


                if (
                    errorMessage != null
                ) {

                    Text(
                        text =
                            errorMessage
                                ?: "",
                        color =
                            DoctorColors.Red,
                        fontSize =
                            10.sp
                    )
                }
            }
        },

        confirmButton = {

            Button(
                enabled =
                    !isSaving,

                onClick = {

                    val amountRands =
                        amount
                            .trim()
                            .toDoubleOrNull()


                    when {

                        service.trim().length < 3 -> {

                            errorMessage =
                                "Please enter the service being provided."
                        }


                        amountRands == null ||
                                amountRands <= 0 -> {

                            errorMessage =
                                "Enter a valid invoice amount."
                        }


                        date.isBlank() -> {

                            errorMessage =
                                "Enter the proposed visit date."
                        }


                        time.isBlank() -> {

                            errorMessage =
                                "Enter the proposed visit time."
                        }


                        location.trim().length < 3 -> {

                            errorMessage =
                                "Enter the physical visit location."
                        }


                        else -> {

                            val amountMinor =
                                (
                                        amountRands *
                                                100.0
                                        )
                                    .roundToInt()


                            scope.launch {

                                isSaving =
                                    true

                                errorMessage =
                                    null


                                DoctorPatientCareRepository
                                    .issuePhysicalVisitInvoice(
                                        requestId =
                                            requestId,

                                        serviceDescription =
                                            service,

                                        amountMinor =
                                            amountMinor,

                                        proposedDate =
                                            date,

                                        proposedTime =
                                            time,

                                        location =
                                            location,

                                        notes =
                                            notes
                                    )
                                    .onSuccess {

                                        onInvoiceIssued()
                                    }
                                    .onFailure {

                                        errorMessage =
                                            it.message
                                                ?: "Could not issue invoice."
                                    }


                                isSaving =
                                    false
                            }
                        }
                    }
                },

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            DoctorColors.Teal
                    )
            ) {

                if (isSaving) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                17.dp
                            ),
                        color =
                            Color.White,
                        strokeWidth =
                            2.dp
                    )

                } else {

                    Text(
                        text =
                            "Issue invoice",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },

        dismissButton = {

            TextButton(
                enabled =
                    !isSaving,

                onClick =
                    onDismiss
            ) {

                Text(
                    text =
                        "Cancel",
                    color =
                        DoctorColors.Muted
                )
            }
        },

        containerColor =
            Color.White,

        shape =
            RoundedCornerShape(
                24.dp
            )
    )
}


@Composable
private fun PhysicalInvoiceCard(
    invoice: PhysicalVisitInvoice
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    DoctorColors.GreenSoft
            ),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
                    16.dp
                )
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.ReceiptLong,
                    contentDescription =
                        null,
                    tint =
                        DoctorColors.Green
                )


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
                            invoice.service_description,
                        color =
                            DoctorColors.Ink,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.sp
                    )

                    Text(
                        text =
                            invoice.status
                                .replaceFirstChar {
                                    it.uppercase()
                                },
                        color =
                            DoctorColors.Green,
                        fontSize =
                            10.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }


                Text(
                    text =
                        formatInvoiceAmount(
                            invoice
                        ),
                    color =
                        DoctorColors.Ink,
                    fontWeight =
                        FontWeight.ExtraBold,
                    fontSize =
                        15.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            CareInfoRow(
                label =
                    "Date",
                value =
                    invoice.proposed_date
            )

            CareInfoRow(
                label =
                    "Time",
                value =
                    invoice.proposed_time
            )

            CareInfoRow(
                label =
                    "Location",
                value =
                    invoice.location_name
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: invoice.location
            )
        }
    }
}


@Composable
private fun HealthSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    rows: List<Pair<String, String>>
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    Color.White
            ),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    0.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(
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
                                40.dp
                            )
                            .background(
                                DoctorColors.TealSoft,
                                RoundedCornerShape(
                                    12.dp
                                )
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
                            DoctorColors.Teal
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(
                            10.dp
                        )
                )


                Text(
                    text =
                        title,
                    color =
                        DoctorColors.Ink,
                    fontWeight =
                        FontWeight.ExtraBold,
                    fontSize =
                        14.sp
                )
            }


            Spacer(
                modifier =
                    Modifier.height(
                        12.dp
                    )
            )


            rows.forEach {
                    row ->

                CareInfoRow(
                    label =
                        row.first,
                    value =
                        row.second
                )
            }
        }
    }
}




@Composable
private fun SnapshotCard(
    title: String,
    value: String,
    background: Color,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier
                .background(
                    background,
                    RoundedCornerShape(
                        18.dp
                    )
                )
                .padding(
                    14.dp
                )
    ) {

        Text(
            text =
                title,
            color =
                DoctorColors.Muted,
            fontSize =
                10.sp,
            fontWeight =
                FontWeight.Bold
        )


        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )


        Text(
            text =
                value,
            color =
                DoctorColors.Ink,
            fontSize =
                13.sp,
            fontWeight =
                FontWeight.ExtraBold
        )
    }
}


@Composable
private fun WorkflowMetricCard(
    value: String,
    title: String,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier
) {

    Column(
        modifier =
            modifier
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
                value,
            color =
                color,
            fontSize =
                22.sp,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            text =
                title,
            color =
                DoctorColors.Ink,
            fontSize =
                11.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun CareInfoRow(
    label: String,
    value: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 7.dp
                ),
        verticalAlignment =
            Alignment.Top
    ) {

        Text(
            text =
                label,
            modifier =
                Modifier.weight(
                    0.42f
                ),
            color =
                DoctorColors.Muted,
            fontSize =
                11.sp
        )


        Text(
            text =
                value,
            modifier =
                Modifier.weight(
                    0.58f
                ),
            color =
                DoctorColors.Ink,
            fontSize =
                11.sp,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}


@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {

    Column {

        Text(
            text =
                title,
            color =
                DoctorColors.Ink,
            fontWeight =
                FontWeight.ExtraBold,
            fontSize =
                17.sp
        )

        Text(
            text =
                subtitle,
            color =
                DoctorColors.Muted,
            fontSize =
                10.sp
        )
    }
}


@Composable
private fun EmptyPhysicalVisitCard() {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    DoctorColors.TealSoft
            ),
        shape =
            RoundedCornerShape(
                20.dp
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

            Icon(
                imageVector =
                    Icons.Outlined.MedicalServices,
                contentDescription =
                    null,
                tint =
                    DoctorColors.Teal,
                modifier =
                    Modifier.size(
                        34.dp
                    )
            )


            Spacer(
                modifier =
                    Modifier.height(
                        9.dp
                    )
            )


            Text(
                text =
                    "No physical visit requests",
                color =
                    DoctorColors.Ink,
                fontWeight =
                    FontWeight.Bold,
                fontSize =
                    13.sp
            )


            Text(
                text =
                    "Requests will appear here after an online consultation.",
                color =
                    DoctorColors.Muted,
                fontSize =
                    10.sp,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}


@Composable
private fun PatientCareError(
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
                    message,
                color =
                    DoctorColors.Red,
                textAlign =
                    TextAlign.Center,
                fontSize =
                    12.sp
            )


            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )


            TextButton(
                onClick =
                    onRetry
            ) {

                Text(
                    text =
                        "Try again",
                    color =
                        DoctorColors.Teal,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}


private fun cleanValue(
    value: String?
): String {

    return value
        ?.trim()
        ?.takeIf {
            it.isNotBlank()
        }
        ?: "Not provided"
}


private fun initials(
    name: String
): String {

    val parts =
        name
            .trim()
            .split(
                " "
            )
            .filter {
                it.isNotBlank()
            }

    return parts
        .take(
            2
        )
        .mapNotNull {
            it.firstOrNull()
        }
        .joinToString(
            ""
        )
        .uppercase()
        .ifBlank {
            "PT"
        }
}


private fun formatInvoiceAmount(
    invoice: PhysicalVisitInvoice
): String {

    val amount =
        invoice.amount_minor / 100.0

    return "${invoice.currency} %.2f".format(
        amount
    )
}