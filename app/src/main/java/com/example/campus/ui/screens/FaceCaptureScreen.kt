package com.example.campus.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
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
import com.airbnb.lottie.compose.*
import com.example.campus.util.CameraHelper
import com.example.campus.util.FaceNetHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

@Composable
fun FaceCaptureScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val previewView = remember { PreviewView(context) }

    val cameraHelper = remember { CameraHelper(context, lifecycleOwner, previewView) }
    val faceNetHelper = remember { FaceNetHelper(context) }

    var studentName by remember { mutableStateOf("") }
    var studentRollNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Student") }

    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                cameraHelper.setupCamera()
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { navController.popBackStack() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Back", color = Color.White)
            }

            Button(
                onClick = { cameraHelper.toggleCamera() },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Flip Camera", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Camera preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            )

            // Overlay for face guidance using Lottie animation
//            LottieFaceOverlay(context, fileName = "face_reading.json")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Student", "Faculty").forEach { role ->
                Button(
                    onClick = { selectedRole = role },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRole == role) Color.Black else Color.LightGray
                    )
                ) {
                    Text(
                        text = role,
                        color = if (selectedRole == role) Color.White else Color.DarkGray
                    )
                }
            }
        }

        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            label = { Text("Enter $selectedRole Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        OutlinedTextField(
            value = studentRollNumber,
            onValueChange = { studentRollNumber = it },
            label = { Text("Enter $selectedRole Roll Number") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Button(
            onClick = {
                if (studentName.isBlank() || studentRollNumber.isBlank()) {
                    Toast.makeText(
                        context,
                        "Please enter both name and roll number",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }
                if (getStudentByRollNumber(context, studentRollNumber) == null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val capturedImages = mutableListOf<Bitmap>()
                        cameraHelper.captureMultipleImages(
                            imageCount = 15,
                            onImageCaptured = { bitmap ->
                                if (bitmap != null) {
                                    capturedImages.add(bitmap)
                                } else {
                                    Log.e("FaceCaptureScreen", "Failed to capture image.")
                                }
                            },
                            onComplete = {
                                if (capturedImages.size == 15) {
                                    val embeddings = capturedImages.mapNotNull {
                                        faceNetHelper.generateEmbedding(it)
                                    }
                                    if (embeddings.size == 15) {
                                        val averagedEmbedding = averageEmbeddings(embeddings)
                                        saveEmbeddingToJSON(
                                            context,
                                            studentName,
                                            studentRollNumber,
                                            averagedEmbedding,
                                            selectedRole
                                        )
                                        Toast.makeText(
                                            context,
                                            "Face Successfully Registered",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to process all images",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                } else {
                    Toast.makeText(context, "Roll Number Already Registered", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Capture Face", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
fun LottieFaceOverlay(context: Context, fileName: String) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset(fileName)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        alignment = Alignment.Center
    )
}

fun saveEmbeddingToJSON(context: Context, name: String, rollNumber: String, embedding: FloatArray, role: String) {
    val file = File(context.filesDir, "registered_faces.json")

    val jsonObject: JSONObject = if (file.exists()) {
        val reader = FileReader(file)
        val existingData = reader.readText()
        reader.close()
        if (existingData.isNotEmpty()) JSONObject(existingData) else JSONObject()
    } else {
        JSONObject()
    }

    val studentArray = if (jsonObject.has("StudentDetails")) {
        jsonObject.getJSONArray("StudentDetails")
    } else {
        JSONArray()
    }

    val studentObject = JSONObject().apply {
        put("name", name)
        put("rollNumber", rollNumber)
        put("role", role)
        put("embedding", JSONArray(embedding.toList()))
    }

    studentArray.put(studentObject)
    jsonObject.put("StudentDetails", studentArray)

    val writer = FileWriter(file, false)
    writer.write(jsonObject.toString(4)) // Pretty-print JSON with indentation
    writer.flush()
    writer.close()
}

fun getStudentByRollNumber(context: Context, rollNumber: String): JSONObject? {
    val file = File(context.filesDir, "registered_faces.json")

    if (!file.exists()) return null

    val jsonData = file.readText()
    val jsonObject = JSONObject(jsonData)

    if (!jsonObject.has("StudentDetails")) return null

    val studentArray = jsonObject.getJSONArray("StudentDetails")

    for (i in 0 until studentArray.length()) {
        val student = studentArray.getJSONObject(i)
        if (student.getString("rollNumber") == rollNumber) {
            return student
        }
    }

    return null // Not found
}

fun averageEmbeddings(embeddings: List<FloatArray>): FloatArray {
    val embeddingSize = embeddings[0].size
    val averagedEmbedding = FloatArray(embeddingSize)

    for (embedding in embeddings) {
        for (i in embedding.indices) {
            averagedEmbedding[i] += embedding[i]
        }
    }

    for (i in averagedEmbedding.indices) {
        averagedEmbedding[i] /= embeddings.size
    }

    return averagedEmbedding
}