package com.example.campus.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build

class WiFiHelper(private val context: Context) {

    fun isConnectedToAdminWiFi(adminSSID: String, adminBSSID: String): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                val connectionInfo = wifiManager.connectionInfo
                val currentSSID = connectionInfo.ssid.replace("\"", "") // Remove quotes
                val currentBSSID = connectionInfo.bssid

                return (currentSSID == adminSSID && currentBSSID == adminBSSID)
            }
        } else {
            // For older Android versions
            val connectionInfo = wifiManager.connectionInfo
            val currentSSID = connectionInfo.ssid.replace("\"", "") // Remove quotes
            val currentBSSID = connectionInfo.bssid

            return (currentSSID == adminSSID && currentBSSID == adminBSSID)
        }
        return false
    }
}
