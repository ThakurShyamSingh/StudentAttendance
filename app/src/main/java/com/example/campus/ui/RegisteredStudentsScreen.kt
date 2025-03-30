package com.example.campus.ui

import androidx.core.net.toUri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.io.File

@Composable
fun RegisteredStudentsScreen(navController: NavController) {
    val context = LocalContext.current
    val registeredStudents = remember { mutableStateOf(loadRegisteredStudentsFromCSV(context)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Registered Students", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))

        LazyColumn {
            items(registeredStudents.value) { student ->
                StudentCard(student)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.popBackStack() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back", fontSize = 18.sp)
        }
    }
}

@Composable
fun StudentCard(student: Student) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            student.bitmap?.let {
                Image(bitmap = it.asImageBitmap(), contentDescription = "Student Face", modifier = Modifier.size(64.dp))
            } ?: Text("No Image", modifier = Modifier.size(64.dp))

            Spacer(modifier = Modifier.width(16.dp))

            Text(student.name, fontSize = 18.sp)
        }
    }
}

data class Student(val name: String, val bitmap: Bitmap?)

private fun loadRegisteredStudentsFromCSV(context: Context): List<Student> {
    val students = mutableListOf<Student>()
    val csvFile = File(context.filesDir, "registered_faces.csv")

    if (!csvFile.exists()) return students

    csvFile.forEachLine { line ->
        val data = line.split(",")
        if (data.size >= 2) {
            val name = data[0]
            val imagePath = data[1]

            val bitmap = loadBitmapFromUri(context, imagePath)

            students.add(Student(name, bitmap))
        }
    }
    return students
}

private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
    return try {
        val uri = uriString.toUri()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        Log.e("RegisteredStudents", "Error loading image: $uriString", e)
        null
    }
}
