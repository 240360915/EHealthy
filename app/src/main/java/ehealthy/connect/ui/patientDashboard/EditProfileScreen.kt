package ehealthy.connect.ui.patientDashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.common.isPasswordStrong
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
@Serializable
data class PatientProfile(
    val name: String = "",
    val surname: String = "",
    val phone: String = "",
    val gender: String = "",
    val address1: String? = null,
    val address2: String? = null,
    val address3: String? = null,
    val postal_code: String? = null,
    val province: String? = null,
    val emergency_contact_name: String? = null,
    val emergency_contact_phone: String? = null,
    val emergency_contact_relationship: String? = null,
    val allergies: String? = null,
    val blood_group: String? = null,
    val chronic: String? = null,
    val medication: String? = null,
    val surgeries: String? = null,
    val disability: String? = null,
    val medical_aid_scheme: String? = null,
    val medical_aid_number: String? = null,
    val medical_aid_plan: String? = null
)

private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
private val bloodGroupOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Not sure")
private val provinceOptions = listOf(
    "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
    "Mpumalanga", "North West", "Northern Cape", "Western Cape"
)
private val relationshipOptions = listOf(
    "Spouse", "Parent", "Child", "Sibling", "Friend", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    fetchProfile: suspend () -> Result<PatientProfile>,
    onSaveProfile: suspend (PatientProfile) -> Result<Unit>,
    onChangePassword: suspend (
        currentPassword: String,
        newPassword: String
    ) -> Result<Unit>,
    onBack: () -> Unit
) {

    val scope =
        rememberCoroutineScope()


    var isLoading by remember {
        mutableStateOf(true)
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    var profile by remember {
        mutableStateOf(
            PatientProfile()
        )
    }


    var isSaving by remember {
        mutableStateOf(false)
    }

    var saveError by remember {
        mutableStateOf<String?>(null)
    }

    var saveSuccess by remember {
        mutableStateOf(false)
    }


    var currentPassword by remember {
        mutableStateOf("")
    }

    var newPassword by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var isChangingPassword by remember {
        mutableStateOf(false)
    }

    var passwordError by remember {
        mutableStateOf<String?>(null)
    }

    var passwordSuccess by remember {
        mutableStateOf(false)
    }


    /*
     * Load patient profile
     */
    LaunchedEffect(Unit) {

        val result =
            fetchProfile()

        isLoading =
            false

        result
            .onSuccess {

                profile =
                    it
            }
            .onFailure {

                loadError =
                    it.message
                        ?: "Could not load your profile."
            }
    }


    /*
     * Save patient profile
     */
    fun save() {

        scope.launch {

            isSaving =
                true

            saveError =
                null

            saveSuccess =
                false


            onSaveProfile(
                profile
            )
                .onSuccess {

                    saveSuccess =
                        true
                }
                .onFailure {

                    saveError =
                        it.message
                            ?: "Could not save your changes."
                }


            isSaving =
                false
        }
    }


    /*
     * Change password
     */
    fun changePassword() {

        passwordError =
            null

        passwordSuccess =
            false


        when {

            currentPassword.isBlank() -> {

                passwordError =
                    "Enter your current password."
            }


            !isPasswordStrong(
                newPassword
            ) -> {

                passwordError =
                    "New password doesn't meet the strength requirements."
            }


            newPassword !=
                    confirmPassword -> {

                passwordError =
                    "New passwords don't match."
            }


            else -> {

                scope.launch {

                    isChangingPassword =
                        true


                    onChangePassword(
                        currentPassword,
                        newPassword
                    )
                        .onSuccess {

                            passwordSuccess =
                                true

                            currentPassword =
                                ""

                            newPassword =
                                ""

                            confirmPassword =
                                ""
                        }
                        .onFailure {

                            passwordError =
                                it.message
                                    ?: "Could not change your password."
                        }


                    isChangingPassword =
                        false
                }
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
                                "Edit Profile",
                            color =
                                PatientColors.TextPrimary,
                            fontWeight =
                                FontWeight.ExtraBold,
                            fontSize =
                                18.sp
                        )


                        Text(
                            text =
                                "Keep your health information up to date",
                            color =
                                PatientColors.TextSecondary,
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

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        36.dp
                                    )
                                    .background(
                                        PatientColors.DoctorCard,
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
                                    PatientColors.DoctorAccent,
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


        /*
         * Loading
         */
        if (isLoading) {

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

                Card(
                    shape =
                        RoundedCornerShape(
                            24.dp
                        ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                PatientColors.DoctorCard
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
                                    .size(
                                        62.dp
                                    )
                                    .background(
                                        PatientColors.DoctorAccent
                                            .copy(
                                                alpha = 0.10f
                                            ),
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
                                    PatientColors.DoctorAccent,

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
                                "Loading your profile",
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
                                    3.dp
                                )
                        )


                        Text(
                            text =
                                "Getting your saved information...",
                            color =
                                PatientColors.TextSecondary,
                            fontSize =
                                10.5.sp
                        )
                    }
                }
            }

            return@Scaffold
        }


        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        horizontal = 18.dp
                    )
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )


            /*
             * Profile introduction
             */
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
                                    52.dp
                                )
                                .background(
                                    PatientColors.DoctorAccent
                                        .copy(
                                            alpha = 0.12f
                                        ),
                                    RoundedCornerShape(
                                        16.dp
                                    )
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Person,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.DoctorAccent,
                            modifier =
                                Modifier.size(
                                    25.dp
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
                                if (
                                    profile.name.isNotBlank()
                                ) {

                                    "Hello, ${profile.name}"

                                } else {

                                    "Your health profile"
                                },

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
                                "Accurate information helps healthcare professionals understand your needs.",
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


            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )


            /*
             * Loading error
             */
            loadError?.let { message ->

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
                                PatientColors.RedSoft
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                13.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Warning,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Red,
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
                                message,
                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                            color =
                                PatientColors.Red,
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
                            14.dp
                        )
                )
            }


            /*
             * PERSONAL INFORMATION
             */
            EditSection(
                icon =
                    Icons.Outlined.Person,
                title =
                    "Personal Info"
            ) {

                LabeledField(
                    label =
                        "First Name",
                    value =
                        profile.name
                ) {

                    profile =
                        profile.copy(
                            name = it
                        )
                }


                LabeledField(
                    label =
                        "Surname",
                    value =
                        profile.surname
                ) {

                    profile =
                        profile.copy(
                            surname = it
                        )
                }


                LabeledField(
                    label =
                        "Phone Number",
                    value =
                        profile.phone,
                    keyboardType =
                        KeyboardType.Phone
                ) {

                    profile =
                        profile.copy(
                            phone = it
                        )
                }


                DropdownField(
                    label =
                        "Gender",
                    value =
                        profile.gender,
                    options =
                        genderOptions
                ) {

                    profile =
                        profile.copy(
                            gender = it
                        )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * ADDRESS
             */
            EditSection(
                icon =
                    Icons.Outlined.LocationOn,
                title =
                    "Address"
            ) {

                LabeledField(
                    label =
                        "Address Line 1",
                    value =
                        profile.address1
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            address1 = it
                        )
                }


                LabeledField(
                    label =
                        "Address Line 2",
                    value =
                        profile.address2
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            address2 = it
                        )
                }


                LabeledField(
                    label =
                        "Address Line 3 (optional)",
                    value =
                        profile.address3
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            address3 =
                                it.ifBlank {
                                    null
                                }
                        )
                }


                LabeledField(
                    label =
                        "Postal Code",
                    value =
                        profile.postal_code
                            ?: "",
                    keyboardType =
                        KeyboardType.Number
                ) {

                    profile =
                        profile.copy(
                            postal_code = it
                        )
                }


                DropdownField(
                    label =
                        "Province",
                    value =
                        profile.province
                            ?: "",
                    options =
                        provinceOptions
                ) {

                    profile =
                        profile.copy(
                            province = it
                        )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * EMERGENCY CONTACT
             */
            EditSection(
                icon =
                    Icons.Outlined.Warning,
                title =
                    "Emergency Contact"
            ) {

                LabeledField(
                    label =
                        "Contact Name",
                    value =
                        profile.emergency_contact_name
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            emergency_contact_name =
                                it
                        )
                }


                LabeledField(
                    label =
                        "Contact Phone",
                    value =
                        profile.emergency_contact_phone
                            ?: "",
                    keyboardType =
                        KeyboardType.Phone
                ) {

                    profile =
                        profile.copy(
                            emergency_contact_phone =
                                it
                        )
                }


                DropdownField(
                    label =
                        "Relationship",
                    value =
                        profile.emergency_contact_relationship
                            ?: "",
                    options =
                        relationshipOptions
                ) {

                    profile =
                        profile.copy(
                            emergency_contact_relationship =
                                it
                        )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * MEDICAL INFORMATION
             */
            EditSection(
                icon =
                    Icons.Outlined.MedicalServices,
                title =
                    "Medical Information"
            ) {

                LabeledField(
                    label =
                        "Allergies",
                    value =
                        profile.allergies
                            ?: "",
                    placeholder =
                        "e.g. Penicillin, peanuts — or \"None\"",
                    singleLine =
                        false
                ) {

                    profile =
                        profile.copy(
                            allergies = it
                        )
                }


                DropdownField(
                    label =
                        "Blood Group",
                    value =
                        profile.blood_group
                            ?: "",
                    options =
                        bloodGroupOptions
                ) {

                    profile =
                        profile.copy(
                            blood_group = it
                        )
                }


                LabeledField(
                    label =
                        "Chronic Conditions",
                    value =
                        profile.chronic
                            ?: "",
                    placeholder =
                        "e.g. Diabetes, hypertension — or \"None\"",
                    singleLine =
                        false
                ) {

                    profile =
                        profile.copy(
                            chronic = it
                        )
                }


                LabeledField(
                    label =
                        "Current Medication",
                    value =
                        profile.medication
                            ?: "",
                    placeholder =
                        "e.g. Metformin 500mg daily — or \"None\"",
                    singleLine =
                        false
                ) {

                    profile =
                        profile.copy(
                            medication = it
                        )
                }


                LabeledField(
                    label =
                        "Past Surgeries",
                    value =
                        profile.surgeries
                            ?: "",
                    singleLine =
                        false
                ) {

                    profile =
                        profile.copy(
                            surgeries = it
                        )
                }


                LabeledField(
                    label =
                        "Disability (optional)",
                    value =
                        profile.disability
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            disability =
                                it.ifBlank {
                                    null
                                }
                        )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )


            /*
             * MEDICAL AID
             */
            EditSection(
                icon =
                    Icons.Outlined.Shield,
                title =
                    "Medical Aid"
            ) {

                LabeledField(
                    label =
                        "Medical Aid Scheme",
                    value =
                        profile.medical_aid_scheme
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            medical_aid_scheme =
                                it
                        )
                }


                LabeledField(
                    label =
                        "Membership Number",
                    value =
                        profile.medical_aid_number
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            medical_aid_number =
                                it
                        )
                }


                LabeledField(
                    label =
                        "Plan / Option",
                    value =
                        profile.medical_aid_plan
                            ?: ""
                ) {

                    profile =
                        profile.copy(
                            medical_aid_plan =
                                it
                        )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )


            /*
             * Save status
             */
            saveError?.let { message ->

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                PatientColors.RedSoft
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.Warning,
                            contentDescription =
                                null,
                            tint =
                                PatientColors.Red,
                            modifier =
                                Modifier.size(
                                    18.dp
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
                                message,
                            color =
                                PatientColors.Red,
                            fontSize =
                                10.5.sp
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(
                            8.dp
                        )
                )
            }


            if (saveSuccess) {

                Card(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            15.dp
                        ),

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                PatientColors.SuccessCard
                        )
                ) {

                    Row(
                        modifier =
                            Modifier.padding(
                                12.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
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
                                    18.dp
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
                                "Profile updated successfully.",
                            color =
                                PatientColors.SuccessAccent,
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
                            8.dp
                        )
                )
            }


            /*
             * Save button
             */
            Button(
                onClick = {
                    save()
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
                    ButtonDefaults.buttonColors(
                        containerColor =
                            PatientColors.DoctorAccent,
                        contentColor =
                            Color.White
                    )
            ) {

                if (isSaving) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(
                                20.dp
                            ),
                        color =
                            Color.White,
                        strokeWidth =
                            2.dp
                    )

                } else {

                    Icon(
                        imageVector =
                            Icons.Outlined.CheckCircle,
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
                            "Save changes",
                        fontWeight =
                            FontWeight.Bold,
                        fontSize =
                            13.sp
                    )
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )


            /*
             * CHANGE PASSWORD
             */
            EditSection(
                icon =
                    Icons.Outlined.Lock,
                title =
                    "Change Password"
            ) {

                Text(
                    text =
                        "For security, enter your current password before choosing a new one.",
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        10.5.sp,
                    lineHeight =
                        15.sp
                )


                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )


                LabeledField(
                    label =
                        "Current Password",
                    value =
                        currentPassword,
                    isPassword =
                        true
                ) {

                    currentPassword =
                        it
                }


                LabeledField(
                    label =
                        "New Password",
                    value =
                        newPassword,
                    isPassword =
                        true
                ) {

                    newPassword =
                        it
                }


                LabeledField(
                    label =
                        "Confirm New Password",
                    value =
                        confirmPassword,
                    isPassword =
                        true
                ) {

                    confirmPassword =
                        it
                }


                passwordError?.let { message ->

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PatientColors.RedSoft
                            )
                    ) {

                        Text(
                            text =
                                message,
                            modifier =
                                Modifier.padding(
                                    11.dp
                                ),
                            color =
                                PatientColors.Red,
                            fontSize =
                                10.5.sp
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )
                }


                if (passwordSuccess) {

                    Card(
                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    PatientColors.SuccessCard
                            )
                    ) {

                        Row(
                            modifier =
                                Modifier.padding(
                                    11.dp
                                ),

                            verticalAlignment =
                                Alignment.CenterVertically
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


                            Spacer(
                                modifier =
                                    Modifier.width(
                                        7.dp
                                    )
                            )


                            Text(
                                text =
                                    "Password changed successfully.",
                                color =
                                    PatientColors.SuccessAccent,
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
                                8.dp
                            )
                    )
                }


                Button(
                    onClick = {
                        changePassword()
                    },

                    enabled =
                        !isChangingPassword,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                50.dp
                            ),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PatientColors.Purple,
                            contentColor =
                                Color.White
                        ),

                    shape =
                        RoundedCornerShape(
                            15.dp
                        )
                ) {

                    if (isChangingPassword) {

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

                        Icon(
                            imageVector =
                                Icons.Outlined.Lock,
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
                                    7.dp
                                )
                        )


                        Text(
                            text =
                                "Update password",
                            fontWeight =
                                FontWeight.Bold,
                            fontSize =
                                12.5.sp
                        )
                    }
                }
            }


            Spacer(
                modifier =
                    Modifier.height(
                        28.dp
                    )
            )
        }
    }
}

