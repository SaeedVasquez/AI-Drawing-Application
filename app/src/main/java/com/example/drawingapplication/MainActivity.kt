package com.example.drawingapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.drawingapplication.ViewModel.DrawingViewModel
import com.example.drawingapplication.ui.theme.DrawingApplicationTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.drawingapplication.ViewModel.DrawingViewModelFactory

/**
 * Main Activity that starts up the application
 *
 * @authors Saeed Vasquez, Danny Le, Ethan Nguyen
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val testKey = BuildConfig.VISION_API_KEY

        setupSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrawingApplicationTheme {
                val navController = rememberNavController()

                // Gets the singleton repositories from the Application class
                val app = application as DrawingApplication
                val repository = app.repository
                val cloudRepository = app.cloudRepository

                // Initialize the ViewModel using the custom Factory
                val vm: DrawingViewModel = viewModel(
                    factory = DrawingViewModelFactory(repository, cloudRepository)
                )

                AppNavHost(navController, vm)
            }
        }
    }

    private fun setupSplashScreen() {
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashView ->
            splashView.view.animate().alpha(0f)
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(600)
                .withEndAction { splashView.remove() }
                .start()
        }
    }
}



