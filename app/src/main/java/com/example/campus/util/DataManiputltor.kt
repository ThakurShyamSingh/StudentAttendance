package com.example.campus.util

import android.content.Context
import android.util.Log
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

    private fun getCurrentHour(): String {
        val timeFormat = SimpleDateFormat("HH-mm", Locale.getDefault())
        return timeFormat.format(Date())
    }

    fun saveData(context: Context, key: String, value: String) {
        try {
            val file = getFile(context)

            val jsonObject = if (file.exists()) {
                JSONObject(file.readText())
            } else {
                JSONObject()
            }

            val date = getCurrentDate()
            val time = getCurrentHour()

            val attendance = jsonObject.optJSONObject("attendance") ?: JSONObject()
            val dateEntry = attendance.optJSONObject(date) ?: JSONObject()
            val hourEntry = dateEntry.optJSONObject(time) ?: JSONObject()

            hourEntry.put(key, value)
            dateEntry.put(time, hourEntry)
            attendance.put(date, dateEntry)
            jsonObject.put("attendance", attendance)

            FileWriter(file).use { writer ->
                writer.write(jsonObject.toString(4))
            }

            Log.d("DataManipulator", "$key saved to JSON: $value")

        } catch (e: Exception) {
            Log.e("DataManipulator", "Error saving $key to JSON", e)
        }
    }

    // Overload to handle Double values
    fun saveData(context: Context, key: String, value: Double) {
        saveData(context, key, value.toString())
    }

    // Save WiFi SSID
    fun saveSSIDToJson(context: Context, ssid: String) {
        saveData(context, "wifissid", ssid.trim('"'))
    }


    // Save Bluetooth host code
    fun saveBluetoothCodeToJson(context: Context, code: String) {
        saveData(context, "bluetoothhost", code)
    }

    // Save Latitude and Longitude
    fun saveLatitudeLongitudeToJson(context: Context, lat: Double, lng: Double) {
        saveData(context, "latitude", lat)
        saveData(context, "longitude", lng)
    }
}
