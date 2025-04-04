package com.example.campus.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
import org.json.JSONObject
import java.io.File
//import androidx.compose.animation.AnimatedVisibility


private const val TAG = "FaceRecognitionScreen"

@Composable
fun FaceRecognitionScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val previewView = remember { PreviewView(context) }
    val cameraHelper = remember { CameraHelper(context, lifecycleOwner, previewView) }
    val faceNetHelper = remember { FaceNetHelper(context) }

    var recognitionMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            Log.d(TAG, "Camera permission result: $granted")
            if (granted) {
                Log.d(TAG, "Setting up camera")
                cameraHelper.setupCamera()
            } else {
                recognitionMessage = "Camera permission denied"
                Toast.makeText(context, recognitionMessage, Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Camera permission denied")
            }
        }
    )

    LaunchedEffect(Unit) {
        Log.d(TAG, "Launching camera permission request")
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(
                onClick = {
                    Log.d(TAG, "Back button clicked")
                    navController.popBackStack()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Back", color = Color.White)
            }
        }

        // Camera Preview with Face Guide Overlay
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Face Guide Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val faceOvalWidth = canvasWidth * 0.7f
                val faceOvalHeight = canvasHeight * 0.6f

                val left = (canvasWidth - faceOvalWidth) / 2f
                val top = (canvasHeight - faceOvalHeight) / 2f

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

        // Scan Face Button with updated logic
        Button(
            onClick = {
                Log.d(TAG, "Scan Face button clicked")
                coroutineScope.launch(Dispatchers.IO) {
                    Log.d(TAG, "Capturing face...")
                    cameraHelper.captureAndProcessFace { bitmap: Bitmap? ->
                        if (bitmap == null) {
                            recognitionMessage = "Failed to capture face"
                            Log.d(TAG, "Face capture failed")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, recognitionMessage, Toast.LENGTH_SHORT).show()
                            }
                            return@captureAndProcessFace
                        }

                        Log.d(TAG, "Generating embedding...")
                        val embedding = faceNetHelper.generateEmbedding(bitmap)
                        if (embedding == null) {
                            recognitionMessage = "Failed to generate embedding"
                            Log.d(TAG, "Embedding generation failed")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, recognitionMessage, Toast.LENGTH_SHORT).show()
                            }
                            return@captureAndProcessFace
                        }

                        Log.d(TAG, "Recognising face...")
                        val recognizedData = faceNetHelper.recognizeFace(embedding)

                        if (recognizedData != null) {
                            val (_, rollNumber) = recognizedData
                            Log.d(TAG, "Face recognized, roll number: $rollNumber")
                            // Look up student details from the local JSON file.
                            val student = getStudentsByRollNumber(context, rollNumber)

                            if (student == null) {
                                Log.d(TAG, "Student not found in local JSON")
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(context, "Face Not Recognised", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val role = student.optString("role", "Student")
                                Log.d(TAG, "Student found with role: $role")
                                if (role != "Faculty" && role != "Admin") {
                                    Log.d(TAG, "Role not permitted")
                                    Handler(Looper.getMainLooper()).post {
                                        Toast.makeText(context, "Not Admin", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Log.d(TAG, "Role permitted, navigating to crowd_sense_screen")
                                    Handler(Looper.getMainLooper()).post {
                                        val name = student.optString("name", "Unknown")
                                        navController.navigate("crowd_sense_screen/${name}/${rollNumber}")

                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "Face recognition did not match any known face")
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "Face Not Recognised", Toast.LENGTH_SHORT).show()
                            }
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



    }
}

/**
 * Retrieves student data from 'registered_faces.json' using the roll number.
 */
fun getStudentsByRollNumber(context: Context, rollNumber: String): JSONObject? {
    Log.d(TAG, "Looking up student with roll number: $rollNumber")
    val file = File(context.filesDir, "registered_faces.json")
    if (!file.exists()) {
        Log.d(TAG, "registered_faces.json not found")
        return null
    }

    val jsonData = file.readText()
    val jsonObject = JSONObject(jsonData)
    if (!jsonObject.has("StudentDetails")) {
        Log.d(TAG, "StudentDetails key not found in JSON")
        return null
    }

    val studentArray = jsonObject.getJSONArray("StudentDetails")
    for (i in 0 until studentArray.length()) {
        val student = studentArray.getJSONObject(i)
        if (student.getString("rollNumber") == rollNumber) {
            Log.d(TAG, "Student found: $student")
            return student
        }
    }
    Log.d(TAG, "No matching student found")
    return null
}
