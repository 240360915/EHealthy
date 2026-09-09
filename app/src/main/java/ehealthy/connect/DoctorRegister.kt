package ehealthy.connect

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val genderOptions = listOf("Male", "Female", "Prefer not to say")

/**
 * Doctor sign-up screen. Collects everything [DoctorRegistrationInfo] needs
 * and hands the finished object back via [onRegister]. Trimmed down from the
 * web version (ehealth-connect.netlify.app/doctor) to practice info +
 * personal info only — no address fields or document uploads yet.
 */
@Composable
fun DoctorRegister(
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRegister: (DoctorRegistrationInfo) -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit
) {
    val background = Color(0xFFFAF9FF)
    val darkText = Color(0xFF182033)
    val blue = Color(0xFF385A9E)
    val lightBlue = Color(0xFFEFF1FF)
    val greyText = Color(0xFF4F555C)
    val navyButton = Color(0xFF293147)
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = blue)
    val fieldShape = RoundedCornerShape(16.dp)

    // Practice information
    var practiceNumber by remember { mutableStateOf("") }
    var practiceName by remember { mutableStateOf("") }
    var hpcsaNumber by remember { mutableStateOf("") }
    var discipline by remember { mutableStateOf("") }
    var qualifications by remember { mutableStateOf("") }
    var operatingHours by remember { mutableStateOf("") }
    var consultationFee by remember { mutableStateOf("") }
    var medicalAidSchemes by remember { mutableStateOf("") }

    // Personal information
    var name by remember { mutableStateOf("") }
    var surname by remember { mutableStateOf("") }
    var idNumber by remember { mutableStateOf("") }
    var cellNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var languagesSpoken by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(genderOptions[0]) }

    // Account security
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val passwordsMatch = confirmPassword.isEmpty() || password == confirmPassword
    val canSubmit = !isLoading &&
            practiceNumber.isNotBlank() &&
            practiceName.isNotBlank() &&
            hpcsaNumber.isNotBlank() &&
            qualifications.isNotBlank() &&
            operatingHours.isNotBlank() &&
            medicalAidSchemes.isNotBlank() &&
            name.isNotBlank() &&
            surname.isNotBlank() &&
            idNumber.isNotBlank() &&
            cellNumber.isNotBlank() &&
            email.isNotBlank() &&
            languagesSpoken.isNotBlank() &&
            password.isNotBlank() &&
            password == confirmPassword

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(paddingValues)
                .padding(vertical = 20.dp)
        ) {

            IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = darkText
                )
            }

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(24.dp))
                    .background(lightBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MedicalServices,
                    contentDescription = null,
                    tint = blue,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Join as a doctor",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = darkText,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Set up your practice profile so patients\ncan find and book with you.",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = greyText,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ------------------------------------------------
            // PRACTICE INFORMATION
            // ------------------------------------------------

            SectionHeader(icon = Icons.Outlined.Work, text = "PRACTICE INFORMATION", tint = blue)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = practiceNumber,
                onValueChange = { practiceNumber = it },
                label = { Text("Practice number") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = practiceName,
                onValueChange = { practiceName = it },
                label = { Text("Practice name") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = hpcsaNumber,
                onValueChange = { hpcsaNumber = it },
                label = { Text("HPCSA registration number") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = discipline,
                onValueChange = { discipline = it },
                label = { Text("Discipline") },
                placeholder = { Text("e.g. General Practitioner") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = qualifications,
                onValueChange = { qualifications = it },
                label = { Text("Qualifications") },
                placeholder = { Text("e.g. MBChB, MMed") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = operatingHours,
                onValueChange = { operatingHours = it },
                label = { Text("Operating hours") },
                placeholder = { Text("e.g. Mon-Fri 08:00-17:00") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = consultationFee,
                onValueChange = { consultationFee = it },
                label = { Text("Consultation fee") },
                placeholder = { Text("e.g. 350") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = medicalAidSchemes,
                onValueChange = { medicalAidSchemes = it },
                label = { Text("Medical aid schemes accepted") },
                placeholder = { Text("e.g. Discovery, Medihelp, GEMS") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ------------------------------------------------
            // PERSONAL INFORMATION
            // ------------------------------------------------

            SectionHeader(icon = Icons.Outlined.Person, text = "PERSONAL INFORMATION", tint = blue)
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("First name") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = surname,
                onValueChange = { surname = it },
                label = { Text("Surname") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = idNumber,
                onValueChange = { new -> if (new.length <= 13 && new.all { it.isDigit() }) idNumber = new },
                label = { Text("ID number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = cellNumber,
                onValueChange = { cellNumber = it },
                label = { Text("Cell phone number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = languagesSpoken,
                onValueChange = { languagesSpoken = it },
                label = { Text("Languages spoken") },
                placeholder = { Text("e.g. English, Zulu, Afrikaans") },
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ------------------------------------------------
            // GENDER
            // ------------------------------------------------

            Text(
                text = "Gender",
                color = darkText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                genderOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .selectable(
                                selected = gender == option,
                                onClick = { gender = option }
                            )
                    ) {
                        RadioButton(
                            selected = gender == option,
                            onClick = { gender = option },
                            colors = RadioButtonDefaults.colors(selectedColor = blue)
                        )
                        Text(text = option, color = greyText, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ------------------------------------------------
            // ACCOUNT SECURITY
            // ------------------------------------------------

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
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
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password") },
                singleLine = true,
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
                shape = fieldShape,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth()
            )

            if (!passwordsMatch) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Passwords don't match.",
                    color = Color(0xFFD64545),
                    fontSize = 13.sp
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFD64545),
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ------------------------------------------------
            // REGISTER
            // ------------------------------------------------

            Button(
                onClick = {
                    onRegister(
                        DoctorRegistrationInfo(
                            practiceNumber = practiceNumber,
                            practiceName = practiceName,
                            hpcsaNumber = hpcsaNumber,
                            discipline = discipline,
                            qualifications = qualifications,
                            operatingHours = operatingHours,
                            consultationFee = consultationFee,
                            medicalAidSchemes = medicalAidSchemes,
                            name = name,
                            surname = surname,
                            idNumber = idNumber,
                            cellNumber = cellNumber,
                            email = email,
                            languagesSpoken = languagesSpoken,
                            gender = gender,
                            password = password
                        )
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(30.dp),
                colors = ButtonDefaults.buttonColors(containerColor = navyButton)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        "Register as doctor",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Already have an account? ",
                    color = greyText,
                    fontSize = 14.sp
                )
                Text(
                    text = "Log in",
                    color = blue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onLogin() }
                )
            }
        }
    }
}

/** Small caps section label with a left accent bar, echoing the web form's section headers. */
@Composable
private fun SectionHeader(icon: ImageVector, text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF1F2F8))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}