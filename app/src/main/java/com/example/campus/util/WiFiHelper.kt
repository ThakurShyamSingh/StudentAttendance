package com.example.campus.util

import android.content.Context
import android.net.wifi.WifiManager
//import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

class WiFiHelper(private val context: Context) {
    private var hotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var isStartingHotspot = false

    fun createHotspot(hour: String) {
        if (hotspotReservation != null || isStartingHotspot) {
            Log.d("WiFiHelper", "Hotspot already active or starting, skipping creation.")
            return
        }

        if (context.checkSelfPermission(android.Manifest.permission.CHANGE_WIFI_STATE) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e("WiFiHelper", "Permission denied: CHANGE_WIFI_STATE required")
            return
        }

        isStartingHotspot = true
        Log.d("WiFiHelper", "Starting hotspot...")

        wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
            override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
                super.onStarted(reservation)
                hotspotReservation = reservation
                isStartingHotspot = false

                val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    reservation.softApConfiguration.wifiSsid?.toString()
                } else {
                    @Suppress("DEPRECATION")
                    reservation.softApConfiguration.ssid
                } ?: WifiManager.UNKNOWN_SSID

                Log.d("WiFiHelper", "Hotspot started - SSID: $ssid")

                // Save SSID to local JSON file
                DataManipulator.saveSSIDToJson(context, ssid, hour)
            }

            override fun onStopped() {
                super.onStopped()
                hotspotReservation = null
                isStartingHotspot = false
                Log.d("WiFiHelper", "Hotspot stopped.")
            }

            override fun onFailed(reason: Int) {
                super.onFailed(reason)
                hotspotReservation = null
                isStartingHotspot = false
                Log.e("WiFiHelper", "Hotspot failed to start with reason code: $reason")
            }
        }, Handler(Looper.getMainLooper()))
    }

    fun stopHotspot() {
        hotspotReservation?.apply {
            Log.d("WiFiHelper", "Manually stopping hotspot.")
            close()
            hotspotReservation = null
        }
    }
}

