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
        onComplete: (Boolean) -> Unit
    ) {
        onDownloading(true)

        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("students").get()
                .addOnSuccessListener { result ->
                    val studentArray = JSONArray()
                    for (document in result) {
                        val studentData = JSONObject()
                        studentData.put("name", document.getString("name"))
                        studentData.put("rollNumber", document.getString("rollNumber"))
                        studentData.put("embedding", JSONArray(document.get("embedding") as List<*>))
                        studentArray.put(studentData)
                    }

                    val jsonObject = JSONObject().apply {
                        put("StudentDetails", studentArray)
                    }

                    saveJSONToFile(context, jsonObject)
                    onDownloading(false)
                    onComplete(true)
                    Log.d("FirestoreDownloader", "Student data downloaded successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreDownloader", "Failed to download student data", e)
                    onDownloading(false)
                    onComplete(false)
                }
        }
    }

    private fun saveJSONToFile(context: Context, jsonObject: JSONObject) {
        val file = File(context.filesDir, "registered_faces.json")
        try {
            file.writeText(jsonObject.toString())
        } catch (e: Exception) {
            Log.e("FirestoreDownloader", "Error saving JSON file", e)
        }
    }
}
