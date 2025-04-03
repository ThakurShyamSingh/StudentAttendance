package com.example.campus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.campus.ui.AppNavigation
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.example.campus.worker.FirestoreDownloadScheduler
import android.util.Log
import com.google.android.gms.security.ProviderInstaller
import android.content.Context




class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {



        super.onCreate(savedInstanceState)

        installSecurityProvider(this)

        // Schedule Firestore download
        FirestoreDownloadScheduler.scheduleDailyDownload(this)

        // Initialize Firebase AppCheck
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // Request necessary permissions before continuing
        requestPermissions {
            setContent {
                AppNavigation() // Load the navigation system only after permissions are granted
            }
        }
    }

    private fun requestPermissions(onGranted: () -> Unit) {
        val requiredPermissions = mutableListOf<String>()

        Log.d("MainActivity", "Checking permissions...")
        Log.d( "Android Version",Build.VERSION.SDK_INT.toString())

        // Bluetooth permissions for devices running Android S or later
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!isPermissionGranted(Manifest.permission.BLUETOOTH_SCAN)) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                Log.d("MainActivity", "Bluetooth Scan permission required")
            }
            if (!isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                Log.d("MainActivity", "Bluetooth Connect permission required")
            }
        }

        // Location permissions for devices
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            Log.d("MainActivity", "Access Fine Location permission required")
        }
        if (!isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            Log.d("MainActivity", "Access Coarse Location permission required")
        }

        // Android 13 (TIRAMISU) permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isPermissionGranted(Manifest.permission.NEARBY_WIFI_DEVICES)) {
                requiredPermissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
                Log.d("MainActivity", "Nearby Wifi Devices permission required")
            }
            if (!isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                Log.d("MainActivity", "Post Notifications permission required")
            }
        }

        // If any required permissions are missing, request them
        if (requiredPermissions.isNotEmpty()) {
            val permissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                permissions.forEach { (permission, isGranted) ->
                    Log.d("MainActivity", "Permission $permission granted: $isGranted")
                }

                // If all required permissions are granted, proceed to the main content
                if (permissions.all { it.value }) {
                    Log.d("MainActivity", "All required permissions granted")
                    onGranted()
                } else {
                    Log.d("MainActivity", "Not all required permissions granted")
                }
            }
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            Log.d("MainActivity", "No permissions required, proceeding to content")
            onGranted()
        }
    }

    // Helper method to check if a permission is granted
    private fun isPermissionGranted(permission: String): Boolean {
        val isGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        Log.d("MainActivity", "Permission check for $permission: $isGranted")
        return isGranted
    }

    fun installSecurityProvider(context: Context) {
        try {
            ProviderInstaller.installIfNeeded(context)
            Log.d("SecurityProvider", "Security Provider installed successfully.")
        } catch (e: Exception) {
            Log.e("SecurityProvider", "Failed to install Security Provider", e)
        }
    }
}