@Composable
private fun EditSection(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {

    val accent =
        when (title) {

            "Personal Info" ->
                PatientColors.DoctorAccent

            "Address" ->
                PatientColors.AppointmentAccent

            "Emergency Contact" ->
                PatientColors.Red

            "Medical Information" ->
                PatientColors.Purple

            "Medical Aid" ->
                PatientColors.SuccessAccent

            "Change Password" ->
                PatientColors.Purple

            else ->
                PatientColors.Primary
        }


    val background =
        when (title) {

            "Personal Info" ->
                PatientColors.DoctorCard

            "Address" ->
                PatientColors.AppointmentCard

            "Emergency Contact" ->
                PatientColors.RedSoft

            "Medical Information" ->
                PatientColors.PurpleSoft

            "Medical Aid" ->
                PatientColors.SuccessCard

            "Change Password" ->
                PatientColors.PurpleSoft

            else ->
                PatientColors.NeutralCard
        }


    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            background,
                            RoundedCornerShape(13.dp)
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
                        Modifier.size(20.dp)
                )
            }


            Spacer(
                modifier =
                    Modifier.width(10.dp)
            )


            Column {

                Text(
                    text =
                        title,
                    color =
                        PatientColors.TextPrimary,
                    fontSize =
                        14.sp,
                    fontWeight =
                        FontWeight.ExtraBold
                )


                Text(
                    text =
                        when (title) {

                            "Personal Info" ->
                                "Basic information about you"

                            "Address" ->
                                "Where you currently live"

                            "Emergency Contact" ->
                                "Someone we can identify in an emergency"

                            "Medical Information" ->
                                "Important details about your health"

                            "Medical Aid" ->
                                "Your healthcare cover information"

                            "Change Password" ->
                                "Keep your account secure"

                            else ->
                                ""
                        },
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        9.5.sp
                )
            }
        }


        Spacer(
            modifier =
                Modifier.height(10.dp)
        )


        Card(
            modifier =
                Modifier.fillMaxWidth(),

            shape =
                RoundedCornerShape(20.dp),

            colors =
                CardDefaults.cardColors(
                    containerColor =
                        MaterialTheme.colorScheme.surface
                ),

            border =
                BorderStroke(
                    width =
                        1.dp,
                    color =
                        accent.copy(alpha = 0.10f)
                ),

            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp)
            ) {

                content()
            }
        }
    }
}
@Composable
private fun LabeledField(
    label: String,
    value: String,
    placeholder: String? = null,
    singleLine: Boolean = true,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        value =
            value,

        onValueChange =
            onValueChange,

        label = {

            Text(
                text =
                    label,
                fontSize =
                    11.sp,
                fontWeight =
                    FontWeight.Medium
            )
        },

        placeholder =
            placeholder?.let { text ->

                {

                    Text(
                        text =
                            text,
                        color =
                            PatientColors.TextSecondary,
                        fontSize =
                            10.sp
                    )
                }
            },

        singleLine =
            singleLine,

        minLines =
            if (singleLine) {
                1
            } else {
                3
            },

        visualTransformation =
            if (isPassword) {

                PasswordVisualTransformation()

            } else {

                androidx.compose.ui.text.input
                    .VisualTransformation.None
            },

        keyboardOptions =
            KeyboardOptions(
                keyboardType =
                    if (isPassword) {
                        KeyboardType.Password
                    } else {
                        keyboardType
                    }
            ),

        colors =
            OutlinedTextFieldDefaults.colors(

                focusedBorderColor =
                    PatientColors.DoctorAccent,

                focusedLabelColor =
                    PatientColors.DoctorAccent,

                cursorColor =
                    PatientColors.DoctorAccent,

                unfocusedBorderColor =
                    MaterialTheme
                        .colorScheme
                        .outlineVariant,

                focusedContainerColor =
                    PatientColors.DoctorCard
                        .copy(alpha = 0.22f),

                unfocusedContainerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),

        shape =
            RoundedCornerShape(14.dp),

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 12.dp
                )
    )
}
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {

    var expanded by remember {
        mutableStateOf(false)
    }


    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 12.dp
                )
    ) {

        OutlinedTextField(
            value =
                value,

            onValueChange = {},

            readOnly =
                true,

            label = {

                Text(
                    text =
                        label,
                    fontSize =
                        11.sp,
                    fontWeight =
                        FontWeight.Medium
                )
            },

            placeholder = {

                Text(
                    text =
                        "Select $label",
                    color =
                        PatientColors.TextSecondary,
                    fontSize =
                        10.sp
                )
            },

            trailingIcon = {

                Box(
                    modifier =
                        Modifier
                            .size(34.dp)
                            .background(
                                PatientColors.DoctorCard,
                                CircleShape
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Outlined.ExpandMore,
                        contentDescription =
                            "Open $label options",
                        tint =
                            PatientColors.DoctorAccent,
                        modifier =
                            Modifier.size(19.dp)
                    )
                }
            },

            colors =
                OutlinedTextFieldDefaults.colors(

                    focusedBorderColor =
                        PatientColors.DoctorAccent,

                    focusedLabelColor =
                        PatientColors.DoctorAccent,

                    unfocusedBorderColor =
                        MaterialTheme
                            .colorScheme
                            .outlineVariant,

                    focusedContainerColor =
                        PatientColors.DoctorCard
                            .copy(alpha = 0.22f),

                    unfocusedContainerColor =
                        MaterialTheme
                            .colorScheme
                            .surface
                ),

            shape =
                RoundedCornerShape(14.dp),

            modifier =
                Modifier.fillMaxWidth()
        )


        // Keeps the reliable dropdown
        // already used in your original screen.
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clickable {
                        expanded = true
                    }
        )


        DropdownMenu(
            expanded =
                expanded,

            onDismissRequest = {
                expanded = false
            },

            modifier =
                Modifier
                    .fillMaxWidth(0.86f)
        ) {

            options.forEach { option ->

                DropdownMenuItem(
                    text = {

                        Row(
                            modifier =
                                Modifier.fillMaxWidth(),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    option,
                                modifier =
                                    Modifier.weight(1f),
                                color =
                                    PatientColors.TextPrimary,
                                fontSize =
                                    12.5.sp,
                                fontWeight =
                                    if (
                                        option == value
                                    ) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                            )


                            if (
                                option == value
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Outlined.CheckCircle,
                                    contentDescription =
                                        null,
                                    tint =
                                        PatientColors.SuccessAccent,
                                    modifier =
                                        Modifier.size(18.dp)
                                )
                            }
                        }
                    },

                    onClick = {

                        onValueChange(
                            option
                        )

                        expanded =
                            false
                    }
                )
            }
        }
    }
}
