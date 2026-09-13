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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import ehealthy.connect.ui.common.PasswordRequirementsChecklist
import ehealthy.connect.ui.common.PasswordStrengthMeter
import ehealthy.connect.ui.common.isPasswordValid
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.IconButton

enum class DoctorRegisterStep(val label: String) {
    PRACTICE("Practice Information"),
    PERSONAL("Personal Information"),
    ADDRESS("Practice Address"),
    DOCUMENTS("Documents & Photo"),
    SECURITY("Account Security")
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

private val titleOptions = listOf("Dr", "Prof", "Mr", "Mrs", "Ms")
private val genderOptions = listOf("Male", "Female", "Prefer not to say")
private val languageOptions = listOf(
    "Afrikaans", "English", "isiNdebele", "isiXhosa", "isiZulu",
    "Sepedi", "Sesotho", "Setswana", "siSwati", "Tshivenda", "itsonga"
)
private val provinceOptions = listOf(
    "Eastern Cape", "Free State", "Gauteng", "KwaZulu-Natal", "Limpopo",
    "Mpumalanga", "Northern Cape", "North West", "Western Cape"
)
private val documentMimeTypes = arrayOf("application/pdf", "image/jpeg", "image/png")

private fun queryDisplayName(context: Context, uri: Uri): String? = try {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
    }
} catch (e: Exception) {
    null
}

