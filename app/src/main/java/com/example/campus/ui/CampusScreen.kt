package com.example.campus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material3.MaterialTheme

@Composable
fun CampusScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Dynamic theme background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp) // Increased padding
        ) {
            Text(
                text = "Welcome to Campus App",
                fontSize = 26.sp, // Slightly larger font
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground // Dynamic text color
            )
            Spacer(modifier = Modifier.height(20.dp)) // More spacing for better UI
            Button(
                onClick = {
                    if (navController.currentDestination?.route != "dashboard") {
                        navController.navigate("dashboard") // Prevents duplicate navigation
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Get Started", color = MaterialTheme.colorScheme.onPrimary, fontSize = 18.sp)
            }
        }
    }
}
