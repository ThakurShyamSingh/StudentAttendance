package com.example.campus.ui.screens

import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
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
import com.example.campus.util.*
import kotlinx.coroutines.delay
import com.example.campus.ui.FirestoreUploader.uploadJSONToFirestore

@Composable
fun CrowdSenseScreen(
    name: String,
    rollNumber: String,
    navController: NavController,
    hour: String
) {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    val wifiHelper = remember { WiFiHelper(context) }
    val bluetoothHelper = remember { BluetoothHelper(context) }
    val locationHelper = remember { LocationHelper(context) }

    var backEnabled by remember { mutableStateOf(false) }
    var processesStarted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Toast.makeText(context, "Fetching location...", Toast.LENGTH_SHORT).show()

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

                    if (hasPermission && !processesStarted) {

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
            Log.d("CrowdSenseScreen", "onDispose triggered – stopping hotspot & Bluetooth")
            wifiHelper.stopHotspot()
            bluetoothHelper.stopBroadcast()
            Toast.makeText(context, "Hotspot and Bluetooth Broadcast Stopped", Toast.LENGTH_SHORT).show()

            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial trigger
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        wifiHelper.stopHotspot()

        if (hasPermission) {
            Toast.makeText(context, "Fetching location...", Toast.LENGTH_SHORT).show()
            delay(500)
            try {
                locationHelper.getLocation { lat, lng ->
                    startProcessesOnce(context, wifiHelper, bluetoothHelper, hour)
                    DataManipulator.saveLatitudeLongitudeToJson(context, lat, lng, hour)
                    Toast.makeText(context, "uploaded", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                Log.e("CrowdSenseScreen", "Location permission error: ${e.message}")
                Toast.makeText(context, "Failed to fetch location due to permission", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        delay(300_000) // wait 5 minutes
        wifiHelper.stopHotspot()
        backEnabled = true
        navController.navigate("dashboard") {
            popUpTo("crowd_sense_screen") { inclusive = true }
        }
    }

    BackHandler(enabled = !backEnabled) {
        Toast.makeText(context, "Attendance in progress. Please wait...", Toast.LENGTH_SHORT).show()
    }

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

@androidx.annotation.RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
private fun startProcessesOnce(
    context: Context,
    wifiHelper: WiFiHelper,
    bluetoothHelper: BluetoothHelper,
    hour: String
) {
    Handler(Looper.getMainLooper()).postDelayed( {
        wifiHelper.createHotspot(hour)
        try{
            bluetoothHelper.startBroadcast(hour)
        }catch (e: Exception){
            Log.e("CrowdSenseScreen", "Bluetooth enable error: ${e.message}")
        }
        Toast.makeText(context, "Hotspot & Bluetooth Started", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            uploadJSONToFirestore(context, {}, {}, {})
        }, 5000)

    }, 1500)
}
