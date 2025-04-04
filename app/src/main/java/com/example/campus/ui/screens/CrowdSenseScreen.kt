package com.example.campus.ui.screens

import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.example.campus.util.LocationHelper
import com.example.campus.util.WiFiHelper
import com.example.campus.util.BluetoothHelper

@Composable
fun CrowdSenseScreen(
    navController: NavController,
    context1: Context,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    val wifiHelper = remember { WiFiHelper(context) }
    val bluetoothHelper = remember { BluetoothHelper(context) }
    val locationHelper = remember { LocationHelper(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Fetching location...", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    locationHelper.getLocation()
                }, 500)
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            locationHelper.getLocation()
                        }, 500)
                    }
                }

                Lifecycle.Event.ON_DESTROY -> {
                    wifiHelper.stopHotspot()
                    bluetoothHelper.stopBroadcast()
                    Toast.makeText(context, "Hotspot and Bluetooth Broadcast Stopped", Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                wifiHelper.createHotspot()
                Toast.makeText(context, "Hotspot Started", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Start Hotspot")
        }

        Button(
            onClick = {
                wifiHelper.stopHotspot()
                Toast.makeText(context, "Hotspot Stopped", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Stop Hotspot")
        }

        Button(
            onClick = {
                bluetoothHelper.startBroadcast()
                Toast.makeText(context, "Bluetooth Broadcast Started", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Start Bluetooth Broadcast")
        }

        Button(
            onClick = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    Toast.makeText(context, "Fetching location...", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        locationHelper.getLocation()
                    }, 500)
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Get Location")
        }
    }
}
