package com.example.campus.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun StudentDataManagerScreen(context: Context) {
    var studentList by remember { mutableStateOf(listOf<JSONObject>()) }
    var selectedRolls by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        studentList = loadStudentsFromJSON(context)
    }

    val filteredList = studentList.filter {
        it.getString("rollNumber").contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by Roll Number") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredList) { student ->
                val rollNumber = student.getString("rollNumber")
                val name = student.getString("name")
                val role = student.getString("role")
                val isSelected = selectedRolls.contains(rollNumber)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedRolls = if (isSelected) {
                                selectedRolls - rollNumber
                            } else {
                                selectedRolls + rollNumber
                            }
                        },
                    elevation = CardDefaults.cardElevation(4.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = {
                                selectedRolls = if (isSelected) {
                                    selectedRolls - rollNumber
                                } else {
                                    selectedRolls + rollNumber
                                }
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(text = "$name ($rollNumber)", style = MaterialTheme.typography.titleMedium)
                            Text(text = role, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (selectedRolls.isNotEmpty()) {
            Button(
                onClick = {
                    deleteStudentsByRollNumbers(context, selectedRolls)
                    selectedRolls.forEach { rollNumber ->
                        deleteStudentFromFirestoreWithFallback(context, rollNumber) {}
                    }
                    studentList = loadStudentsFromJSON(context)
                    selectedRolls = emptySet()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Delete Selected (${selectedRolls.size})")
            }
        }
    }
}

fun loadStudentsFromJSON(context: Context): List<JSONObject> {
    val file = File(context.filesDir, "registered_faces.json")
    if (!file.exists()) return emptyList()

    val jsonData = file.readText()
    val jsonObject = JSONObject(jsonData)
    val studentArray = jsonObject.optJSONArray("StudentDetails") ?: JSONArray()

    return List(studentArray.length()) { i -> studentArray.getJSONObject(i) }
}

fun deleteStudentsByRollNumbers(context: Context, rollNumbers: Set<String>) {
    val file = File(context.filesDir, "registered_faces.json")
    if (!file.exists()) return

    val jsonData = file.readText()
    val jsonObject = JSONObject(jsonData)
    val studentArray = jsonObject.optJSONArray("StudentDetails") ?: return

    val newArray = JSONArray()
    for (i in 0 until studentArray.length()) {
        val student = studentArray.getJSONObject(i)
        if (!rollNumbers.contains(student.getString("rollNumber"))) {
            newArray.put(student)
        }
    }

    jsonObject.put("StudentDetails", newArray)
    file.writeText(jsonObject.toString(4))
}


fun deleteStudentFromFirestoreWithFallback(
    context: Context,
    rollNumber: String,
    onComplete: (Boolean) -> Unit
) {
    val firestore = Firebase.firestore

    firestore.collection("students").document(rollNumber)
        .delete()
        .addOnSuccessListener {
            onComplete(true)
        }
        .addOnFailureListener { e ->
            e.printStackTrace()
            // Save to deleteData.json
            saveFailedDeletion(context, rollNumber)
            onComplete(false)
        }
}

private fun saveFailedDeletion(context: Context, rollNumber: String) {
    val file = File(context.filesDir, "deleteData.json")

    val jsonObject = if (file.exists()) {
        val existingData = file.readText()
        try {
            JSONObject(existingData)
        } catch (e: Exception) {
            JSONObject().put("rollNumbers", JSONArray())
        }
    } else {
        JSONObject().put("rollNumbers", JSONArray())
    }

    val rollArray = jsonObject.optJSONArray("rollNumbers") ?: JSONArray()

    // Avoid duplicates
    val alreadyExists = (0 until rollArray.length()).any {
        rollArray.getString(it) == rollNumber
    }

    if (!alreadyExists) {
        rollArray.put(rollNumber)
        jsonObject.put("rollNumbers", rollArray)
        file.writeText(jsonObject.toString(4))
    }
}



