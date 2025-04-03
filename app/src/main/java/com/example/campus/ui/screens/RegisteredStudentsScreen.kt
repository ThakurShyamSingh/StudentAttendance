package com.example.campus.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.json.JSONObject
import java.io.File

@Composable
fun RegisteredStudentsScreen(navController: NavController) {
    val context = LocalContext.current
    val registeredStudents = remember { mutableStateOf(loadRegisteredStudentsFromJSON(context)) }

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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Name: ${student.name}", fontSize = 18.sp)
            Text("Roll Number: ${student.rollNumber}", fontSize = 16.sp)
        }
    }
}

data class Student(val name: String, val rollNumber: String, val embedding: List<Float>)

private fun loadRegisteredStudentsFromJSON(context: Context): List<Student> {
    val students = mutableListOf<Student>()
    val jsonFile = File(context.filesDir, "registered_faces.json")

    if (!jsonFile.exists()) return students

    return try {
        val jsonData = jsonFile.readText()
        val jsonObject = JSONObject(jsonData)
        val studentArray = jsonObject.getJSONArray("StudentDetails")

        for (i in 0 until studentArray.length()) {
            val studentObj = studentArray.getJSONObject(i)
            val name = studentObj.getString("name")
            val rollNumber = studentObj.getString("rollNumber")
            val embeddingArray = studentObj.getJSONArray("embedding")
            val embedding = List(embeddingArray.length()) { embeddingArray.getDouble(it).toFloat() }

            students.add(Student(name, rollNumber, embedding))
        }
        students
    } catch (e: Exception) {
        Log.e("RegisteredStudents", "Error reading JSON", e)
        students
    }
}