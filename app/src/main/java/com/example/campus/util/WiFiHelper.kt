package com.example.campus.util

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

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
                DataManipulator.saveSSIDToJson(context, ssid)
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
}
