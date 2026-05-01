package com.example.drawingapplication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.drawingapplication.View.AuthScreen
import com.example.drawingapplication.View.DrawingScreen
import com.example.drawingapplication.View.HomeScreen
import com.example.drawingapplication.ViewModel.DrawingViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun AppNavHost(navController: NavHostController, viewModel: DrawingViewModel) {
    // If already logged in, skip login screen
    val startDestination = if (Firebase.auth.currentUser != null) "home" else "login"
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            AuthScreen(navController)
        }

        composable("home") {
            HomeScreen(navController, viewModel,
                onSignOut = {
                Firebase.auth.signOut()
                navController.navigate("login") {
                    popUpTo("home") { inclusive = true }
                    }
                }
            )
        }

        composable("draw") {
            DrawingScreen(viewModel, navController)
        }

        composable(
            route = "draw/{drawingId}",
            arguments = listOf(navArgument("drawingId") { type = NavType.IntType })
        ) { backStackEntry ->
            val drawingId = backStackEntry.arguments?.getInt("drawingId") ?: -1
            // Load the saved drawing when navigating to this route
            LaunchedEffect(drawingId) {
                val drawing = viewModel.allSavedDrawings.value.find { it.id == drawingId }
                drawing?.let { viewModel.loadDrawing(it.id, it.filePath) }
            }
            DrawingScreen(viewModel, navController)
        }
    }
}
