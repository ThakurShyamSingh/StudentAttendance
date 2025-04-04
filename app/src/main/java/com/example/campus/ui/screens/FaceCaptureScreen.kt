package com.example.campus.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.navigation.NavController
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
import com.example.campus.util.CameraHelper
import com.example.campus.util.FaceNetHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileReader

@Composable
fun FaceCaptureScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner
    val previewView = remember { PreviewView(context) }

    val cameraHelper = remember { CameraHelper(context, lifecycleOwner, previewView) }
    val faceNetHelper = remember { FaceNetHelper(context) }

    var registrationMessage by remember { mutableStateOf("") }
    var studentName by remember { mutableStateOf("") }
    var studentRollNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("Student") }

    val coroutineScope = rememberCoroutineScope()

// Show Toast when message changes



    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                cameraHelper.setupCamera()
            } else {
                registrationMessage = "Camera permission denied"
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

        // BUTTON ROW: Back / Flip
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

        // CAMERA PREVIEW WITH GUIDE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
            )

            Canvas(modifier = Modifier.matchParentSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val faceOvalWidth = canvasWidth * 0.7f
                val faceOvalHeight = canvasHeight * 0.8f

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

                val eyeOffsetY = faceOvalHeight * 0.08f
                val eyeOffsetX = faceOvalWidth * 0.2f
                val eyeRadius = 8.dp.toPx()

                drawCircle(Color.White.copy(alpha = 0.8f), eyeRadius, Offset(centerX - eyeOffsetX, centerY - eyeOffsetY))
                drawCircle(Color.White.copy(alpha = 0.8f), eyeRadius, Offset(centerX + eyeOffsetX, centerY - eyeOffsetY))
                drawCircle(Color.Red.copy(alpha = 0.6f), 6.dp.toPx(), Offset(centerX, centerY + eyeOffsetY))

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

        // ROLE TOGGLE
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

        // TEXT INPUTS
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

        // ✅ CAPTURE FACE BUTTON WITH LOGIC
        Button(
            onClick = {
                if (studentName.isBlank() || studentRollNumber.isBlank()) {
                    Toast.makeText(
                        context,
                        "Please enter both name and roll number",
                        Toast.LENGTH_SHORT
                    ).show()//
                    return@Button
                }
                if (getStudentByRollNumber(context, studentRollNumber) == null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        cameraHelper.captureAndProcessFace { bitmap: Bitmap? ->
                            if (bitmap == null) {
                                Toast.makeText(context, "Failed to capture face", Toast.LENGTH_SHORT)
                                    .show()//
                                return@captureAndProcessFace
                            }

                            val embedding = faceNetHelper.generateEmbedding(bitmap)
                            if (embedding == null) {
                                Toast.makeText(
                                    context,
                                    "Failed to generate embedding",
                                    Toast.LENGTH_SHORT
                                ).show()//
                                return@captureAndProcessFace
                            }

                            saveEmbeddingToJSON(
                                context,
                                studentName,
                                studentRollNumber,
                                embedding,
                                selectedRole
                            )
                            Toast.makeText(context, "Face Successfully Registered", Toast.LENGTH_SHORT)
                                .show()//
                        }
                    }
                }else{
                    Toast.makeText(context, "Roll Number Already Registered", Toast.LENGTH_SHORT)
                        .show()//
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

//         FEEDBACK TEXT
//        if (registrationMessage.isNotEmpty()) {
//            Toast.makeText(context, registrationMessage, Toast.LENGTH_SHORT).show()//            Text(
////                text = registrationMessage,
////                color = if (registrationMessage.contains("success", true)) Color.Green else Color.Red,
////                fontSize = 18.sp,
////                modifier = Modifier
////                    .padding(top = 8.dp)
////                    .align(Alignment.CenterHorizontally)
////            )
//        }

    }
}


// Updated to include role
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

