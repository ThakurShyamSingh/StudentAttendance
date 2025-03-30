package com.example.campus.ui

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

object FirestoreUploader {
    private val firestore = Firebase.firestore

    fun uploadJSONToFirestore(
        context: Context,
        onProgress: (Float) -> Unit,
        onUploading: (Boolean) -> Unit
    ) {
        val file = File(context.filesDir, "registered_faces.json")

        if (!file.exists()) {
            Log.e("FirestoreUploader", "JSON file not found!")
            return
        }

        val jsonData = try {
            file.reader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("FirestoreUploader", "Error reading JSON file", e)
            return
        }

        try {
            val jsonObject = JSONObject(jsonData)
            if (!jsonObject.has("StudentDetails")) {
                Log.e("FirestoreUploader", "Missing StudentDetails in JSON")
                return
            }

            val studentArray = jsonObject.getJSONArray("StudentDetails")
            val totalStudents = studentArray.length()
            var uploadedCount = 0

            onUploading(true)

            CoroutineScope(Dispatchers.IO).launch {
                for (i in 0 until totalStudents) {
                    val student = studentArray.getJSONObject(i)
                    val rollNumber = student.getString("rollNumber")

                    val embeddingArray = student.getJSONArray("embedding")
                    val embeddingList = List(embeddingArray.length()) { j ->
                        embeddingArray.getDouble(j).toFloat()
                    }

                    val studentData = mapOf(
                        "name" to student.getString("name"),
                        "rollNumber" to rollNumber,
                        "embedding" to embeddingList
                    )

                    firestore.collection("students").document(rollNumber)
                        .set(studentData)
                        .addOnSuccessListener {
                            uploadedCount++
                            onProgress(uploadedCount.toFloat() / totalStudents)

                            if (uploadedCount == totalStudents) {
                                onUploading(false)
                                Log.d("FirestoreUploader", "All students uploaded successfully!")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirestoreUploader", "Failed to upload $rollNumber", e)
                            onUploading(false)
                        }
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreUploader", "Error parsing JSON", e)
            onUploading(false)
        }
    }
}
