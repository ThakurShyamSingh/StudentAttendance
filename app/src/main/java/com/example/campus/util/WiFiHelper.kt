package com.example.campus.util

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WiFiHelper(private val context: Context) {
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun createHotspot() {
        if (context.checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("WiFiHelper", "Permission denied: CHANGE_WIFI_STATE required")
            return
        }

        val mainHandler = Handler(Looper.getMainLooper())

        wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                hotspotReservation = reservation

                val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    reservation.softApConfiguration.wifiSsid?.toString()
                } else {
                    @Suppress("DEPRECATION")
                    reservation.softApConfiguration.ssid
                } ?: WifiManager.UNKNOWN_SSID

                Log.d("WiFiHelper", "Hotspot started - SSID: $ssid")

                // Save SSID to local JSON file
                saveSSIDToJson(ssid)
            }

            override fun onStopped() {
                super.onStopped()
                Log.d("WiFiHelper", "Hotspot stopped")
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                Log.e("WiFiHelper", "Hotspot failed: $reason")
            }
        }, mainHandler)
    }

    fun stopHotspot() {
        hotspotReservation?.close()
        hotspotReservation = null
        Log.d("WiFiHelper", "Hotspot manually stopped")
    }

    private fun saveSSIDToJson(ssid: String) {
        try {
            val file = File(context.filesDir, "crowdsense.json")

            // Read existing JSON data or create a new JSON object
            val jsonObject = if (file.exists()) {
                JSONObject(file.readText())
            } else {
                JSONObject()
            }

            // Get current date and time
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH-mm", Locale.getDefault())
            val date = dateFormat.format(Date())
            val time = timeFormat.format(Date())

            // Add data to JSON object
            val attendance = jsonObject.optJSONObject("attendance") ?: JSONObject()
            val dateEntry = attendance.optJSONObject(date) ?: JSONObject()
            val hourEntry = dateEntry.optJSONObject(time) ?: JSONObject()

            hourEntry.put("wifissid", ssid)
            dateEntry.put(time, hourEntry)
            attendance.put(date, dateEntry)
            jsonObject.put("attendance", attendance)

            // Write updated JSON back to file
            FileWriter(file).use { writer ->
                writer.write(jsonObject.toString(4)) // Pretty print with indentation
            }

            Log.d("WiFiHelper", "SSID saved to crowdsense.json: $ssid")

        } catch (e: Exception) {
            Log.e("WiFiHelper", "Error saving SSID to JSON", e)
        }
    }
}
