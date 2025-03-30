package com.example.campus.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun DashboardScreen(navController: NavController, context: Context) {
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Campus", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { navController.navigate("face_capture") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Capture Face", color = Color.White, fontSize = 18.sp)
        }

        Button(
            onClick = { navController.navigate("registered_students") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("View Registered Students", color = Color.White, fontSize = 18.sp)
        }

        Button(
            onClick = { navController.navigate("face_recognition") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
        ) {
            Text("Verify", color = Color.White, fontSize = 18.sp)
        }

        Button(
            onClick = {
                FirestoreUploader.uploadJSONToFirestore(
                    context,
                    onProgress = { progress -> uploadProgress = progress },
                    onUploading = { isUploading = it }
                )
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
            enabled = !isUploading
        ) {
            Text("Upload Face Data", color = Color.White, fontSize = 18.sp)
        }

        if (isUploading) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
            Text("Uploading... ${ (uploadProgress * 100).toInt() }%", fontSize = 16.sp)
        }
    }
}
