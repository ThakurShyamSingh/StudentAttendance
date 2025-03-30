package com.example.campus.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Enum class for navigation routes (safer than a plain object)
enum class Routes(val route: String) {
    CAMPUS("campus"),
    DASHBOARD("dashboard"),
    FACE_CAPTURE("face_capture"),
    REGISTERED_STUDENTS("registered_students"),
    FACE_RECOGNITION("face_recognition") // New Route for Face Recognition
}


@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current // Get the context

    NavHost(navController = navController, startDestination = Routes.CAMPUS.route) {
        composable(Routes.CAMPUS.route) { CampusScreen(navController) }
        composable(Routes.DASHBOARD.route) { DashboardScreen(navController, context) } // Pass context here
        composable(Routes.FACE_CAPTURE.route) { FaceCaptureScreen(navController) }
        composable(Routes.REGISTERED_STUDENTS.route) { RegisteredStudentsScreen(navController) }
        composable(Routes.FACE_RECOGNITION.route) { FaceRecognitionScreen(navController) }
    }
}

