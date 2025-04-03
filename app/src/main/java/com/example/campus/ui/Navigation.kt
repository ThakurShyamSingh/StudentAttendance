package com.example.campus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.campus.ui.screens.*

enum class Routes(val route: String) {
    CAMPUS("campus"),
    DASHBOARD("dashboard"),
    FACE_CAPTURE("face_capture"),
    REGISTERED_STUDENTS("registered_students"),
    FACE_RECOGNITION("face_recognition"),
    CROWD_SENSE("crowd_sense_screen"),
    DISPLAY_DETAILS_SCREEN("DisplayDetailsScreen")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current // Correct way to get LifecycleOwner

    NavHost(navController = navController, startDestination = Routes.CAMPUS.route) {
        composable(Routes.CAMPUS.route) { CampusScreen(navController) }
        composable(Routes.DASHBOARD.route) { DashboardScreen(navController) }
        composable(Routes.FACE_CAPTURE.route) { FaceCaptureScreen(navController) }
        composable(Routes.REGISTERED_STUDENTS.route) { RegisteredStudentsScreen(navController) }
        composable(Routes.FACE_RECOGNITION.route) { FaceRecognitionScreen(navController) }
        composable(Routes.CROWD_SENSE.route) { CrowdSenseScreen(navController, context, lifecycleOwner  ) }
        composable(Routes.DISPLAY_DETAILS_SCREEN.route) {
            DisplayDetailsScreen(context, lifecycleOwner)
        }
    }
}
