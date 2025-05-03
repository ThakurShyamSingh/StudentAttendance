package com.example.campus.util

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlin.apply

@Composable
fun AttendancePieChart(present: Int, absent: Int, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                val entries = kotlin.collections.listOf(
                    PieEntry(present.toFloat(), "Present"),
                    PieEntry(absent.toFloat(), "Absent")
                )

                val dataSet = PieDataSet(entries, "Attendance")
                dataSet.colors = kotlin.collections.listOf(
                    Color.rgb(76, 175, 80),
                    Color.rgb(244, 67, 54)
                ) // green, red
                dataSet.valueTextSize = 14f
                dataSet.valueTextColor = Color.BLACK

                data = PieData(dataSet)

                description.isEnabled = false
                legend.orientation = Legend.LegendOrientation.HORIZONTAL
                legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                setUsePercentValues(true)
                setDrawEntryLabels(true)
                setEntryLabelColor(Color.BLACK)
                animateY(800)
            }
        },
        modifier = modifier
    )
}