@Composable
fun DoctorRegister(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRegister: (DoctorRegistrationInfo, DoctorRegistrationUris) -> Unit,
    onLogin: () -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val blue = Color(0xFF385A9E)
    val greyText = Color(0xFF4F555C)
    val navyButton = Color(0xFF293147)
    val context = LocalContext.current

    @Composable
    fun fieldColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = blue,
        unfocusedBorderColor = Color(0xFFE2E5EC),
        focusedTextColor = darkText,
        unfocusedTextColor = darkText,
        cursorColor = blue,
        focusedLabelColor = blue,
        unfocusedLabelColor = greyText,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedPlaceholderColor = greyText,
        unfocusedPlaceholderColor = greyText
    )

    var step by remember { mutableStateOf(DoctorRegisterStep.PRACTICE) }

    var practiceNumber by remember { mutableStateOf("") }
    var practiceName by remember { mutableStateOf("") }
    var hpcsaNumber by remember { mutableStateOf("") }
    var discipline by remember { mutableStateOf("") }
    var qualifications by remember { mutableStateOf("") }
    var operatingHours by remember { mutableStateOf("") }
    var consultationFee by remember { mutableStateOf("") }
    var medicalAidSchemes by remember { mutableStateOf("") }

    var title by remember { mutableStateOf(titleOptions[0]) }
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var cellNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var languagesSpoken by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var addressLine3 by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }

    var profilePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var idDocumentUri by remember { mutableStateOf<Uri?>(null) }
    var hpcsaCertUri by remember { mutableStateOf<Uri?>(null) }
    var medicalDegreeUri by remember { mutableStateOf<Uri?>(null) }
    var specialistCertUri by remember { mutableStateOf<Uri?>(null) }
    var practiceCertUri by remember { mutableStateOf<Uri?>(null) }
    var proofOfAddressUri by remember { mutableStateOf<Uri?>(null) }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordFieldFocused by remember { mutableStateOf(false) }

    var stepError by remember { mutableStateOf<String?>(null) }

    val profilePhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) profilePhotoUri = uri
    }
    val idDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) idDocumentUri = uri
    }
    val hpcsaCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) hpcsaCertUri = uri
    }
    val medicalDegreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) medicalDegreeUri = uri
    }
    val specialistCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) specialistCertUri = uri
    }
    val practiceCertLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) practiceCertUri = uri
    }
    val proofOfAddressLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) proofOfAddressUri = uri
    }

    val passwordsMatch = confirmPassword.isEmpty() ||
            confirmPassword.length < password.length ||
            password == confirmPassword

    fun validateAndAdvance() {
        stepError = null
        when (step) {
            DoctorRegisterStep.PRACTICE -> {
                when {
                    practiceNumber.isBlank() -> stepError = "Please enter your practice number."
                    practiceName.isBlank() -> stepError = "Please enter your practice name."
                    hpcsaNumber.isBlank() -> stepError = "Please enter your HPCSA registration number."
                    qualifications.isBlank() -> stepError = "Please enter your qualifications."
                    operatingHours.isBlank() -> stepError = "Please enter your operating hours."
                    medicalAidSchemes.isBlank() -> stepError = "Please enter accepted medical aid schemes."
                    else -> step = DoctorRegisterStep.PERSONAL
                }
            }

            DoctorRegisterStep.PERSONAL -> {
                when {
                    name.isBlank() || surname.isBlank() -> stepError = "Please enter your full name."
                    idNumber.length != 13 -> stepError = "ID number must be exactly 13 digits."
                    cellNumber.length != 10 -> stepError = "Cell phone number must be exactly 10 digits."
                    email.isBlank() -> stepError = "Please enter your email address."
                    languagesSpoken.isBlank() -> stepError = "Please select a language."
                    gender.isBlank() -> stepError = "Please select a gender option."
                    else -> step = DoctorRegisterStep.ADDRESS
                }
            }

            DoctorRegisterStep.ADDRESS -> {
                when {
                    province.isBlank() -> stepError = "Please select a province."
                    city.isBlank() -> stepError = "Please enter a city."
                    addressLine1.isBlank() -> stepError = "Please enter address line 1."
                    postalCode.isBlank() -> stepError = "Please enter a postal code."
                    else -> step = DoctorRegisterStep.DOCUMENTS
                }
            }

            DoctorRegisterStep.DOCUMENTS -> {
                when {
                    idDocumentUri == null -> stepError = "Please upload a certified copy of your ID."
                    hpcsaCertUri == null -> stepError = "Please upload your HPCSA registration certificate."
                    medicalDegreeUri == null -> stepError = "Please upload your medical degree certificate."
                    practiceCertUri == null -> stepError = "Please upload your practice registration certificate."
                    proofOfAddressUri == null -> stepError = "Please upload proof of address."
                    else -> step = DoctorRegisterStep.SECURITY
                }
            }

            DoctorRegisterStep.SECURITY -> {
                when {
                    !isPasswordValid(password) -> stepError = "Your password doesn't meet all requirements yet."
                    password != confirmPassword -> stepError = "Passwords do not match."
                    else -> onRegister(
                        DoctorRegistrationInfo(
                            practiceNumber = practiceNumber,
                            practiceName = practiceName,
                            hpcsaNumber = hpcsaNumber,
                            discipline = discipline,
                            qualifications = qualifications,
                            operatingHours = operatingHours,
                            consultationFee = consultationFee,
                            medicalAidSchemes = medicalAidSchemes,
                            title = title,
                            name = name,
                            surname = surname,
                            idNumber = idNumber,
                            cellNumber = cellNumber,
                            email = email,
                            languagesSpoken = languagesSpoken,
                            gender = gender,
                            province = province,
                            city = city,
                            addressLine1 = addressLine1,
                            addressLine2 = addressLine2,
                            addressLine3 = addressLine3,
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
            }
        }
    }

    fun goBack() {
        stepError = null
        step = when (step) {
            DoctorRegisterStep.PRACTICE -> DoctorRegisterStep.PRACTICE
            DoctorRegisterStep.PERSONAL -> DoctorRegisterStep.PRACTICE
            DoctorRegisterStep.ADDRESS -> DoctorRegisterStep.PERSONAL
            DoctorRegisterStep.DOCUMENTS -> DoctorRegisterStep.ADDRESS
            DoctorRegisterStep.SECURITY -> DoctorRegisterStep.DOCUMENTS
        }
    }

    val stepIndex = DoctorRegisterStep.entries.indexOf(step)
    val totalSteps = DoctorRegisterStep.entries.size

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
            Text("Doctor Registration", color = darkText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Step ${stepIndex + 1} of $totalSteps · ${step.label}", color = greyText, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { (stepIndex + 1f) / totalSteps },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = blue,
                trackColor = Color(0xFFE3E6EC)
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                DoctorRegisterStep.PRACTICE -> {
                    OutlinedTextField(practiceNumber, { practiceNumber = it }, label = { Text("Practice number") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(practiceName, { practiceName = it }, label = { Text("Practice name") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(hpcsaNumber, { hpcsaNumber = it }, label = { Text("HPCSA registration number") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(discipline, { discipline = it }, label = { Text("Discipline") }, placeholder = { Text("e.g. General Practitioner") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(qualifications, { qualifications = it }, label = { Text("Qualifications") }, placeholder = { Text("e.g. MBChB, MMed") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(operatingHours, { operatingHours = it }, label = { Text("Operating hours") }, placeholder = { Text("e.g. Mon-Fri 08:00-17:00") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(consultationFee, { consultationFee = it }, label = { Text("Consultation fee") }, placeholder = { Text("e.g. 350") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(medicalAidSchemes, { medicalAidSchemes = it }, label = { Text("Medical aid schemes accepted") }, placeholder = { Text("e.g. Discovery, Medihelp, GEMS") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                }

                DoctorRegisterStep.PERSONAL -> {
                    OutlinedTextField(name, { name = it }, label = { Text("First name") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(surname, { surname = it }, label = { Text("Surname") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    var titleExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = title, onValueChange = {}, label = { Text("Title") }, singleLine = true, readOnly = true,
                            colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select title") }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { titleExpanded = true })
                        DropdownMenu(expanded = titleExpanded, onDismissRequest = { titleExpanded = false }) {
                            titleOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { title = option; titleExpanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(idNumber, { new -> if (new.length <= 13 && new.all { it.isDigit() }) idNumber = new }, label = { Text("ID number (13 digits)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(cellNumber, { new -> if (new.length <= 10) cellNumber = new }, label = { Text("Cell phone number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(email, { email = it }, label = { Text("Email address") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))

                    var languageExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = languagesSpoken, onValueChange = {}, label = { Text("Languages spoken") }, singleLine = true, readOnly = true,
                            colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select language") }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { languageExpanded = true })
                        DropdownMenu(expanded = languageExpanded, onDismissRequest = { languageExpanded = false }) {
                            languageOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { languagesSpoken = option; languageExpanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Gender", color = darkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        genderOptions.forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                                RadioButton(selected = gender == option, onClick = { gender = option }, colors = RadioButtonDefaults.colors(selectedColor = blue))
                                Text(option, fontSize = 12.sp, color = darkText)
                            }
                        }
                    }
                }

                DoctorRegisterStep.ADDRESS -> {
                    var provinceExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = province, onValueChange = {}, label = { Text("Province") }, singleLine = true, readOnly = true,
                            colors = fieldColors(), modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select province") }
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { provinceExpanded = true })
                        DropdownMenu(expanded = provinceExpanded, onDismissRequest = { provinceExpanded = false }) {
                            provinceOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { province = option; provinceExpanded = false })
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(city, { city = it }, label = { Text("City") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(addressLine1, { addressLine1 = it }, label = { Text("Address line 1") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(addressLine2, { addressLine2 = it }, label = { Text("Address line 2") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(addressLine3, { addressLine3 = it }, label = { Text("Address line 3 (optional)") }, singleLine = true, colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(postalCode, { postalCode = it }, label = { Text("Postal code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = fieldColors(), modifier = Modifier.fillMaxWidth())
                }

                DoctorRegisterStep.DOCUMENTS -> {
                    Text(
                        "Upload clear copies (PDF, JPG, or PNG). Your account stays pending until an administrator verifies these.",
                        color = greyText, fontSize = 12.5.sp, lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF1FF))
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
                            if (profilePhotoUri != null) "Change photo" else "Add profile photo",
                            color = blue, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                profilePhotoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    DocumentPickerField("Certified ID copy", idDocumentUri?.let { queryDisplayName(context, it) }, "Certified copy of your South African ID", blue) {
                        idDocumentLauncher.launch(documentMimeTypes)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField("HPCSA registration certificate", hpcsaCertUri?.let { queryDisplayName(context, it) }, null, blue) {
                        hpcsaCertLauncher.launch(documentMimeTypes)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField("Medical degree certificate", medicalDegreeUri?.let { queryDisplayName(context, it) }, "e.g. MBChB degree certificate", blue) {
                        medicalDegreeLauncher.launch(documentMimeTypes)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField("Specialist certificate (optional)", specialistCertUri?.let { queryDisplayName(context, it) }, "Only if applicable to your discipline", blue) {
                        specialistCertLauncher.launch(documentMimeTypes)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField("Practice registration certificate", practiceCertUri?.let { queryDisplayName(context, it) }, null, blue) {
                        practiceCertLauncher.launch(documentMimeTypes)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    DocumentPickerField("Proof of address", proofOfAddressUri?.let { queryDisplayName(context, it) }, "Utility bill or bank statement (not older than 3 months)", blue) {
                        proofOfAddressLauncher.launch(documentMimeTypes)
                    }
                }

                DoctorRegisterStep.SECURITY -> {
                    OutlinedTextField(
                        value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth().onFocusEvent { passwordFieldFocused = it.isFocused }
                    )
                    if (passwordFieldFocused || password.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        PasswordStrengthMeter(password = password)
                        Spacer(modifier = Modifier.height(10.dp))
                        PasswordRequirementsChecklist(password = password)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm password") }, singleLine = true,
                        isError = !passwordsMatch,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        colors = fieldColors(), modifier = Modifier.fillMaxWidth()
                    )
                    if (!passwordsMatch) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Passwords don't match.", color = Color(0xFFD64545), fontSize = 13.sp)
                    }
                }
            }

            (stepError ?: errorMessage)?.let {
                Spacer(modifier = Modifier.height(14.dp))
                Text(it, color = Color(0xFFD64545), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (step != DoctorRegisterStep.PRACTICE) {
                    OutlinedButton(
                        onClick = ::goBack, enabled = !isLoading,
                        modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Back", color = darkText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Button(
                    onClick = ::validateAndAdvance, enabled = !isLoading,
                    modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = navyButton)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.height(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (step == DoctorRegisterStep.SECURITY) "Register" else "Next",
                            color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("Already have an account? ", color = greyText, fontSize = 14.sp)
                Text("Log in", color = blue, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onLogin() })
            }
        }
    }
}

@Composable
private fun DocumentPickerField(
    label: String,
    fileName: String?,
    helperText: String?,
    accent: Color,
    onClick: () -> Unit
) {
    Column {
        Text(label, color = Color(0xFF182033), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFE2E5EC), RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.UploadFile, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                fileName ?: "Choose file",
                color = if (fileName != null) Color(0xFF182033) else Color(0xFF4F555C),
                fontSize = 14.sp
            )
        }
        if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(helperText, color = Color(0xFF4F555C), fontSize = 12.sp)
        }
    }
}