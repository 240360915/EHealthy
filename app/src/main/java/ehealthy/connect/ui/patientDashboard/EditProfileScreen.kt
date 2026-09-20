package ehealthy.connect.ui.patientDashboard

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
import androidx.compose.material.icons.filled.ArrowBack
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
    onChangePassword: suspend (currentPassword: String, newPassword: String) -> Result<Unit>,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var profile by remember { mutableStateOf(PatientProfile()) }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var saveSuccess by remember { mutableStateOf(false) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isChangingPassword by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val result = fetchProfile()
        isLoading = false
        result
            .onSuccess { profile = it }
            .onFailure { loadError = it.message ?: "Could not load your profile." }
    }

    fun save() {
        scope.launch {
            isSaving = true
            saveError = null
            saveSuccess = false
            onSaveProfile(profile)
                .onSuccess { saveSuccess = true }
                .onFailure { saveError = it.message ?: "Could not save your changes." }
            isSaving = false
        }
    }

    fun changePassword() {
        passwordError = null
        passwordSuccess = false
        when {
            currentPassword.isBlank() -> passwordError = "Enter your current password."
            !isPasswordStrong(newPassword) -> passwordError = "New password doesn't meet the strength requirements."
            newPassword != confirmPassword -> passwordError = "New passwords don't match."
            else -> {
                scope.launch {
                    isChangingPassword = true
                    onChangePassword(currentPassword, newPassword)
                        .onSuccess {
                            passwordSuccess = true
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                        .onFailure { passwordError = it.message ?: "Could not change your password." }
                    isChangingPassword = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            loadError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ---- PERSONAL INFO ----
            EditSection(Icons.Outlined.Person, "Personal Info") {
                LabeledField("First Name", profile.name) { profile = profile.copy(name = it) }
                LabeledField("Surname", profile.surname) { profile = profile.copy(surname = it) }
                LabeledField(
                    "Phone Number", profile.phone,
                    keyboardType = KeyboardType.Phone
                ) { profile = profile.copy(phone = it) }
                DropdownField("Gender", profile.gender, genderOptions) { profile = profile.copy(gender = it) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- ADDRESS ----
            EditSection(Icons.Outlined.LocationOn, "Address") {
                LabeledField("Address Line 1", profile.address1 ?: "") { profile = profile.copy(address1 = it) }
                LabeledField("Address Line 2", profile.address2 ?: "") { profile = profile.copy(address2 = it) }
                LabeledField("Address Line 3 (optional)", profile.address3 ?: "") { profile = profile.copy(address3 = it.ifBlank { null }) }
                LabeledField(
                    "Postal Code", profile.postal_code ?: "",
                    keyboardType = KeyboardType.Number
                ) { profile = profile.copy(postal_code = it) }
                DropdownField("Province", profile.province ?: "", provinceOptions) { profile = profile.copy(province = it) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- EMERGENCY CONTACT ----
            EditSection(Icons.Outlined.Warning, "Emergency Contact") {
                LabeledField(
                    "Contact Name", profile.emergency_contact_name ?: ""
                ) { profile = profile.copy(emergency_contact_name = it) }
                LabeledField(
                    "Contact Phone", profile.emergency_contact_phone ?: "",
                    keyboardType = KeyboardType.Phone
                ) { profile = profile.copy(emergency_contact_phone = it) }
                DropdownField(
                    "Relationship", profile.emergency_contact_relationship ?: "", relationshipOptions
                ) { profile = profile.copy(emergency_contact_relationship = it) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- MEDICAL INFO ----
            EditSection(Icons.Outlined.MedicalServices, "Medical Information") {
                LabeledField(
                    "Allergies", profile.allergies ?: "",
                    placeholder = "e.g. Penicillin, peanuts — or \"None\"",
                    singleLine = false
                ) { profile = profile.copy(allergies = it) }
                DropdownField("Blood Group", profile.blood_group ?: "", bloodGroupOptions) { profile = profile.copy(blood_group = it) }
                LabeledField(
                    "Chronic Conditions", profile.chronic ?: "",
                    placeholder = "e.g. Diabetes, hypertension — or \"None\"",
                    singleLine = false
                ) { profile = profile.copy(chronic = it) }
                LabeledField(
                    "Current Medication", profile.medication ?: "",
                    placeholder = "e.g. Metformin 500mg daily — or \"None\"",
                    singleLine = false
                ) { profile = profile.copy(medication = it) }
                LabeledField(
                    "Past Surgeries", profile.surgeries ?: "",
                    singleLine = false
                ) { profile = profile.copy(surgeries = it) }
                LabeledField(
                    "Disability (optional)", profile.disability ?: ""
                ) { profile = profile.copy(disability = it.ifBlank { null }) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- MEDICAL AID ----
            EditSection(Icons.Outlined.Shield, "Medical Aid") {
                LabeledField(
                    "Medical Aid Scheme", profile.medical_aid_scheme ?: ""
                ) { profile = profile.copy(medical_aid_scheme = it) }
                LabeledField(
                    "Membership Number", profile.medical_aid_number ?: ""
                ) { profile = profile.copy(medical_aid_number = it) }
                LabeledField(
                    "Plan / Option", profile.medical_aid_plan ?: ""
                ) { profile = profile.copy(medical_aid_plan = it) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (saveSuccess) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Profile updated.", color = Color(0xFF10B981), fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { save() },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ---- CHANGE PASSWORD ----
            EditSection(Icons.Outlined.Lock, "Change Password") {
                LabeledField(
                    "Current Password", currentPassword,
                    isPassword = true
                ) { currentPassword = it }
                LabeledField(
                    "New Password", newPassword,
                    isPassword = true
                ) { newPassword = it }
                LabeledField(
                    "Confirm New Password", confirmPassword,
                    isPassword = true
                ) { confirmPassword = it }

                passwordError?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                if (passwordSuccess) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Password changed.", color = Color(0xFF10B981), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { changePassword() },
                    enabled = !isChangingPassword,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Update Password", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun EditSection(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
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
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, fontSize = 12.sp) } },
        singleLine = singleLine,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else keyboardType
        ),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(bottom = 12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Outlined.ExpandMore, contentDescription = null) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay so the tap reliably opens the dropdown — a plain
        // .clickable on a readOnly OutlinedTextField can get swallowed by its
        // own internal focus handling.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
