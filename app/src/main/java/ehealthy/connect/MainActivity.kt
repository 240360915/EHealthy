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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ehealthy.connect.ui.common.PrivacyPolicyScreen
import ehealthy.connect.ui.common.ContactUsScreen
import ehealthy.connect.ui.common.isPasswordStrong
import ehealthy.connect.ui.doctorDashboard.DoctorAccountSettings
import ehealthy.connect.ui.doctorDashboard.DoctorDashboard
import ehealthy.connect.ui.doctorDashboard.DoctorForgotPassword
import ehealthy.connect.ui.doctor.DoctorLogin
import ehealthy.connect.ui.doctorDashboard.DoctorPrescriptions
import ehealthy.connect.ui.doctorDashboard.DoctorProfile
import ehealthy.connect.ui.doctorDashboard.DoctorProfileRow
import ehealthy.connect.ui.doctor.DoctorRegister
import ehealthy.connect.ui.doctorDashboard.DoctorRegistrationFiles
import ehealthy.connect.ui.doctor.DoctorResetPassword
import ehealthy.connect.ui.doctorDashboard.DoctorSettingsScreen
import ehealthy.connect.ui.doctorDashboard.DoctorTimeSlots
import ehealthy.connect.ui.doctorDashboard.PrescriptionPatient
import ehealthy.connect.ui.doctor.fetchConfirmedPrescriptionPatients
import ehealthy.connect.ui.doctor.fetchDoctorTimeSlots
import ehealthy.connect.ui.doctor.friendlyAuthError
import ehealthy.connect.ui.doctor.registerDoctor
import ehealthy.connect.ui.doctor.saveDoctorTimeSlots
import ehealthy.connect.ui.doctor.savePrescription
import ehealthy.connect.ui.doctor.sendDoctorPasswordResetEmail
import ehealthy.connect.ui.doctor.signInDoctorWithEmail
import ehealthy.connect.ui.doctorDashboard.toDoctorProfile
import ehealthy.connect.ui.doctorDashboard.uploadDoctorProfilePhoto
import ehealthy.connect.ui.doctorDashboard.uriToDoctorFileUpload
import ehealthy.connect.ui.doctor.verifyDoctorResetCodeAndSetPassword
import ehealthy.connect.ui.onboarding.ChooseRoleScreen
import ehealthy.connect.ui.onboarding.OnBoardingScreenThree
import ehealthy.connect.ui.onboarding.OnboardingFour
import ehealthy.connect.ui.onboarding.OnboardingOne
import ehealthy.connect.ui.onboarding.OnboardingScreenTwo
import ehealthy.connect.ui.patient.LoginMode
import ehealthy.connect.ui.patient.PatientLogin
import ehealthy.connect.ui.patient.PatientRegister
import ehealthy.connect.ui.patientDashboard.Appointment
import android.content.Intent
import ehealthy.connect.data.BookingRepository
import ehealthy.connect.data.ProfileRepository
import ehealthy.connect.ui.patientDashboard.BookAppointmentScreen
import ehealthy.connect.ui.patientDashboard.BookingConfirmation
import ehealthy.connect.ui.patientDashboard.BookingConfirmedScreen
import ehealthy.connect.ui.patientDashboard.DoctorBookingInfo

import ehealthy.connect.ui.patientDashboard.DoctorProfileScreen
import ehealthy.connect.ui.patientDashboard.DoctorReviewItem
import ehealthy.connect.ui.patientDashboard.EditProfileScreen
import ehealthy.connect.ui.patientDashboard.FindDoctors.FindDoctorsScreen
import ehealthy.connect.ui.patientDashboard.HealthTipsScreen
import ehealthy.connect.ui.patientDashboard.HealthProfileScreen
import ehealthy.connect.ui.patientDashboard.MedicalRecord
import ehealthy.connect.ui.patientDashboard.MedicalRecordDisplay
import ehealthy.connect.ui.patientDashboard.MedicalRecordsScreen
import ehealthy.connect.ui.patientDashboard.MessagingScreen

import ehealthy.connect.ui.patientDashboard.PatientDashboard
import ehealthy.connect.ui.patientDashboard.PatientPrescriptionsScreen
import ehealthy.connect.ui.patientDashboard.PatientProfile
import ehealthy.connect.ui.patientDashboard.Prescription

import ehealthy.connect.ui.patientDashboard.PatientSettings

import ehealthy.connect.ui.patientDashboard.Review
import ehealthy.connect.ui.patientDashboard.ReviewDisplay
import ehealthy.connect.ui.patientDashboard.SettingsScreen
import ehealthy.connect.ui.theme.EHealthyTheme
import ehealthy.connect.util.SupabaseClientProvider
import ehealthy.connect.util.ThemeManager
import ehealthy.connect.util.showBoldToast
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ehealthy.connect.ui.patientDashboard.DoctorProfile as PatientDoctorProfile
import ehealthy.connect.data.patient.PatientAppointment
import ehealthy.connect.data.patient.PatientRepository
import ehealthy.connect.ui.patientDashboard.ConsultDoctorNow.OnDemandConsultationScreen
import ehealthy.connect.ui.patientDashboard.FindDoctors.DoctorListing
import ehealthy.connect.ui.patientDashboard.myVisits.PatientVisitsScreen
import ehealthy.connect.ui.patientDashboard.myVisits.RateVisitScreen
import ehealthy.connect.ui.patientDashboard.myVisits.RescheduleAppointmentScreen
import ehealthy.connect.ui.doctorDashboard.RefillRequest
import ehealthy.connect.ui.patientDashboard.PatientNotificationsScreen
import ehealthy.connect.ui.patientDashboard.PatientInvoicesScreen
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
private object BookingConfirmationHolder {
    var confirmation: BookingConfirmation? = null
}

