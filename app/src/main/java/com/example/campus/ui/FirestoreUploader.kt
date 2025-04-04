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
        onUploading: (Boolean) -> Unit,
        onUploadComplete: (Boolean) -> Unit // Add this parameter
    ) {
        onUploading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val totalTasks = 2 // Two files to upload
            var completedTasks = 0

            uploadRegisteredFaces(context, onProgress) { success ->
                if (success) completedTasks++
                checkCompletion(onUploading, onUploadComplete, completedTasks, totalTasks)
            }

            uploadCrowdSenseData(context) { success ->
                if (success) completedTasks++
                checkCompletion(onUploading, onUploadComplete, completedTasks, totalTasks)
            }
        }
    }

    private fun uploadRegisteredFaces(
        context: Context,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        val file = File(context.filesDir, "registered_faces.json")

        if (!file.exists()) {
            Log.e("FirestoreUploader", "registered_faces.json not found!")
            onComplete(false)
            return
        }

        val jsonData = file.readText()

        try {
            val jsonObject = JSONObject(jsonData)
            if (!jsonObject.has("StudentDetails")) {
                Log.e("FirestoreUploader", "Missing StudentDetails in JSON")
                onComplete(false)
                return
            }

            val studentArray = jsonObject.getJSONArray("StudentDetails")
            val totalStudents = studentArray.length()
            var uploadedCount = 0

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
                    "role" to student.getString("role"),
                    "embedding" to embeddingList
                )


                firestore.collection("students").document(rollNumber)
                    .set(studentData)
                    .addOnSuccessListener {
                        uploadedCount++
                        onProgress(uploadedCount.toFloat() / totalStudents)

                        if (uploadedCount == totalStudents) {
                            onComplete(true)
                            Log.d("FirestoreUploader", "All students uploaded successfully!")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreUploader", "Failed to upload $rollNumber", e)
                        onComplete(false)
                    }
            }
        } catch (e: Exception) {
            Log.e("FirestoreUploader", "Error parsing registered_faces.json", e)
            onComplete(false)
        }
    }

    private fun uploadCrowdSenseData(context: Context, onComplete: (Boolean) -> Unit) {
        val file = File(context.filesDir, "crowdsense.json")

        if (!file.exists()) {
            Log.e("FirestoreUploader", "crowdsense.json not found!")
            onComplete(false)
            return
        }

        val jsonData = file.readText()

        try {
            val jsonObject = JSONObject(jsonData)
            if (!jsonObject.has("attendance")) {
                Log.e("FirestoreUploader", "Missing attendance data in JSON")
                onComplete(false)
                return
            }

            val attendanceObject = jsonObject.getJSONObject("attendance")
            val totalDates = attendanceObject.length()
            var uploadedDates = 0

            val keys = attendanceObject.keys()
            while (keys.hasNext()) {
                val date = keys.next()
                val dateData = attendanceObject.getJSONObject(date)

                firestore.collection("attendance").document(date)
                    .set(dateData.toMap())
                    .addOnSuccessListener {
                        uploadedDates++
                        if (uploadedDates == totalDates) {
                            onComplete(true)
                            Log.d("FirestoreUploader", "All attendance data uploaded successfully!")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreUploader", "Failed to upload attendance for $date", e)
                        onComplete(false)
                    }
            }
        } catch (e: Exception) {
            Log.e("FirestoreUploader", "Error parsing crowdsense.json", e)
            onComplete(false)
        }
    }

    private fun checkCompletion(
        onUploading: (Boolean) -> Unit,
        onComplete: (Boolean) -> Unit,
        completedTasks: Int,
        totalTasks: Int
    ) {
        if (completedTasks == totalTasks) {
            onUploading(false)
            onComplete(true)
        } else {
            onComplete(false)
        }
    }

    private fun JSONObject.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = this.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = when (val value = this.get(key)) {
                is JSONObject -> value.toMap()
                else -> value
            }
        }
        return map
    }
}
