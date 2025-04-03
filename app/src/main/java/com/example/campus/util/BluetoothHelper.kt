package com.example.campus.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BluetoothHelper(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private var isBroadcasting = false
    private val firebaseDb = FirebaseFirestore.getInstance()
    private val localFile = File(context.filesDir, "attendance.json")

    private var advertiseCallback: AdvertiseCallback? = null

    private fun generateUniqueCode(): String {
        return UUID.randomUUID().toString().replace("-", "").take(8) // Reduce size to fit within 31 bytes
    }

    fun startBroadcast() {
        if (!hasRequiredPermissions()) {
            Log.e("BluetoothHelper", "Bluetooth permissions not granted")
            requestBluetoothPermissions()
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("BluetoothHelper", "Bluetooth not available or disabled")
            return
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.e("BluetoothHelper", "BLE advertising not supported on this device")
            return
        }

        try {
            isBroadcasting = true
            broadcastCode()
        } catch (e: SecurityException) {
            Log.e("BluetoothHelper", "Permission error: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun broadcastCode() {
        if (!isBroadcasting) return

        val uniqueCode = generateUniqueCode()
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val serviceUuid = ParcelUuid(UUID.randomUUID())

        val dataBytes = uniqueCode.toByteArray(Charsets.UTF_8)
        Log.d("BluetoothHelper", "Advertising data size: ${dataBytes.size} bytes")

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false) // Remove device name
            .addServiceData(serviceUuid, dataBytes) // Ensure it's short
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d("BluetoothHelper", "Broadcasting: $uniqueCode")
                saveToLocal(uniqueCode)
                saveToFirebase(uniqueCode)
            }

            override fun onStartFailure(errorCode: Int) {
                Log.e("BluetoothHelper", "Advertising failed: $errorCode")
            }
        }

        try {
            advertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback!!)
        } catch (e: SecurityException) {
            Log.e("BluetoothHelper", "Failed to start advertising: ${e.message}")
        }
    }

    fun stopBroadcast() {
        try {
            advertiseCallback?.let {
                advertiser?.stopAdvertising(it)
                isBroadcasting = false
                Log.d("BluetoothHelper", "Bluetooth Broadcast Stopped")
            }
        } catch (e: SecurityException) {
            Log.e("BluetoothHelper", "Failed to stop advertising: ${e.message}")
        }
    }

    private fun saveToLocal(code: String) {
        val json = "{ \"bluetoothhost\": \"$code\" }"
        localFile.appendText("$json\n")
    }


    private fun saveToFirebase(code: String) {
        val data = hashMapOf("bluetoothhost" to code)
        firebaseDb.collection("attendance").document(date).collection(time)
            .add(mapOf("bluetoothhost" to code))
            .addOnSuccessListener { Log.d("BluetoothHelper", "Saved to Firebase") }
            .addOnFailureListener { Log.e("BluetoothHelper", "Firebase error", it) }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothConnectPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            val bluetoothScanPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            val bluetoothAdvertisePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)

            bluetoothConnectPermission == PackageManager.PERMISSION_GRANTED &&
                    bluetoothScanPermission == PackageManager.PERMISSION_GRANTED &&
                    bluetoothAdvertisePermission == PackageManager.PERMISSION_GRANTED
        } else {
            val bluetoothAdminPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
            bluetoothAdminPermission == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        if (permissions.isNotEmpty()) {
            AlertDialog.Builder(context)
                .setTitle("Bluetooth Permissions Required")
                .setMessage("This feature requires Bluetooth permissions to function. Please grant them.")
                .setPositiveButton("Grant Permissions") { _, _ ->
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        permissions.toTypedArray(),
                        1001
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
    val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH-mm", Locale.getDefault())
    val date: String = dateFormat.format(Date())
    val time: String = timeFormat.format(Date())
}