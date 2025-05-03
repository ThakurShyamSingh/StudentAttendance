package com.example.campus.util

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object DataManipulator {

    private fun getFile(context: Context): File {
        return File(context.filesDir, "crowdsense.json")
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }


    fun saveData(context: Context, key: String, value: String, hour: String) {
        try {
            val file = getFile(context)

            val jsonObject = if (file.exists()) {
                JSONObject(file.readText())
            } else {
                JSONObject()
            }

            val date = getCurrentDate()
            val attendance = jsonObject.optJSONObject("attendance") ?: JSONObject()
            val dateEntry = attendance.optJSONObject(date) ?: JSONObject()

            if (dateEntry.has("hour")) {
                dateEntry.remove("hour")
                Log.d("DataManipulator", "Previous 'hour' entry removed from JSON")
            }

            val db = FirebaseFirestore.getInstance()
            db.collection("attendance")
                .document(date)
                .update(mapOf("hour" to FieldValue.delete()))
                .addOnSuccessListener {
                    Log.d("Firestore", "'hour' field successfully deleted from Firestore for date: $date")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error deleting 'hour' from Firestore", e)
                }

            // Add the key-value data inside the hour block
            val hourEntry = dateEntry.optJSONObject(hour) ?: JSONObject()
            hourEntry.put(key, value)
            dateEntry.put(hour, hourEntry)

            // Add the "hour": "10" field alongside the hour block
            dateEntry.put("hour", hour)

            // Update attendance and root object
            attendance.put(date, dateEntry)
            jsonObject.put("attendance", attendance)

            FileWriter(file).use { writer ->
                writer.write(jsonObject.toString(4))
            }

            Log.d("DataManipulator", "$key and hour saved to JSON: $value, hour: $hour")

        } catch (e: Exception) {
            Log.e("DataManipulator", "Error saving $key and hour to JSON", e)
        }
    }

    // Overload to handle Double values
    fun saveData(context: Context, key: String, value: Double, hour: String) {
        saveData(context, key, value.toString(), hour)
    }

    // Save WiFi SSID
    fun saveSSIDToJson(context: Context, ssid: String, hour: String) {
        saveData(context, "wifissid", ssid.trim('"'), hour)
    }


    // Save Bluetooth host code
    fun saveBluetoothCodeToJson(context: Context, code: String, hour: String) {
        saveData(context, "bluetoothhost", code, hour)
    }

    // Save Latitude and Longitude
    fun saveLatitudeLongitudeToJson(context: Context, lat: Double, lng: Double, hour: String) {
        saveData(context, "latitude", lat, hour)
        saveData(context, "longitude", lng, hour)
    }
}
