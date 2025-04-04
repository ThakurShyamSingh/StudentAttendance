package com.example.campus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.campus.ui.BottomNavigationBar
import com.example.campus.ui.FirestoreDownloader
import com.example.campus.ui.FirestoreUploader

@Composable
fun EditFaceButton(text: String, color: Color, enabled: Boolean = true, onClick: () -> Unit) {
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

@Composable
fun EditFaceScreen(navController: NavController) {
    var isUploading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Edit Face Data", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(20.dp))

            EditFaceButton("View Registered Students", Color.Gray) {
                navController.navigate("student_manager")
            }

            EditFaceButton("Check Data", Color(0xFF9C27B0)) {
                navController.navigate("DisplayDetailsScreen")
            }

            EditFaceButton("Upload Face Data", Color.Green, enabled = !isUploading) {
                FirestoreUploader.uploadJSONToFirestore(
                    context,
                    onUploading = { isUploading = it },
                    onProgress = {},
                    onUploadComplete = { isUploading = false }
                )
            }

            if (isUploading) {
                LoadingIndicator("Uploading...")
            }

            EditFaceButton("Download Face Data", Color.Red, enabled = !isDownloading) {
                FirestoreDownloader.downloadJSONFromFirestore(
                    context,
                    onDownloading = { isDownloading = it },
                    onDownloadComplete = { isDownloading = false },
                    onProgress = {}
                )
            }

            if (isDownloading) {
                LoadingIndicator("Downloading...")
            }
        }
    }
}
