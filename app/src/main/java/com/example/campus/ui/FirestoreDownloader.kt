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
import kotlin.collections.iterator

object FirestoreDownloader {

    private fun getFirestore() = Firebase.firestore

    fun downloadJSONFromFirestore(
        context: Context,
        onProgress: (Float) -> Unit,
        onDownloading: (Boolean) -> Unit,
        onDownloadComplete: (Boolean) -> Unit
    ) {
        onDownloading(true)

        CoroutineScope(Dispatchers.IO).launch {
            var completedTasks = 0

            downloadRegisteredFaces(context, onProgress) { success ->
                if (success) completedTasks++
                checkCompletion(onDownloading, onDownloadComplete, completedTasks)
            }

            downloadCrowdSenseData(context) { success ->
                if (success) completedTasks++
                checkCompletion(onDownloading, onDownloadComplete, completedTasks)
            }

            downloadStudentAttendance(context) { success ->
                if (success) completedTasks++
                checkCompletion(onDownloading, onDownloadComplete, completedTasks)
            }
        }
    }

    private fun downloadRegisteredFaces(
        context: Context,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        getFirestore().collection("students")
            .get()
            .addOnSuccessListener { snapshot ->
                val studentArray = JSONArray()
                val totalStudents = snapshot.size()
                var processed = 0

                for (doc in snapshot.documents) {
                    val studentData = JSONObject()
                    studentData.put("name", doc.getString("name"))
                    studentData.put("rollNumber", doc.getString("rollNumber"))
                    studentData.put("role", doc.getString("role"))

                    val embeddingList = doc.get("embedding")
                    studentData.put("embedding", JSONArray(embeddingList as? List<*> ?: listOf<Any>()))

                    studentArray.put(studentData)
                    processed++
                    onProgress(processed.toFloat() / totalStudents)
                }

                val jsonObject = JSONObject().apply {
                    put("StudentDetails", studentArray)
                }

                val file = File(context.filesDir, "registered_faces.json")
                file.writeText(jsonObject.toString(4))
                Log.d("FirestoreDownloader", "Downloaded registered_faces.json successfully")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDownloader", "Failed to download registered_faces.json", e)
                onComplete(false)
            }
    }

    private fun downloadCrowdSenseData(
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        getFirestore().collection("attendance")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val attendanceObject = JSONObject()

                    for (doc in snapshot.documents) {
                        val date = doc.id
                        val dateDataMap = doc.data ?: continue
                        val timeSlotObject = JSONObject()

                        for ((time, value) in dateDataMap) {
                            if (value is Map<*, *>) {
                                timeSlotObject.put(time, JSONObject(value as Map<*, *>))
                            } else {
                                timeSlotObject.put(time, value)
                            }
                        }

                        attendanceObject.put(date, timeSlotObject)
                    }

                    val finalJson = JSONObject().apply {
                        put("attendance", attendanceObject)
                    }

                    val file = File(context.filesDir, "crowdsense.json")
                    file.writeText(finalJson.toString(4))
                    Log.d("FirestoreDownloader", "Downloaded crowdsense.json successfully")
                    onComplete(true)

                } catch (e: Exception) {
                    Log.e("FirestoreDownloader", "Error processing attendance data", e)
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDownloader", "Failed to download crowdsense.json", e)
                onComplete(false)
            }
    }

    private fun downloadStudentAttendance(
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        getFirestore().collection("StudentAttendance")
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val studentAttendance = JSONObject()

                    for (doc in snapshot.documents) {
                        val dateKey = doc.id
                        val dataMap = doc.data ?: continue

                        val hourObject = JSONObject()

                        for ((hourKey, value) in dataMap) {
                            if (value is Map<*, *>) {
                                val hourData = JSONObject()
                                for ((rollKey, present) in value) {
                                    if (rollKey != null && present is Boolean) {
                                        hourData.put(rollKey.toString(), present)
                                    }
                                }
                                hourObject.put(hourKey, hourData)
                            }
                        }

                        studentAttendance.put(dateKey, hourObject)
                    }

                    val fullJson = JSONObject().apply {
                        put("StudentAttendance", studentAttendance)
                    }

                    val file = File(context.filesDir, "attendance.json")
                    file.writeText(fullJson.toString(4))
                    Log.d("FirestoreDownloader", "Downloaded attendance.json successfully")
                    onComplete(true)
                } catch (e: Exception) {
                    Log.e("FirestoreDownloader", "Error processing attendance.json", e)
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDownloader", "Failed to download attendance.json", e)
                onComplete(false)
            }
    }

    private fun checkCompletion(
        onDownloading: (Boolean) -> Unit,
        onComplete: (Boolean) -> Unit,
        completedTasks: Int
    ) {
        if (completedTasks == 3) {
            onDownloading(false)
            onComplete(true)
        }
    }
}
