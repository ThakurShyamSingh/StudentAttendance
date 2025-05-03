package com.example.campus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Visibility
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
fun EditFaceButton(text: String, color: Color, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        enabled = enabled
    ) {
        Text(text, color = Color.White, fontSize = 18.sp)
    }
}

@Composable
fun EditFaceButton2(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
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
        bottomBar = { BottomNavigationBar2(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Edit Data", fontSize = 24.sp)
            Spacer(modifier = Modifier.height(20.dp))


            EditFaceButton2("Check Data", Color.Blue,Modifier.fillMaxWidth().height(150.dp)) {
                navController.navigate("DisplayDetailsScreen")
            }

            Spacer(modifier = Modifier.height(40.dp))

            EditFaceButton("Upload Face Data", Color.Black, enabled = !isUploading, Modifier.fillMaxWidth().height(150.dp)) {
                FirestoreUploader.uploadJSONToFirestore(
                    context,
                    onUploading = { isUploading = it },
                    onProgress = {},
                    onUploadComplete = { isUploading = false }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (isUploading) {
                LoadingIndicator("Uploading...")
            }

            EditFaceButton("Download Face Data", Color.Gray, enabled = !isDownloading, Modifier.fillMaxWidth().height(150.dp)) {
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

@Composable
fun BottomNavigationBar2(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = false,
            onClick = { navController.navigate("dashboard") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Edit, contentDescription = "Manage Attendance") },
            label = { Text("Manage") },
            selected = true,
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

