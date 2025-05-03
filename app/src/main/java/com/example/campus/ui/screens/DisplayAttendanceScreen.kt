package com.example.campus.ui.screens

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.campus.ui.Routes
import com.example.campus.util.*
import java.io.File
import java.io.FileWriter
import org.json.JSONObject
import kotlin.collections.count
import kotlin.io.readText
import kotlin.io.use
import kotlin.sequences.asSequence
import kotlin.sequences.map
import kotlin.sequences.toList
import kotlin.to
import kotlin.toString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayAttendanceScreen(navController: NavController) {
    val context = LocalContext.current
    val attendanceData = remember { loadAttendanceData(context) }

    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedHour by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = selectedDate != null || selectedHour != null) {
        if (selectedHour != null) selectedHour = null
        else if (selectedDate != null) selectedDate = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Viewer") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedHour != null) selectedHour = null
                        else if (selectedDate != null) selectedDate = null
                        else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar1(navController) }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            when {
                attendanceData == null -> {
                    Text("No attendance data found.", style = MaterialTheme.typography.bodyLarge)
                }

                selectedDate == null -> {
                    val dates = attendanceData
                        .getJSONObject("StudentAttendance")
                        .keys()
                        .asSequence()
                        .toList()
                    Text("📅 Select a Date:", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(dates) { date ->
                            Text(
                                text = date,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDate = date }
                                    .padding(12.dp)
                            )
                            Divider()
                        }
                    }
                }

                selectedHour == null -> {
                    val hours = attendanceData
                        .getJSONObject("StudentAttendance")
                        .getJSONObject(selectedDate.toString())
                        .keys()
                        .asSequence()
                        .toList()

                    Text(
                        "🕓 Select an Hour for $selectedDate:",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(hours) { hour ->
                            Text(
                                text = hour,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedHour = hour }
                                    .padding(12.dp)
                            )
                            Divider()
                        }
                    }
                }

                else -> {
                    val rollMap = attendanceData
                        .getJSONObject("StudentAttendance")
                        .getJSONObject(selectedDate.toString())
                        .getJSONObject(selectedHour.toString())

                    val rollList = rollMap.keys()
                        .asSequence()
                        .map { it to rollMap.getBoolean(it) }
                        .toList()

                    val totalPresent = rollList.count { it.second }
                    val totalAbsent = rollList.size - totalPresent

                    AttendancePieChart(
                        present = totalPresent,
                        absent = totalAbsent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(vertical = 12.dp)
                    )

                    Text("✅ Present: $totalPresent", style = MaterialTheme.typography.bodyLarge)
                    Text("❌ Absent: $totalAbsent", style = MaterialTheme.typography.bodyLarge)

                    Spacer(Modifier.height(12.dp))
                    LazyColumn {
                        items(rollList) { (roll, status) ->
                            Text(
                                text = "$roll → ${if (status) "✅ Present" else "❌ Absent"}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Divider()
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row {
                        Button(onClick = { selectedHour = null }) {
                            Text("Back to Hours")
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = {
                            selectedHour = null
                            selectedDate = null
                        }) {
                            Text("Back to Dates")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        val success = exportAttendanceToCSV(
                            selectedDate.toString(),
                            selectedHour.toString(),
                            rollList
                        )
                        Toast.makeText(
                            context,
                            if (success) "Exported to Downloads" else "Export failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text("Export to CSV")
                    }
                }
            }
        }
    }
}

private fun loadAttendanceData(context: Context): JSONObject? {
    return try {
        val file = File(context.filesDir, "attendance.json")
        if (file.exists()) JSONObject(file.readText()) else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun exportAttendanceToCSV(
    date: String,
    hour: String,
    data: List<Pair<String, Boolean>>
): Boolean {
    return try {
        val fileName = "Attendance_${date}_Hour${hour}.csv"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("Roll Number,Status\n")
            for ((roll, status) in data) {
                writer.append("$roll,${if (status) "Present" else "Absent"}\n")
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

@Composable
fun BottomNavigationBar1(navController: NavController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = false,
            onClick = { navController.navigate("dashboard") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Edit, contentDescription = "Manage Attendance") },
            label = { Text("Manage") },
            selected = false,
            onClick = { navController.navigate("edit_face_screen") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Visibility, contentDescription = "View Attendance") },
            label = { Text("View") },
            selected = true,
            onClick = {  }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Face, contentDescription = "Edit Face Data") },
            label = { Text("Edit Faces") },
            selected = false,
            onClick = { navController.navigate("student_manager") }
        )
    }
}
