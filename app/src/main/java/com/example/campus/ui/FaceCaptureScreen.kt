package com.example.campus.ui

import android.Manifest
import android.graphics.Bitmap
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
import com.example.campus.util.CameraHelper
import com.example.campus.util.FaceNetHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
//import java.io.BufferedWriter
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
    val coroutineScope = rememberCoroutineScope()

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

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(
                onClick = { navController.popBackStack() },
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

        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            label = { Text("Enter Student Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = studentRollNumber,
            onValueChange = { studentRollNumber = it },
            label = { Text("Enter Student Roll Number") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (studentName.isBlank() || studentRollNumber.isBlank()) {
                    registrationMessage = "Please enter both name and roll number"
                    return@Button
                }

                coroutineScope.launch(Dispatchers.IO) {
                    cameraHelper.captureAndProcessFace { bitmap: Bitmap? ->
                        if (bitmap == null) {
                            registrationMessage = "Failed to capture face"
                            return@captureAndProcessFace
                        }

                        val embedding = faceNetHelper.generateEmbedding(bitmap)
                        if (embedding == null) {
                            registrationMessage = "Failed to generate embedding"
                            return@captureAndProcessFace
                        }

                        saveEmbeddingToJSON(context, studentName, studentRollNumber, embedding)
                        registrationMessage = "Face Successfully Registered"
                    }
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

        if (registrationMessage.isNotEmpty()) {
            Text(
                text = registrationMessage,
                color = if (registrationMessage.contains("success", true)) Color.Green else Color.Red,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}




fun saveEmbeddingToJSON(context: android.content.Context, name: String, rollNumber: String, embedding: FloatArray) {
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
        put("embedding", JSONArray(embedding.toList()))
    }

    studentArray.put(studentObject)
    jsonObject.put("StudentDetails", studentArray)

    val writer = FileWriter(file, false)
    writer.write(jsonObject.toString(4)) // Pretty-print JSON with indentation
    writer.flush()
    writer.close()
}
