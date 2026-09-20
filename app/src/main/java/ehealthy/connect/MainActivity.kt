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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ehealthy.connect.ui.common.PrivacyPolicyScreen
import ehealthy.connect.ui.common.isPasswordStrong
import ehealthy.connect.ui.doctor.DoctorDashboard
import ehealthy.connect.ui.doctor.DoctorForgotPassword
import ehealthy.connect.ui.doctor.DoctorLogin
import ehealthy.connect.ui.doctor.DoctorPrescriptions
import ehealthy.connect.ui.doctor.DoctorProfile
import ehealthy.connect.ui.doctor.DoctorProfileRow
import ehealthy.connect.ui.doctor.DoctorRegister
import ehealthy.connect.ui.doctor.DoctorRegistrationFiles
import ehealthy.connect.ui.doctor.DoctorResetPassword
import ehealthy.connect.ui.doctor.PrescriptionPatient
import ehealthy.connect.ui.doctor.fetchConfirmedPrescriptionPatients
import ehealthy.connect.ui.doctor.registerDoctor
import ehealthy.connect.ui.doctor.savePrescription
import ehealthy.connect.ui.doctor.sendDoctorPasswordResetEmail
import ehealthy.connect.ui.doctor.signInDoctorWithEmail
import ehealthy.connect.ui.doctor.toDoctorProfile
import ehealthy.connect.ui.doctor.uploadDoctorProfilePhoto
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
import ehealthy.connect.ui.patientDashboard.AppointmentType
import ehealthy.connect.ui.patientDashboard.BookAppointmentScreen
import ehealthy.connect.ui.patientDashboard.DoctorBookingInfo
import ehealthy.connect.ui.patientDashboard.DoctorListing
import ehealthy.connect.ui.patientDashboard.DoctorProfileScreen
import ehealthy.connect.ui.patientDashboard.DoctorReviewItem
import ehealthy.connect.ui.patientDashboard.FindDoctorsScreen
import ehealthy.connect.ui.patientDashboard.HealthTipsScreen
import ehealthy.connect.ui.patientDashboard.MedicalRecord
import ehealthy.connect.ui.patientDashboard.MedicalRecordDisplay
import ehealthy.connect.ui.patientDashboard.MedicalRecordsScreen
import ehealthy.connect.ui.patientDashboard.MessagingScreen
import ehealthy.connect.ui.patientDashboard.PatientAppointmentsScreen
import ehealthy.connect.ui.patientDashboard.PatientDashboard
import ehealthy.connect.ui.patientDashboard.PatientSettings
import ehealthy.connect.ui.patientDashboard.RescheduleAppointmentScreen
import ehealthy.connect.ui.patientDashboard.Review
import ehealthy.connect.ui.patientDashboard.ReviewDisplay
import ehealthy.connect.ui.patientDashboard.SettingsScreen
import ehealthy.connect.ui.splash.SplashScreen
import ehealthy.connect.ui.theme.EHealthyTheme
import ehealthy.connect.util.SupabaseClientProvider
import ehealthy.connect.util.ThemeManager
import ehealthy.connect.util.signInWithGoogle
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ehealthy.connect.ui.patientDashboard.DoctorProfile as PatientDoctorProfile
import ehealthy.connect.ui.patientDashboard.Prescription
import ehealthy.connect.ui.doctor.DoctorAccountSettings
import ehealthy.connect.ui.doctor.DoctorSettingsScreen
import ehealthy.connect.ui.doctor.DoctorTimeSlots
import ehealthy.connect.ui.doctor.fetchDoctorTimeSlots
import ehealthy.connect.ui.doctor.saveDoctorTimeSlots
import ehealthy.connect.ui.doctor.friendlyAuthError
import ehealthy.connect.ui.patientDashboard.PatientPrescriptionsScreen
import ehealthy.connect.ui.patientDashboard.PrescriptionDisplay



@Serializable
private data class PatientLookup(
    val id: String? = null,
    val name: String? = null,
    val surname: String? = null,

    @SerialName("profile_image_url")
    val profileImageUrl: String? = null
)

