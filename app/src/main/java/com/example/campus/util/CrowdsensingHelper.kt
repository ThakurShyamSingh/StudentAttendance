package com.example.campus.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CrowdsensingHelper(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingPermission")
    fun collectCrowdsensingData() {
        if (!hasPermissions()) {
            Log.e("CrowdsensingHelper", "Missing required permissions!")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            // Scan WiFi Networks
            val wifiArray = JSONArray()
            try {
                val wifiList: List<ScanResult> = wifiManager.scanResults
                for (wifi in wifiList) {
                    val wifiObject = JSONObject()
                    wifiObject.put("SSID", getConnectedSSID())
                    wifiObject.put("BSSID", wifi.BSSID ?: "Unknown")
                    wifiArray.put(wifiObject)
                }
            } catch (e: SecurityException) {
                Log.e("CrowdsensingHelper", "WiFi scanning failed due to missing permissions", e)
            }

            // Scan Bluetooth Devices
            val bluetoothArray = JSONArray()
            if (bluetoothAdapter?.isEnabled == true && hasBluetoothPermission()) {
                try {
                    val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
                    for (device in pairedDevices) {
                        val bluetoothObject = JSONObject()
                        bluetoothObject.put("DeviceName", device.name ?: "Unknown")
                        bluetoothObject.put("MAC", device.address ?: "Unknown")
                        bluetoothArray.put(bluetoothObject)
                    }
                } catch (e: SecurityException) {
                    Log.e("CrowdsensingHelper", "Bluetooth scanning failed due to missing permissions", e)
                }
            } else {
                Log.e("CrowdsensingHelper", "Bluetooth scanning skipped (disabled or missing permissions)")
            }

            // Generate Date & Time Keys
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val currentTime = timeFormat.format(Date())

            // Prepare Data Map
            val crowdsensingData = mapOf(
                "wifi" to wifiArray.toString(),
                "bluetooth" to bluetoothArray.toString()
            )

            // Save Data Under Date -> Time Structure
            firestore.collection("crowdsensing").document(currentDate)
                .update(currentTime, crowdsensingData)
                .addOnSuccessListener {
                    Log.d("CrowdsensingHelper", "Crowdsensing data uploaded successfully!")
                }
                .addOnFailureListener { e ->
                    Log.e("CrowdsensingHelper", "Error uploading crowdsensing data", e)
                    firestore.collection("crowdsensing").document(currentDate)
                        .set(mapOf(currentTime to crowdsensingData))
                        .addOnSuccessListener {
                            Log.d("CrowdsensingHelper", "Created new document & uploaded data!")
                        }
                        .addOnFailureListener { err ->
                            Log.e("CrowdsensingHelper", "Failed to create document", err)
                        }
                }
        }
    }

    // **FIXED SSID Retrieval**
    private fun getConnectedSSID(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return "No active network"
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return "No WiFi"

        return if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val wifiInfo = networkCapabilities.transportInfo as? android.net.wifi.WifiInfo
                wifiInfo?.ssid?.replace("\"", "") ?: "Unknown"
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.connectionInfo.ssid.replace("\"", "")  // Deprecated but used for < API 29
            }
        } else {
            "Not connected to WiFi"
        }
    }


    private fun hasPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            requiredPermissions.add(Manifest.permission.BLUETOOTH)
        }

        return requiredPermissions.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
