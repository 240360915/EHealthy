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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ehealthy.connect.ui.common.AuthColors
import ehealthy.connect.ui.common.PasswordStrengthChecklist
import ehealthy.connect.ui.common.authTextFieldColors
import ehealthy.connect.ui.common.digitsOnly
import ehealthy.connect.ui.common.isPasswordValid
import ehealthy.connect.ui.common.isValidEmail
import ehealthy.connect.ui.common.isValidSaIdNumber
import ehealthy.connect.ui.common.isValidSaPhoneNumber
import ehealthy.connect.ui.common.isValidSaPostalCode
import java.util.Calendar

enum class RegisterStep(val label: String, val helper: String) {
    PERSONAL("About you", "Tell us who you are."),
    CONTACT("Contact & address", "How we can reach you and where you are based."),
    HEALTH("Health profile", "Optional — add it now or complete it later from your profile."),
    SECURITY("Secure your account", "Create your sign-in details and review consent.")
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

private val patientTitles = listOf("Mr", "Ms", "Mrs")
private val patientLanguages = listOf(
    "Afrikaans", "English", "isiNdebele", "isiXhosa", "isiZulu",
    "Sepedi", "Sesotho", "Setswana", "siSwati", "Tshivenda", "itsonga"
)
private val patientProvinces = listOf(
    "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
    "Mpumalanga", "Northern Cape", "North West", "Western Cape"
)

@SuppressLint("DefaultLocale")
@Composable
fun PatientRegister(
    initialEmail: String,
    isLoading: Boolean,
    errorMessage: String?,
    isGoogleSignup: Boolean = false,
    existingAccount: Boolean = false,
    initialName: String = "",
    initialSurname: String = "",
    onRegister: (PatientRegistrationData) -> Unit,
    onBackToLogin: () -> Unit,
    onViewPrivacyPolicy: () -> Unit
) {
    val background = AuthColors.Background
    val darkText = AuthColors.TextPrimary
    val green = AuthColors.PatientAccent
    val greyText = AuthColors.TextSecondary
    val navy = AuthColors.Button
    val errorRed = AuthColors.Error
    val context = LocalContext.current

    val fieldColors = authTextFieldColors(green)

    var stepIndex by rememberSaveable { mutableStateOf(0) }
    val step = RegisterStep.entries[stepIndex]

    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var name by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var surname by rememberSaveable(initialSurname) { mutableStateOf(initialSurname) }
    var title by rememberSaveable { mutableStateOf("") }
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    var idNumber by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }

