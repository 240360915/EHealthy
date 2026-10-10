package ehealthy.connect.ui.doctor

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ehealthy.connect.ui.common.AuthColors
import ehealthy.connect.ui.common.PasswordRequirementsChecklist
import ehealthy.connect.ui.common.authTextFieldColors
import ehealthy.connect.ui.common.PasswordStrengthMeter
import ehealthy.connect.ui.common.digitsOnly
import ehealthy.connect.ui.common.isPasswordValid
import ehealthy.connect.ui.common.isValidEmail
import ehealthy.connect.ui.common.isValidSaIdNumber
import ehealthy.connect.ui.common.isValidSaPhoneNumber
import ehealthy.connect.ui.common.isValidSaPostalCode

enum class DoctorRegisterStep(val label: String, val helper: String) {
    PROFESSIONAL("Professional details", "Your registration and practice information."),
    PERSONAL("About you", "Your personal and contact details."),
    PRACTICE("Practice location", "Where patients can find your practice."),
    DOCUMENTS("Verification documents", "Required documents are reviewed before your doctor account is approved."),
    SECURITY("Secure your account", "Create your sign-in password and submit your application.")
}

data class DoctorRegistrationUris(
    val profilePhoto: Uri?,
    val idDocument: Uri?,
    val hpcsaCertificate: Uri?,
    val medicalDegree: Uri?,
    val specialistCertificate: Uri?,
    val practiceCertificate: Uri?,
    val proofOfAddress: Uri?
)

private val doctorGenderOptions = listOf("Male", "Female", "Prefer not to say")
private val doctorLanguageOptions = listOf(
    "Afrikaans", "English", "isiNdebele", "isiXhosa", "isiZulu",
    "Sepedi", "Sesotho", "Setswana", "siSwati", "Tshivenda", "itsonga"
)
private val doctorProvinceOptions = listOf(
    "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
    "Mpumalanga", "Northern Cape", "North West", "Western Cape"
)
private val documentMimeTypes = arrayOf("application/pdf")

private fun queryDisplayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    }
} catch (_: Exception) {
    null
}

