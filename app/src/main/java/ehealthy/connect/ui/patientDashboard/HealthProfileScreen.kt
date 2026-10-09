package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.data.patient.PatientHealthProfile
import ehealthy.connect.data.patient.PatientRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthProfileScreen(
    onBack: () -> Unit,
    onOpenCareFile: () -> Unit = {}
) {

    val scope =
        rememberCoroutineScope()

    var profile by remember {
        mutableStateOf(
            PatientHealthProfile()
        )
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var successMessage by remember {
        mutableStateOf<String?>(
            null
        )
    }

    fun updateProfile(
        transform: (PatientHealthProfile) -> PatientHealthProfile
    ) {

        profile =
            transform(
                profile
            )

        errorMessage =
            null

        successMessage =
            null
    }

    LaunchedEffect(Unit) {

        PatientRepository
            .getMyHealthProfile()
            .onSuccess {
                profile =
                    it
            }
            .onFailure {
                errorMessage =
                    it.message
                        ?: "Could not load your health profile."
            }

        isLoading =
            false
    }

    Scaffold(

        containerColor =
            MaterialTheme
                .colorScheme
                .background,

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text =
                                "Health profile",
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                18.sp
                        )

                        Text(
                            text =
                                "Information your doctor may need",
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

                        Icon(
                            imageVector =
                                Icons.AutoMirrored
                                    .Filled
                                    .ArrowBack,
                            contentDescription =
                                "Back"
                        )
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

        if (isLoading) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                CircularProgressIndicator()
            }

            return@Scaffold
        }


        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    ),

            contentPadding =
                PaddingValues(
                    horizontal =
                        16.dp,
                    vertical =
                        16.dp
                ),

            verticalArrangement =
                Arrangement.spacedBy(
                    14.dp
                )

        ) {

            item {

                HealthIntroCard(
                    patientName =
                        profile.patientName
                )
            }

            item {
                OutlinedButton(
                    onClick = onOpenCareFile,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Open My Care File") }
            }


            item {

                HealthSection(
                    title =
                        "Basic health information",
                    subtitle =
                        "Keep these details accurate for future consultations."
                ) {

                    HealthTextField(
                        label =
                            "Date of birth",
                        value =
                            profile.date_of_birth
                                .orEmpty(),
                        placeholder =
                            "YYYY-MM-DD"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                date_of_birth =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Blood group",
                        value =
                            profile.blood_group
                                .orEmpty(),
                        placeholder =
                            "Example: O+"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                blood_group =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Preferred language",
                        value =
                            profile.preferred_language
                                .orEmpty(),
                        placeholder =
                            "Example: Siswati, English"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                preferred_language =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Height (cm)",
                        value =
                            profile.height_cm
                                ?.toString()
                                .orEmpty(),
                        placeholder =
                            "Example: 175"
                    ) { value ->

                        updateProfile { current ->

                            current.copy(
                                height_cm =
                                    value
                                        .toDoubleOrNull()
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Weight (kg)",
                        value =
                            profile.weight_kg
                                ?.toString()
                                .orEmpty(),
                        placeholder =
                            "Example: 72"
                    ) { value ->

                        updateProfile { current ->

                            current.copy(
                                weight_kg =
                                    value
                                        .toDoubleOrNull()
                            )
                        }
                    }
                }
            }


            item {

                HealthSection(
                    title =
                        "Medical background",
                    subtitle =
                        "These details help a doctor understand your history."
                ) {

                    HealthTextField(
                        label =
                            "Allergies",
                        value =
                            profile.allergies
                                .orEmpty(),
                        placeholder =
                            "Medicine, food or other allergies",
                        minLines =
                            2
                    ) {

                        updateProfile { current ->

                            current.copy(
                                allergies =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Chronic conditions",
                        value =
                            profile.chronic
                                .orEmpty(),
                        placeholder =
                            "Example: asthma, diabetes",
                        minLines =
                            2
                    ) {

                        updateProfile { current ->

                            current.copy(
                                chronic =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Current medication",
                        value =
                            profile.medication
                                .orEmpty(),
                        placeholder =
                            "List medicine you currently take",
                        minLines =
                            2
                    ) {

                        updateProfile { current ->

                            current.copy(
                                medication =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Previous operations / surgeries",
                        value =
                            profile.surgeries
                                .orEmpty(),
                        placeholder =
                            "Include important previous procedures",
                        minLines =
                            2
                    ) {

                        updateProfile { current ->

                            current.copy(
                                surgeries =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Disability or accessibility needs",
                        value =
                            profile.disability
                                .orEmpty(),
                        placeholder =
                            "Optional",
                        minLines =
                            2
                    ) {

                        updateProfile { current ->

                            current.copy(
                                disability =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Family medical history",
                        value =
                            profile.family_medical_history
                                .orEmpty(),
                        placeholder =
                            "Important conditions in close family members",
                        minLines =
                            2
                    ) {

                        updateProfile { current ->

                            current.copy(
                                family_medical_history =
                                    it
                            )
                        }
                    }
                }
            }


            item {

                HealthSection(
                    title =
                        "Lifestyle",
                    subtitle =
                        "Optional information that may be relevant to treatment."
                ) {

                    HealthTextField(
                        label =
                            "Smoking status",
                        value =
                            profile.smoking_status
                                .orEmpty(),
                        placeholder =
                            "Never / Former / Current"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                smoking_status =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Alcohol use",
                        value =
                            profile.alcohol_use
                                .orEmpty(),
                        placeholder =
                            "None / Occasionally / Frequently"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                alcohol_use =
                                    it
                            )
                        }
                    }
                }
            }


            item {

                HealthSection(
                    title =
                        "Current health questionnaire",
                    subtitle =
                        "Update this before a consultation so the doctor understands your current concern."
                ) {

                    HealthTextField(
                        label =
                            "Main symptoms",
                        value =
                            profile.current_symptoms
                                .orEmpty(),
                        placeholder =
                            "Example: headache, fever and dizziness",
                        minLines =
                            3
                    ) {

                        updateProfile { current ->

                            current.copy(
                                current_symptoms =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "How long have you had these symptoms?",
                        value =
                            profile.symptom_duration
                                .orEmpty(),
                        placeholder =
                            "Example: 3 days"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                symptom_duration =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Severity from 0 to 10",
                        value =
                            profile.symptom_severity
                                ?.toString()
                                .orEmpty(),
                        placeholder =
                            "0 = very mild, 10 = severe"
                    ) { value ->

                        val number =
                            value
                                .toIntOrNull()
                                ?.coerceIn(
                                    0,
                                    10
                                )

                        updateProfile { current ->

                            current.copy(
                                symptom_severity =
                                    number
                            )
                        }
                    }
                }
            }


            item {

                HealthSection(
                    title =
                        "Emergency contact",
                    subtitle =
                        "Who should a healthcare professional contact if necessary?"
                ) {

                    HealthTextField(
                        label =
                            "Contact name",
                        value =
                            profile.emergency_contact_name
                                .orEmpty(),
                        placeholder =
                            "Full name"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                emergency_contact_name =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Contact number",
                        value =
                            profile.emergency_contact_phone
                                .orEmpty(),
                        placeholder =
                            "Phone number"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                emergency_contact_phone =
                                    it
                            )
                        }
                    }


                    HealthTextField(
                        label =
                            "Relationship",
                        value =
                            profile.emergency_contact_relationship
                                .orEmpty(),
                        placeholder =
                            "Example: Parent, spouse"
                    ) {

                        updateProfile { current ->

                            current.copy(
                                emergency_contact_relationship =
                                    it
                            )
                        }
                    }
                }
            }


            if (
                errorMessage != null
            ) {

                item {

                    MessageCard(
                        message =
                            errorMessage!!,
                        success =
                            false
                    )
                }
            }


            if (
                successMessage != null
            ) {

                item {

                    MessageCard(
                        message =
                            successMessage!!,
                        success =
                            true
                    )
                }
            }


            item {

                Button(

                    onClick = {

                        scope.launch {

                            isSaving =
                                true

                            errorMessage =
                                null

                            successMessage =
                                null


                            PatientRepository
                                .saveMyHealthProfile(
                                    profile
                                )
                                .onSuccess {

                                    successMessage =
                                        "Health profile saved successfully."
                                }
                                .onFailure {

                                    errorMessage =
                                        it.message
                                            ?: "Could not save your health profile."
                                }


                            isSaving =
                                false
                        }
                    },

                    enabled =
                        !isSaving,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                54.dp
                            ),

                    shape =
                        RoundedCornerShape(
                            16.dp
                        ),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    PatientColors
                                        .AppointmentAccent
                            )
                ) {

                    if (isSaving) {

                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(
                                    20.dp
                                ),
                            strokeWidth =
                                2.dp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onPrimary
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )
                    }


                    Text(
                        text =
                            if (isSaving) {
                                "Saving..."
                            } else {
                                "Save health profile"
                            },
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }


            item {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                PatientColors
                                    .PurpleSoft,
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .padding(
                                13.dp
                            ),
                    verticalAlignment =
                        Alignment.Top
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.Lock,
                        contentDescription =
                            null,
                        tint =
                            PatientColors.Purple,
                        modifier =
                            Modifier.size(
                                19.dp
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                9.dp
                            )
                    )

                    Text(
                        text =
                            "Only authorised healthcare users should be able to access this information. " +
                                    "EHealthy will use the protected doctor-access function for the doctor portal.",
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.5.sp,
                        lineHeight =
                            15.sp
                    )
                }
            }


            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )
            }
        }
    }
}


@Composable
private fun HealthIntroCard(
    patientName: String
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
                    PatientColors
                        .AppointmentCard
            ),
        border =
            BorderStroke(
                1.dp,
                PatientColors
                    .AppointmentAccent
                    .copy(
                        alpha =
                            0.12f
                    )
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
                            PatientColors
                                .AppointmentAccent
                                .copy(
                                    alpha =
                                        0.12f
                                ),
                            CircleShape
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.Info,
                    contentDescription =
                        null,
                    tint =
                        PatientColors
                            .AppointmentAccent
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
                        patientName,
                    fontWeight =
                        FontWeight.ExtraBold,
                    color =
                        PatientColors.TextPrimary
                )

                Text(
                    text =
                        "Complete this once and keep it updated before consultations.",
                    fontSize =
                        10.5.sp,
                    color =
                        PatientColors.TextSecondary
                )
            }
        }
    }
}


