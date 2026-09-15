package ehealthy.connect

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ehealthy.connect.ui.common.PrivacyPolicyScreen
import ehealthy.connect.ui.common.isPasswordStrong
import ehealthy.connect.ui.doctor.DoctorDashboard
import ehealthy.connect.ui.doctor.DoctorForgotPassword
import ehealthy.connect.ui.doctor.DoctorLogin
import ehealthy.connect.ui.doctor.DoctorRegister
import ehealthy.connect.ui.doctor.DoctorRegistrationFiles
import ehealthy.connect.ui.doctor.DoctorResetPassword
import ehealthy.connect.ui.doctor.registerDoctor
import ehealthy.connect.ui.doctor.sendDoctorPasswordResetEmail
import ehealthy.connect.ui.doctor.signInDoctorWithEmail
import ehealthy.connect.ui.doctor.uriToDoctorFileUpload
import ehealthy.connect.ui.doctor.verifyDoctorResetCodeAndSetPassword
import ehealthy.connect.ui.onboarding.ChooseRoleScreen
import ehealthy.connect.ui.onboarding.OnBoardingScreenThree
import ehealthy.connect.ui.onboarding.OnboardingFour
import ehealthy.connect.ui.onboarding.OnboardingOne
import ehealthy.connect.ui.onboarding.OnboardingScreenTwo
import ehealthy.connect.ui.patient.CompleteProfile
import ehealthy.connect.ui.patient.LoginMode
import ehealthy.connect.ui.patient.PatientLogin
import ehealthy.connect.ui.patient.PatientPhoneEntry
import ehealthy.connect.ui.patient.PatientRegister
import ehealthy.connect.ui.patient.PatientSignupState
import ehealthy.connect.ui.patientDashboard.Appointment
import ehealthy.connect.ui.patientDashboard.BookAppointmentScreen
import ehealthy.connect.ui.patientDashboard.DoctorBookingInfo
import ehealthy.connect.ui.patientDashboard.DoctorListing
import ehealthy.connect.ui.patientDashboard.FindDoctorsScreen
import ehealthy.connect.ui.patientDashboard.HealthTipsScreen
import ehealthy.connect.ui.patientDashboard.MedicalRecord
import ehealthy.connect.ui.patientDashboard.MedicalRecordDisplay
import ehealthy.connect.ui.patientDashboard.MedicalRecordsScreen
import ehealthy.connect.ui.patientDashboard.PatientAppointmentsScreen
import ehealthy.connect.ui.patientDashboard.PatientDashboard
import ehealthy.connect.ui.patientDashboard.RescheduleAppointmentScreen
import ehealthy.connect.ui.patientDashboard.Review
import ehealthy.connect.ui.patientDashboard.ReviewDisplay
import ehealthy.connect.ui.splash.SplashScreen
import ehealthy.connect.ui.theme.EHealthyTheme
import ehealthy.connect.ui.doctor.DoctorProfile
import ehealthy.connect.util.SupabaseClientProvider
import ehealthy.connect.util.signInWithGoogle
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import io.github.jan.supabase.postgrest.query.Columns
import ehealthy.connect.ui.doctor.DoctorProfileRow
import ehealthy.connect.ui.doctor.toDoctorProfile
import ehealthy.connect.ui.doctor.uploadDoctorProfilePhoto
import ehealthy.connect.ui.doctor.DoctorPrescriptions
import ehealthy.connect.ui.doctor.PrescriptionPatient
import ehealthy.connect.ui.doctor.fetchConfirmedPrescriptionPatients
import ehealthy.connect.ui.doctor.savePrescription

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EHealthyTheme {
                AppNavGraph()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("onboarding1") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("onboarding1") {
            OnboardingOne(
                onContinue = { navController.navigate("onboarding2") },
                onSkip = { navController.navigate("choose") }
            )
        }

        composable("onboarding2") {
            OnboardingScreenTwo(
                onContinue = { navController.navigate("onboarding3") },
                onSkip = { navController.navigate("choose") }
            )
        }

        composable("onboarding3") {
            OnBoardingScreenThree(
                onContinue = { navController.navigate("onboarding4") },
                onSkip = { navController.navigate("choose") }
            )
        }

        composable("onboarding4") {
            OnboardingFour(
                onGetStarted = { navController.navigate("choose") }
            )
        }

        composable("choose") {
            ChooseRoleScreen(
                onPatientSelected = { navController.navigate("patientLogin") },
                onDoctorSelected = { navController.navigate("doctorLogin") }
            )
        }

        composable("patientLogin") {
            val scope = rememberCoroutineScope()

            var mode by remember { mutableStateOf(LoginMode.LOGIN) }
            var isLoading by remember { mutableStateOf(false) }
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var otp by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var successMessage by remember { mutableStateOf<String?>(null) }

            PatientLogin(
                mode = mode,
                isLoading = isLoading,
                email = email,
                password = password,
                otp = otp,
                newPassword = newPassword,
                confirmPassword = confirmPassword,
                errorMessage = errorMessage,
                successMessage = successMessage,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                onOtpChange = { otp = it },
                onNewPasswordChange = { newPassword = it },
                onConfirmPasswordChange = { confirmPassword = it },

                // Email + password submitted together
                onLogin = {
                    scope.launch {
                        errorMessage = null
                        successMessage = null
                        isLoading = true
                        try {
                            SupabaseClientProvider.client.auth.signInWith(Email) {
                                this.email = email
                                this.password = password
                            }
                            isLoading = false
                            navController.navigate("patientDashboard") {
                                popUpTo("patientLogin") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Log.e("LoginDebug", "Login failed", e)
                            errorMessage = e.message ?: "Login failed."
                        }
                    }
                },

                onForgotPasswordClick = {
                    errorMessage = null
                    successMessage = null
                    mode = LoginMode.FORGOT_REQUEST
                },

                // Step 1: send the reset code to that email
                onSendResetCode = {
                    scope.launch {
                        errorMessage = null
                        isLoading = true
                        try {
                            SupabaseClientProvider.client.auth.resetPasswordForEmail(email)
                            isLoading = false
                            successMessage = null
                            mode = LoginMode.FORGOT_VERIFY
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Couldn't send a code to that email: ${e.message}"
                        }
                    }
                },

                // Step 2: verify the recovery code
                onVerifyResetCode = {
                    scope.launch {
                        errorMessage = null
                        isLoading = true
                        try {
                            SupabaseClientProvider.client.auth.verifyEmailOtp(
                                type = OtpType.Email.RECOVERY,
                                email = email,
                                token = otp
                            )
                            isLoading = false
                            mode = LoginMode.FORGOT_RESET
                        } catch (_: Exception) {
                            isLoading = false
                            errorMessage = "Incorrect or expired code."
                        }
                    }
                },

                // Step 3: set the new password
                onResetPassword = {
                    scope.launch {
                        errorMessage = null
                        if (!isPasswordStrong(newPassword)) {
                            errorMessage = "Password doesn't meet the strength requirements."
                            return@launch
                        }
                        if (newPassword != confirmPassword) {
                            errorMessage = "Passwords don't match."
                            return@launch
                        }
                        isLoading = true
                        try {
                            SupabaseClientProvider.client.auth.updateUser { password = newPassword }
                            SupabaseClientProvider.client.auth.signOut()
                            isLoading = false
                            password = ""
                            otp = ""
                            newPassword = ""
                            confirmPassword = ""
                            successMessage = "Password updated — please log in."
                            mode = LoginMode.LOGIN
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Couldn't update password: ${e.message}"
                        }
                    }
                },

                onBackToLogin = {
                    errorMessage = null
                    successMessage = null
                    otp = ""
                    newPassword = ""
                    confirmPassword = ""
                    mode = LoginMode.LOGIN
                },

                onGoToRegister = {
                    navController.navigate("patientRegister?email=$email")
                }
            )
        }

        composable(
            "patientRegister?email={email}",
            arguments = listOf(navArgument("email") { defaultValue = "" })
        ) { backStackEntry ->
            val prefilledEmail = backStackEntry.arguments?.getString("email") ?: ""
            val scope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            PatientRegister(
                initialEmail = prefilledEmail,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBackToLogin = { navController.popBackStack() },
                onViewPrivacyPolicy = { navController.navigate("privacyPolicy") },
                onRegister = { data ->
                    scope.launch {
                        errorMessage = null
                        isLoading = true
                        try {
                            SupabaseClientProvider.client.auth.signUpWith(Email) {
                                email = prefilledEmail
                                password = data.password
                            }
                            val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                                ?: throw Exception("Could not create account")

                            SupabaseClientProvider.client.postgrest.from("patients").insert(
                                mapOf(
                                    "id" to userId,
                                    "user_id" to userId,
                                    "name" to data.name,
                                    "surname" to data.surname,
                                    "title" to data.title,
                                    "date_of_birth" to data.dateOfBirth,
                                    "id_number" to data.idNumber,
                                    "phone" to data.phone,
                                    "languege" to data.language,
                                    "email" to prefilledEmail,
                                    "gender" to data.gender,
                                    "province" to data.province,
                                    "address1" to data.address1,
                                    "address2" to data.address2,
                                    "address3" to data.address3.ifBlank { null },
                                    "postal_code" to data.postalCode,
                                    "allergies" to data.allergies.ifBlank { null },
                                    "medication" to data.medication.ifBlank { null },
                                    "conditions" to data.conditions.ifBlank { null },
                                    "chronic" to data.chronic.ifBlank { null },
                                    "surgeries" to data.surgeries.ifBlank { null },
                                    "blood_group" to data.bloodGroup.ifBlank { null },
                                    "disability" to data.disability.ifBlank { null },
                                    "emergency_contact_name" to data.emergencyContactName.ifBlank { null },
                                    "emergency_contact_phone" to data.emergencyContactPhone.ifBlank { null },
                                    "emergency_contact_relationship" to data.emergencyContactRelationship.ifBlank { null }
                                )
                            )

                            isLoading = false
                            navController.navigate("patientLogin") {
                                popUpTo("patientRegister?email={email}") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Registration failed: ${e.message}"
                        }
                    }
                }
            )
        }

        composable("privacyPolicy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }

        composable("doctorLogin") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var isGoogleLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            DoctorLogin(
                email = email,
                password = password,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                isLoading = isLoading,
                isGoogleLoading = isGoogleLoading,
                errorMessage = errorMessage,
                onLogin = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        val result = signInDoctorWithEmail(email, password)
                        isLoading = false
                        result.onSuccess {
                            navController.navigate("doctorDashboard")
                        }.onFailure { error ->
                            errorMessage = error.message
                        }
                    }
                },
                onForgotPassword = { navController.navigate("forgotPassword") },

                onRegister = { navController.navigate("doctorRegister") },
                onContinueWithGoogle = {
                    scope.launch {
                        isGoogleLoading = true
                        errorMessage = null
                        val result = signInWithGoogle(context)
                        isGoogleLoading = false
                        result.onSuccess {
                            navController.navigate("doctorDashboard")
                        }.onFailure { error ->
                            errorMessage = "Google sign-in failed: ${error.message}"
                        }
                    }
                }
            )
        }

        // ------------------------------------------------
        // DOCTOR REGISTER
        // ------------------------------------------------

        composable("doctorRegister") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            DoctorRegister(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onRegister = { info, uris ->
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        val files = DoctorRegistrationFiles(
                            profilePhoto = uris.profilePhoto?.let {
                                uriToDoctorFileUpload(
                                    context,
                                    it
                                )
                            },
                            idDocument = uris.idDocument?.let {
                                uriToDoctorFileUpload(
                                    context,
                                    it
                                )
                            },
                            hpcsaCertificate = uris.hpcsaCertificate?.let {
                                uriToDoctorFileUpload(
                                    context,
                                    it
                                )
                            },
                            medicalDegree = uris.medicalDegree?.let {
                                uriToDoctorFileUpload(
                                    context,
                                    it
                                )
                            },
                            specialistCertificate = uris.specialistCertificate?.let {
                                uriToDoctorFileUpload(
                                    context,
                                    it
                                )
                            },
                            practiceCertificate = uris.practiceCertificate?.let {
                                uriToDoctorFileUpload(
                                    context,
                                    it
                                )
                            },
                            proofOfAddress = uris.proofOfAddress?.let {
                                uriToDoctorFileUpload(
                                    context,
                                    it
                                )
                            }
                        )
                        val result = registerDoctor(info, files)
                        isLoading = false
                        result.onSuccess {
                            navController.navigate("doctorDashboard") {
                                popUpTo("doctorLogin") { inclusive = true }
                            }
                        }.onFailure { error ->
                            val message = error.message ?: "Registration failed — please try again."
                            if (message.contains("confirm your email", ignoreCase = true)) {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                navController.navigate("doctorLogin") {
                                    popUpTo("doctorRegister") { inclusive = true }
                                }
                            } else {
                                errorMessage = message
                            }
                        }
                    }
                },
                onLogin = {
                    navController.navigate("doctorLogin") {
                        popUpTo("doctorRegister") { inclusive = true }
                    }
                }
            )
        }

        // ------------------------------------------------
        // FORGOT PASSWORD (request code)
        // ------------------------------------------------

        composable("forgotPassword") {
            val scope = rememberCoroutineScope()
            var email by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            DoctorForgotPassword(
                email = email,
                onEmailChange = { email = it; errorMessage = null },
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSendCode = {
                    scope.launch {
                        isLoading = true
                        val result = sendDoctorPasswordResetEmail(email)
                        isLoading = false
                        result.onSuccess {
                            navController.navigate("resetPassword?email=$email")
                        }.onFailure { error ->
                            errorMessage =
                                error.message ?: "Couldn't send the code — please try again."
                        }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // ------------------------------------------------
        // RESET PASSWORD (enter code + new password)
        // ------------------------------------------------

        composable(
            "resetPassword?email={email}",
            arguments = listOf(navArgument("email") { defaultValue = "" })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            DoctorResetPassword(
                email = email,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onSubmit = { code, newPassword ->
                    scope.launch {
                        isLoading = true
                        val result = verifyDoctorResetCodeAndSetPassword(email, code, newPassword)
                        isLoading = false
                        result.onSuccess {
                            Toast.makeText(
                                context,
                                "Password updated — please log in.",
                                Toast.LENGTH_LONG
                            ).show()
                            navController.navigate("doctorLogin") {
                                popUpTo("forgotPassword") { inclusive = true }
                            }
                        }.onFailure { error ->
                            errorMessage =
                                error.message ?: "That code didn't work — please try again."
                        }
                    }
                },
                onResendCode = {
                    scope.launch { sendDoctorPasswordResetEmail(email) }
                }
            )
        }

        // ------------------------------------------------
        // DOCTOR DASHBOARD (placeholder)
        // ------------------------------------------------

        composable("doctorDashboard") {
            val scope = rememberCoroutineScope()
            val appContext = LocalContext.current
            var doctorProfile by remember { mutableStateOf<DoctorProfile?>(null) }
            var isLoadingProfile by remember { mutableStateOf(true) }
            var isUploadingPhoto by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    try {
                        val row = SupabaseClientProvider.client.postgrest
                            .from("doctors")
                            .select(
                                columns = Columns.list(
                                    "id", "name", "surname", "practice_name", "discipline",
                                    "profile_image_url", "verification_status"
                                )
                            ) {
                                filter { eq("user_id", userId) }
                            }
                            .decodeSingleOrNull<DoctorProfileRow>()
                        doctorProfile = row?.toDoctorProfile()
                    } catch (e: Exception) {
                        Log.e("DoctorDashboard", "Failed to fetch doctor profile", e)
                    }
                }
                isLoadingProfile = false
            }

            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        isUploadingPhoto = true
                        try {
                            val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                                ?: throw Exception("Not logged in")
                            val file = uriToDoctorFileUpload(appContext, uri)
                                ?: throw Exception("Could not read the selected image")
                            val publicUrl = uploadDoctorProfilePhoto(userId, file)
                            SupabaseClientProvider.client.postgrest.from("doctors")
                                .update({ set("profile_image_url", publicUrl) }) {
                                    filter { eq("user_id", userId) }
                                }
                            // Cache-buster — same trick the patient side uses, otherwise
                            // Coil keeps showing the old cached image after a re-upload.
                            doctorProfile = doctorProfile?.copy(
                                profileImageUrl = "$publicUrl?t=${System.currentTimeMillis()}"
                            )
                        } catch (e: Exception) {
                            Log.e("PhotoUpload", "Upload failed", e)
                        } finally {
                            isUploadingPhoto = false
                        }
                    }
                }
            }

            DoctorDashboard(
                doctorProfile = doctorProfile,
                isLoadingProfile = isLoadingProfile,
                isUploadingPhoto = isUploadingPhoto,
                onUploadPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onLogout = {
                    scope.launch {
                        SupabaseClientProvider.client.auth.signOut()
                        navController.navigate("choose") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                fetchAppointments = { doctorId ->
                    try {
                        val data = SupabaseClientProvider.client.postgrest
                            .from("appointments")
                            .select { filter { eq("doctor_id", doctorId) } }
                            .decodeList<Appointment>()
                        Result.success(data)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                onUpdateAppointmentStatus = { appointmentId, newStatus ->
                    try {
                        SupabaseClientProvider.client.postgrest.from("appointments")
                            .update({ set("status", newStatus) }) {
                                filter { eq("id", appointmentId) }
                            }
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                onNavigatePrescriptions = { navController.navigate("doctorPrescriptions") }
            )
        }

        composable("doctorPrescriptions") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var patients by remember { mutableStateOf<List<PrescriptionPatient>>(emptyList()) }
            var isLoadingPatients by remember { mutableStateOf(true) }
            var isSaving by remember { mutableStateOf(false) }
            var saveError by remember { mutableStateOf<String?>(null) }
            var doctorId by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    val row = SupabaseClientProvider.client.postgrest
                        .from("doctors")
                        .select(columns = Columns.list("id")) { filter { eq("user_id", userId) } }
                        .decodeSingleOrNull<Map<String, String?>>()
                    val id = row?.get("id")
                    doctorId = id
                    if (id != null) {
                        fetchConfirmedPrescriptionPatients(id)
                            .onSuccess { patients = it }
                            .onFailure { saveError = it.message }
                    }
                }
                isLoadingPatients = false
            }

            DoctorPrescriptions(
                patients = patients,
                isLoadingPatients = isLoadingPatients,
                isSaving = isSaving,
                saveError = saveError,
                onBack = { navController.popBackStack() },
                onSave = { patientId, medications ->
                    val docId = doctorId ?: return@DoctorPrescriptions
                    scope.launch {
                        isSaving = true
                        saveError = null
                        savePrescription(patientId, docId, medications)
                            .onSuccess {
                                Toast.makeText(context, "Prescription saved successfully", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            }
                            .onFailure { saveError = it.message ?: "Could not save prescription." }
                        isSaving = false
                    }
                }
            )
        }

        composable("patientPhoneEntry") {
            var phoneNumber by remember { mutableStateOf("") }
            PatientPhoneEntry(
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it },
                onContinue = {
                    navController.navigate("completeProfile?phone=$phoneNumber")
                }
            )
        }

        composable(
            "completeProfile?phone={phone}",
            arguments = listOf(navArgument("phone") { defaultValue = "" })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val scope = rememberCoroutineScope()

            CompleteProfile(
                onContinue = { title, idNumber, dateOfBirth, gender, language ->
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.postgrest.from("patients").insert(
                                mapOf(
                                    "title" to title,
                                    "id_number" to idNumber,
                                    "date_of_birth" to dateOfBirth,
                                    "gender" to gender,
                                    "languege" to language,
                                    "phone" to phone,
                                    "name" to PatientSignupState.firstName,
                                    "surname" to PatientSignupState.lastName,
                                    "email" to PatientSignupState.email,
                                    "user_id" to PatientSignupState.userId
                                )
                            )
                            Log.d("PatientInsert", "Insert succeeded")
                            navController.navigate("patientDashboard")
                        } catch (e: Exception) {
                            Log.e("PatientInsert", "Insert failed", e)
                        }
                    }
                }
            )
        }
        composable("patientDashboard") {
            val scope = rememberCoroutineScope()
            val appContext = LocalContext.current
            var patientName by remember { mutableStateOf("Patient") }
            var patientAvatarUrl by remember { mutableStateOf<String?>(null) }
            var isUploadingPhoto by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                if (userId != null) {
                    try {
                        val result = SupabaseClientProvider.client.postgrest
                            .from("patients")
                            .select { filter { eq("user_id", userId) } }
                            .decodeSingleOrNull<Map<String, String?>>()
                        val first = result?.get("name")
                        val last = result?.get("surname")
                        if (first != null) patientName = "$first ${last ?: ""}".trim()
                        patientAvatarUrl = result?.get("profile_image_url")
                            ?.let { "$it?t=${System.currentTimeMillis()}" }
                    } catch (_: Exception) {
                    }
                }
            }

            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        isUploadingPhoto = true
                        try {
                            val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                                ?: throw Exception("Not logged in")
                            val bytes = appContext.contentResolver.openInputStream(uri)
                                ?.use { it.readBytes() }
                                ?: throw Exception("Could not read the selected image")
                            val path = "$userId/avatar.jpg"
                            SupabaseClientProvider.client.storage
                                .from("patient-avatars")
                                .upload(path, bytes) { upsert = true }
                            val publicUrl = SupabaseClientProvider.client.storage
                                .from("patient-avatars")
                                .publicUrl(path)
                            SupabaseClientProvider.client.postgrest.from("patients")
                                .update({ set("profile_image_url", publicUrl) }) {
                                    filter { eq("user_id", userId) }
                                }
                            // Cache-buster: without this, Coil sees the same URL as before
                            // and keeps showing the OLD cached image after every re-upload.
                            patientAvatarUrl = "$publicUrl?t=${System.currentTimeMillis()}"
                        } catch (e: Exception) {
                            Log.e("PhotoUpload", "Upload failed", e)
                        } finally {
                            isUploadingPhoto = false
                        }
                    }
                }
            }

            PatientDashboard(
                patientName = patientName,
                patientAvatarUrl = patientAvatarUrl,
                isUploadingPhoto = isUploadingPhoto,
                onNavigateFindDoctors = { navController.navigate("findDoctors") },
                onNavigateAppointments = { navController.navigate("patientAppointments") },
                onNavigateMedicalRecords = { navController.navigate("medicalRecords") },
                onNavigateHealthTips = { navController.navigate("healthTips") },
                onUploadPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onLogout = {
                    scope.launch {
                        SupabaseClientProvider.client.auth.signOut()
                        navController.navigate("patientLogin") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onRescheduleAppointment = { appt ->
                    navController.navigate("rescheduleAppointment/${appt.id}")
                },
                onCancelAppointment = { appointmentId ->
                    try {
                        SupabaseClientProvider.client.postgrest.from("appointments")
                            .update({ set("status", "cancelled") }) {
                                filter { eq("id", appointmentId) }
                            }
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                fetchAppointments = {
                    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val data = SupabaseClientProvider.client.postgrest
                                .from("appointments")
                                .select { filter { eq("patient_id", userId) } }
                                .decodeList<Appointment>()
                            Result.success(data)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },
                fetchReviews = {
                    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val reviewList = SupabaseClientProvider.client.postgrest
                                .from("reviews")
                                .select { filter { eq("patient_id", userId) } }
                                .decodeList<Review>()

                            val doctorIds = reviewList.map { it.doctor_id }.distinct()
                            val doctorNames: Map<String, String> = if (doctorIds.isEmpty()) {
                                emptyMap()
                            } else {
                                SupabaseClientProvider.client.postgrest
                                    .from("doctors")
                                    .select { filter { isIn("id", doctorIds) } }
                                    .decodeList<Map<String, String?>>()
                                    .associate { doc ->
                                        (doc["id"] ?: "") to "Dr. ${doc["name"] ?: ""} ${doc["surname"] ?: ""}".trim()
                                    }
                            }

                            val display = reviewList
                                .sortedByDescending { it.created_at }
                                .map {
                                    ReviewDisplay(
                                        review = it,
                                        doctorName = doctorNames[it.doctor_id] ?: "Unknown Doctor"
                                    )
                                }

                            Result.success(display)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                }
            )
        }
        composable("findDoctors") {
            FindDoctorsScreen(
                onBack = { navController.popBackStack() },
                onSelectDoctor = { doc ->
                    navController.navigate("bookAppointment/${doc.id}")
                },
                fetchDoctors = {
                    try {
                        val data = SupabaseClientProvider.client.postgrest
                            .from("doctors")
                            .select {
                                filter { eq("verification_status", "approved") }
                            }
                            .decodeList<DoctorListing>()
                        Result.success(data)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            )
        }
        composable("healthTips") {
            HealthTipsScreen(
                onBack = { navController.popBackStack() },
                onBookConsultation = { navController.navigate("findDoctors") }
            )
        }
        composable("medicalRecords") {
            MedicalRecordsScreen(
                onBack = { navController.popBackStack() },
                fetchRecords = {
                    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        Result.failure(Exception("Please log in to view your records."))
                    } else {
                        try {
                            val patientId = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select { filter { eq("user_id", userId) } }
                                .decodeSingleOrNull<Map<String, String?>>()
                                ?.get("id")
                                ?: userId

                            val records = SupabaseClientProvider.client.postgrest
                                .from("medical_records")
                                .select { filter { eq("patient_id", patientId) } }
                                .decodeList<MedicalRecord>()

                            val doctorIds = records.mapNotNull { it.doctor_id }.distinct()
                            val doctorNames: Map<String, String> = if (doctorIds.isEmpty()) {
                                emptyMap()
                            } else {
                                SupabaseClientProvider.client.postgrest
                                    .from("doctors")
                                    .select { filter { isIn("id", doctorIds) } }
                                    .decodeList<Map<String, String?>>()
                                    .associate { doc ->
                                        val id = doc["id"] ?: ""
                                        val name =
                                            "Dr. ${doc["name"] ?: ""} ${doc["surname"] ?: ""}".trim()
                                        id to name
                                    }
                            }

                            val display = records.map { record ->
                                MedicalRecordDisplay(
                                    record = record,
                                    doctorName = doctorNames[record.doctor_id] ?: "Unknown Doctor"
                                )
                            }

                            Result.success(display)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                }
            )
        }
        composable(
            "bookAppointment/{doctorId}",
            arguments = listOf(navArgument("doctorId") { defaultValue = "" })
        ) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            val scope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            BookAppointmentScreen(
                isLoading = isLoading,
                errorMessage = errorMessage,
                onBack = { navController.popBackStack() },
                fetchDoctor = {
                    try {
                        val doc = SupabaseClientProvider.client.postgrest
                            .from("doctors")
                            .select { filter { eq("id", doctorId) } }
                            .decodeSingleOrNull<Map<String, String?>>()

                        if (doc == null) {
                            Result.failure(Exception("Doctor not found."))
                        } else {
                            Result.success(
                                DoctorBookingInfo(
                                    id = doctorId,
                                    name = doc["name"] ?: "",
                                    surname = doc["surname"] ?: "",
                                    discipline = doc["discipline"],
                                    hourlyRate = doc["hourly_rate"]?.toDoubleOrNull(),
                                    operatingHours = doc["operating_hours"]
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                fetchBookedTimes = { date ->
                    try {
                        val booked = SupabaseClientProvider.client.postgrest
                            .from("appointments")
                            .select {
                                filter {
                                    eq("doctor_id", doctorId)
                                    eq("date", date)
                                    neq("status", "cancelled")
                                }
                            }
                            .decodeList<Map<String, String?>>()
                            .mapNotNull { it["time"]?.take(5) }
                            .toSet()
                        Result.success(booked)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                onConfirmBooking = { submission ->
                    scope.launch {
                        errorMessage = null
                        isLoading = true
                        try {
                            val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                                ?: throw Exception("Please log in to book an appointment.")

                            val patient = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select { filter { eq("user_id", userId) } }
                                .decodeSingleOrNull<Map<String, String?>>()

                            val patientName =
                                "${patient?.get("name") ?: ""} ${patient?.get("surname") ?: ""}".trim()

                            SupabaseClientProvider.client.postgrest.from("appointments").insert(
                                mapOf(
                                    "doctor_id" to doctorId,
                                    "patient_id" to userId,
                                    "patient_name" to patientName.ifBlank { null },
                                    "date" to submission.date,
                                    "time" to submission.time,
                                    "reason" to submission.reason,
                                    "status" to "pending",
                                    "payment_method" to submission.paymentReference,
                                    "amount_paid" to submission.fee
                                )
                            )

                            isLoading = false
                            navController.navigate("bookingConfirmed") {
                                popUpTo("findDoctors") { inclusive = false }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Booking failed: ${e.message}"
                        }
                    }
                }
            )
        }
        composable("patientAppointments") {
            val scope = rememberCoroutineScope()
            var isSubmittingReview by remember { mutableStateOf(false) }
            var refreshTrigger by remember { mutableIntStateOf(0) }

            PatientAppointmentsScreen(
                onBack = { navController.popBackStack() },
                onFindDoctors = { navController.navigate("findDoctors") },
                fetchAppointments = {
                    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            refreshTrigger // read to force recomposition of LaunchedEffect key if you wire that up later
                            val data = SupabaseClientProvider.client.postgrest
                                .from("appointments")
                                .select { filter { eq("patient_id", userId) } }
                                .decodeList<Appointment>()
                            Result.success(data)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },
                fetchReviewedAppointmentIds = {
                    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val ids = SupabaseClientProvider.client.postgrest
                                .from("reviews")
                                .select { filter { eq("patient_id", userId) } }
                                .decodeList<Review>()
                                .mapNotNull { it.appointment_id }
                                .toSet()
                            Result.success(ids)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },
                isSubmittingReview = isSubmittingReview,
                onSubmitReview = { appointmentId, _, rating, comment ->
                    scope.launch {
                        isSubmittingReview = true
                        try {
                            val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                                ?: throw Exception("Not logged in")

                            val appt = SupabaseClientProvider.client.postgrest
                                .from("appointments")
                                .select { filter { eq("id", appointmentId) } }
                                .decodeSingleOrNull<Map<String, String?>>()
                            val doctorId = appt?.get("doctor_id")
                                ?: throw Exception("Could not find the doctor for this appointment")

                            SupabaseClientProvider.client.postgrest.from("reviews").insert(
                                mapOf(
                                    "patient_id" to userId,
                                    "doctor_id" to doctorId,
                                    "appointment_id" to appointmentId,
                                    "rating" to rating,
                                    "comment" to comment.ifBlank { null }
                                )
                            )
                            refreshTrigger++
                        } catch (e: Exception) {
                            Log.e("ReviewSubmit", "Failed to submit review", e)
                        } finally {
                            isSubmittingReview = false
                        }
                    }
                }
            )
        }
        composable(
            "rescheduleAppointment/{appointmentId}",
            arguments = listOf(navArgument("appointmentId") { defaultValue = "" })
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            val scope = rememberCoroutineScope()
            var appointment by remember { mutableStateOf<Appointment?>(null) }
            var doctorId by remember { mutableStateOf<String?>(null) }
            var isLoadingAppt by remember { mutableStateOf(true) }
            var isSubmitting by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(appointmentId) {
                try {
                    val row = SupabaseClientProvider.client.postgrest
                        .from("appointments")
                        .select { filter { eq("id", appointmentId) } }
                        .decodeSingleOrNull<Map<String, String?>>()
                    doctorId = row?.get("doctor_id")
                    appointment = Appointment(
                        id = appointmentId,
                        doctor_id = doctorId,
                        patient_name = row?.get("patient_name"),
                        reason = row?.get("reason"),
                        date = row?.get("date"),
                        time = row?.get("time"),
                        status = row?.get("status"),
                        payment_method = row?.get("payment_method"),
                        amount_paid = row?.get("amount_paid")?.toDoubleOrNull()
                    )
                } catch (e: Exception) {
                    errorMessage = "Could not load appointment: ${e.message}"
                }
                isLoadingAppt = false
            }

            when {
                isLoadingAppt -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
                appointment == null || doctorId == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMessage ?: "Appointment not found.")
                    }
                }
                else -> {
                    RescheduleAppointmentScreen(
                        appointment = appointment!!,
                        isLoading = isSubmitting,
                        errorMessage = errorMessage,
                        onBack = { navController.popBackStack() },
                        fetchBookedTimes = { date ->
                            try {
                                val booked = SupabaseClientProvider.client.postgrest
                                    .from("appointments")
                                    .select {
                                        filter {
                                            eq("doctor_id", doctorId!!)
                                            eq("date", date)
                                            neq("status", "cancelled")
                                            neq("id", appointmentId) // don't show this booking's own slot as "taken"
                                        }
                                    }
                                    .decodeList<Map<String, String?>>()
                                    .mapNotNull { it["time"]?.take(5) }
                                    .toSet()
                                Result.success(booked)
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        },
                        onConfirmReschedule = { newDate, newTime ->
                            scope.launch {
                                isSubmitting = true
                                errorMessage = null
                                try {
                                    SupabaseClientProvider.client.postgrest.from("appointments")
                                        .update({
                                            set("date", newDate)
                                            set("time", newTime)
                                            set("status", "pending")
                                        }) {
                                            filter { eq("id", appointmentId) }
                                        }
                                    isSubmitting = false
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    isSubmitting = false
                                    errorMessage = "Reschedule failed: ${e.message}"
                                }
                            }
                        }
                    )
                }
            }
        }

// ⚠️ PLACEHOLDER — replace with your real BookingConfirmedScreen once you share it.
// This just stops the app from crashing when a booking succeeds.
        composable("bookingConfirmed") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Appointment booked!", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        navController.navigate("patientDashboard") {
                            popUpTo("patientDashboard") { inclusive = true }
                        }
                    }) {
                        Text("Back to Dashboard")
                    }
                }
            }
        }

    }


}