@Composable
fun DoctorRegister(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    initialEmail: String = "",
    existingAccount: Boolean = false,
    onRegister: (DoctorRegistrationInfo, DoctorRegistrationUris) -> Unit,
    onLogin: () -> Unit
) {
    val background = AuthColors.Background
    val darkText = AuthColors.TextPrimary
    val blue = AuthColors.DoctorAccent
    val greyText = AuthColors.TextSecondary
    val navyButton = AuthColors.Button
    val errorRed = AuthColors.Error
    val context = LocalContext.current

    val fieldColors = authTextFieldColors(blue)

    var stepIndex by rememberSaveable { mutableStateOf(0) }
    val step = DoctorRegisterStep.entries[stepIndex]

    var practiceNumber by rememberSaveable { mutableStateOf("") }
    var practiceName by rememberSaveable { mutableStateOf("") }
    var hpcsaNumber by rememberSaveable { mutableStateOf("") }
    var discipline by rememberSaveable { mutableStateOf("") }
    var qualifications by rememberSaveable { mutableStateOf("") }
    var operatingHours by rememberSaveable { mutableStateOf("") }
    var consultationFee by rememberSaveable { mutableStateOf("") }
    var medicalAidSchemes by rememberSaveable { mutableStateOf("") }

    val title = "Dr"
    var name by rememberSaveable { mutableStateOf("") }
    var surname by rememberSaveable { mutableStateOf("") }
    var idNumber by rememberSaveable { mutableStateOf("") }
    var cellNumber by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable(initialEmail) { mutableStateOf(initialEmail) }
    var languagesSpoken by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf("") }

    var province by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var addressLine1 by rememberSaveable { mutableStateOf("") }
    var addressLine2 by rememberSaveable { mutableStateOf("") }
    var addressLine3 by rememberSaveable { mutableStateOf("") }
    var postalCode by rememberSaveable { mutableStateOf("") }

    var profilePhotoUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var idDocumentUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var hpcsaCertUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var medicalDegreeUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var specialistCertUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceCertUriText by rememberSaveable { mutableStateOf<String?>(null) }
    var proofOfAddressUriText by rememberSaveable { mutableStateOf<String?>(null) }

    val profilePhotoUri = profilePhotoUriText?.let(Uri::parse)
    val idDocumentUri = idDocumentUriText?.let(Uri::parse)
    val hpcsaCertUri = hpcsaCertUriText?.let(Uri::parse)
    val medicalDegreeUri = medicalDegreeUriText?.let(Uri::parse)
    val specialistCertUri = specialistCertUriText?.let(Uri::parse)
    val practiceCertUri = practiceCertUriText?.let(Uri::parse)
    val proofOfAddressUri = proofOfAddressUriText?.let(Uri::parse)

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordFieldFocused by remember { mutableStateOf(false) }

    var fieldErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val profilePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) profilePhotoUriText = uri.toString()
    }
    val idDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            idDocumentUriText = uri.toString()
            fieldErrors = fieldErrors - "idDocument"
        }
    }
    val hpcsaCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            hpcsaCertUriText = uri.toString()
            fieldErrors = fieldErrors - "hpcsaCert"
        }
    }
    val medicalDegreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            medicalDegreeUriText = uri.toString()
            fieldErrors = fieldErrors - "medicalDegree"
        }
    }
    val specialistCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) specialistCertUriText = uri.toString()
    }
    val practiceCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            practiceCertUriText = uri.toString()
            fieldErrors = fieldErrors - "practiceCert"
        }
    }
    val proofOfAddressLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            proofOfAddressUriText = uri.toString()
            fieldErrors = fieldErrors - "proofOfAddress"
        }
    }

    fun validateCurrentStep(): Boolean {
        val errors = mutableMapOf<String, String>()

        when (step) {
            DoctorRegisterStep.PROFESSIONAL -> {
                if (practiceNumber.isBlank()) errors["practiceNumber"] = "Practice number is required."
                if (practiceName.isBlank()) errors["practiceName"] = "Practice name is required."
                if (hpcsaNumber.isBlank()) errors["hpcsaNumber"] = "HPCSA registration number is required."
                if (discipline.isBlank()) errors["discipline"] = "Discipline is required."
                if (qualifications.isBlank()) errors["qualifications"] = "Qualifications are required."
                if (consultationFee.isNotBlank() && consultationFee.toDoubleOrNull() == null) {
                    errors["consultationFee"] = "Enter a valid amount, for example 350."
                }
            }

            DoctorRegisterStep.PERSONAL -> {
                if (name.isBlank()) errors["name"] = "First name is required."
                if (surname.isBlank()) errors["surname"] = "Surname is required."
                if (!isValidSaIdNumber(idNumber)) errors["id"] = "Enter a 13-digit South African ID number."
                if (!isValidSaPhoneNumber(cellNumber)) errors["phone"] = "Enter a 10-digit phone number."
                if (!isValidEmail(email)) errors["email"] = "Enter a valid email address."
                if (languagesSpoken.isBlank()) errors["language"] = "Select a preferred language."
                if (gender.isBlank()) errors["gender"] = "Select a gender option."
            }

            DoctorRegisterStep.PRACTICE -> {
                if (province.isBlank()) errors["province"] = "Select a province."
                if (city.isBlank()) errors["city"] = "City is required."
                if (addressLine1.isBlank()) errors["address1"] = "Practice address is required."
                if (!isValidSaPostalCode(postalCode)) errors["postal"] = "Enter a 4-digit postal code."
            }

            DoctorRegisterStep.DOCUMENTS -> {
                if (idDocumentUri == null) errors["idDocument"] = "Upload a certified ID copy."
                if (hpcsaCertUri == null) errors["hpcsaCert"] = "Upload your HPCSA certificate."
                if (medicalDegreeUri == null) errors["medicalDegree"] = "Upload your medical degree certificate."
                if (practiceCertUri == null) errors["practiceCert"] = "Upload your practice registration certificate."
                if (proofOfAddressUri == null) errors["proofOfAddress"] = "Upload proof of address."
            }

            DoctorRegisterStep.SECURITY -> {
                if (!existingAccount) {
                    if (!isPasswordValid(password)) errors["password"] = "Your password does not meet all requirements yet."
                    if (password != confirmPassword) errors["confirmPassword"] = "Passwords do not match."
                }
            }
        }

        fieldErrors = errors
        return errors.isEmpty()
    }

    fun continueFlow() {
        if (!validateCurrentStep()) return

        if (step != DoctorRegisterStep.SECURITY) {
            stepIndex += 1
            fieldErrors = emptyMap()
            return
        }

        onRegister(
            DoctorRegistrationInfo(
                practiceNumber = practiceNumber.trim(),
                practiceName = practiceName.trim(),
                hpcsaNumber = hpcsaNumber.trim(),
                discipline = discipline.trim(),
                qualifications = qualifications.trim(),
                operatingHours = operatingHours.trim(),
                consultationFee = consultationFee.trim(),
                medicalAidSchemes = medicalAidSchemes.trim(),
                title = title,
                name = name.trim(),
                surname = surname.trim(),
                idNumber = idNumber,
                cellNumber = cellNumber,
                email = email.trim(),
                languagesSpoken = languagesSpoken,
                gender = gender,
                province = province,
                city = city.trim(),
                addressLine1 = addressLine1.trim(),
                addressLine2 = addressLine2.trim(),
                addressLine3 = addressLine3.trim(),
                postalCode = postalCode,
                password = password
            ),
            DoctorRegistrationUris(
                profilePhoto = profilePhotoUri,
                idDocument = idDocumentUri,
                hpcsaCertificate = hpcsaCertUri,
                medicalDegree = medicalDegreeUri,
                specialistCertificate = specialistCertUri,
                practiceCertificate = practiceCertUri,
                proofOfAddress = proofOfAddressUri
            )
        )
    }

    fun goBack() {
        fieldErrors = emptyMap()
        if (stepIndex > 0) stepIndex -= 1
    }

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
            Text("Doctor registration", color = darkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Step ${stepIndex + 1} of ${DoctorRegisterStep.entries.size} · ${step.label}",
                color = greyText,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(step.helper, color = greyText, fontSize = 12.5.sp, lineHeight = 18.sp)
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / DoctorRegisterStep.entries.size },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = blue,
                trackColor = Color(0xFFE3E6EC)
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                DoctorRegisterStep.PROFESSIONAL -> {
                    DoctorField(practiceNumber, { practiceNumber = it; fieldErrors = fieldErrors - "practiceNumber" }, "Practice number", fieldErrors["practiceNumber"], fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(practiceName, { practiceName = it; fieldErrors = fieldErrors - "practiceName" }, "Practice name", fieldErrors["practiceName"], fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(hpcsaNumber, { hpcsaNumber = it; fieldErrors = fieldErrors - "hpcsaNumber" }, "HPCSA registration number", fieldErrors["hpcsaNumber"], fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(discipline, { discipline = it; fieldErrors = fieldErrors - "discipline" }, "Discipline / speciality", fieldErrors["discipline"], fieldColors, placeholder = "e.g. General Practitioner")
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(qualifications, { qualifications = it; fieldErrors = fieldErrors - "qualifications" }, "Qualifications", fieldErrors["qualifications"], fieldColors, placeholder = "e.g. MBChB")

                    Spacer(modifier = Modifier.height(24.dp))
                    DoctorSectionHeading(
                        "Practice preferences",
                        "Optional for registration. You can complete or update these later from your profile.",
                        darkText,
                        greyText
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DoctorField(operatingHours, { operatingHours = it }, "Operating hours (optional)", null, fieldColors, placeholder = "e.g. Mon-Fri 08:00-17:00")
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(
                        consultationFee,
                        { consultationFee = it.filter { ch -> ch.isDigit() || ch == '.' }; fieldErrors = fieldErrors - "consultationFee" },
                        "Consultation fee (optional)",
                        fieldErrors["consultationFee"],
                        fieldColors,
                        keyboardType = KeyboardType.Decimal,
                        placeholder = "e.g. 350"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(medicalAidSchemes, { medicalAidSchemes = it }, "Medical aid schemes (optional)", null, fieldColors, placeholder = "e.g. Discovery, GEMS")
                }

                DoctorRegisterStep.PERSONAL -> {
                    DoctorField(name, { name = it; fieldErrors = fieldErrors - "name" }, "First name", fieldErrors["name"], fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(surname, { surname = it; fieldErrors = fieldErrors - "surname" }, "Surname", fieldErrors["surname"], fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(
                        idNumber,
                        { idNumber = digitsOnly(it, 13); fieldErrors = fieldErrors - "id" },
                        "South African ID number",
                        fieldErrors["id"],
                        fieldColors,
                        keyboardType = KeyboardType.Number
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(
                        cellNumber,
                        { cellNumber = digitsOnly(it, 10); fieldErrors = fieldErrors - "phone" },
                        "Cell phone number",
                        fieldErrors["phone"],
                        fieldColors,
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(
                        email,
                        { if (!existingAccount) email = it; fieldErrors = fieldErrors - "email" },
                        "Email address",
                        fieldErrors["email"],
                        fieldColors,
                        keyboardType = KeyboardType.Email,
                        readOnly = existingAccount
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorSelectionField(
                        value = languagesSpoken,
                        label = "Preferred language",
                        options = doctorLanguageOptions,
                        accent = blue,
                        colors = fieldColors,
                        error = fieldErrors["language"],
                        onSelected = { languagesSpoken = it; fieldErrors = fieldErrors - "language" }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gender", color = darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    doctorGenderOptions.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = gender == option,
                                onClick = { gender = option; fieldErrors = fieldErrors - "gender" }
                            )
                            Text(option, color = darkText, fontSize = 13.sp)
                        }
                    }
                    fieldErrors["gender"]?.let { Text(it, color = errorRed, fontSize = 12.sp) }
                }

                DoctorRegisterStep.PRACTICE -> {
                    DoctorSelectionField(
                        value = province,
                        label = "Province",
                        options = doctorProvinceOptions,
                        accent = blue,
                        colors = fieldColors,
                        error = fieldErrors["province"],
                        onSelected = { province = it; fieldErrors = fieldErrors - "province" }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(city, { city = it; fieldErrors = fieldErrors - "city" }, "City / town", fieldErrors["city"], fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(addressLine1, { addressLine1 = it; fieldErrors = fieldErrors - "address1" }, "Practice address line 1", fieldErrors["address1"], fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(addressLine2, { addressLine2 = it }, "Address line 2 (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(addressLine3, { addressLine3 = it }, "Address line 3 (optional)", null, fieldColors)
                    Spacer(modifier = Modifier.height(12.dp))
                    DoctorField(
                        postalCode,
                        { postalCode = digitsOnly(it, 4); fieldErrors = fieldErrors - "postal" },
                        "Postal code",
                        fieldErrors["postal"],
                        fieldColors,
                        keyboardType = KeyboardType.Number
                    )
                }

                DoctorRegisterStep.DOCUMENTS -> {
                    Text(
                        "Your documents stay private and are used to verify your professional registration. Required documents must be added before submission.",
                        color = greyText,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E5EC), CircleShape)
                                .clickable {
                                    profilePhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profilePhotoUri != null) {
                                AsyncImage(
                                    model = profilePhotoUri,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Outlined.AddAPhoto, contentDescription = "Add profile photo", tint = blue, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (profilePhotoUri != null) "Change profile photo" else "Add profile photo (optional)",
                            color = blue,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                profilePhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    DocumentPickerField(
                        label = "Certified ID copy",
                        fileName = idDocumentUri?.let { queryDisplayName(context, it) },
                        helperText = "Required · PDF",
                        accent = blue,
                        error = fieldErrors["idDocument"]
                    ) { idDocumentLauncher.launch(documentMimeTypes) }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField(
                        "HPCSA registration certificate",
                        hpcsaCertUri?.let { queryDisplayName(context, it) },
                        "Required · PDF",
                        blue,
                        fieldErrors["hpcsaCert"]
                    ) { hpcsaCertLauncher.launch(documentMimeTypes) }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField(
                        "Medical degree certificate",
                        medicalDegreeUri?.let { queryDisplayName(context, it) },
                        "Required · PDF",
                        blue,
                        fieldErrors["medicalDegree"]
                    ) { medicalDegreeLauncher.launch(documentMimeTypes) }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField(
                        "Specialist certificate",
                        specialistCertUri?.let { queryDisplayName(context, it) },
                        "Optional · only if applicable",
                        blue,
                        null
                    ) { specialistCertLauncher.launch(documentMimeTypes) }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField(
                        "Practice registration certificate",
                        practiceCertUri?.let { queryDisplayName(context, it) },
                        "Required · PDF",
                        blue,
                        fieldErrors["practiceCert"]
                    ) { practiceCertLauncher.launch(documentMimeTypes) }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField(
                        "Proof of address",
                        proofOfAddressUri?.let { queryDisplayName(context, it) },
                        "Required · PDF · recent utility bill or bank statement",
                        blue,
                        fieldErrors["proofOfAddress"]
                    ) { proofOfAddressLauncher.launch(documentMimeTypes) }
                }

                DoctorRegisterStep.SECURITY -> {
                    if (existingAccount) {
                        Text(
                            "Your sign-in account already exists. Your current password will stay unchanged when you submit this doctor profile.",
                            color = greyText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    } else {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; fieldErrors = fieldErrors - "password" },
                            label = { Text("Password") },
                            singleLine = true,
                            isError = fieldErrors["password"] != null,
                            supportingText = fieldErrors["password"]?.let { message -> { Text(message, color = errorRed) } },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            colors = fieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusEvent { passwordFieldFocused = it.isFocused }
                        )
                        if (passwordFieldFocused || password.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            PasswordStrengthMeter(password = password)
                            Spacer(modifier = Modifier.height(10.dp))
                            PasswordRequirementsChecklist(password = password)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; fieldErrors = fieldErrors - "confirmPassword" },
                            label = { Text("Confirm password") },
                            singleLine = true,
                            isError = fieldErrors["confirmPassword"] != null,
                            supportingText = fieldErrors["confirmPassword"]?.let { message -> { Text(message, color = errorRed) } },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            colors = fieldColors,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "After submission, your account remains pending until your documents are reviewed and approved. You can check your verification status from the app.",
                        color = greyText,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            errorMessage?.let {
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
                    colors = ButtonDefaults.buttonColors(containerColor = navyButton)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (step == DoctorRegisterStep.SECURITY) {
                                if (existingAccount) "Submit profile" else "Submit application"
                            } else {
                                "Continue"
                            },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!existingAccount) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text("Already have an account? ", color = greyText, fontSize = 14.sp)
                    Text(
                        "Log in",
                        color = blue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onLogin() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    colors: androidx.compose.material3.TextFieldColors,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { valueText -> { Text(valueText) } },
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
private fun DoctorSelectionField(
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
            Box(modifier = Modifier.matchParentSize().clickable { expanded = true })
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
private fun DoctorSectionHeading(
    title: String,
    subtitle: String,
    color: Color,
    secondary: Color
) {
    Text(title, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(subtitle, color = secondary, fontSize = 12.5.sp, lineHeight = 18.sp)
}

@Composable
private fun DocumentPickerField(
    label: String,
    fileName: String?,
    helperText: String?,
    accent: Color,
    error: String?,
    onClick: () -> Unit
) {
    Column {
        Text(label, color = Color(0xFF182033), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(
                    1.dp,
                    if (error != null) Color(0xFFD64545) else Color(0xFFE2E5EC),
                    RoundedCornerShape(16.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                fileName ?: "Choose PDF",
                color = if (fileName != null) Color(0xFF182033) else Color(0xFF4F555C),
                fontSize = 14.sp
            )
        }
        helperText?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = Color(0xFF4F555C), fontSize = 12.sp)
        }
        error?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, color = Color(0xFFD64545), fontSize = 12.sp)
        }
    }
}