@Serializable
private data class PatientLookup(
    val id: String? = null,
    val name: String? = null,
    val surname: String? = null,
    val profile_image_url: String? = null
)
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        enableEdgeToEdge()
        val pendingRoute = intent.getStringExtra("navigateTo")
        setContent {
            EHealthyTheme(darkTheme = ThemeManager.isDarkMode) {   // add darkTheme = ThemeManager.isDarkMode
                AppNavGraph(pendingRoute = pendingRoute)
            }
        }
    }
}
@Serializable
private data class ProfileExistsRow(
    val user_id: String? = null
)
@Serializable
private data class DoctorLookup(
    val name: String? = null,
    val surname: String? = null,
    val discipline: String? = null,
    val hourly_rate: Double? = null,
    val operating_hours: String? = null
)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph(pendingRoute: String? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable(
            route = "consultationCall/{requestId}/{role}",
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                },
                navArgument("role") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val requestId =
                backStackEntry.arguments
                    ?.getString("requestId")
                    .orEmpty()

            val role =
                backStackEntry.arguments
                    ?.getString("role")
                    .orEmpty()

            val isDoctor =
                role == "doctor"


            ehealthy.connect.consultation.ConsultationCallScreen(
                requestId = requestId,
                displayName = if (isDoctor) {
                    "Doctor"
                } else {
                    "Patient"
                },
                isDoctorView = isDoctor,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("splash") {
            var startupError by remember { mutableStateOf(false) }
            var retry by remember { mutableIntStateOf(0) }
            LaunchedEffect(retry) {
                startupError = false
                try {
                    val home = ProfileRepository.startupRoute()
                    val scheduledPatientCall =
                        pendingRoute
                            ?.matches(
                                Regex(
                                    "call/[0-9a-fA-F-]{36}"
                                )
                            ) == true &&
                                home == "patientDashboard"


                    val scheduledDoctorCall =
                        pendingRoute
                            ?.matches(
                                Regex(
                                    "doctorCall/[0-9a-fA-F-]{36}"
                                )
                            ) == true &&
                                home == "doctorDashboard"


                    val consultationPatientCall =
                        pendingRoute
                            ?.matches(
                                Regex(
                                    "consultationCall/[0-9a-fA-F-]{36}/patient"
                                )
                            ) == true &&
                                home == "patientDashboard"


                    val consultationDoctorCall =
                        pendingRoute
                            ?.matches(
                                Regex(
                                    "consultationCall/[0-9a-fA-F-]{36}/doctor"
                                )
                            ) == true &&
                                home == "doctorDashboard"


                    val doctorAvailabilityRoute =
                        home == "doctorDashboard" &&
                                pendingRoute == "doctorAvailability"


                    val destination =
                        if (
                            scheduledPatientCall ||
                            scheduledDoctorCall ||
                            consultationPatientCall ||
                            consultationDoctorCall ||
                            doctorAvailabilityRoute
                        ) {
                            pendingRoute!!
                        } else {
                            home
                        }


                    navController.navigate(
                        destination
                    ) {

                        popUpTo(
                            "splash"
                        ) {
                            inclusive = true
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                } catch (_: Exception) { startupError = true }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (startupError) androidx.compose.material3.TextButton(onClick = { retry++ }) {
                    Text("Could not restore your profile. Tap to retry.")
                } else CircularProgressIndicator()
            }
        }
        composable("editProfile") {
            val scope = rememberCoroutineScope()

            EditProfileScreen(
                onBack = { navController.popBackStack() },
                fetchProfile = {
                    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val row = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select(
                                    columns = Columns.list(
                                        "name", "surname", "phone", "gender",
                                        "address1", "address2", "address3", "postal_code", "province",
                                        "emergency_contact_name", "emergency_contact_phone", "emergency_contact_relationship",
                                        "allergies", "blood_group", "chronic", "medication", "surgeries", "disability",
                                        "medical_aid_scheme", "medical_aid_number", "medical_aid_plan"
                                    )
                                ) { filter { eq("user_id", userId) } }
                                .decodeSingleOrNull<PatientProfile>()
                            Result.success(row ?: PatientProfile())
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },
                onSaveProfile = { profile ->
                    val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            SupabaseClientProvider.client.postgrest.from("patients").update(
                                mapOf(
                                    "name" to profile.name,
                                    "surname" to profile.surname,
                                    "phone" to profile.phone,
                                    "gender" to profile.gender,
                                    "address1" to profile.address1,
                                    "address2" to profile.address2,
                                    "address3" to profile.address3,
                                    "postal_code" to profile.postal_code,
                                    "province" to profile.province,
                                    "emergency_contact_name" to profile.emergency_contact_name,
                                    "emergency_contact_phone" to profile.emergency_contact_phone,
                                    "emergency_contact_relationship" to profile.emergency_contact_relationship,
                                    "allergies" to profile.allergies,
                                    "blood_group" to profile.blood_group,
                                    "chronic" to profile.chronic,
                                    "medication" to profile.medication,
                                    "surgeries" to profile.surgeries,
                                    "disability" to profile.disability,
                                    "medical_aid_scheme" to profile.medical_aid_scheme,
                                    "medical_aid_number" to profile.medical_aid_number,
                                    "medical_aid_plan" to profile.medical_aid_plan
                                )
                            ) { filter { eq("user_id", userId) } }
                            Result.success(Unit)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },
                onChangePassword = { currentPassword, newPassword ->
                    try {
                        val email = SupabaseClientProvider.client.auth.currentUserOrNull()?.email
                            ?: throw Exception("Not logged in.")

                        // Re-authenticate with the current password first — this confirms the
                        // person changing the password actually knows the old one, rather than
                        // relying only on "device has an active session" (e.g. an unlocked
                        // phone left unattended shouldn't be enough to change the password).
                        SupabaseClientProvider.client.auth.signInWith(Email) {
                            this.email = email.trim()
                            this.password = currentPassword
                        }

                        SupabaseClientProvider.client.auth.updateUser { password = newPassword }
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure(Exception("Current password is incorrect, or the update failed: ${e.message}"))
                    }
                }
            )
        }
        composable("healthProfile") {

            HealthProfileScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }


        composable("settings") {
            val scope = rememberCoroutineScope()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted or denied — token fetch below runs either way */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                // Always try to fetch and save the token, regardless of permission outcome.
                try {
                    ehealthy.connect.consultation.ConsultationDevices.sync()
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to get/save token")
                }
            }
            SettingsScreen(
                isDarkMode = ThemeManager.isDarkMode,

                onToggleDarkMode = {
                    ThemeManager.updateDarkMode(it)
                },

                onNavigateEditProfile = {
                    navController.navigate("editProfile")
                },

                onNavigateHealthProfile = {
                    navController.navigate("healthProfile")
                },

                onNavigateInvoices = {
                    navController.navigate("patientInvoices")
                },

                onNavigateContactUs = {
                    navController.navigate("contactUs")
                },

                onBack = {
                    navController.popBackStack()
                },

                onViewPrivacyPolicy = {
                    navController.navigate("privacyPolicy")
                },

                onLogout = {
                    scope.launch {
                        ehealthy.connect.consultation
                            .ConsultationDevices
                            .signOut()

                        navController.navigate("patientLogin") {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                },
                fetchSettings = {
                    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val row = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select(columns = Columns.list("email_notifications", "sms_notifications", "profile_visible")) {
                                    filter { eq("user_id", userId) }
                                }
                                .decodeSingleOrNull<PatientSettings>()
                            Result.success(row ?: PatientSettings())
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },
                onUpdateSetting = { newSettings ->
                    scope.launch {
                        val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id ?: return@launch
                        try {
                            SupabaseClientProvider.client.postgrest.from("patients").update(
                                mapOf(
                                    "email_notifications" to newSettings.email_notifications,
                                    "sms_notifications" to newSettings.sms_notifications,
                                    "profile_visible" to newSettings.profile_visible
                                )
                            ) { filter { eq("user_id", userId) } }
                        } catch (e: Exception) {
                            Log.e("SettingsUpdate", "Failed to save setting")
                        }
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
            val context = LocalContext.current

            var mode by remember { mutableStateOf(LoginMode.LOGIN) }
            var isLoading by remember { mutableStateOf(false) }
            var isGoogleLoading by remember { mutableStateOf(false) }
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
                                this.email = email.trim()
                                this.password = password
                            }
                            val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                                ?: throw Exception("Sign-in did not create a session. Please sign in again.")

                            val patientRow = SupabaseClientProvider.client.postgrest.from("patients")
                                .select(columns = Columns.list("id")) { filter { eq("user_id", userId) } }
                                .decodeSingleOrNull<Map<String, String?>>()
                            if (patientRow == null) {
                                isLoading = false
                                navController.navigate("patientRegister?email=${Uri.encode(email.trim())}")
                                return@launch
                            }

                            isLoading = false
                            navController.navigate("patientDashboard") {
                                popUpTo("patientLogin") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            Log.e("LoginDebug", "Login failed", e)
                            errorMessage = if (e.message?.contains("invalid_credentials", ignoreCase = true) == true ||
                                e.message?.contains("Invalid login credentials", ignoreCase = true) == true
                            ) {
                                "Invalid email or password. Please try again."
                            } else {
                                "Login failed. Please try again."
                            }
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
                            ehealthy.connect.consultation.ConsultationDevices.signOut()
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
                    navController.navigate("patientRegister?email=${Uri.encode(email.trim())}")
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
                            ehealthy.connect.data.RegistrationRepository.registerPatient(data)

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

        composable("contactUs") {

            val context =
                LocalContext.current

            ContactUsScreen(

                onBack = {
                    navController.popBackStack()
                },


                // ---------------------------------
                // EMAIL SUPPORT
                // ---------------------------------
                onEmailSupport = {

                    val intent =
                        Intent(
                            Intent.ACTION_SENDTO
                        ).apply {

                            data =
                                Uri.parse(
                                    "mailto:ehealthy.enquiries@gmail.com"
                                )

                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "EHealthy Support Enquiry"
                            )
                        }


                    try {

                        context.startActivity(
                            intent
                        )

                    } catch (_: Exception) {

                        Toast.makeText(
                            context,
                            "No email application was found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },


                // ---------------------------------
                // CALL SUPPORT
                // ---------------------------------
                onCallSupport = {

                    val intent =
                        Intent(
                            Intent.ACTION_DIAL
                        ).apply {

                            data =
                                Uri.parse(
                                    "tel:0812074720"
                                )
                        }


                    try {

                        context.startActivity(
                            intent
                        )

                    } catch (_: Exception) {

                        Toast.makeText(
                            context,
                            "Unable to open the phone application.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },


                // ---------------------------------
                // REPORT A PROBLEM
                // ---------------------------------
                onReportProblem = {

                    val intent =
                        Intent(
                            Intent.ACTION_SENDTO
                        ).apply {

                            data =
                                Uri.parse(
                                    "mailto:ehealthy.enquiries@gmail.com"
                                )

                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "EHealthy App Problem Report"
                            )

                            putExtra(
                                Intent.EXTRA_TEXT,
                                """
                        Hello EHealthy Support,

                        I would like to report a problem with the EHealthy app.

                        Problem:
                        
                        
                        What I was trying to do:
                        
                        
                        What happened:
                        
                        
                        Thank you.
                        """.trimIndent()
                            )
                        }


                    try {

                        context.startActivity(
                            intent
                        )

                    } catch (_: Exception) {

                        Toast.makeText(
                            context,
                            "No email application was found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        composable("doctorLogin") {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            DoctorLogin(
                email = email,
                password = password,
                onEmailChange = { email = it },
                onPasswordChange = { password = it },
                isLoading = isLoading,
                errorMessage = errorMessage,
                onLogin = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val result = signInDoctorWithEmail(email, password)
                            result.onSuccess {
                                val userId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id
                                val doctorRow = userId?.let {
                                    SupabaseClientProvider.client.postgrest.from("doctors")
                                        .select(columns = Columns.list("id")) { filter { eq("user_id", it) } }
                                        .decodeSingleOrNull<Map<String, String?>>()
                                }
                                if (doctorRow == null) {
                                    navController.navigate("doctorRegister")
                                } else {
                                    navController.navigate("doctorDashboard")
                                }
                            }.onFailure { error ->
                                errorMessage = error.message
                            }
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e
                        } catch (_: Exception) { errorMessage = "Could not load your doctor profile. Please retry." }
                        isLoading = false
                    }
                },
                onForgotPassword = { navController.navigate("forgotPassword") },

                onRegister = { navController.navigate("doctorRegister") },
                onContinueWithGoogle = { errorMessage = "Google sign-in is unavailable. Please use email." }
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
                    } catch (_: Exception) {
                        Log.e("DoctorDashboard", "Failed to fetch doctor profile")
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
                        } catch (_: Exception) {
                            Log.e("PhotoUpload", "Upload failed")
                        } finally {
                            isUploadingPhoto = false
                        }
                    }
                }
            }

            DoctorDashboard(
                onNavigateAvailability = { navController.navigate("doctorAvailability") },
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
                        ehealthy.connect.consultation.ConsultationDevices.signOut()
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
                        Result.failure(Exception(friendlyAuthError(e)))
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
                        Result.failure(Exception(friendlyAuthError(e)))
                    }
                },
                onNavigatePrescriptions = { navController.navigate("doctorPrescriptions") },
                onNavigateSettings = { navController.navigate("doctorSettings") },
                onNavigateTimeSlots = { navController.navigate("doctorTimeSlots") },
                onStartCall = { appointmentId -> navController.navigate("doctorCall/$appointmentId") },
                onVerifyCompletionCode = { appointmentId, enteredCode ->
                    try {
                        val row = SupabaseClientProvider.client.postgrest
                            .from("appointments")
                            .select(columns = Columns.list("completion_code")) {
                                filter { eq("id", appointmentId) }
                            }
                            .decodeSingleOrNull<Map<String, String?>>()
                        val realCode = row?.get("completion_code")

                        if (realCode.isNullOrBlank()) {
                            Result.failure(Exception("The patient hasn't requested a completion code yet."))
                        } else if (realCode != enteredCode) {
                            Result.failure(Exception("Incorrect code — please try again."))
                        } else {
                            SupabaseClientProvider.client.postgrest.from("appointments")
                                .update({
                                    set("status", "completed")
                                    set("funds_released", true)
                                    set("completion_code_verified", true)
                                }) {
                                    filter { eq("id", appointmentId) }
                                }
                            Result.success(Unit)
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            )
        }

        composable(
            "doctorCall/{appointmentId}",
            arguments = listOf(navArgument("appointmentId") { defaultValue = "" })
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""

            ehealthy.connect.ui.call.CallScreen(
                appointmentId = appointmentId,
                displayName = "Doctor",
                isDoctorView = true,
                fetchAppointmentStatus = {
                    try {
                        val row = SupabaseClientProvider.client.postgrest
                            .from("appointments")
                            .select(columns = Columns.list("status", "cancelled_reason")) {
                                filter { eq("id", appointmentId) }
                            }
                            .decodeSingleOrNull<Map<String, String?>>()
                        Result.success(
                            ehealthy.connect.ui.call.CallAppointmentStatus(
                                status = row?.get("status"),
                                cancelledReason = row?.get("cancelled_reason")
                            )
                        )
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                onJoined = {
                    try {
                        SupabaseClientProvider.client.postgrest.from("appointments")
                            .update({ set("call_joined_doctor_at", java.time.Instant.now().toString()) }) {
                                filter { eq("id", appointmentId) }
                            }
                    } catch (_: Exception) {
                        Log.e("CallScreen", "Failed to mark doctor joined")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("doctorPrescriptions") {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var patients by remember {
                mutableStateOf<List<PrescriptionPatient>>(emptyList())
            }

            var refillRequests by remember {
                mutableStateOf<List<RefillRequest>>(emptyList())
            }

            var isLoadingPatients by remember {
                mutableStateOf(true)
            }

            var isLoadingRefills by remember {
                mutableStateOf(true)
            }

            var isSaving by remember {
                mutableStateOf(false)
            }

            var saveError by remember {
                mutableStateOf<String?>(null)
            }

            var doctorId by remember {
                mutableStateOf<String?>(null)
            }


            suspend fun loadRefillRequests(
                docId: String
            ): Result<List<RefillRequest>> {

                return try {

                    val rows =
                        SupabaseClientProvider.client
                            .postgrest
                            .from("prescriptions")
                            .select(
                                columns = Columns.list(
                                    "id",
                                    "patient_id",
                                    "medication",
                                    "refill_requested_at"
                                )
                            ) {

                                filter {

                                    eq(
                                        "doctor_id",
                                        docId
                                    )

                                    eq(
                                        "refill_status",
                                        "requested"
                                    )
                                }
                            }
                            .decodeList<Map<String, String?>>()


                    val patientIds =
                        rows
                            .mapNotNull {
                                it["patient_id"]
                            }
                            .distinct()


                    val patientNames =
                        if (
                            patientIds.isEmpty()
                        ) {

                            emptyMap()

                        } else {

                            SupabaseClientProvider.client
                                .postgrest
                                .from("patients")
                                .select(
                                    columns = Columns.list(
                                        "id",
                                        "name",
                                        "surname"
                                    )
                                ) {

                                    filter {

                                        isIn(
                                            "id",
                                            patientIds
                                        )
                                    }
                                }
                                .decodeList<Map<String, String?>>()
                                .associate { patient ->

                                    val id =
                                        patient["id"]
                                            ?: ""

                                    val name =
                                        patient["name"]
                                            ?: ""

                                    val surname =
                                        patient["surname"]
                                            ?: ""

                                    id to
                                            "$name $surname"
                                                .trim()
                                }
                        }


                    val requests =
                        rows
                            .mapNotNull { row ->

                                val id =
                                    row["id"]
                                        ?: return@mapNotNull null

                                val patientId =
                                    row["patient_id"]
                                        ?: return@mapNotNull null

                                val medication =
                                    row["medication"]
                                        ?: "Medication"


                                RefillRequest(
                                    id = id,
                                    patientId = patientId,
                                    patientName =
                                        patientNames[
                                            patientId
                                        ]
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: "Patient",
                                    medication = medication,
                                    requestedAt =
                                        row[
                                            "refill_requested_at"
                                        ]
                                )
                            }
                            .sortedByDescending {
                                it.requestedAt
                                    ?: ""
                            }


                    Result.success(
                        requests
                    )

                } catch (
                    e: Exception
                ) {

                    Result.failure(
                        e
                    )
                }
            }


            LaunchedEffect(Unit) {

                try {

                    val userId =
                        SupabaseClientProvider.client
                            .auth
                            .currentUserOrNull()
                            ?.id


                    if (
                        userId == null
                    ) {

                        saveError =
                            "Doctor account is not signed in."

                        return@LaunchedEffect
                    }


                    val row =
                        SupabaseClientProvider.client
                            .postgrest
                            .from("doctors")
                            .select(
                                columns =
                                    Columns.list(
                                        "id"
                                    )
                            ) {

                                filter {

                                    eq(
                                        "user_id",
                                        userId
                                    )
                                }
                            }
                            .decodeSingleOrNull<
                                    Map<String, String?>
                                    >()


                    val id =
                        row?.get(
                            "id"
                        )


                    if (
                        id == null
                    ) {

                        saveError =
                            "Doctor profile could not be found."

                        return@LaunchedEffect
                    }


                    doctorId =
                        id


                    fetchConfirmedPrescriptionPatients(
                        id
                    )
                        .onSuccess {

                            patients =
                                it
                        }
                        .onFailure {

                            saveError =
                                it.message
                                    ?: "Could not load patients."
                        }


                    loadRefillRequests(
                        id
                    )
                        .onSuccess {

                            refillRequests =
                                it
                        }
                        .onFailure {

                            saveError =
                                it.message
                                    ?: "Could not load refill requests."
                        }


                } catch (
                    e: Exception
                ) {

                    saveError =
                        e.message
                            ?: "Could not load prescriptions."

                } finally {

                    isLoadingPatients =
                        false

                    isLoadingRefills =
                        false
                }
            }


            DoctorPrescriptions(

                patients =
                    patients,

                refillRequests =
                    refillRequests,

                isLoadingPatients =
                    isLoadingPatients,

                isLoadingRefills =
                    isLoadingRefills,

                isSaving =
                    isSaving,

                saveError =
                    saveError,

                onBack = {

                    navController
                        .popBackStack()
                },


                /*
                 * Create a normal new prescription
                 */
                onSave = {
                        patientId,
                        medications ->

                    val docId =
                        doctorId
                            ?: return@DoctorPrescriptions


                    scope.launch {

                        isSaving =
                            true

                        saveError =
                            null


                        savePrescription(
                            patientId,
                            docId,
                            medications
                        )
                            .onSuccess {

                                Toast.makeText(
                                    context,
                                    "Prescription saved successfully",
                                    Toast.LENGTH_LONG
                                ).show()

                                navController
                                    .popBackStack()
                            }
                            .onFailure {

                                saveError =
                                    it.message
                                        ?: "Could not save prescription."
                            }


                        isSaving =
                            false
                    }
                },


                /*
                 * Approve patient refill request
                 */
                onApproveRefill = { request ->

                    scope.launch {

                        try {

                            saveError = null

                            SupabaseClientProvider.client
                                .postgrest
                                .rpc(
                                    "doctor_update_refill_status",
                                    buildJsonObject {
                                        put(
                                            "p_prescription_id",
                                            JsonPrimitive(request.id)
                                        )

                                        put(
                                            "p_status",
                                            JsonPrimitive("approved")
                                        )
                                    }
                                )

                            refillRequests =
                                refillRequests.filterNot {
                                    it.id == request.id
                                }

                            Toast.makeText(
                                context,
                                "Refill approved",
                                Toast.LENGTH_SHORT
                            ).show()

                        } catch (e: Exception) {

                            Log.e(
                                "RefillApproval",
                                "Could not approve refill",
                                e
                            )

                            saveError =
                                e.message
                                    ?: "Could not approve refill."
                        }
                    }
                },


                /*
                 * Decline patient refill request
                 */
                onDeclineRefill = { request ->

                    scope.launch {

                        try {

                            saveError = null

                            SupabaseClientProvider.client
                                .postgrest
                                .rpc(
                                    "doctor_update_refill_status",
                                    buildJsonObject {
                                        put(
                                            "p_prescription_id",
                                            JsonPrimitive(request.id)
                                        )

                                        put(
                                            "p_status",
                                            JsonPrimitive("declined")
                                        )
                                    }
                                )

                            refillRequests =
                                refillRequests.filterNot {
                                    it.id == request.id
                                }

                            Toast.makeText(
                                context,
                                "Refill declined",
                                Toast.LENGTH_SHORT
                            ).show()

                        } catch (e: Exception) {

                            Log.e(
                                "RefillDecline",
                                "Could not decline refill",
                                e
                            )

                            saveError =
                                e.message
                                    ?: "Could not decline refill."
                        }
                    }
                }
            )
        }

        composable("doctorTimeSlots") {
            var doctorId by remember { mutableStateOf<String?>(null) }
            var identityError by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                try { doctorId = ProfileRepository.doctorId() }
                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) { identityError = true }
            }
            if (doctorId == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (identityError) "Doctor profile could not be loaded. Go back and retry." else "Loading doctor profile…")
                }
                return@composable
            }
            DoctorTimeSlots(
                doctorId = doctorId,
                onBack = { navController.popBackStack() },
                fetchSlots = { id, date -> fetchDoctorTimeSlots(id, date) },
                saveSlots = { id, date, openOrBookedRows, closedTimes ->
                    saveDoctorTimeSlots(id, date, openOrBookedRows, closedTimes)
                }
            )
        }

        composable("doctorSettings") {
            val scope = rememberCoroutineScope()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* granted or denied — token fetch below runs either way */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                try {
                    ehealthy.connect.consultation.ConsultationDevices.sync()
                } catch (_: Exception) {
                    Log.e("FCM", "Failed to get/save doctor token")
                }
            }

            DoctorSettingsScreen(
                isDarkMode = ThemeManager.isDarkMode,
                onToggleDarkMode = { ThemeManager.updateDarkMode(it) },
                onBack = { navController.popBackStack() },
                onViewPrivacyPolicy = { navController.navigate("privacyPolicy") },
                onLogout = {
                    scope.launch {
                        ehealthy.connect.consultation.ConsultationDevices.signOut()
                        navController.navigate("choose") { popUpTo(0) { inclusive = true } }
                    }
                },
                fetchSettings = {
                    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val row = SupabaseClientProvider.client.postgrest
                                .from("doctors")
                                .select(columns = Columns.list("email_notifications", "sms_notifications", "profile_visible")) {
                                    filter { eq("user_id", userId) }
                                }
                                .decodeSingleOrNull<DoctorAccountSettings>()
                            Result.success(row ?: DoctorAccountSettings())
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },
                onUpdateSetting = { newSettings ->
                    scope.launch {
                        val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id ?: return@launch
                        try {
                            SupabaseClientProvider.client.postgrest.from("doctors").update(
                                mapOf(
                                    "email_notifications" to newSettings.email_notifications,
                                    "sms_notifications" to newSettings.sms_notifications,
                                    "profile_visible" to newSettings.profile_visible
                                )
                            ) { filter { eq("user_id", userId) } }
                        } catch (_: Exception) {
                            Log.e("DoctorSettingsUpdate", "Failed to save setting")
                        }
                    }
                }
            )
        }
        composable("patientNotifications") {

            PatientNotificationsScreen(
                onBack = {
                    navController.popBackStack()
                },
                onOpenInvoice = { invoiceId ->

                    navController.navigate(
                        "patientInvoices?invoiceId=${
                            Uri.encode(
                                invoiceId
                            )
                        }"
                    )
                }
            )
        }


        composable(
            route =
                "patientInvoices?invoiceId={invoiceId}",
            arguments =
                listOf(
                    navArgument(
                        "invoiceId"
                    ) {
                        type =
                            NavType.StringType

                        defaultValue =
                            ""
                    }
                )
        ) { backStackEntry ->

            val initialInvoiceId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "invoiceId"
                    )
                    ?.takeIf {
                        it.isNotBlank()
                    }

            PatientInvoicesScreen(
                initialInvoiceId =
                    initialInvoiceId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("patientDashboard") {
            val scope = rememberCoroutineScope()
            val appContext = LocalContext.current
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(
                    contract =
                        ActivityResultContracts.RequestPermission()
                ) {
                    // Token registration still runs whether
                    // notification permission is granted or denied.
                }

            LaunchedEffect(Unit) {

                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {

                    notificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                }

                try {

                    ehealthy.connect.consultation
                        .ConsultationDevices
                        .sync()

                } catch (_: Exception) {

                    Log.e(
                        "PatientFCM",
                        "Could not register consultation device token"
                    )
                }
            }
            var patientName by remember { mutableStateOf("Patient") }
            var patientAvatarUrl by remember { mutableStateOf<String?>(null) }
            var isUploadingPhoto by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                if (userId != null) {
                    try {
                        val result = SupabaseClientProvider.client.postgrest
                            .from("patients")
                            .select(columns = Columns.list("name", "surname", "profile_image_url")) {
                                filter { eq("user_id", userId) }
                            }
                            .decodeSingleOrNull<PatientLookup>()
                        if (result?.name != null) patientName = "${result.name} ${result.surname ?: ""}".trim()
                        patientAvatarUrl = result?.profile_image_url?.let { "$it?t=${System.currentTimeMillis()}" }
                    } catch (_: Exception) {}
                }
            }

            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        isUploadingPhoto = true
                        try {

                            SupabaseClientProvider.client.auth.awaitInitialization()

                            val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                                ?: throw Exception("Not logged in")

                            val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
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
                            patientAvatarUrl = "$publicUrl?t=${System.currentTimeMillis()}"
                            Toast.makeText(appContext, "Profile photo updated", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("PhotoUpload", "Upload failed")
                            Toast.makeText(appContext, "Photo upload failed: ${e.message ?: "Please try again"}", Toast.LENGTH_LONG).show()
                        } finally {
                            isUploadingPhoto = false
                        }
                    }
                }
            }

            PatientDashboard(
                onStartCall = { appointmentId ->
                    navController.navigate("call/$appointmentId")
                },
                patientName = patientName,
                patientAvatarUrl = patientAvatarUrl,
                isUploadingPhoto = isUploadingPhoto,
                onNavigateSettings = {
                    navController.navigate("settings")
                },
                onNavigateNotifications = {
                    navController.navigate("patientNotifications")
                },

                onNavigateFindDoctors = {
                    navController.navigate("findDoctors")
                },

                onNavigateAppointments = {
                    navController.navigate("patientAppointments")
                },

                onNavigateMedicalRecords = {
                    navController.navigate("medicalRecords")
                },

                onNavigatePrescriptions = {
                    navController.navigate("patientPrescriptions")
                },

                onNavigateConsultations = {
                    navController.navigate("consultations")
                },

                onNavigateHealthTips = {
                    navController.navigate("healthTips")
                },
                onUploadPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onLogout = {
                    scope.launch {
                        ehealthy.connect.consultation.ConsultationDevices.signOut()
                        navController.navigate("patientLogin") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                fetchAppointments = {
                    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val data = SupabaseClientProvider.client.postgrest
                                .from("appointments")
                                .select { filter { eq("patient_id", ProfileRepository.patientId()) } }
                                .decodeList<Appointment>()
                            Result.success(data)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },

                onRescheduleAppointment = { appointmentId ->
                    navController.navigate("rescheduleAppointment/$appointmentId")
                },
                cancelAppointment = { appointmentId ->

                    PatientRepository
                        .cancelAppointment(
                            appointmentId = appointmentId,
                            reason = "Cancelled by patient"
                        )
                        .map {
                            Unit
                        }
                },

                fetchReviews = {
                    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val reviewList = SupabaseClientProvider.client.postgrest
                                .from("reviews")
                                .select { filter { eq("patient_id", ProfileRepository.patientId()) } }
                                .decodeList<Review>()

                            val doctorIds = reviewList.map { it.doctor_id }.distinct()
                            val doctorNames: Map<String, String> = if (doctorIds.isEmpty()) {
                                emptyMap()
                            } else {
                                SupabaseClientProvider.client.postgrest
                                    .from("doctors")
                                    .select { filter { isIn("id", doctorIds) } }
                                    .decodeList<DoctorListing>().filter { it.is_deactivated != true }
                                    .associate { doc -> doc.id to "Dr. ${doc.name} ${doc.surname}".trim() }
                            }

                            val display = reviewList
                                .sortedByDescending { it.created_at }
                                .map { ReviewDisplay(review = it, doctorName = doctorNames[it.doctor_id] ?: "Unknown Doctor") }

                            Result.success(display)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                }

            )
        }
        composable(
            "call/{appointmentId}",
            arguments = listOf(navArgument("appointmentId") { defaultValue = "" })
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""

            ehealthy.connect.ui.call.CallScreen(
                appointmentId = appointmentId,
                displayName = "Patient",
                fetchAppointmentStatus = {
                    try {
                        val row = SupabaseClientProvider.client.postgrest
                            .from("appointments")
                            .select(columns = Columns.list("status", "cancelled_reason")) {
                                filter { eq("id", appointmentId) }
                            }
                            .decodeSingleOrNull<Map<String, String?>>()
                        Result.success(
                            ehealthy.connect.ui.call.CallAppointmentStatus(
                                status = row?.get("status"),
                                cancelledReason = row?.get("cancelled_reason")
                            )
                        )
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                onJoined = {
                    try {
                        SupabaseClientProvider.client.postgrest.from("appointments")
                            .update({ set("call_joined_patient_at", java.time.Instant.now().toString()) }) {
                                filter { eq("id", appointmentId) }
                            }
                    } catch (e: Exception) {
                        Log.e("CallScreen", "Failed to mark patient joined")
                    }
                },
                onBack = { navController.popBackStack() },
                onReschedule = {
                    navController.navigate("rescheduleAppointment/$appointmentId") {
                        popUpTo("patientDashboard") { inclusive = false }
                    }
                }
            )
        }

        composable("consultations") {

            val context =
                LocalContext.current

            OnDemandConsultationScreen(

                initialDoctorId =
                    null,

                onBack = {
                    navController.popBackStack()
                },

                onChooseDoctor = {

                    navController.navigate(
                        "findDoctors"
                    )
                },

                onConsultationReady = {
                        requestId,
                        claimedDoctorId ->

                    Log.d(
                        "Consultation",
                        "Claimed request=$requestId doctor=$claimedDoctorId"
                    )

                    navController.navigate(
                        "consultationCall/$requestId/patient"
                    ) {

                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route =
                "privateConsultation/{doctorId}",
            arguments =
                listOf(
                    navArgument(
                        "doctorId"
                    ) {
                        type =
                            NavType.StringType
                    }
                )
        ) { entry ->

            val context =
                LocalContext.current

            val doctorId =
                entry.arguments
                    ?.getString(
                        "doctorId"
                    )
                    .orEmpty()

            OnDemandConsultationScreen(

                initialDoctorId =
                    doctorId,

                onBack = {
                    navController.popBackStack()
                },

                onChooseDoctor = {

                    navController.navigate(
                        "findDoctors"
                    )
                },

                onConsultationReady = { requestId, claimedDoctorId ->

                    Log.d(
                        "Consultation",
                        "Claimed request=$requestId doctor=$claimedDoctorId"
                    )

                    navController.navigate(
                        "consultationCall/$requestId/patient"
                    ) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("doctorAvailability") {
            ehealthy.connect.consultation.DoctorAvailabilityScreen(onBack = { navController.popBackStack() })
        }
        composable("findDoctors") {
            FindDoctorsScreen(
                onBack = { navController.popBackStack() },
                onSelectDoctor = { doc ->
                    navController.navigate("bookAppointment/${doc.id}")
                },
                onViewProfile = { doc ->
                    navController.navigate("doctorProfile/${doc.id}")
                },
                fetchDoctors = {

                    try {

                        val data =
                            SupabaseClientProvider.client
                                .postgrest
                                .from("doctors")
                                .select {

                                    filter {

                                        eq(
                                            "verification_status",
                                            "approved"
                                        )

                                        eq(
                                            "is_deactivated",
                                            false
                                        )
                                    }
                                }
                                .decodeList<DoctorListing>()

                        Result.success(data)

                    } catch (e: Exception) {

                        Result.failure(e)
                    }
                }
            )
        }

        composable(
            "doctorProfile/{doctorId}",
            arguments = listOf(
                navArgument("doctorId") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val doctorId =
                backStackEntry
                    .arguments
                    ?.getString("doctorId")
                    ?: ""

            DoctorProfileScreen(
                onRequestPrivate = { id -> navController.navigate("privateConsultation/$id") },

                onBack = {
                    navController.popBackStack()
                },

                onBookAppointment = { id ->

                    navController.navigate(
                        "bookAppointment/$id"
                    )
                },

                fetchDoctor = {

                    try {

                        val doc =
                            SupabaseClientProvider.client
                                .postgrest
                                .from("doctors")
                                .select {

                                    filter {
                                        eq(
                                            "id",
                                            doctorId
                                        )
                                    }
                                }
                                .decodeSingleOrNull<PatientDoctorProfile>()

                        if (doc == null) {

                            Result.failure(
                                Exception(
                                    "Doctor not found."
                                )
                            )

                        } else {

                            Result.success(doc)
                        }

                    } catch (e: Exception) {

                        Result.failure(e)
                    }
                },

                fetchReviews = {

                    try {

                        val items =
                            SupabaseClientProvider.client
                                .postgrest
                                .from("reviews")
                                .select {

                                    filter {
                                        eq(
                                            "doctor_id",
                                            doctorId
                                        )
                                    }
                                }
                                .decodeList<Review>()
                                .sortedByDescending {
                                    it.created_at
                                }
                                .map {

                                    DoctorReviewItem(

                                        rating =
                                            it.rating ?: 0,

                                        comment =
                                            it.comment,

                                        patientName =
                                            null,

                                        createdAt =
                                            it.created_at
                                    )
                                }

                        Result.success(items)

                    } catch (e: Exception) {

                        Result.failure(e)
                    }
                },

                // Message icon destination
                onMessageDoctor = { doctorId ->
                    navController.navigate("messages/$doctorId")
                }
            )
        }


        composable(
            route = "messages/{doctorId}",
            arguments = listOf(
                navArgument("doctorId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""

            MessagingScreen(
                doctorId = doctorId,
                onBack = { navController.popBackStack() }
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
                    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) {
                        Result.failure(Exception("Please log in to view your records."))
                    } else {
                        try {
                            val patientId = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select(columns = Columns.list("id")) {
                                    filter { eq("user_id", userId) }
                                }
                                .decodeSingleOrNull<PatientLookup>()
                                ?.id
                                ?: throw IllegalStateException("Patient profile is missing or inaccessible.")

                            val records = SupabaseClientProvider.client.postgrest
                                .from("medical_records")
                                .select { filter { eq("patient_id", patientId) } }
                                .decodeList<MedicalRecord>()

                            val prescriptions = SupabaseClientProvider.client.postgrest
                                .from("prescriptions")
                                .select { filter { eq("patient_id", patientId) } }
                                .decodeList<Prescription>()

                            val doctorIds = (records.mapNotNull { it.doctor_id } + prescriptions.mapNotNull { it.doctor_id }).distinct()
                            val doctorNames: Map<String, String>
                            val doctorPhotos: Map<String, String>
                            if (doctorIds.isEmpty()) {
                                doctorNames = emptyMap()
                                doctorPhotos = emptyMap()
                            } else {
                                val doctors = SupabaseClientProvider.client.postgrest
                                    .from("doctors")
                                    .select { filter { isIn("id", doctorIds) } }
                                    .decodeList<DoctorListing>().filter { it.is_deactivated != true }
                                doctorNames = doctors.associate { doc -> doc.id to "Dr. ${doc.name} ${doc.surname}".trim() }
                                doctorPhotos = doctors.mapNotNull { doc ->
                                    doc.profile_image_url?.takeIf { it.isNotBlank() }?.let { doc.id to it }
                                }.toMap()
                            }

                            val recordDisplays = records.map { record ->
                                MedicalRecordDisplay(
                                    record = record,
                                    doctorName = doctorNames[record.doctor_id] ?: "Unknown Doctor",
                                    doctorPhoto = record.doctor_id?.let { doctorPhotos[it] }
                                )
                            }

                            val prescriptionDisplays = prescriptions.map { rx ->
                                MedicalRecordDisplay(
                                    record = MedicalRecord(
                                        id = "rx-${rx.id}",
                                        record_type = "prescription",
                                        medications = listOfNotNull(rx.medication),
                                        notes = if (rx.refill_status == "requested") "Refill requested" else null,
                                        created_at = rx.created_at ?: "",
                                        doctor_id = rx.doctor_id
                                    ),
                                    doctorName = doctorNames[rx.doctor_id] ?: "Unknown Doctor",
                                    doctorPhoto = rx.doctor_id?.let { doctorPhotos[it] }
                                )
                            }

                            val display = (recordDisplays + prescriptionDisplays)
                                .sortedByDescending { it.record.created_at }

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
                            .select(
                                columns = Columns.list(
                                    "name",
                                    "surname",
                                    "discipline",
                                    "hourly_rate",
                                    "operating_hours"
                                )
                            ) {
                                filter {
                                    eq("id", doctorId)
                                    eq("verification_status", "approved")
                                    eq("is_deactivated", false)
                                }
                            }
                            .decodeSingleOrNull<DoctorLookup>()

                        if (doc == null) {
                            Result.failure(
                                Exception(
                                    "This doctor is currently unavailable for appointments."
                                )
                            )
                        } else {
                            Result.success(
                                DoctorBookingInfo(
                                    id = doctorId,
                                    name = doc.name ?: "",
                                    surname = doc.surname ?: "",
                                    discipline = doc.discipline,
                                    hourlyRate = doc.hourly_rate,
                                    operatingHours = doc.operating_hours
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                },
                fetchAvailableTimes = { date -> BookingRepository.availableTimes(doctorId, date) },
                onConfirmBooking = { _ -> errorMessage = BookingRepository.BACKEND_REQUIRED }
            )
        }
        composable("patientPrescriptions") {
            var prescriptionLoadError by remember { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()
            var prescriptions by remember { mutableStateOf<List<Prescription>>(emptyList()) }
            var doctorNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var doctorPhotos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                try {
                    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) throw IllegalStateException("Not logged in.")
                    val patientId = SupabaseClientProvider.client.postgrest.from("patients")
                        .select(columns = Columns.list("id")) { filter { eq("user_id", userId) } }
                        .decodeSingleOrNull<Map<String, String?>>()?.get("id") ?: throw IllegalStateException("Patient profile is missing or inaccessible.")
                    val fetchedPrescriptions = SupabaseClientProvider.client.postgrest.from("prescriptions")
                        .select { filter { eq("patient_id", patientId) } }
                        .decodeList<Prescription>()
                    prescriptions = fetchedPrescriptions

                    val doctorIds = fetchedPrescriptions.mapNotNull { it.doctor_id }.distinct()
                    if (doctorIds.isNotEmpty()) {
                        val doctors = SupabaseClientProvider.client.postgrest.from("doctors")
                            .select(columns = Columns.list("id", "name", "surname", "profile_image_url")) {
                                filter { isIn("id", doctorIds) }
                            }
                            .decodeList<Map<String, String?>>()
                        doctorNames = doctors.associate { doc ->
                            (doc["id"] ?: "") to "Dr. ${doc["name"] ?: ""} ${doc["surname"] ?: ""}".trim()
                        }
                        doctorPhotos = doctors.mapNotNull { doc ->
                            val id = doc["id"]
                            val photo = doc["profile_image_url"]
                            if (id != null && !photo.isNullOrBlank()) id to photo else null
                        }.toMap()
                    }
                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                } catch (_: Exception) {
                    prescriptionLoadError = "Could not load prescriptions. Go back and retry."
                } finally {
                    isLoading = false
                }
            }
            if (prescriptionLoadError != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.TextButton(onClick = { navController.popBackStack() }) {
                        Text(prescriptionLoadError!!)
                    }
                }
                return@composable
            }
            PatientPrescriptionsScreen(
                prescriptions = prescriptions,
                isLoading = isLoading,
                onBack = { navController.popBackStack() },
                doctorNames = doctorNames,
                doctorPhotos = doctorPhotos,
                onRequestRefill = { prescription ->
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.postgrest.from("prescriptions")
                                .update({
                                    set("refill_status", "requested")
                                    set("refill_requested_at", java.time.Instant.now().toString())
                                }) {
                                    filter { eq("id", prescription.id) }
                                }
                            prescriptions = prescriptions.map {
                                if (it.id == prescription.id) it.copy(refill_status = "requested") else it
                            }
                        } catch (_: Exception) {
                            Log.e("RefillRequest", "Failed to request refill")
                        }
                    }
                }
            )
        }
        composable(
            "patientAppointments"
        ) {

            PatientVisitsScreen(

                onBack = {
                    navController.popBackStack()
                },

                onOpenInvoices = {
                    navController.navigate(
                        "patientInvoices"
                    )
                },

                onReschedule = {
                        appointmentId ->

                    navController.navigate(
                        "rescheduleAppointment/$appointmentId"
                    )
                },

                onMessageDoctor = {
                        doctorId ->

                    navController.navigate(
                        "messages/$doctorId"
                    )
                },

                onJoinScheduledCall = {
                        appointment ->

                    navController.navigate(
                        "call/${appointment.id}"
                    )
                },

                onRateAppointment = {
                        appointment ->

                    navController.navigate(
                        "rateVisit/${appointment.id}"
                    )
                }
            )
        }
        composable(
            route =
                "rateVisit/{appointmentId}",
            arguments =
                listOf(
                    navArgument(
                        "appointmentId"
                    ) {
                        type =
                            NavType.StringType
                    }
                )
        ) { backStackEntry ->

            val appointmentId =
                backStackEntry
                    .arguments
                    ?.getString(
                        "appointmentId"
                    )
                    .orEmpty()

            var appointment by remember {
                mutableStateOf<PatientAppointment?>(
                    null
                )
            }

            var isLoading by remember {
                mutableStateOf(true)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }

            LaunchedEffect(
                appointmentId
            ) {

                PatientRepository
                    .getMyAppointments()
                    .onSuccess { appointments ->

                        appointment =
                            appointments
                                .firstOrNull {
                                    it.id ==
                                            appointmentId
                                }

                        if (
                            appointment == null
                        ) {

                            errorMessage =
                                "Appointment not found."
                        }
                    }
                    .onFailure { error ->

                        errorMessage =
                            error.message
                                ?: "Could not load appointment."
                    }

                isLoading =
                    false
            }

            when {

                isLoading -> {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }
                }

                appointment == null -> {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                errorMessage
                                    ?: "Appointment unavailable."
                            )

                            androidx.compose.material3
                                .TextButton(
                                    onClick = {
                                        navController
                                            .popBackStack()
                                    }
                                ) {

                                    Text(
                                        "Back"
                                    )
                                }
                        }
                    }
                }

                else -> {

                    RateVisitScreen(

                        appointment =
                            appointment!!,

                        onBack = {
                            navController
                                .popBackStack()
                        },

                        onSubmitted = {

                            navController.navigate(
                                "patientAppointments"
                            ) {

                                popUpTo(
                                    "patientAppointments"
                                ) {
                                    inclusive =
                                        true
                                }
                            }
                        }
                    )
                }
            }
        }
        composable(
            "rescheduleAppointment/{appointmentId}",
            arguments = listOf(navArgument("appointmentId") { defaultValue = "" })
        ) { backStackEntry ->
            val appointmentId = backStackEntry.arguments?.getString("appointmentId") ?: ""
            val scope = rememberCoroutineScope()
            var appointment by remember { mutableStateOf<Appointment?>(null) }
            var isLoadingAppt by remember { mutableStateOf(true) }
            var isSubmitting by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(appointmentId) {
                try {
                    appointment = SupabaseClientProvider.client.postgrest
                        .from("appointments")
                        .select { filter { eq("id", appointmentId) } }
                        .decodeSingleOrNull<Appointment>()
                } catch (e: Exception) {
                    errorMessage = "Could not load appointment: ${e.message}"
                }
                isLoadingAppt = false
            }

            when {
                isLoadingAppt -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                appointment == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(errorMessage ?: "Appointment not found.")
                    }
                }
                else -> {
                    val doctorIdForFetch = appointment!!.doctor_id

                    RescheduleAppointmentScreen(
                        appointment = appointment!!,
                        isLoading = isSubmitting,
                        errorMessage = errorMessage,
                        onBack = { navController.popBackStack() },
                        fetchAvailableTimes = { date ->
                            doctorIdForFetch?.let { BookingRepository.availableTimes(it, date) }
                                ?: Result.failure(IllegalStateException("Doctor not found."))
                        },
                        onConfirmReschedule = { _, _, _ ->
                            errorMessage = "Rescheduling requires secure server support. Your existing appointment has not changed."
                        }
                    )
                }
            }
        }

// ⚠️ PLACEHOLDER — replace with your real BookingConfirmedScreen once you share it.
// This just stops the app from crashing when a booking succeeds.
        composable("bookingConfirmed") {
            val confirmation = BookingConfirmationHolder.confirmation

            if (confirmation == null) {
                // Shouldn't normally happen (only reachable via the booking flow),
                // but guards against a direct/deep-link navigation with nothing to show.
                LaunchedEffect(Unit) {
                    navController.navigate("patientDashboard") {
                        popUpTo("patientDashboard") { inclusive = true }
                    }
                }
            } else {
                BookingConfirmedScreen(
                    confirmation = confirmation,
                    onGoToDashboard = {
                        BookingConfirmationHolder.confirmation = null
                        navController.navigate("patientDashboard") {
                            popUpTo("patientDashboard") { inclusive = true }
                        }
                    },
                    onViewAppointments = {
                        BookingConfirmationHolder.confirmation = null
                        navController.navigate("patientAppointments") {
                            popUpTo("patientDashboard") { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}
