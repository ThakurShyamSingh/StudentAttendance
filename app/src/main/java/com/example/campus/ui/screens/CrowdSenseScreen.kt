package com.example.campus.ui.screens

import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.airbnb.lottie.compose.*
import com.example.campus.util.LocationHelper
import com.example.campus.util.WiFiHelper
import com.example.campus.util.BluetoothHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CrowdSenseScreen(
    name: String,
    rollNumber: String,
    navController: NavController
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    val wifiHelper = remember { WiFiHelper(context) }
    val bluetoothHelper = remember { BluetoothHelper(context) }
    val locationHelper = remember { LocationHelper(context) }

    var backEnabled by remember { mutableStateOf(false) }

    // Launchers
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

    // Lifecycle observer
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

    // Start all processes once
    LaunchedEffect(Unit) {
        wifiHelper.createHotspot()
        bluetoothHelper.startBroadcast()
        Toast.makeText(context, "Hotspot & Bluetooth Started", Toast.LENGTH_SHORT).show()

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            Toast.makeText(context, "Fetching location...", Toast.LENGTH_SHORT).show()
            delay(500)
            locationHelper.getLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Auto navigate to dashboard after 1 minute
        delay(60_000)
        backEnabled = true
        navController.navigate("dashboard") {
            popUpTo("crowd_sense_screen") { inclusive = true }
        }
    }

    // Prevent premature back navigation
    BackHandler(enabled = !backEnabled) {
        Toast.makeText(context, "Attendance in progress. Please wait...", Toast.LENGTH_SHORT).show()
    }

    // Animation (using Lottie)
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("Animation.json"))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome, $name", fontSize = 20.sp)
        Text("Roll Number: $rollNumber", fontSize = 16.sp, modifier = Modifier.padding(bottom = 16.dp))

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(250.dp)
        )

        Text("Taking attendance...\nPlease wait a moment.", fontSize = 16.sp, modifier = Modifier.padding(top = 24.dp))
    }
}
