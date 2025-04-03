package com.example.campus.ui.screens

import android.Manifest
import android.graphics.Bitmap
//import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.campus.util.BluetoothHelper
import com.example.campus.util.CameraHelper
import com.example.campus.util.FaceNetHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun FaceRecognitionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val previewView = remember { PreviewView(context) }
    val cameraHelper = remember { CameraHelper(context, lifecycleOwner, previewView) }
    val faceNetHelper = remember { FaceNetHelper(context) }
//    val bluetoothHelper = remember { BluetoothHelper(context) }

    var recognitionMessage by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var hostrollnumber by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                cameraHelper.setupCamera()
            } else {
                recognitionMessage = "Camera permission denied"
            }
        }
    )

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(
                onClick = {
                    navController.popBackStack()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Back", color = Color.White)
            }
        }

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch(Dispatchers.IO) {
                    cameraHelper.captureAndProcessFace { bitmap: Bitmap? ->
                        if (bitmap == null) {
                            recognitionMessage = "Failed to capture face"
                            return@captureAndProcessFace
                        }

                        val embedding = faceNetHelper.generateEmbedding(bitmap)
                        if (embedding == null) {
                            recognitionMessage = "Failed to generate embedding"
                            return@captureAndProcessFace
                        }

                        val recognizedData = faceNetHelper.recognizeFace(embedding)

                        if (recognizedData != null) {
                            val (name, rollNumber) = recognizedData
                            hostname = name
                            hostrollnumber = rollNumber
                            recognitionMessage = "Verified: $name ($rollNumber)"

                            // Send data back to CrowdSense screen
                            navController.previousBackStackEntry?.savedStateHandle?.set(
                                "recognized_student",
                                Pair(name, rollNumber)
                            )
                            navController.popBackStack()
                        } else {
                            recognitionMessage = "Face Not Recognized"
                        }
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Scan Face", color = Color.White, fontSize = 18.sp)
        }

        if (recognitionMessage.isNotEmpty()) {
            Text(
                text = recognitionMessage,
                color = if (recognitionMessage.contains("Verified", true)) Color.Green else Color.Red,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
