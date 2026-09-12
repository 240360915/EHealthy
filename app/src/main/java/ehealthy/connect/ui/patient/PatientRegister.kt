package ehealthy.connect.ui.patient

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.common.PasswordStrengthChecklist
import ehealthy.connect.ui.common.isPasswordStrong
import java.util.Calendar

enum class RegisterStep(val label: String) {
    PERSONAL("Personal Details"),
    CONTACT("Contact & Address"),
    MEDICAL("Medical Information"),
    EMERGENCY("Emergency Contact"),
    SECURITY("Account Security")
}

data class PatientRegistrationData(
    val name: String,
    val surname: String,
    val title: String,
    val dateOfBirth: String,
    val idNumber: String,
    val phone: String,
    val language: String,
    val province: String,
    val address1: String,
    val address2: String,
    val address3: String,
    val postalCode: String,
    val gender: String,
    val password: String,
    val allergies: String,
    val medication: String,
    val conditions: String,
    val chronic: String,
    val surgeries: String,
    val bloodGroup: String,
    val disability: String,
    val emergencyContactName: String,
    val emergencyContactPhone: String,
    val emergencyContactRelationship: String,
    val email: String
)

@SuppressLint("DefaultLocale")
@Composable
fun PatientRegister(
    initialEmail: String,
    isLoading: Boolean,
    errorMessage: String?,
    onRegister: (PatientRegistrationData) -> Unit,
    onBackToLogin: () -> Unit,
    onViewPrivacyPolicy: () -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val green = Color(0xFF218B78)
    val greyText = Color(0xFF4F555C)
    val navy = Color(0xFF293147)

    var step by remember { mutableStateOf(RegisterStep.PERSONAL) }
    val context = LocalContext.current
    var email by remember { mutableStateOf(initialEmail) }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    var phone by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var address1 by remember { mutableStateOf("") }
    var address2 by remember { mutableStateOf("") }
    var address3 by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }

    var allergies by remember { mutableStateOf("") }
    var medication by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var chronic by remember { mutableStateOf("") }
    var surgeries by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var disability by remember { mutableStateOf("") }

    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var emergencyRelationship by remember { mutableStateOf("") }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var consentGiven by remember { mutableStateOf(false) }

    var stepError by remember { mutableStateOf<String?>(null) }

    @Composable
    fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = green)

    fun validateAndAdvance() {
        stepError = null
        when (step) {
            RegisterStep.PERSONAL -> {
                when {
                    name.isBlank() || surname.isBlank() -> stepError =
                        "Please enter your full name."

                    title.isBlank() -> stepError = "Please enter a title."
                    dateOfBirth.isBlank() -> stepError = "Please enter your date of birth."
                    idNumber.length != 13 -> stepError = "ID number must be exactly 13 digits."
                    gender.isBlank() -> stepError = "Please select a gender option."
                    else -> step = RegisterStep.CONTACT
                }
            }

            RegisterStep.CONTACT -> {
                when {
                    phone.length != 10 -> stepError = "Phone number must be exactly 10 digits."
                    language.isBlank() -> stepError = "Please enter a preferred language."
                    province.isBlank() -> stepError = "Please enter your province."
                    address1.isBlank() -> stepError = "Please enter address line 1."
                    postalCode.isBlank() -> stepError = "Please enter a postal code."
                    else -> step = RegisterStep.MEDICAL
                }
            }

            RegisterStep.MEDICAL -> {
                step = RegisterStep.EMERGENCY
            }

            RegisterStep.EMERGENCY -> {
                step = RegisterStep.SECURITY
            }

            RegisterStep.SECURITY -> {
                when {
                    !isPasswordStrong(password) -> stepError =
                        "Your password doesn't meet all requirements yet."

                    password != confirmPassword -> stepError = "Passwords do not match."
                    !consentGiven -> stepError = "Please accept the consent agreement to continue."
                    else -> onRegister(
                        PatientRegistrationData(
                            name,
                            surname,
                            title,
                            dateOfBirth,
                            idNumber,
                            phone,
                            language,
                            province,
                            address1,
                            address2,
                            address3,
                            postalCode,
                            gender,
                            password,
                            allergies,
                            medication,
                            conditions,
                            chronic,
                            surgeries,
                            bloodGroup,
                            disability,
                            emergencyName,
                            emergencyPhone,
                            emergencyRelationship,
                            email
                        )
                    )
                }
            }
        }
    }

    fun goBack() {
        stepError = null
        step = when (step) {
            RegisterStep.PERSONAL -> RegisterStep.PERSONAL
            RegisterStep.CONTACT -> RegisterStep.PERSONAL
            RegisterStep.MEDICAL -> RegisterStep.CONTACT
            RegisterStep.EMERGENCY -> RegisterStep.MEDICAL
            RegisterStep.SECURITY -> RegisterStep.EMERGENCY
        }
    }

    val stepIndex = RegisterStep.entries.indexOf(step)
    val totalSteps = RegisterStep.entries.size

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(paddingValues)
                .padding(vertical = 24.dp)
        ) {
            Text(
                "Patient Registration",
                color = darkText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Step ${stepIndex + 1} of $totalSteps · ${step.label}",
                color = greyText, fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / totalSteps },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = green,
                trackColor = Color(0xFFE3E6EC)
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                RegisterStep.PERSONAL -> {
                    OutlinedTextField(
                        name,
                        { name = it },
                        label = { Text("First name") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        surname,
                        { surname = it },
                        label = { Text("Surname") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    var titleExpanded by remember { mutableStateOf(false) }

                    val titles = listOf("Mr", "Ms", "Mrs")

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = {},
                            label = {
                                Text("Title")
                            },
                            singleLine = true,
                            readOnly = true,
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select title"
                                )
                            }
                        )

                        // Invisible clickable area over the field
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    titleExpanded = true
                                }
                        )

                        DropdownMenu(
                            expanded = titleExpanded,
                            onDismissRequest = {
                                titleExpanded = false
                            }
                        ) {
                            titles.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(option)
                                    },
                                    onClick = {
                                        title = option
                                        titleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val calendar = Calendar.getInstance()

                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = { dateOfBirth = it },
                        label = {
                            Text("Date of birth")
                        },
                        singleLine = true,
                        readOnly = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select date",
                                modifier = Modifier.clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            dateOfBirth = String.format(
                                                "%04d-%02d-%02d",
                                                year,
                                                month + 1,
                                                day
                                            )
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        idNumber, { if (it.length <= 13) idNumber = it },
                        label = { Text("ID number (13 digits)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Gender",
                        color = darkText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        listOf("Male", "Female", "Prefer not to say").forEach { option ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                RadioButton(
                                    selected = gender == option,
                                    onClick = { gender = option })
                                Text(option, fontSize = 12.sp, color = darkText)
                            }
                        }
                    }
                }

                RegisterStep.CONTACT -> {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email address") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        phone, { if (it.length <= 10) phone = it },
                        label = { Text("Phone number") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    var languageExpanded by remember { mutableStateOf(false) }

                    val languages = listOf(
                        "Afrikaans",
                        "English",
                        "isiNdebele",
                        "isiXhosa",
                        "isiZulu",
                        "Sepedi",
                        "Sesotho",
                        "Setswana",
                        "siSwati",
                        "Tshivenda",
                        "itsonga"
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(
                            value = language,
                            onValueChange = {},
                            label = {
                                Text("Preferred language")
                            },
                            singleLine = true,
                            readOnly = true,
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select language"
                                )
                            }
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    languageExpanded = true
                                }
                        )

                        DropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = {
                                languageExpanded = false
                            }
                        ) {
                            languages.forEach { option ->

                                DropdownMenuItem(
                                    text = {
                                        Text(option)
                                    },
                                    onClick = {
                                        language = option
                                        languageExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    var provinceExpanded by remember { mutableStateOf(false) }

                    val provinces = listOf(
                        "Eastern Cape",
                        "Free State",
                        "Gauteng",
                        "KwaZulu-Natal",
                        "Limpopo",
                        "Mpumalanga",
                        "Northern Cape",
                        "North West",
                        "Western Cape"
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        OutlinedTextField(
                            value = province,
                            onValueChange = {},
                            label = {
                                Text("Province")
                            },
                            singleLine = true,
                            readOnly = true,
                            colors = fieldColors(),
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select province"
                                )
                            }
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    provinceExpanded = true
                                }
                        )

                        DropdownMenu(
                            expanded = provinceExpanded,
                            onDismissRequest = {
                                provinceExpanded = false
                            }
                        ) {
                            provinces.forEach { option ->

                                DropdownMenuItem(
                                    text = {
                                        Text(option)
                                    },
                                    onClick = {
                                        province = option
                                        provinceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        address1,
                        { address1 = it },
                        label = { Text("Address line 1") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        address2,
                        { address2 = it },
                        label = { Text("Address line 2") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        address3,
                        { address3 = it },
                        label = { Text("Address line 3 (optional)") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        postalCode,
                        { postalCode = it },
                        label = { Text("Postal code") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                RegisterStep.MEDICAL -> {
                    Text(
                        "This information helps your doctor treat you safely. All fields here are optional.",
                        color = greyText, fontSize = 12.5.sp, lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        allergies,
                        { allergies = it },
                        label = { Text("Known allergies") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        medication,
                        { medication = it },
                        label = { Text("Current medication") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        conditions,
                        { conditions = it },
                        label = { Text("Past medical conditions") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        chronic,
                        { chronic = it },
                        label = { Text("Chronic illness (if any)") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        surgeries,
                        { surgeries = it },
                        label = { Text("Previous surgeries") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        bloodGroup,
                        { bloodGroup = it },
                        label = { Text("Blood group (optional)") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        disability,
                        { disability = it },
                        label = { Text("Disability (if any)") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                RegisterStep.EMERGENCY -> {
                    Text(
                        "Who should we contact in case of an emergency? This is optional but recommended.",
                        color = greyText, fontSize = 12.5.sp, lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        emergencyName,
                        { emergencyName = it },
                        label = { Text("Contact name") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        emergencyPhone, { if (it.length <= 10) emergencyPhone = it },
                        label = { Text("Contact phone number") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        emergencyRelationship,
                        { emergencyRelationship = it },
                        label = { Text("Relationship (e.g. Spouse, Parent)") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                RegisterStep.SECURITY -> {
                    OutlinedTextField(
                        password,
                        { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PasswordStrengthChecklist(password)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        confirmPassword,
                        { confirmPassword = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(checked = consentGiven, onCheckedChange = { consentGiven = it })
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                "I consent to receive healthcare services on this platform and agree my information may be stored securely and used only for healthcare purposes.",
                                fontSize = 13.sp, color = greyText, lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Read our Privacy Policy",
                                fontSize = 13.sp,
                                color = green,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onViewPrivacyPolicy() }
                            )
                        }
                    }
                }
            }

            (stepError ?: errorMessage)?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(it, color = Color(0xFFD64545), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (step != RegisterStep.PERSONAL) {
                    OutlinedButton(
                        onClick = ::goBack,
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text(
                            "Back",
                            color = darkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = ::validateAndAdvance,
                    enabled = !isLoading,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navy)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            if (step == RegisterStep.SECURITY) "Register" else "Next",
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", color = greyText, fontSize = 14.sp)
                Text(
                    "Login here", color = green, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onBackToLogin() }
                )
            }
        }
    }
}