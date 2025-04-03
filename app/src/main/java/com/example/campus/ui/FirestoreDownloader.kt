package com.example.campus.ui

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FirestoreDownloader {
    private val firestore get() = Firebase.firestore

    fun downloadJSONFromFirestore(
        context: Context,
        onDownloading: (Boolean) -> Unit,
        onDownloadComplete: (Boolean) -> Unit
    ) {
        onDownloading(true)

        CoroutineScope(Dispatchers.IO).launch {
            val totalTasks = 2 // Two files to download
            var completedTasks = 0

            if (downloadRegisteredFaces(context)) {
                completedTasks++
            }
            if (downloadCrowdSenseData(context)) {
                completedTasks++
            }

            onDownloading(false)
            if (completedTasks == totalTasks) {
                onDownloadComplete(true)
                Log.d("FirestoreDownloader", "All data downloaded successfully!")
            } else {
                onDownloadComplete(false)
                Log.e("FirestoreDownloader", "Some data failed to download.")
            }
        }
    }

    private fun downloadRegisteredFaces(context: Context): Boolean {
        return try {
            firestore.collection("students").get()
                .addOnSuccessListener { result ->
                    val studentArray = JSONArray()
                    for (document in result) {
                        val studentData = JSONObject().apply {
                            put("name", document.getString("name"))
                            put("rollNumber", document.getString("rollNumber"))
                            put("embedding", JSONArray(document.get("embedding") as List<*>))
                        }
                        studentArray.put(studentData)
                    }

                    val jsonObject = JSONObject().apply {
                        put("StudentDetails", studentArray)
                    }

                    saveJSONToFile(context, "registered_faces.json", jsonObject)
                    Log.d("FirestoreDownloader", "Student data downloaded successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDownloader", "Failed to download student data", e)
                }
            true
        } catch (e: Exception) {
            Log.e("FirestoreDownloader", "Error downloading registered_faces.json", e)
            false
        }
    }

    private fun downloadCrowdSenseData(context: Context): Boolean {
        return try {
            firestore.collection("attendance").get()
                .addOnSuccessListener { result ->
                    val attendanceData = JSONObject()

                    for (document in result.documents) {
                        val date = document.id
                        val dateObject = JSONObject(document.data ?: emptyMap<String, Any>())

                        attendanceData.put(date, dateObject)
                    }

                    val jsonObject = JSONObject().apply {
                        put("attendance", attendanceData)
                    }

                    saveJSONToFile(context, "crowdsense.json", jsonObject)
                    Log.d("FirestoreDownloader", "Attendance data downloaded successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDownloader", "Failed to download attendance data", e)
                }
            true
        } catch (e: Exception) {
            Log.e("FirestoreDownloader", "Error downloading crowdsense.json", e)
            false
        }
    }

    private fun saveJSONToFile(context: Context, fileName: String, jsonObject: JSONObject) {
        val file = File(context.filesDir, fileName)
        try {
            file.writeText(jsonObject.toString(4)) // Pretty print with indentation
        } catch (e: Exception) {
            Log.e("FirestoreDownloader", "Error saving $fileName", e)
        }
    }
}
