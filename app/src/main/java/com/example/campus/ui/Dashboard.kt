package com.example.campus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController


@Composable
fun DashboardScreen(navController: NavController) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Welcome to Campus", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(20.dp))

            DashboardButton("Register Students", Color.Black, Modifier.fillMaxWidth().height(100.dp)) {
                navController.navigate("face_capture")
            }

            DashboardButton("Verify", Color.Blue, Modifier.fillMaxWidth().height(100.dp)) {
                navController.navigate("face_recognition")
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = true,
            onClick = { navController.navigate("dashboard") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Edit, contentDescription = "Manage Attendance") },
            label = { Text("Manage") },
            selected = false,
            onClick = { navController.navigate("edit_face_screen") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Visibility, contentDescription = "View Attendance") },
            label = { Text("View") },
            selected = false,
            onClick = { navController.navigate("display_attendance") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Face, contentDescription = "Edit Face Data") },
            label = { Text("Edit Faces") },
            selected = false,
            onClick = { navController.navigate("student_manager") }
        )
    }
}

@Composable
fun DashboardButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 18.sp)
    }
}
