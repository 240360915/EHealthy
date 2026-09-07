package ehealthy.connect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ehealthy.connect.ui.theme.EHealthyTheme

class MainActivity : ComponentActivity() {
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
                onSkip = { navController.navigate("choose") } // wire once role-selection exists
            )
        }

        composable("onboarding2") {
            OnboardingScreenTwo(
                onContinue = { navController.navigate("onboarding3") },
                onSkip = { navController.navigate("choose") }
            )
        }
        composable("onboarding3") {
            OnboardingThree(
                onContinue = { navController.navigate("onboarding4") },
                onSkip = { navController.navigate("choose") }
            )
        }
        composable("onboarding4") {
            OnboardingFour(
                onGetStarted = { navController.navigate("choose") }
            )
        }
    }
}