@Serializable
private data class DoctorLookup(
    val name: String? = null,
    val surname: String? = null,
    val discipline: String? = null,

    @SerialName("hourly_rate")
    val hourlyRate: Double? = null,

    @SerialName("operating_hours")
    val operatingHours: String? = null
)


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager.init(this)

        enableEdgeToEdge()

        setContent {
            EHealthyTheme(
                darkTheme = ThemeManager.isDarkMode
            ) {
                AppNavGraph()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavGraph() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // ------------------------------------------------
        // SPLASH
        // ------------------------------------------------

        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("onboarding1") {
                        popUpTo("splash") {
                            inclusive = true
                        }
                    }
                }
            )
        }


        // ------------------------------------------------
        // SETTINGS
        // ------------------------------------------------

        composable("settings") {

            val scope = rememberCoroutineScope()

            SettingsScreen(
                isDarkMode = ThemeManager.isDarkMode,

                onToggleDarkMode = {
                    ThemeManager.updateDarkMode(it)
                },

                onBack = {
                    navController.popBackStack()
                },

                onViewPrivacyPolicy = {
                    navController.navigate("privacyPolicy")
                },

                onLogout = {
                    scope.launch {
                        SupabaseClientProvider.client.auth.signOut()

                        navController.navigate("patientLogin") {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                },

                fetchSettings = {

                    val userId =
                        SupabaseClientProvider.client.auth
                            .currentSessionOrNull()
                            ?.user
                            ?.id

                    if (userId == null) {

                        Result.failure(
                            Exception("Not logged in.")
                        )

                    } else {

                        try {

                            val row =
                                SupabaseClientProvider.client.postgrest
                                    .from("patients")
                                    .select(
                                        columns = Columns.list(
                                            "email_notifications",
                                            "sms_notifications",
                                            "profile_visible"
                                        )
                                    ) {
                                        filter {
                                            eq("user_id", userId)
                                        }
                                    }
                                    .decodeSingleOrNull<PatientSettings>()

                            Result.success(
                                row ?: PatientSettings()
                            )

                        } catch (e: Exception) {

                            Result.failure(e)
                        }
                    }
                },

                onUpdateSetting = { newSettings ->

                    scope.launch {

                        val userId =
                            SupabaseClientProvider.client.auth
                                .currentSessionOrNull()
                                ?.user
                                ?.id
                                ?: return@launch

                        try {

                            SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .update(
                                    mapOf(
                                        "email_notifications" to newSettings.email_notifications,
                                        "sms_notifications" to newSettings.sms_notifications,
                                        "profile_visible" to newSettings.profile_visible
                                    )
                                ) {
                                    filter {
                                        eq("user_id", userId)
                                    }
                                }

                        } catch (e: Exception) {

                            Log.e(
                                "SettingsUpdate",
                                "Failed to save setting",
                                e
                            )
                        }
                    }
                }
            )
        }


        // ------------------------------------------------
        // ONBOARDING
        // ------------------------------------------------

        composable("onboarding1") {

            OnboardingOne(
                onContinue = {
                    navController.navigate("onboarding2")
                },

                onSkip = {
                    navController.navigate("choose")
                }
            )
        }


        composable("onboarding2") {

            OnboardingScreenTwo(
                onContinue = {
                    navController.navigate("onboarding3")
                },

                onSkip = {
                    navController.navigate("choose")
                }
            )
        }


        composable("onboarding3") {

            OnBoardingScreenThree(
                onContinue = {
                    navController.navigate("onboarding4")
                },

                onSkip = {
                    navController.navigate("choose")
                }
            )
        }


        composable("onboarding4") {

            OnboardingFour(
                onGetStarted = {
                    navController.navigate("choose")
                }
            )
        }


        // ------------------------------------------------
        // CHOOSE ROLE
        // ------------------------------------------------

        composable("choose") {

            ChooseRoleScreen(

                onPatientSelected = {
                    navController.navigate("patientLogin")
                },

                onDoctorSelected = {
                    navController.navigate("doctorLogin")
                }
            )
        }


        // ------------------------------------------------
        // PATIENT LOGIN
        // ------------------------------------------------

        composable("patientLogin") {

            val scope = rememberCoroutineScope()

            var mode by remember {
                mutableStateOf(LoginMode.LOGIN)
            }

            var isLoading by remember {
                mutableStateOf(false)
            }

            var email by remember {
                mutableStateOf("")
            }

            var password by remember {
                mutableStateOf("")
            }

            var otp by remember {
                mutableStateOf("")
            }

            var newPassword by remember {
                mutableStateOf("")
            }

            var confirmPassword by remember {
                mutableStateOf("")
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }

            var successMessage by remember {
                mutableStateOf<String?>(null)
            }


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


                onEmailChange = {
                    email = it
                },

                onPasswordChange = {
                    password = it
                },

                onOtpChange = {
                    otp = it
                },

                onNewPasswordChange = {
                    newPassword = it
                },

                onConfirmPasswordChange = {
                    confirmPassword = it
                },


                onLogin = {

                    scope.launch {

                        errorMessage = null
                        successMessage = null
                        isLoading = true

                        try {

                            SupabaseClientProvider.client.auth
                                .signInWith(Email) {

                                    this.email = email
                                    this.password = password
                                }

                            if (
                                SupabaseClientProvider.client.auth
                                    .currentSessionOrNull() == null
                            ) {

                                throw Exception(
                                    "Sign-in did not create a session. Please sign in again."
                                )
                            }

                            isLoading = false

                            navController.navigate(
                                "patientDashboard"
                            ) {
                                popUpTo("patientLogin") {
                                    inclusive = true
                                }
                            }

                        } catch (e: Exception) {

                            isLoading = false

                            Log.e(
                                "LoginDebug",
                                "Login failed",
                                e
                            )

                            errorMessage =
                                e.message ?: "Login failed."
                        }
                    }
                },


                onForgotPasswordClick = {

                    errorMessage = null
                    successMessage = null

                    mode = LoginMode.FORGOT_REQUEST
                },


                onSendResetCode = {

                    scope.launch {

                        errorMessage = null
                        isLoading = true

                        try {

                            SupabaseClientProvider.client.auth
                                .resetPasswordForEmail(email)

                            isLoading = false
                            successMessage = null

                            mode = LoginMode.FORGOT_VERIFY

                        } catch (e: Exception) {

                            isLoading = false

                            errorMessage =
                                "Couldn't send a code to that email: ${e.message}"
                        }
                    }
                },


                onVerifyResetCode = {

                    scope.launch {

                        errorMessage = null
                        isLoading = true

                        try {

                            SupabaseClientProvider.client.auth
                                .verifyEmailOtp(
                                    type = OtpType.Email.RECOVERY,
                                    email = email,
                                    token = otp
                                )

                            isLoading = false

                            mode = LoginMode.FORGOT_RESET

                        } catch (_: Exception) {

                            isLoading = false

                            errorMessage =
                                "Incorrect or expired code."
                        }
                    }
                },


                onResetPassword = {

                    scope.launch {

                        errorMessage = null

                        if (!isPasswordStrong(newPassword)) {

                            errorMessage =
                                "Password doesn't meet the strength requirements."

                            return@launch
                        }

                        if (newPassword != confirmPassword) {

                            errorMessage =
                                "Passwords don't match."

                            return@launch
                        }

                        isLoading = true

                        try {

                            SupabaseClientProvider.client.auth
                                .updateUser {
                                    password = newPassword
                                }

                            SupabaseClientProvider.client.auth
                                .signOut()

                            isLoading = false

                            password = ""
                            otp = ""
                            newPassword = ""
                            confirmPassword = ""

                            successMessage =
                                "Password updated — please log in."

                            mode = LoginMode.LOGIN

                        } catch (e: Exception) {

                            isLoading = false

                            errorMessage =
                                "Couldn't update password: ${e.message}"
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

                    navController.navigate(
                        "patientRegister?email=$email"
                    )
                }
            )
        }


        // ------------------------------------------------
        // PATIENT REGISTER
        // ------------------------------------------------

        composable(
            "patientRegister?email={email}",
            arguments = listOf(
                navArgument("email") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val prefilledEmail =
                backStackEntry.arguments
                    ?.getString("email")
                    ?: ""

            val scope = rememberCoroutineScope()

            var isLoading by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }


            PatientRegister(

                initialEmail = prefilledEmail,

                isLoading = isLoading,

                errorMessage = errorMessage,


                onBackToLogin = {
                    navController.popBackStack()
                },

                onViewPrivacyPolicy = {
                    navController.navigate("privacyPolicy")
                },


                onRegister = { data ->

                    scope.launch {

                        val registrationEmail = data.email.ifBlank { prefilledEmail }

                        errorMessage = null
                        isLoading = true

                        try {

                            SupabaseClientProvider.client.auth
                                .signUpWith(Email) {

                                    email = prefilledEmail
                                    password = data.password
                                }

                            val userId =
                                SupabaseClientProvider.client.auth
                                    .currentUserOrNull()
                                    ?.id
                                    ?: throw Exception(
                                        "Could not create account"
                                    )


                            SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .insert(
                                    mapOf(
                                        "id" to userId,
                                        "user_id" to userId,
                                        "name" to data.name,
                                        "surname" to data.surname,
                                        "title" to data.title,
                                        "date_of_birth" to data.dateOfBirth,
                                        "id_number" to data.idNumber,
                                        "phone" to data.phone,

                                        // Keep this exactly as your
                                        // current database column is named.
                                        "languege" to data.language,

                                        "email" to registrationEmail,
                                        "gender" to data.gender,
                                        "province" to data.province,
                                        "address1" to data.address1,
                                        "address2" to data.address2,
                                        "address3" to data.address3.ifBlank {
                                            null
                                        },
                                        "postal_code" to data.postalCode,
                                        "allergies" to data.allergies.ifBlank {
                                            null
                                        },
                                        "medication" to data.medication.ifBlank {
                                            null
                                        },
                                        "conditions" to data.conditions.ifBlank {
                                            null
                                        },
                                        "chronic" to data.chronic.ifBlank {
                                            null
                                        },
                                        "surgeries" to data.surgeries.ifBlank {
                                            null
                                        },
                                        "blood_group" to data.bloodGroup.ifBlank {
                                            null
                                        },
                                        "disability" to data.disability.ifBlank {
                                            null
                                        },
                                        "emergency_contact_name" to data.emergencyContactName.ifBlank {
                                            null
                                        },
                                        "emergency_contact_phone" to data.emergencyContactPhone.ifBlank {
                                            null
                                        },
                                        "emergency_contact_relationship" to data.emergencyContactRelationship.ifBlank {
                                            null
                                        }
                                    )
                                )

                            isLoading = false

                            navController.navigate(
                                "patientLogin"
                            ) {
                                popUpTo(
                                    "patientRegister?email={email}"
                                ) {
                                    inclusive = true
                                }
                            }

                        } catch (e: Exception) {

                            isLoading = false

                            errorMessage =
                                "Registration failed: ${e.message}"
                        }
                    }
                }
            )
        }


        // ------------------------------------------------
        // PRIVACY POLICY
        // ------------------------------------------------

        composable("privacyPolicy") {

            PrivacyPolicyScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }


        // ------------------------------------------------
        // DOCTOR LOGIN
        // ------------------------------------------------

        composable("doctorLogin") {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var email by remember {
                mutableStateOf("")
            }

            var password by remember {
                mutableStateOf("")
            }

            var isLoading by remember {
                mutableStateOf(false)
            }

            var isGoogleLoading by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }


            DoctorLogin(

                email = email,

                password = password,

                onEmailChange = {
                    email = it
                },

                onPasswordChange = {
                    password = it
                },

                isLoading = isLoading,

                isGoogleLoading = isGoogleLoading,

                errorMessage = errorMessage,


                onLogin = {

                    scope.launch {

                        isLoading = true
                        errorMessage = null

                        val result =
                            signInDoctorWithEmail(
                                email,
                                password
                            )

                        isLoading = false

                        result.onSuccess {

                            navController.navigate(
                                "doctorDashboard"
                            )

                        }.onFailure { error ->

                            errorMessage =
                                error.message
                        }
                    }
                },


                onForgotPassword = {

                    navController.navigate(
                        "forgotPassword"
                    )
                },


                onRegister = {

                    navController.navigate(
                        "doctorRegister"
                    )
                },


                onContinueWithGoogle = {

                    scope.launch {

                        isGoogleLoading = true
                        errorMessage = null

                        val result =
                            signInWithGoogle(context)

                        isGoogleLoading = false

                        result.onSuccess {

                            navController.navigate(
                                "doctorDashboard"
                            )

                        }.onFailure { error ->

                            errorMessage =
                                "Google sign-in failed: ${error.message}"
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

            var isLoading by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }


            DoctorRegister(

                isLoading = isLoading,

                errorMessage = errorMessage,


                onRegister = { info, uris ->

                    scope.launch {

                        isLoading = true
                        errorMessage = null


                        val files =
                            DoctorRegistrationFiles(

                                profilePhoto =
                                    uris.profilePhoto?.let {
                                        uriToDoctorFileUpload(
                                            context,
                                            it
                                        )
                                    },

                                idDocument =
                                    uris.idDocument?.let {
                                        uriToDoctorFileUpload(
                                            context,
                                            it
                                        )
                                    },

                                hpcsaCertificate =
                                    uris.hpcsaCertificate?.let {
                                        uriToDoctorFileUpload(
                                            context,
                                            it
                                        )
                                    },

                                medicalDegree =
                                    uris.medicalDegree?.let {
                                        uriToDoctorFileUpload(
                                            context,
                                            it
                                        )
                                    },

                                specialistCertificate =
                                    uris.specialistCertificate?.let {
                                        uriToDoctorFileUpload(
                                            context,
                                            it
                                        )
                                    },

                                practiceCertificate =
                                    uris.practiceCertificate?.let {
                                        uriToDoctorFileUpload(
                                            context,
                                            it
                                        )
                                    },

                                proofOfAddress =
                                    uris.proofOfAddress?.let {
                                        uriToDoctorFileUpload(
                                            context,
                                            it
                                        )
                                    }
                            )


                        val result =
                            registerDoctor(
                                info,
                                files
                            )

                        isLoading = false


                        result.onSuccess {

                            navController.navigate(
                                "doctorDashboard"
                            ) {
                                popUpTo("doctorLogin") {
                                    inclusive = true
                                }
                            }

                        }.onFailure { error ->

                            val message =
                                error.message
                                    ?: "Registration failed — please try again."


                            if (
                                message.contains(
                                    "confirm your email",
                                    ignoreCase = true
                                )
                            ) {

                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()

                                navController.navigate(
                                    "doctorLogin"
                                ) {
                                    popUpTo(
                                        "doctorRegister"
                                    ) {
                                        inclusive = true
                                    }
                                }

                            } else {

                                errorMessage = message
                            }
                        }
                    }
                },


                onLogin = {

                    navController.navigate(
                        "doctorLogin"
                    ) {
                        popUpTo(
                            "doctorRegister"
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }


        // ------------------------------------------------
        // DOCTOR FORGOT PASSWORD
        // ------------------------------------------------

        composable("forgotPassword") {

            val scope = rememberCoroutineScope()

            var email by remember {
                mutableStateOf("")
            }

            var isLoading by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }


            DoctorForgotPassword(

                email = email,

                onEmailChange = {
                    email = it
                    errorMessage = null
                },

                isLoading = isLoading,

                errorMessage = errorMessage,


                onSendCode = {

                    scope.launch {

                        isLoading = true

                        val result =
                            sendDoctorPasswordResetEmail(
                                email
                            )

                        isLoading = false

                        result.onSuccess {

                            navController.navigate(
                                "resetPassword?email=$email"
                            )

                        }.onFailure { error ->

                            errorMessage =
                                error.message
                                    ?: "Couldn't send the code — please try again."
                        }
                    }
                },


                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }


        // ------------------------------------------------
        // DOCTOR RESET PASSWORD
        // ------------------------------------------------

        composable(
            "resetPassword?email={email}",
            arguments = listOf(
                navArgument("email") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val email =
                backStackEntry.arguments
                    ?.getString("email")
                    ?: ""

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var isLoading by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }


            DoctorResetPassword(

                email = email,

                isLoading = isLoading,

                errorMessage = errorMessage,


                onSubmit = { code, newPassword ->

                    scope.launch {

                        isLoading = true

                        val result =
                            verifyDoctorResetCodeAndSetPassword(
                                email,
                                code,
                                newPassword
                            )

                        isLoading = false


                        result.onSuccess {

                            Toast.makeText(
                                context,
                                "Password updated — please log in.",
                                Toast.LENGTH_LONG
                            ).show()

                            navController.navigate(
                                "doctorLogin"
                            ) {
                                popUpTo(
                                    "forgotPassword"
                                ) {
                                    inclusive = true
                                }
                            }

                        }.onFailure { error ->

                            errorMessage =
                                error.message
                                    ?: "That code didn't work — please try again."
                        }
                    }
                },


                onResendCode = {

                    scope.launch {
                        sendDoctorPasswordResetEmail(
                            email
                        )
                    }
                }
            )
        }


        // ------------------------------------------------
        // DOCTOR DASHBOARD
        // ------------------------------------------------

        composable("doctorDashboard") {

            val scope = rememberCoroutineScope()
            val appContext = LocalContext.current

            var doctorProfile by remember {
                mutableStateOf<DoctorProfile?>(null)
            }

            var isLoadingProfile by remember {
                mutableStateOf(true)
            }

            var isUploadingPhoto by remember {
                mutableStateOf(false)
            }


            LaunchedEffect(Unit) {

                val userId =
                    SupabaseClientProvider.client.auth
                        .currentUserOrNull()
                        ?.id

                if (userId != null) {

                    try {

                        val row =
                            SupabaseClientProvider.client.postgrest
                                .from("doctors")
                                .select(
                                    columns = Columns.list(
                                        "id",
                                        "name",
                                        "surname",
                                        "practice_name",
                                        "discipline",
                                        "profile_image_url",
                                        "verification_status"
                                    )
                                ) {
                                    filter {
                                        eq(
                                            "user_id",
                                            userId
                                        )
                                    }
                                }
                                .decodeSingleOrNull<DoctorProfileRow>()

                        doctorProfile =
                            row?.toDoctorProfile()

                    } catch (e: Exception) {

                        Log.e(
                            "DoctorDashboard",
                            "Failed to fetch doctor profile",
                            e
                        )
                    }
                }

                isLoadingProfile = false
            }


            val photoPickerLauncher =
                rememberLauncherForActivityResult(
                    contract =
                        ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->

                    if (uri != null) {

                        scope.launch {

                            isUploadingPhoto = true

                            try {

                                val userId =
                                    SupabaseClientProvider.client.auth
                                        .currentUserOrNull()
                                        ?.id
                                        ?: throw Exception(
                                            "Not logged in"
                                        )

                                val file =
                                    uriToDoctorFileUpload(
                                        appContext,
                                        uri
                                    )
                                        ?: throw Exception(
                                            "Could not read the selected image"
                                        )

                                val publicUrl =
                                    uploadDoctorProfilePhoto(
                                        userId,
                                        file
                                    )


                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("doctors")
                                    .update(
                                        {
                                            set(
                                                "profile_image_url",
                                                publicUrl
                                            )
                                        }
                                    ) {
                                        filter {
                                            eq(
                                                "user_id",
                                                userId
                                            )
                                        }
                                    }


                                doctorProfile =
                                    doctorProfile?.copy(
                                        profileImageUrl =
                                            "$publicUrl?t=${System.currentTimeMillis()}"
                                    )

                            } catch (e: Exception) {

                                Log.e(
                                    "PhotoUpload",
                                    "Upload failed",
                                    e
                                )

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
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },


                onLogout = {

                    scope.launch {

                        SupabaseClientProvider.client.auth
                            .signOut()

                        navController.navigate(
                            "choose"
                        ) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                },


                fetchAppointments = { doctorId ->

                    try {

                        val data =
                            SupabaseClientProvider.client.postgrest
                                .from("appointments")
                                .select {
                                    filter {
                                        eq(
                                            "doctor_id",
                                            doctorId
                                        )
                                    }
                                }
                                .decodeList<Appointment>()

                        Result.success(data)

                    } catch (e: Exception) {


                        Result.failure(Exception(friendlyAuthError(e)))

                    }
                },


                onUpdateAppointmentStatus = {
                        appointmentId,
                        newStatus ->

                    try {

                        SupabaseClientProvider.client.postgrest
                            .from("appointments")
                            .update(
                                {
                                    set(
                                        "status",
                                        newStatus
                                    )
                                }
                            ) {
                                filter {
                                    eq(
                                        "id",
                                        appointmentId
                                    )
                                }
                            }

                        Result.success(Unit)

                    } catch (e: Exception) {


                        Result.failure(Exception(friendlyAuthError(e)))

                    }
                },


                onNavigatePrescriptions = {

                    navController.navigate(
                        "doctorPrescriptions"
                    )
                },


                onNavigateSettings = {

                    navController.navigate(
                        "settings"
                    )
                },


                onNavigateTimeSlots = {

                    Toast.makeText(
                        appContext,
                        "Time slot management coming soon",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }


        // ------------------------------------------------
        // DOCTOR PRESCRIPTIONS
        // ------------------------------------------------

        composable("doctorPrescriptions") {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var patients by remember {
                mutableStateOf<List<PrescriptionPatient>>(
                    emptyList()
                )
            }

            var isLoadingPatients by remember {
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


            LaunchedEffect(Unit) {

                val userId =
                    SupabaseClientProvider.client.auth
                        .currentUserOrNull()
                        ?.id

                if (userId != null) {

                    val row =
                        SupabaseClientProvider.client.postgrest
                            .from("doctors")
                            .select(
                                columns =
                                    Columns.list("id")
                            ) {
                                filter {
                                    eq(
                                        "user_id",
                                        userId
                                    )
                                }
                            }
                            .decodeSingleOrNull<Map<String, String?>>()

                    val id =
                        row?.get("id")

                    doctorId = id

                    if (id != null) {

                        fetchConfirmedPrescriptionPatients(
                            id
                        )
                            .onSuccess {
                                patients = it
                            }
                            .onFailure {
                                saveError = it.message
                            }
                    }
                }

                isLoadingPatients = false
            }


            DoctorPrescriptions(

                patients = patients,

                isLoadingPatients =
                    isLoadingPatients,

                isSaving = isSaving,

                saveError = saveError,


                onBack = {
                    navController.popBackStack()
                },


                onSave = {
                        patientId,
                        medications ->

                    val docId =
                        doctorId
                            ?: return@DoctorPrescriptions

                    scope.launch {

                        isSaving = true
                        saveError = null

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

                                navController.popBackStack()
                            }
                            .onFailure {

                                saveError =
                                    it.message
                                        ?: "Could not save prescription."
                            }

                        isSaving = false
                    }
                }
            )
        }



        // ------------------------------------------------
        // PATIENT PHONE
        // ------------------------------------------------

        composable("doctorTimeSlots") {
            val doctorId = SupabaseClientProvider.client.auth.currentUserOrNull()?.id

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
            DoctorSettingsScreen(
                isDarkMode = ThemeManager.isDarkMode,
                onToggleDarkMode = { ThemeManager.updateDarkMode(it) },
                onBack = { navController.popBackStack() },
                onViewPrivacyPolicy = { navController.navigate("privacyPolicy") },
                onLogout = {
                    scope.launch {
                        SupabaseClientProvider.client.auth.signOut()
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
                        } catch (e: Exception) {
                            Log.e("DoctorSettingsUpdate", "Failed to save setting", e)
                        }
                    }
                }
            )
        }

        composable("patientPhoneEntry") {

            var phoneNumber by remember {
                mutableStateOf("")
            }


            PatientPhoneEntry(

                phoneNumber = phoneNumber,

                onPhoneNumberChange = {
                    phoneNumber = it
                },

                onContinue = {

                    navController.navigate(
                        "completeProfile?phone=$phoneNumber"
                    )
                }
            )
        }


        // ------------------------------------------------
        // COMPLETE PROFILE
        // ------------------------------------------------

        composable(
            "completeProfile?phone={phone}",
            arguments = listOf(
                navArgument("phone") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val phone =
                backStackEntry.arguments
                    ?.getString("phone")
                    ?: ""

            val scope = rememberCoroutineScope()


            CompleteProfile(

                onContinue = {
                        title,
                        idNumber,
                        dateOfBirth,
                        gender,
                        language ->

                    scope.launch {

                        try {

                            SupabaseClientProvider.client
                                .postgrest
                                .from("patients")
                                .insert(
                                    mapOf(

                                        "title" to title,

                                        "id_number" to idNumber,

                                        "date_of_birth" to dateOfBirth,

                                        "gender" to gender,

                                        // Keep existing DB column.
                                        "languege" to language,

                                        "phone" to phone,

                                        "name" to PatientSignupState.firstName,

                                        "surname" to PatientSignupState.lastName,

                                        "email" to PatientSignupState.email,

                                        "user_id" to PatientSignupState.userId
                                    )
                                )

                            Log.d(
                                "PatientInsert",
                                "Insert succeeded"
                            )

                            navController.navigate(
                                "patientDashboard"
                            )

                        } catch (e: Exception) {

                            Log.e(
                                "PatientInsert",
                                "Insert failed",
                                e
                            )
                        }
                    }
                }
            )
        }


        // ------------------------------------------------
        // PATIENT DASHBOARD
        // ------------------------------------------------

        composable("patientDashboard") {

            val scope = rememberCoroutineScope()
            val appContext = LocalContext.current


            var patientName by remember {
                mutableStateOf("Patient")
            }

            var patientAvatarUrl by remember {
                mutableStateOf<String?>(null)
            }

            var isUploadingPhoto by remember {
                mutableStateOf(false)
            }



            LaunchedEffect(Unit) {

                val userId =
                    SupabaseClientProvider.client.auth
                        .currentSessionOrNull()
                        ?.user
                        ?.id

                if (userId != null) {

                    try {

                        val result =
                            SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select(
                                    columns = Columns.list(
                                        "name",
                                        "surname",
                                        "profile_image_url"
                                    )
                                ) {
                                    filter {
                                        eq(
                                            "user_id",
                                            userId
                                        )
                                    }
                                }
                                .decodeSingleOrNull<PatientLookup>()


                        if (result?.name != null) {

                            patientName =
                                "${result.name} ${result.surname ?: ""}"
                                    .trim()
                        }


                        patientAvatarUrl =
                            result?.profileImageUrl?.let {
                                "$it?t=${System.currentTimeMillis()}"
                            }

                    } catch (_: Exception) {
                    }
                }
            }


            val photoPickerLauncher =
                rememberLauncherForActivityResult(
                    contract =
                        ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->

                    if (uri != null) {

                        scope.launch {

                            isUploadingPhoto = true

                            try {

                                SupabaseClientProvider.client.auth
                                    .awaitInitialization()


                                val userId =
                                    SupabaseClientProvider.client.auth
                                        .currentSessionOrNull()
                                        ?.user
                                        ?.id
                                        ?: throw Exception(
                                            "Not logged in"
                                        )


                                val bytes =
                                    appContext.contentResolver
                                        .openInputStream(uri)
                                        ?.use {
                                            it.readBytes()
                                        }
                                        ?: throw Exception(
                                            "Could not read the selected image"
                                        )


                                val path =
                                    "$userId/avatar.jpg"


                                SupabaseClientProvider.client
                                    .storage
                                    .from("patient-avatars")
                                    .upload(
                                        path,
                                        bytes
                                    ) {
                                        upsert = true
                                    }


                                val publicUrl =
                                    SupabaseClientProvider.client
                                        .storage
                                        .from("patient-avatars")
                                        .publicUrl(path)


                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("patients")
                                    .update(
                                        {
                                            set(
                                                "profile_image_url",
                                                publicUrl
                                            )
                                        }
                                    ) {
                                        filter {
                                            eq(
                                                "user_id",
                                                userId
                                            )
                                        }
                                    }


                                patientAvatarUrl =
                                    "$publicUrl?t=${System.currentTimeMillis()}"


                                Toast.makeText(
                                    appContext,
                                    "Profile photo updated",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (e: Exception) {

                                Log.e(
                                    "PhotoUpload",
                                    "Upload failed",
                                    e
                                )

                                Toast.makeText(
                                    appContext,
                                    "Photo upload failed: ${e.message ?: "Please try again"}",
                                    Toast.LENGTH_LONG
                                ).show()

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


                onNavigateFindDoctors = {
                    navController.navigate(
                        "findDoctors"
                    )
                },

                onNavigateAppointments = {
                    navController.navigate(
                        "patientAppointments"
                    )
                },

                onNavigatePrescriptions = {
                    navController.navigate(
                        "patientPrescriptions") },
                onNavigateMedicalRecords = {
                    navController.navigate(
                        "medicalRecords"
                    )
                },

                onNavigateHealthTips = {
                    navController.navigate(
                        "healthTips"
                    )
                },

                onNavigateSettings = {
                    navController.navigate(
                        "settings"
                    )
                },


                onUploadPhoto = {

                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },


                onLogout = {

                    scope.launch {

                        SupabaseClientProvider.client.auth
                            .signOut()

                        navController.navigate(
                            "patientLogin"
                        ) {
                            popUpTo(0) {
                                inclusive = true
                            }
                        }
                    }
                },


                fetchAppointments = {

                    val userId =
                        SupabaseClientProvider.client.auth
                            .currentSessionOrNull()
                            ?.user
                            ?.id


                    if (userId == null) {

                        Result.failure(
                            Exception("Not logged in.")
                        )

                    } else {

                        try {

                            val data =
                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("appointments")
                                    .select {
                                        filter {
                                            eq(
                                                "patient_id",
                                                userId
                                            )
                                        }
                                    }
                                    .decodeList<Appointment>()

                            Result.success(data)

                        } catch (e: Exception) {

                            Result.failure(e)
                        }
                    }
                },


                fetchPrescriptions = {
                    val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                    if (userId == null) {
                        Result.failure(Exception("Not logged in."))
                    } else {
                        try {
                            val patientId = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select(columns = Columns.list("id")) {
                                    filter { eq("user_id", userId) }
                                }
                                .decodeSingleOrNull<Map<String, String?>>()
                                ?.get("id")
                                ?: userId

                            val prescriptionList = SupabaseClientProvider.client.postgrest
                                .from("prescriptions")
                                .select { filter { eq("patient_id", patientId) } }
                                .decodeList<Prescription>()

                            val doctorIds = prescriptionList.mapNotNull { it.doctor_id }.distinct()
                            val doctorNames: Map<String, String> = if (doctorIds.isEmpty()) {
                                emptyMap()
                            } else {
                                SupabaseClientProvider.client.postgrest
                                    .from("doctors")
                                    .select(columns = Columns.list("id", "name", "surname")) {
                                        filter { isIn("id", doctorIds) }
                                    }
                                    .decodeList<Map<String, String?>>()
                                    .associate { doc ->
                                        (doc["id"] ?: "") to "Dr. ${doc["name"] ?: ""} ${doc["surname"] ?: ""}".trim()
                                    }
                            }

                            val display = prescriptionList
                                .sortedByDescending { it.created_at }
                                .map { PrescriptionDisplay(it, doctorNames[it.doctor_id] ?: "Unknown Doctor") }

                            Result.success(display)
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }
                },

                fetchReviews = {

                    val userId =
                        SupabaseClientProvider.client.auth
                            .currentSessionOrNull()
                            ?.user
                            ?.id


                    if (userId == null) {

                        Result.failure(
                            Exception("Not logged in.")
                        )

                    } else {

                        try {

                            val reviewList =
                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("reviews")
                                    .select {
                                        filter {
                                            eq(
                                                "patient_id",
                                                userId
                                            )
                                        }
                                    }
                                    .decodeList<Review>()


                            val doctorIds =
                                reviewList
                                    .map {
                                        it.doctor_id
                                    }
                                    .distinct()


                            val doctorNames:
                                    Map<String, String> =

                                if (doctorIds.isEmpty()) {

                                    emptyMap()

                                } else {

                                    SupabaseClientProvider.client
                                        .postgrest
                                        .from("doctors")
                                        .select(columns = Columns.list("id", "name", "surname")) {
                                            filter { isIn("id", doctorIds) }
                                        }
                                        .decodeList<Map<String, String?>>()
                                        .associate { doc ->
                                            (doc["id"] ?: "") to
                                                    "Dr. ${doc["name"] ?: ""} ${doc["surname"] ?: ""}".trim()
                                        }
                                }


                            val display =
                                reviewList
                                    .sortedByDescending {
                                        it.created_at
                                    }
                                    .map {

                                        ReviewDisplay(
                                            review = it,
                                            doctorName =
                                                doctorNames[
                                                    it.doctor_id
                                                ]
                                                    ?: "Unknown Doctor"
                                        )
                                    }


                            Result.success(display)

                        } catch (e: Exception) {

                            Result.failure(e)
                        }
                    }
                },


                // ------------------------------------------------
                // NEW: FETCH DOCTOR FOR APPOINTMENT CARD
                // ------------------------------------------------

                fetchDoctor = { doctorId: String ->

                    try {

                        val doctor =
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


                        if (doctor == null) {

                            Result.failure(
                                Exception(
                                    "Doctor not found."
                                )
                            )

                        } else {

                            Result.success(doctor)
                        }

                    } catch (e: Exception) {

                        Result.failure(e)
                    }
                },


                // ------------------------------------------------
                // NEW: VIEW DOCTOR PROFILE FROM APPOINTMENT CARD
                // ------------------------------------------------

                onViewDoctorProfile = { doctorId: String ->

                    navController.navigate(
                        "doctorProfile/$doctorId"
                    )
                },


                onRescheduleAppointment = { appointmentId ->

                    navController.navigate(
                        "rescheduleAppointment/$appointmentId"
                    )
                },


                cancelAppointment = { appointmentId ->

                    try {

                        SupabaseClientProvider.client
                            .postgrest
                            .from("appointments")
                            .update(
                                {
                                    set(
                                        "status",
                                        "cancelled"
                                    )
                                }
                            ) {
                                filter {
                                    eq(
                                        "id",
                                        appointmentId
                                    )
                                }
                            }

                        Result.success(Unit)

                    } catch (e: Exception) {

                        Result.failure(e)
                    }
                },


                onStartCall = { appointmentId ->

                    navController.navigate(
                        "callPlaceholder/$appointmentId"
                    )
                }
            )
        }


        // ------------------------------------------------
        // CALL PLACEHOLDER
        // ------------------------------------------------

        composable(
            "callPlaceholder/{appointmentId}",
            arguments = listOf(
                navArgument("appointmentId") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val appointmentId =
                backStackEntry.arguments
                    ?.getString("appointmentId")
                    ?: ""


            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        "Call screen coming soon",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        "Appointment: $appointmentId",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Text("Back")
                    }
                }
            }
        }


        // ------------------------------------------------
        // FIND DOCTORS
        // ------------------------------------------------

        composable("findDoctors") {

            FindDoctorsScreen(

                onBack = {
                    navController.popBackStack()
                },


                onSelectDoctor = { doc ->

                    navController.navigate(
                        "bookAppointment/${doc.id}"
                    )
                },


                onViewProfile = { doc ->

                    navController.navigate(
                        "doctorProfile/${doc.id}"
                    )
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


        // ------------------------------------------------
        // DOCTOR PROFILE
        // ------------------------------------------------

        composable(
            "doctorProfile/{doctorId}",
            arguments = listOf(
                navArgument("doctorId") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val doctorId =
                backStackEntry.arguments
                    ?.getString("doctorId")
                    ?: ""


            DoctorProfileScreen(

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

                                        // rating is already non-nullable Int
                                        rating = it.rating,

                                        comment = it.comment,

                                        patientName = null,

                                        createdAt = it.created_at
                                    )
                                }


                        Result.success(items)

                    } catch (e: Exception) {

                        Result.failure(e)
                    }
                },


                onMessageDoctor = { selectedDoctorId ->

                    navController.navigate(
                        "messages/$selectedDoctorId"
                    )
                }
            )
        }


        // ------------------------------------------------
        // MESSAGES
        // ------------------------------------------------

        composable(
            route = "messages/{doctorId}",
            arguments = listOf(
                navArgument("doctorId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val doctorId =
                backStackEntry.arguments
                    ?.getString("doctorId")
                    ?: ""


            MessagingScreen(

                doctorId = doctorId,

                onBack = {
                    navController.popBackStack()
                }
            )
        }


        // ------------------------------------------------
        // HEALTH TIPS
        // ------------------------------------------------

        composable("healthTips") {

            HealthTipsScreen(

                onBack = {
                    navController.popBackStack()
                },

                onBookConsultation = {
                    navController.navigate(
                        "findDoctors"
                    )
                }
            )
        }


        // ------------------------------------------------
        // MEDICAL RECORDS
        // ------------------------------------------------

        composable("medicalRecords") {

            MedicalRecordsScreen(

                onBack = {
                    navController.popBackStack()
                },


                fetchRecords = {

                    val userId =
                        SupabaseClientProvider.client.auth
                            .currentSessionOrNull()
                            ?.user
                            ?.id


                    if (userId == null) {

                        Result.failure(
                            Exception(
                                "Please log in to view your records."
                            )
                        )

                    } else {

                        try {

                            val patientId =
                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("patients")
                                    .select(
                                        columns =
                                            Columns.list("id")
                                    ) {
                                        filter {
                                            eq(
                                                "user_id",
                                                userId
                                            )
                                        }
                                    }
                                    .decodeSingleOrNull<PatientLookup>()
                                    ?.id
                                    ?: userId


                            val records =
                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("medical_records")
                                    .select {
                                        filter {
                                            eq(
                                                "patient_id",
                                                patientId
                                            )
                                        }
                                    }
                                    .decodeList<MedicalRecord>()


                            val doctorIds =
                                records
                                    .mapNotNull {
                                        it.doctor_id
                                    }
                                    .distinct()


                            val doctorNames:
                                    Map<String, String> =

                                if (doctorIds.isEmpty()) {

                                    emptyMap()

                                } else {

                                    SupabaseClientProvider.client
                                        .postgrest
                                        .from("doctors")
                                        .select(columns = Columns.list("id", "name", "surname")) {
                                            filter { isIn("id", doctorIds) }
                                        }
                                        .decodeList<Map<String, String?>>()
                                        .associate { doc ->
                                            val id = doc["id"] ?: ""
                                            val name = "Dr. ${doc["name"] ?: ""} ${doc["surname"] ?: ""}".trim()
                                            id to name
                                        }
                                }


                            val display =
                                records.map { record ->

                                    MedicalRecordDisplay(

                                        record = record,

                                        doctorName =
                                            doctorNames[
                                                record.doctor_id
                                            ]
                                                ?: "Unknown Doctor"
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


        // ------------------------------------------------
        // BOOK APPOINTMENT
        // ------------------------------------------------

        composable(
            "bookAppointment/{doctorId}",
            arguments = listOf(
                navArgument("doctorId") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val doctorId =
                backStackEntry.arguments
                    ?.getString("doctorId")
                    ?: ""

            val scope = rememberCoroutineScope()

            var isLoading by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }


            BookAppointmentScreen(

                isLoading = isLoading,

                errorMessage = errorMessage,


                onBack = {
                    navController.popBackStack()
                },


                fetchDoctor = {

                    try {

                        val doc =
                            SupabaseClientProvider.client
                                .postgrest
                                .from("doctors")
                                .select(
                                    columns =
                                        Columns.list(
                                            "name",
                                            "surname",
                                            "discipline",
                                            "hourly_rate",
                                            "operating_hours"
                                        )
                                ) {
                                    filter {
                                        eq(
                                            "id",
                                            doctorId
                                        )
                                    }
                                }
                                .decodeSingleOrNull<DoctorLookup>()


                        if (doc == null) {

                            Result.failure(
                                Exception(
                                    "Doctor not found."
                                )
                            )

                        } else {

                            Result.success(

                                DoctorBookingInfo(

                                    id = doctorId,

                                    name =
                                        doc.name
                                            ?: "",

                                    surname =
                                        doc.surname
                                            ?: "",

                                    discipline =
                                        doc.discipline,

                                    hourlyRate =
                                        doc.hourlyRate,

                                    operatingHours =
                                        doc.operatingHours
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
                            .select(columns = Columns.list("time")) {
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

                            val userId =
                                SupabaseClientProvider.client
                                    .auth
                                    .currentSessionOrNull()
                                    ?.user
                                    ?.id
                                    ?: throw Exception(
                                        "Please log in to book an appointment."
                                    )


                            val patient = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select(columns = Columns.list("name", "surname")) {
                                    filter { eq("user_id", userId) }
                                }
                                .decodeSingleOrNull<Map<String, String?>>()


                            val patientName =
                                "${
                                    patient?.get("name")
                                        ?: ""
                                } ${
                                    patient?.get("surname")
                                        ?: ""
                                }"
                                    .trim()


                            val row =
                                buildJsonObject {

                                    put(
                                        "doctor_id",
                                        doctorId
                                    )

                                    put(
                                        "patient_id",
                                        userId
                                    )

                                    if (
                                        patientName.isNotBlank()
                                    ) {

                                        put(
                                            "patient_name",
                                            patientName
                                        )
                                    }

                                    put(
                                        "date",
                                        submission.date
                                    )

                                    put(
                                        "time",
                                        submission.time
                                    )

                                    put(
                                        "reason",
                                        submission.reason
                                    )

                                    put(
                                        "status",
                                        "pending"
                                    )

                                    put(
                                        "payment_method",
                                        submission.paymentReference
                                    )

                                    put(
                                        "amount_paid",
                                        submission.fee
                                    )

                                    put(
                                        "appointment_type",
                                        if (
                                            submission.appointmentType ==
                                            AppointmentType.ONLINE
                                        ) {
                                            "online"
                                        } else {
                                            "in_person"
                                        }
                                    )
                                }


                            SupabaseClientProvider.client
                                .postgrest
                                .from("appointments")
                                .insert(row)


                            isLoading = false


                            navController.navigate(
                                "bookingConfirmed"
                            ) {
                                popUpTo(
                                    "findDoctors"
                                ) {
                                    inclusive = false
                                }
                            }

                        } catch (e: Exception) {

                            isLoading = false

                            errorMessage =
                                "Booking failed: ${e.message}"
                        }
                    }
                }
            )
        }


        // ------------------------------------------------
        // PATIENT APPOINTMENTS
        // ------------------------------------------------


        composable("patientAppointments") {

            val scope = rememberCoroutineScope()

            var isSubmittingReview by remember {
                mutableStateOf(false)
            }

            var refreshTrigger by remember {
                mutableIntStateOf(0)
            }

            PatientAppointmentsScreen(

                onBack = {
                    navController.popBackStack()
                },

                onFindDoctors = {
                    navController.navigate(
                        "findDoctors"
                    )
                },

                onViewDoctorProfile = { doctorId ->
                    navController.navigate(
                        "doctorProfile/$doctorId"
                    )
                },

                fetchDoctor = { doctorId ->

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

                fetchAppointments = {

                    val userId =
                        SupabaseClientProvider.client
                            .auth
                            .currentSessionOrNull()
                            ?.user
                            ?.id

                    if (userId == null) {

                        Result.failure(
                            Exception(
                                "Not logged in."
                            )
                        )

                    } else {

                        try {

                            // Re-run the fetch when a review is submitted.
                            refreshTrigger

                            val data =
                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("appointments")
                                    .select {
                                        filter {
                                            eq(
                                                "patient_id",
                                                userId
                                            )
                                        }
                                    }
                                    .decodeList<Appointment>()

                            Result.success(data)

                        } catch (e: Exception) {

                            Result.failure(e)
                        }
                    }
                },

                fetchReviewedAppointmentIds = {

                    val userId =
                        SupabaseClientProvider.client
                            .auth
                            .currentSessionOrNull()
                            ?.user
                            ?.id

                    if (userId == null) {

                        Result.failure(
                            Exception(
                                "Not logged in."
                            )
                        )

                    } else {

                        try {

                            val ids =
                                SupabaseClientProvider.client
                                    .postgrest
                                    .from("reviews")
                                    .select {
                                        filter {
                                            eq(
                                                "patient_id",
                                                userId
                                            )
                                        }
                                    }
                                    .decodeList<Review>()
                                    .mapNotNull {
                                        it.appointment_id
                                    }
                                    .toSet()

                            Result.success(ids)

                        } catch (e: Exception) {

                            Result.failure(e)
                        }
                    }
                },

                isSubmittingReview =
                    isSubmittingReview,

                onSubmitReview = {
                        appointmentId,
                        _,
                        rating,
                        comment ->

                    scope.launch {

                        isSubmittingReview = true

                        try {

                            val userId =
                                SupabaseClientProvider.client
                                    .auth
                                    .currentSessionOrNull()
                                    ?.user
                                    ?.id
                                    ?: throw Exception(
                                        "Not logged in"
                                    )

                            val appt = SupabaseClientProvider.client.postgrest
                                .from("appointments")
                                .select(columns = Columns.list("doctor_id")) {
                                    filter { eq("id", appointmentId) }
                                }
                                .decodeSingleOrNull<Map<String, String?>>()

                            val doctorId =
                                appt?.get(
                                    "doctor_id"
                                )
                                    ?: throw Exception(
                                        "Could not find the doctor for this appointment"
                                    )

                            SupabaseClientProvider.client
                                .postgrest
                                .from("reviews")
                                .insert(
                                    mapOf(

                                        "patient_id" to
                                                userId,

                                        "doctor_id" to
                                                doctorId,

                                        "appointment_id" to
                                                appointmentId,

                                        "rating" to
                                                rating,

                                        "comment" to
                                                comment.ifBlank {
                                                    null
                                                }
                                    )
                                )

                            refreshTrigger++

                        } catch (e: Exception) {

                            Log.e(
                                "ReviewSubmit",
                                "Failed to submit review",
                                e
                            )

                        } finally {

                            isSubmittingReview = false
                        }
                    }
                },

                onRescheduleAppointment = { appointmentId ->

                    navController.navigate(
                        "rescheduleAppointment/$appointmentId"
                    )
                },

                cancelAppointment = { appointmentId ->

                    try {

                        SupabaseClientProvider.client
                            .postgrest
                            .from("appointments")
                            .update(
                                {
                                    set(
                                        "status",
                                        "cancelled"
                                    )
                                }
                            ) {
                                filter {
                                    eq(
                                        "id",
                                        appointmentId
                                    )
                                }
                            }

                        Result.success(Unit)

                    } catch (e: Exception) {

                        Result.failure(e)
                    }
                }
            )
        }
        // ------------------------------------------------
        // RESCHEDULE APPOINTMENT
        // ------------------------------------------------

        composable(
            "rescheduleAppointment/{appointmentId}",
            arguments = listOf(
                navArgument("appointmentId") {
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->

            val appointmentId =
                backStackEntry.arguments
                    ?.getString("appointmentId")
                    ?: ""


            val scope = rememberCoroutineScope()


            var appointment by remember {
                mutableStateOf<Appointment?>(null)
            }

            var isLoadingAppt by remember {
                mutableStateOf(true)
            }

            var isSubmitting by remember {
                mutableStateOf(false)
            }

            var errorMessage by remember {
                mutableStateOf<String?>(null)
            }


            LaunchedEffect(appointmentId) {

                try {

                    appointment =
                        SupabaseClientProvider.client
                            .postgrest
                            .from("appointments")
                            .select {
                                filter {
                                    eq(
                                        "id",
                                        appointmentId
                                    )
                                }
                            }
                            .decodeSingleOrNull<Appointment>()

                } catch (e: Exception) {

                    errorMessage =
                        "Could not load appointment: ${e.message}"
                }

                isLoadingAppt = false
            }


            when {

                isLoadingAppt -> {

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

                        Text(
                            errorMessage
                                ?: "Appointment not found."
                        )
                    }
                }


                else -> {

                    val doctorIdForFetch =
                        appointment!!.doctor_id


                    RescheduleAppointmentScreen(

                        appointment =
                            appointment!!,

                        isLoading =
                            isSubmitting,

                        errorMessage =
                            errorMessage,


                        onBack = {
                            navController.popBackStack()
                        },


                        fetchBookedTimes = { date ->

                            if (
                                doctorIdForFetch == null
                            ) {

                                Result.success(
                                    emptySet()
                                )

                            } else {

                                try {

                                    val booked = SupabaseClientProvider.client.postgrest
                                        .from("appointments")
                                        .select(columns = Columns.list("time")) {
                                            filter {
                                                eq("doctor_id", doctorIdForFetch)
                                                eq("date", date)
                                                neq("status", "cancelled")
                                                neq("id", appointmentId)
                                            }
                                        }
                                        .decodeList<Map<String, String?>>()
                                        .mapNotNull { it["time"]?.take(5) }
                                        .toSet()


                                    Result.success(
                                        booked
                                    )

                                } catch (e: Exception) {

                                    Result.failure(e)
                                }
                            }
                        },


                        // "reason" is intentionally ignored here because
                        // rescheduling only changes date/time/status.
                        onConfirmReschedule = {
                                newDate,
                                newTime,
                                _ ->

                            scope.launch {

                                isSubmitting = true
                                errorMessage = null

                                try {

                                    SupabaseClientProvider.client
                                        .postgrest
                                        .from("appointments")
                                        .update({

                                            set(
                                                "date",
                                                newDate
                                            )

                                            set(
                                                "time",
                                                newTime
                                            )

                                            set(
                                                "status",
                                                "pending"
                                            )

                                        }) {

                                            filter {
                                                eq(
                                                    "id",
                                                    appointmentId
                                                )
                                            }
                                        }


                                    isSubmitting = false

                                    navController.popBackStack()

                                } catch (e: Exception) {

                                    isSubmitting = false

                                    errorMessage =
                                        "Reschedule failed: ${e.message}"
                                }
                            }
                        }
                    )
                }
            }
        }


        // ------------------------------------------------
        // BOOKING CONFIRMED
        // ------------------------------------------------

        composable("bookingConfirmed") {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        "Appointment booked!",
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Button(
                        onClick = {

                            navController.navigate(
                                "patientDashboard"
                            ) {

                                popUpTo(
                                    "patientDashboard"
                                ) {
                                    inclusive = true
                                }
                            }
                        }
                    ) {

                        Text(
                            "Back to Dashboard"
                        )
                    }
                }
            }
        }

        composable("patientPrescriptions") {
            var prescriptions by remember { mutableStateOf<List<Prescription>>(emptyList()) }
            var isLoading by remember { mutableStateOf(true) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                val userId = SupabaseClientProvider.client.auth.currentSessionOrNull()?.user?.id
                if (userId == null) {
                    errorMessage = "Not logged in."
                } else {
                    try {
                        val patientId = SupabaseClientProvider.client.postgrest
                            .from("patients")
                            .select(columns = Columns.list("id")) {
                                filter { eq("user_id", userId) }
                            }
                            .decodeSingleOrNull<Map<String, String?>>()
                            ?.get("id")
                            ?: userId

                        prescriptions = SupabaseClientProvider.client.postgrest
                            .from("prescriptions")
                            .select { filter { eq("patient_id", patientId) } }
                            .decodeList<Prescription>()
                            .sortedByDescending { it.created_at }
                    } catch (e: Exception) {
                        Log.e("PatientPrescriptions", "Failed to load prescriptions", e)
                        errorMessage = e.message ?: "Could not load prescriptions."
                    }
                }
                isLoading = false
            }

            androidx.compose.material3.Scaffold(
                topBar = {
                    androidx.compose.material3.TopAppBar(
                        title = { Text("My Prescriptions") },
                        navigationIcon = {
                            androidx.compose.material3.IconButton(
                                onClick = { navController.popBackStack() }
                            ) {
                                androidx.compose.material3.Icon(
                                    androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    if (errorMessage != null) {
                        Text(errorMessage!!, modifier = Modifier.padding(16.dp))
                    } else {
                        PatientPrescriptionsScreen(
                            prescriptions = prescriptions,
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
}