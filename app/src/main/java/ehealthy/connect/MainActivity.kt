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
import io.github.jan.supabase.auth.auth
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

            PatientLogin(
                isLoading = isLoading,
                onContinueWithGoogle = {
                    scope.launch {
                        isLoading = true
                        val result = signInWithGoogle(context)
                        isLoading = false
                        result.onSuccess { info ->
                            PatientSignupState.email = info.email
                            PatientSignupState.firstName = info.firstName
                            PatientSignupState.lastName = info.lastName
                            PatientSignupState.userId =
                                SupabaseClientProvider.client.auth.currentUserOrNull()?.id ?: ""
                            navController.navigate("patientPhoneEntry")
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                "Sign-in failed: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
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