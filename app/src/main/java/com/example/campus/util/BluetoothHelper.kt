package com.example.campus.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission

class BluetoothHelper(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val discoveredDevices = mutableSetOf<String>() // Store discovered device names

    // Broadcast Receiver to detect nearby devices
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceName = it.name ?: "Unknown"
                        val deviceAddress = it.address
                        Log.d("BluetoothHelper", "Discovered: $deviceName [$deviceAddress]")

                        // Add to discovered list
                        discoveredDevices.add(deviceName)
                    }
                }
            }
        }
    }

    // Start scanning for nearby Bluetooth devices
    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (bluetoothAdapter?.isEnabled == true) {
            discoveredDevices.clear()
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(bluetoothReceiver, filter)
            bluetoothAdapter?.startDiscovery()
            Log.d("BluetoothHelper", "Scanning for Bluetooth devices...")
        } else {
            Log.e("BluetoothHelper", "Bluetooth is disabled.")
        }
    }

    // Stop scanning for Bluetooth devices
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        bluetoothAdapter?.cancelDiscovery()
        context.unregisterReceiver(bluetoothReceiver)
        Log.d("BluetoothHelper", "Stopped scanning.")
    }

    // Check if a specific student device is nearby
    fun isStudentPresent(studentID: String): Boolean {
        return discoveredDevices.contains(studentID)
    }
}
