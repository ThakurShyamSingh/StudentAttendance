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
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class BluetoothHelper(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private var isBroadcasting = false

    private var advertiseCallback: AdvertiseCallback? = null

    private fun generateUniqueCode(): String {
        return UUID.randomUUID().toString().replace("-", "").take(8)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun startBroadcast(hour: String) {
        if (!hasRequiredPermissions()) {
            Log.e("BluetoothHelper", "Bluetooth permissions not granted")
            requestBluetoothPermissions()
            return
        }

        if (bluetoothAdapter == null) {
            Log.e("BluetoothHelper", "Bluetooth not supported")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (context is Activity) {
                context.startActivityForResult(enableBtIntent, 1002)
            }
            return
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.e("BluetoothHelper", "BLE advertising not supported on this device")
            return
        }

        try {
            isBroadcasting = true
            broadcastCode(hour)
        } catch (e: SecurityException) {
            Log.e("BluetoothHelper", "Permission error: ${e.message}")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun broadcastCode(hour: String) {
        if (!isBroadcasting) return

        val uniqueCode = generateUniqueCode()
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .build()

        val serviceUuid = ParcelUuid(UUID.randomUUID())
        val dataBytes = uniqueCode.toByteArray(Charsets.UTF_8)

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceData(serviceUuid, dataBytes)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d("BluetoothHelper", "Broadcasting: $uniqueCode")
                DataManipulator.saveBluetoothCodeToJson(context, uniqueCode,hour)
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

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val connect = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
            val scan = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            val advertise = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE)
            connect == PackageManager.PERMISSION_GRANTED &&
                    scan == PackageManager.PERMISSION_GRANTED &&
                    advertise == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED
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

        if (permissions.isNotEmpty() && context is Activity) {
            ActivityCompat.requestPermissions(context, permissions.toTypedArray(), 1001)
        } else {
            AlertDialog.Builder(context)
                .setTitle("Bluetooth Permissions Required")
                .setMessage("Please grant the required Bluetooth permissions.")
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
