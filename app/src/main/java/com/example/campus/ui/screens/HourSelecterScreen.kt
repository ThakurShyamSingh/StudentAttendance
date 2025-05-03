package com.example.campus.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HourSelectorScreen(
    navController: NavController,
    name: String,
    rollNumber: String
) {
    val context = LocalContext.current
    val existingHours = remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        existingHours.value = readSavedHours(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Hello $name ($rollNumber)",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Select Hour to mark the attendance",
            style = MaterialTheme.typography.headlineSmall.copy(),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        for (hour in 1..6) {
            val hourStr = hour.toString()
            val isSaved = existingHours.value.contains(hourStr)

            ElevatedButton(
                onClick = {
                    if (!isSaved) {
                        navController.navigate("crowd_sense_screen/$name/$rollNumber/$hour")
                    }
                },
                enabled = !isSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .height(72.dp), // ⬅️ Increased height
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hour $hour",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Icon(
                        imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.Default.HighlightOff,
                        contentDescription = if (isSaved) "Hour Completed" else "Hour Pending",
                        tint = if (isSaved) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }

        }
    }
}

private fun readSavedHours(context: Context): Set<String> {
    return try {
        val file = File(context.filesDir, "crowdsense.json")
        if (!file.exists()) return emptySet()

        val json = JSONObject(file.readText())
        val attendance = json.optJSONObject("attendance") ?: return emptySet()

        val dateKey = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val dateEntry = attendance.optJSONObject(dateKey) ?: return emptySet()

        dateEntry.keys().asSequence()
            .filter { it != "hour" }
            .toSet()
    } catch (e: Exception) {
        e.printStackTrace()
        emptySet()
    }
}