    var phone by rememberSaveable { mutableStateOf("") }
    var language by rememberSaveable { mutableStateOf("") }
    var province by rememberSaveable { mutableStateOf("") }
    var address1 by rememberSaveable { mutableStateOf("") }
    var address2 by rememberSaveable { mutableStateOf("") }
    var address3 by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }

    var allergies by rememberSaveable { mutableStateOf("") }
    var medication by rememberSaveable { mutableStateOf("") }
    var conditions by rememberSaveable { mutableStateOf("") }
    var chronic by rememberSaveable { mutableStateOf("") }
    var surgeries by rememberSaveable { mutableStateOf("") }
    var bloodGroup by rememberSaveable { mutableStateOf("") }
    var disability by rememberSaveable { mutableStateOf("") }
    var emergencyName by rememberSaveable { mutableStateOf("") }
    var emergencyPhone by rememberSaveable { mutableStateOf("") }
    var emergencyRelationship by rememberSaveable { mutableStateOf("") }

    // Passwords deliberately use remember rather than rememberSaveable.
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var consentGiven by rememberSaveable { mutableStateOf(false) }

    var fieldErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var stepError by remember { mutableStateOf<String?>(null) }

    fun clearErrors() {
        fieldErrors = emptyMap()
        stepError = null
    }

    fun validateCurrentStep(): Boolean {
        val errors = mutableMapOf<String, String>()

        when (step) {
            RegisterStep.PERSONAL -> {
                if (name.isBlank()) errors["name"] = "First name is required."
                if (surname.isBlank()) errors["surname"] = "Surname is required."
                if (title.isBlank()) errors["title"] = "Select a title."
                if (dateOfBirth.isBlank()) errors["dob"] = "Select your date of birth."
                if (!isValidSaIdNumber(idNumber)) errors["id"] = "Enter a 13-digit South African ID number."
                if (gender.isBlank()) errors["gender"] = "Select a gender option."
            }

            RegisterStep.CONTACT -> {
                if (!isValidEmail(email)) errors["email"] = "Enter a valid email address."
                if (!isValidSaPhoneNumber(phone)) errors["phone"] = "Enter a 10-digit phone number."
                if (language.isBlank()) errors["language"] = "Select your preferred language."
                if (province.isBlank()) errors["province"] = "Select your province."
                if (address1.isBlank()) errors["address1"] = "Address line 1 is required."
                if (!isValidSaPostalCode(postalCode)) errors["postal"] = "Enter a 4-digit postal code."
            }

            RegisterStep.HEALTH -> {
                val emergencyStarted = emergencyName.isNotBlank() ||
                        emergencyPhone.isNotBlank() || emergencyRelationship.isNotBlank()
                if (emergencyStarted) {
                    if (emergencyName.isBlank()) errors["emergencyName"] = "Enter the contact's name."
                    if (!isValidSaPhoneNumber(emergencyPhone)) {
                        errors["emergencyPhone"] = "Enter a 10-digit phone number."
                    }
                    if (emergencyRelationship.isBlank()) {
                        errors["emergencyRelationship"] = "Enter the relationship."
                    }
                }
            }

            RegisterStep.SECURITY -> {
                if (!isGoogleSignup && !existingAccount) {
                    if (!isPasswordValid(password)) {
                        errors["password"] = "Your password does not meet all requirements yet."
                    }
                    if (password != confirmPassword) {
                        errors["confirmPassword"] = "Passwords do not match."
                    }
                }
                if (!consentGiven) errors["consent"] = "Consent is required to create your profile."
            }
        }

        fieldErrors = errors
        return errors.isEmpty()
    }

    fun continueFlow() {
        stepError = null
        if (!validateCurrentStep()) return

        if (step != RegisterStep.SECURITY) {
            stepIndex += 1
            fieldErrors = emptyMap()
            return
        }

        onRegister(
            PatientRegistrationData(
                name = name.trim(),
                surname = surname.trim(),
                title = title,
                dateOfBirth = dateOfBirth,
                idNumber = idNumber,
                phone = phone,
                language = language,
                province = province,
                address1 = address1.trim(),
                address2 = address2.trim(),
                address3 = address3.trim(),
                postalCode = postalCode,
                gender = gender,
                password = password,
                allergies = allergies.trim(),
                medication = medication.trim(),
                conditions = conditions.trim(),
                chronic = chronic.trim(),
                surgeries = surgeries.trim(),
                bloodGroup = bloodGroup.trim(),
                disability = disability.trim(),
                emergencyContactName = emergencyName.trim(),
                emergencyContactPhone = emergencyPhone,
                emergencyContactRelationship = emergencyRelationship.trim(),
                email = email.trim()
            )
        )
    }

    fun goBack() {
        clearErrors()
        if (stepIndex > 0) stepIndex -= 1
    }

    val healthFieldsBlank = listOf(
        allergies, medication, conditions, chronic, surgeries, bloodGroup, disability,
        emergencyName, emergencyPhone, emergencyRelationship
    ).all { it.isBlank() }

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
            Text("Create patient account", color = darkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Step ${stepIndex + 1} of ${RegisterStep.entries.size} · ${step.label}",
                color = greyText,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(step.helper, color = greyText, fontSize = 12.5.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / RegisterStep.entries.size },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = green,
                trackColor = Color(0xFFE3E6EC)
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                RegisterStep.PERSONAL -> {
                    RegistrationField(
                        value = name,
                        onValueChange = { name = it; fieldErrors = fieldErrors - "name" },
                        label = "First name",
                        error = fieldErrors["name"],
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        value = surname,
                        onValueChange = { surname = it; fieldErrors = fieldErrors - "surname" },
                        label = "Surname",
                        error = fieldErrors["surname"],
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionField(
                        value = title,
                        label = "Title",
                        options = patientTitles,
                        accent = green,
                        colors = fieldColors,
                        error = fieldErrors["title"],
                        onSelected = { title = it; fieldErrors = fieldErrors - "title" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val calendar = Calendar.getInstance()
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = {},
                            label = { Text("Date of birth") },
                            singleLine = true,
                            readOnly = true,
                            isError = fieldErrors["dob"] != null,
                            supportingText = fieldErrors["dob"]?.let { message ->
                                { Text(message, color = errorRed) }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Select date")
                            }
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            dateOfBirth = String.format("%04d-%02d-%02d", year, month + 1, day)
                                            fieldErrors = fieldErrors - "dob"
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).apply {
                                        datePicker.maxDate = System.currentTimeMillis()
                                    }.show()
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        value = idNumber,
                        onValueChange = { idNumber = digitsOnly(it, 13); fieldErrors = fieldErrors - "id" },
                        label = "South African ID number",
                        error = fieldErrors["id"],
                        colors = fieldColors,
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gender", color = darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    listOf("Male", "Female", "Prefer not to say").forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender == option,
                                onClick = { gender = option; fieldErrors = fieldErrors - "gender" }
                            )
                            Text(option, fontSize = 13.sp, color = darkText)
                        }
                    }
                    fieldErrors["gender"]?.let {
                        Text(it, color = errorRed, fontSize = 12.sp)
                    }
                }

                RegisterStep.CONTACT -> {
                    RegistrationField(
                        value = email,
                        onValueChange = { if (!existingAccount) email = it; fieldErrors = fieldErrors - "email" },
                        label = "Email address",
                        error = fieldErrors["email"],
                        colors = fieldColors,
                        keyboardType = KeyboardType.Email,
                        readOnly = existingAccount
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        value = phone,
                        onValueChange = { phone = digitsOnly(it, 10); fieldErrors = fieldErrors - "phone" },
                        label = "Phone number",
                        error = fieldErrors["phone"],
                        colors = fieldColors,
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionField(
                        value = language,
                        label = "Preferred language",
                        options = patientLanguages,
                        accent = green,
                        colors = fieldColors,
                        error = fieldErrors["language"],
                        onSelected = { language = it; fieldErrors = fieldErrors - "language" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionField(
                        value = province,
                        label = "Province",
                        options = patientProvinces,
                        accent = green,
                        colors = fieldColors,
                        error = fieldErrors["province"],
                        onSelected = { province = it; fieldErrors = fieldErrors - "province" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        value = address1,
                        onValueChange = { address1 = it; fieldErrors = fieldErrors - "address1" },
                        label = "Address line 1",
                        error = fieldErrors["address1"],
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        value = address2,
                        onValueChange = { address2 = it },
                        label = "Address line 2 (optional)",
                        error = null,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        value = address3,
                        onValueChange = { address3 = it },
                        label = "Address line 3 (optional)",
                        error = null,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        value = postalCode,
                        onValueChange = { postalCode = digitsOnly(it, 4); fieldErrors = fieldErrors - "postal" },
                        label = "Postal code",
                        error = fieldErrors["postal"],
                        colors = fieldColors,
                        keyboardType = KeyboardType.Number
                    )
                }

                RegisterStep.HEALTH -> {
                    SectionHeading(
                        title = "Medical information",
                        subtitle = "Optional. This can help a doctor understand your health history more quickly.",
                        color = darkText,
                        secondary = greyText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    RegistrationField(allergies, { allergies = it }, "Known allergies (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(medication, { medication = it }, "Current medication (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(conditions, { conditions = it }, "Past medical conditions (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(chronic, { chronic = it }, "Chronic illness (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(surgeries, { surgeries = it }, "Previous surgeries (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(bloodGroup, { bloodGroup = it }, "Blood group (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(disability, { disability = it }, "Disability (optional)", null, fieldColors)

                    Spacer(modifier = Modifier.height(28.dp))
                    SectionHeading(
                        title = "Emergency contact",
                        subtitle = "Optional. If you add one detail, complete all three fields.",
                        color = darkText,
                        secondary = greyText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    RegistrationField(
                        emergencyName,
                        { emergencyName = it; fieldErrors = fieldErrors - "emergencyName" },
                        "Contact name",
                        fieldErrors["emergencyName"],
                        fieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        emergencyPhone,
                        { emergencyPhone = digitsOnly(it, 10); fieldErrors = fieldErrors - "emergencyPhone" },
                        "Contact phone number",
                        fieldErrors["emergencyPhone"],
                        fieldColors,
                        KeyboardType.Phone
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RegistrationField(
                        emergencyRelationship,
                        { emergencyRelationship = it; fieldErrors = fieldErrors - "emergencyRelationship" },
                        "Relationship",
                        fieldErrors["emergencyRelationship"],
                        fieldColors
                    )
                }

                RegisterStep.SECURITY -> {
                    if (existingAccount) {
                        Text(
                            "Your sign-in account already exists. We will keep your current password and finish creating your patient profile.",
                            color = greyText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    } else if (isGoogleSignup) {
                        Text(
                            "Your sign-in account is already handled by your provider, so you do not need to create another password here.",
                            color = greyText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; fieldErrors = fieldErrors - "password" },
                            label = { Text("Password") },
                            singleLine = true,
                            isError = fieldErrors["password"] != null,
                            supportingText = fieldErrors["password"]?.let { message -> { Text(message, color = errorRed) } },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = fieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PasswordStrengthChecklist(password)
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; fieldErrors = fieldErrors - "confirmPassword" },
                            label = { Text("Confirm password") },
                            singleLine = true,
                            isError = fieldErrors["confirmPassword"] != null,
                            supportingText = fieldErrors["confirmPassword"]?.let { message -> { Text(message, color = errorRed) } },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = fieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        Checkbox(
                            checked = consentGiven,
                            onCheckedChange = {
                                consentGiven = it
                                fieldErrors = fieldErrors - "consent"
                            }
                        )
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                "I consent to receive healthcare services on this platform and agree that my information may be stored securely and used for healthcare purposes.",
                                fontSize = 13.sp,
                                color = greyText,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Read our Privacy Policy",
                                fontSize = 13.sp,
                                color = green,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onViewPrivacyPolicy() }
                            )
                            fieldErrors["consent"]?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(it, color = errorRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            (stepError ?: errorMessage)?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(it, color = errorRed, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (stepIndex > 0) {
                    OutlinedButton(
                        onClick = ::goBack,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Back", color = darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Button(
                    onClick = ::continueFlow,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navy)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        val label = when (step) {
                            RegisterStep.HEALTH -> if (healthFieldsBlank) "Skip for now" else "Continue"
                            RegisterStep.SECURITY -> if (existingAccount) "Complete profile" else "Create account"
                            else -> "Continue"
                        }
                        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!existingAccount) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("Already have an account? ", color = greyText, fontSize = 14.sp)
                    Text(
                        "Log in",
                        color = green,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBackToLogin() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegistrationField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    colors: androidx.compose.material3.TextFieldColors,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        readOnly = readOnly,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message, color = Color(0xFFD64545)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = colors,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectionField(
    value: String,
    label: String,
    options: List<String>,
    accent: Color,
    colors: androidx.compose.material3.TextFieldColors,
    error: String?,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                label = { Text(label) },
                readOnly = true,
                singleLine = true,
                isError = error != null,
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select $label", tint = accent)
                },
                colors = colors,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
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
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        error?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = Color(0xFFD64545), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String,
    color: Color,
    secondary: Color
) {
    Text(title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(subtitle, color = secondary, fontSize = 12.5.sp, lineHeight = 18.sp)
}
