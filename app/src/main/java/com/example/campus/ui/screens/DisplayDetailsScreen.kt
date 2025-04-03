package com.example.campus.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DisplayDetailsScreen(context: Context, lifecycleOwner: LifecycleOwner) {
    var attendanceData by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        attendanceData = loadSSIDFromJson(context)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Attendance Records", fontSize = 20.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(attendanceData) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.LightGray)
                ) {
                    Text(record, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

private fun loadSSIDFromJson(context: Context): List<String> {
    val file = File(context.filesDir, "crowdsense.json")
    val records = mutableListOf<String>()

    if (!file.exists()) return records

    try {
        val jsonObject = JSONObject(file.readText())
        val attendance = jsonObject.optJSONObject("attendance") ?: JSONObject()

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val dateEntry = attendance.optJSONObject(currentDate) ?: JSONObject()

        dateEntry.keys().forEach { time ->
            val ssid = dateEntry.getJSONObject(time).optString("wifissid", "Unknown SSID")
            records.add("$currentDate $time: $ssid")
        }
    } catch (e: Exception) {
        Log.e("WiFiHelper", "Error loading SSID from JSON", e)
    }

    return records
}
