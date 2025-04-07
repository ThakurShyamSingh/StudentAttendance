package com.example.campus.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.campus.ui.screens.*

enum class Routes(val route: String) {
    CAMPUS("campus"),
    DASHBOARD("dashboard"),
    FACE_CAPTURE("face_capture"),
    REGISTERED_STUDENTS("registered_students"),
    FACE_RECOGNITION("face_recognition"),
    CROWD_SENSE("crowd_sense_screen"),
    DISPLAY_DETAILS_SCREEN("DisplayDetailsScreen"),
    STUDENT_MANAGER("student_manager"),
    EDIT_FACE_SCREEN("edit_face_screen"),
    HOUR_SELECTOR("hour_selector_screen")

}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    NavHost(navController = navController, startDestination = Routes.CAMPUS.route) {
        composable(Routes.CAMPUS.route) { CampusScreen(navController) }
        composable(Routes.DASHBOARD.route) { DashboardScreen(navController) }
        composable(Routes.FACE_CAPTURE.route) { FaceCaptureScreen(navController) }
        composable(Routes.REGISTERED_STUDENTS.route) { RegisteredStudentsScreen(navController) }
        composable(Routes.FACE_RECOGNITION.route) { FaceRecognitionScreen(navController) }

        // Updated CROWD_SENSE route with name and rollNumber arguments
        composable(
            route = "${Routes.CROWD_SENSE.route}/{name}/{rollNumber}/{hour}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("rollNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            val rollNumber = backStackEntry.arguments?.getString("rollNumber") ?: "Unknown"
            val hour = backStackEntry.arguments?.getString("hour") ?: "Unknown"
            CrowdSenseScreen(
                name = name, rollNumber = rollNumber,
                navController = navController,hour = hour
            )
        }

        composable(
            route = "${Routes.HOUR_SELECTOR.route}/{name}/{rollNumber}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("rollNumber") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: "Unknown"
            val rollNumber = backStackEntry.arguments?.getString("rollNumber") ?: "Unknown"
            HourSelectorScreen(navController = navController, name = name, rollNumber = rollNumber)
        }


        composable(Routes.DISPLAY_DETAILS_SCREEN.route) {
            DisplayDetailsScreen(context, lifecycleOwner)
        }

        composable(Routes.STUDENT_MANAGER.route) {
            StudentDataManagerScreen(context, navController)
        }

        composable(Routes.EDIT_FACE_SCREEN.route) {
            EditFaceScreen(navController)
        }

    }
}
