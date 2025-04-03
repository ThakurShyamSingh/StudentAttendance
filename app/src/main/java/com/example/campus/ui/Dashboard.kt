package com.example.campus.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun DashboardScreen(navController: NavController) {
    var isUploading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadComplete by remember { mutableStateOf(false) }
    var uploadComplete by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Handle upload completion toast only once
    LaunchedEffect(uploadComplete) {
        if (uploadComplete) {
            Toast.makeText(context, "Upload Complete!", Toast.LENGTH_SHORT).show()
            uploadComplete = false // Reset state to prevent re-triggering on recomposition
        }
    }

    // Handle download completion toast only once
    LaunchedEffect(downloadComplete) {
        if (downloadComplete) {
            Toast.makeText(context, "Download Complete!", Toast.LENGTH_SHORT).show()
            downloadComplete = false // Reset state to prevent re-triggering on recomposition
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Welcome to Campus", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        DashboardButton("Capture Face", Color.Black) {
            navController.navigate("face_capture")
        }

        DashboardButton("View Registered Students", Color.Gray) {
            navController.navigate("registered_students")
        }

        DashboardButton("Verify", Color.Blue) {
            navController.navigate("face_recognition")
        }

        DashboardButton("Upload Face Data", Color.Green, enabled = !isUploading) {
            FirestoreUploader.uploadJSONToFirestore(
                context,
                onUploading = { isUploading = it },
                onProgress = {},
                onUploadComplete = { uploadComplete = it }
            )
        }

        if (isUploading) {
            LoadingIndicator("Uploading...")
        }

        DashboardButton("Download Face Data", Color.Red, enabled = !isDownloading) {
            FirestoreDownloader.downloadJSONFromFirestore(
                context,
                onDownloading = { isDownloading = it },
                onDownloadComplete = { downloadComplete = it }
            )
        }

        if (isDownloading) {
            LoadingIndicator("Downloading...")
        }

        DashboardButton("Take Attendance", Color.Cyan) {
            navController.navigate("crowd_sense_screen")
        }

        DashboardButton("CHECK DATA", Color(0xFF9C27B0)) {
            navController.navigate("DisplayDetailsScreen")
        }
    }
}

@Composable
fun DashboardButton(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        enabled = enabled
    ) {
        Text(text, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
fun LoadingIndicator(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(10.dp))
        CircularProgressIndicator(modifier = Modifier.size(24.dp))
        Text(message, fontSize = 16.sp)
    }
}
