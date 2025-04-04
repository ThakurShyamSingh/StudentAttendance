package com.example.campus.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
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

            // Face Guide Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val faceOvalWidth = canvasWidth * 0.7f
                val faceOvalHeight = canvasHeight * 0.6f

                val left = (canvasWidth - faceOvalWidth) / 2f
                val top = (canvasHeight - faceOvalHeight) / 2f

                // Draw face oval
                drawOval(
                    color = Color.White.copy(alpha = 0.3f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(faceOvalWidth, faceOvalHeight),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )

                val centerX = canvasWidth / 2f
                val centerY = canvasHeight / 2f

                // Eyes
                val eyeOffsetY = faceOvalHeight * 0.08f
                val eyeOffsetX = faceOvalWidth * 0.2f
                val eyeRadius = 8.dp.toPx()

                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = eyeRadius,
                    center = Offset(centerX - eyeOffsetX, centerY - eyeOffsetY)
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = eyeRadius,
                    center = Offset(centerX + eyeOffsetX, centerY - eyeOffsetY)
                )

                // Nose
                drawCircle(
                    color = Color.Red.copy(alpha = 0.6f),
                    radius = 6.dp.toPx(),
                    center = Offset(centerX, centerY + eyeOffsetY)
                )

                // Mouth
                val mouthY = centerY + faceOvalHeight * 0.25f
                val mouthWidth = faceOvalWidth * 0.2f

                drawLine(
                    color = Color.Green.copy(alpha = 0.6f),
                    start = Offset(centerX - mouthWidth / 2f, mouthY),
                    end = Offset(centerX + mouthWidth / 2f, mouthY),
                    strokeWidth = 4.dp.toPx()
                )
            }
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
