package ehealthy.connect

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ehealthy.connect.ui.theme.EHealthyTheme
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

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
                        } catch (_: Exception) {
                            isLoading = false
                            errorMessage = "Incorrect email or password."
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
                email = prefilledEmail,
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
                onRegister = { info ->
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        val result = registerDoctor(info)
                        isLoading = false
                        result.onSuccess {
                            navController.navigate("doctorDashboard") {
                                popUpTo("doctorLogin") { inclusive = true }
                            }
                        }.onFailure { error ->
                            val message = error.message ?: "Registration failed — please try again."
                            if (message.contains("confirm your email", ignoreCase = true)) {
                                // Email confirmation required, no session yet — not a real
                                // error, just send them to log in once they've confirmed.
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
                onBack = { navController.popBackStack() },
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
                }
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
            DoctorDashboard(
                onLogOut = {
                    scope.launch {
                        SupabaseClientProvider.client.auth.signOut()
                        navController.navigate("choose") {
                            popUpTo("doctorDashboard") { inclusive = true }
                        }
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
    }
}