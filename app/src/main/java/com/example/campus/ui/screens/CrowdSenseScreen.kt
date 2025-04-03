package com.example.campus.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                wifiHelper.stopHotspot()
                bluetoothHelper.stopBroadcast()
                Toast.makeText(context, "Hotspot and Bluetooth Broadcast Stopped", Toast.LENGTH_SHORT).show()
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
    }
}
