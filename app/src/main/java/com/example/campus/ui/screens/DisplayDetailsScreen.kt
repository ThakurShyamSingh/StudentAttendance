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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.ui.Alignment



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayDetailsScreen(context: Context, lifecycleOwner: LifecycleOwner) {
    var attendanceMap by remember { mutableStateOf<Map<String, Map<String, Map<String, String>>>>(emptyMap()) }
    var triggerRecompose by remember { mutableStateOf(0) }
    var editMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<Pair<String, String>>() } // (date, time)

    LaunchedEffect(triggerRecompose) {
        attendanceMap = withContext(Dispatchers.IO) {
            readCrowdSenseJson(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CrowdSense Data") },
                actions = {
                    var expanded by remember { mutableStateOf(false) }

                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (!editMode) "Edit" else "Done") },
                                onClick = {
                                    expanded = false
                                    editMode = !editMode
                                    selectedItems.clear()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (editMode && selectedItems.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        selectedItems.forEach { (date, time) ->
                            deleteEntryFromCrowdsenseJson(context, date, time)
                        }
                        selectedItems.clear()
                        triggerRecompose++
                    },
                    containerColor = Color.Red
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            attendanceMap.forEach { (date, hoursMap) ->
                item {
                    Text(
                        text = "Date: $date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(hoursMap.keys.toList()) { hour ->
                    val detailsMap = hoursMap[hour] ?: emptyMap()
                    val isSelected = selectedItems.contains(date to hour)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Time: $hour", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                                if (editMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedItems.add(date to hour)
                                            } else {
                                                selectedItems.remove(date to hour)
                                            }
                                        }
                                    )
                                }
                            }

                            detailsMap.forEach { (key, value) ->
                                Text(text = "$key: $value")
                            }
                        }
                    }
                }
            }
        }
    }
}



fun readCrowdSenseJson(context: Context): Map<String, Map<String, Map<String, String>>> {
    val file = File(context.filesDir, "crowdsense.json")
    if (!file.exists()) return emptyMap()

    return try {
        val jsonObject = JSONObject(file.readText())
        val attendance = jsonObject.optJSONObject("attendance") ?: return emptyMap()
        val result = mutableMapOf<String, Map<String, Map<String, String>>>()

        for (date in attendance.keys()) {
            val dateEntry = attendance.getJSONObject(date)
            val timeMap = mutableMapOf<String, Map<String, String>>()

            for (time in dateEntry.keys()) {
                val entryObject = dateEntry.getJSONObject(time)
                val entryMap = mutableMapOf<String, String>()
                for (key in entryObject.keys()) {
                    entryMap[key] = entryObject.getString(key)
                }
                timeMap[time] = entryMap
            }

            result[date] = timeMap
        }

        result
    } catch (e: Exception) {
        Log.e("DisplayScreen", "Error reading JSON", e)
        emptyMap()
    }
}

fun deleteEntryFromCrowdsenseJson(context: Context, date: String, time: String) {
    val file = File(context.filesDir, "crowdsense.json")
    if (!file.exists()) return

    try {
        val jsonObject = JSONObject(file.readText())
        val attendance = jsonObject.optJSONObject("attendance") ?: return

        val dateObject = attendance.optJSONObject(date) ?: return
        dateObject.remove(time)

        if (dateObject.length() == 0) {
            attendance.remove(date)
        } else {
            attendance.put(date, dateObject)
        }

        jsonObject.put("attendance", attendance)

        FileWriter(file).use {
            it.write(jsonObject.toString(4))
        }

        Log.d("DisplayScreen", "Deleted $date -> $time")
    } catch (e: Exception) {
        Log.e("DisplayScreen", "Error deleting entry", e)
    }
}