@Composable
private fun HealthSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                20.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),
        border =
            BorderStroke(
                1.dp,
                PatientColors
                    .AppointmentAccent
                    .copy(
                        alpha =
                            0.08f
                    )
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
                ),
            verticalArrangement =
                Arrangement.spacedBy(
                    10.dp
                )
        ) {

            Text(
                text =
                    title,
                color =
                    PatientColors.TextPrimary,
                fontWeight =
                    FontWeight.ExtraBold,
                fontSize =
                    14.sp
            )

            Text(
                text =
                    subtitle,
                color =
                    PatientColors.TextSecondary,
                fontSize =
                    10.5.sp
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            content()
        }
    }
}


@Composable
private fun HealthTextField(
    label: String,
    value: String,
    placeholder: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        value =
            value,
        onValueChange =
            onValueChange,
        modifier =
            Modifier.fillMaxWidth(),
        label = {
            Text(
                text =
                    label
            )
        },
        placeholder = {
            Text(
                text =
                    placeholder,
                fontSize =
                    11.sp
            )
        },
        minLines =
            minLines,
        maxLines =
            if (
                minLines > 1
            ) {
                5
            } else {
                1
            },
        shape =
            RoundedCornerShape(
                14.dp
            )
    )
}


@Composable
private fun MessageCard(
    message: String,
    success: Boolean
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                16.dp
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (success) {
                        PatientColors.SuccessCard
                    } else {
                        PatientColors.RedSoft
                    }
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
                        13.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                imageVector =
                    if (success) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Outlined.Info
                    },
                contentDescription =
                    null,
                tint =
                    if (success) {
                        PatientColors.SuccessAccent
                    } else {
                        PatientColors.Red
                    }
            )

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Text(
                text =
                    message,
                color =
                    PatientColors.TextPrimary,
                fontSize =
                    11.sp
            )
        }
    }
}
