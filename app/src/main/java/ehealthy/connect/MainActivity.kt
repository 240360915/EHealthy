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
import io.github.jan.supabase.auth.providers.builtin.OTP
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
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            var isLoading by remember { mutableStateOf(false) }
            var email by remember { mutableStateOf("") }

            PatientLogin(
                isLoading = isLoading,
                email = email,
                onEmailChange = { email = it },
                onContinueWithGoogle = {
                    scope.launch {
                        isLoading = true
                        val result = signInWithGoogle(context)
                        isLoading = false
                        result.onSuccess { info ->
                            // Check if this Google email already has a patient record
                            val existing = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select { filter { eq("email", info.email) } }
                                .decodeSingleOrNull<Map<String, String>>()

                            if (existing != null) {
                                // Returning user — skip signup, go straight in
                                navController.navigate("patientDashboard")
                            } else {
                                // New user — start the signup flow you already built
                                PatientSignupState.email = info.email
                                PatientSignupState.firstName = info.firstName
                                PatientSignupState.lastName = info.lastName
                                PatientSignupState.userId =
                                    SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: ""
                                navController.navigate("patientPhoneEntry")
                            }
                        }.onFailure {
                            Toast.makeText(
                                context,
                                "Sign-in failed: ${it.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onContinueWithEmail = {
                    scope.launch {
                        try {
                            SupabaseClientProvider.client.auth.signInWith(OTP) {
                                this.email = email
                            }
                            navController.navigate("patientEmailOtp?email=$email")
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Couldn't send code: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
        }

        composable(
            "patientEmailOtp?email={email}",
            arguments = listOf(navArgument("email") { defaultValue = "" })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var otpValues by remember { mutableStateOf(List(6) { "" }) }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            PatientEmailOtpVerify(
                email = email,
                otpValues = otpValues,
                onOtpValueChange = { index, value ->
                    otpValues = otpValues.toMutableList().also { it[index] = value }
                },
                errorMessage = errorMessage,
                onVerify = {
                    scope.launch {
                        try {
                            val code = otpValues.joinToString("")
                            SupabaseClientProvider.client.auth.verifyEmailOtp(
                                type = OtpType.Email.EMAIL,
                                email = email,
                                token = code
                            )

                            // Confirm this email actually has a patient record
                            val existing = SupabaseClientProvider.client.postgrest
                                .from("patients")
                                .select { filter { eq("email", email) } }
                                .decodeSingleOrNull<Map<String, String>>()

                            if (existing != null) {
                                navController.navigate("patientDashboard")
                            } else {
                                errorMessage =
                                    "No account found for this email. Please sign up with Google first."
                            }
                        } catch (e: Exception) {
                            errorMessage = "That code didn't work — try again."
                        }
                    }
                },
                onResendCode = {
                    scope.launch {
                        SupabaseClientProvider.client.auth.signInWith(OTP) { this.email = email }
                        Toast.makeText(context, "New code sent", Toast.LENGTH_SHORT).show()
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
                onForgotPassword = {
                    // wire to a doctor password-reset screen when you build one
                },
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