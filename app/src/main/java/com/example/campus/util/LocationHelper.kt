package com.example.campus.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.io.File
import java.util.Calendar

class LocationHelper(private val context: Context) {

    private val firebaseDb = FirebaseFirestore.getInstance()
    private val localFile = File(context.filesDir, "attendance.json")

    fun getLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 🔴 Check for location permissions before proceeding
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationHelper", "Location permission not granted")
            return
        }

        try {
            val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            location?.let {
                val lat = it.latitude
                val lng = it.longitude
                saveToLocal(lat, lng)
                saveToFirebase(lat, lng)
            } ?: Log.e("LocationHelper", "Failed to get location")
        } catch (e: SecurityException) {
            Log.e("LocationHelper", "SecurityException: Location permission denied", e)
        }
    }

    private fun saveToLocal(lat: Double, lng: Double) {
        val json = "{ \"locationhost\": { \"lat\": $lat, \"lng\": $lng } }"
        localFile.appendText("$json\n")
    }

    private fun saveToFirebase(lat: Double, lng: Double) {
        val currentDate = getCurrentDate()
        val currentHour = getCurrentHour()

        val data = hashMapOf(
            "locationhost" to mapOf("lat" to lat, "lng" to lng),
            "timestamp" to FieldValue.serverTimestamp()
        )

        firebaseDb.collection("attendance").document(currentDate)
            .collection(currentHour)
            .add(data)
            .addOnSuccessListener { Log.d("LocationHelper", "Saved to Firebase") }
            .addOnFailureListener { Log.e("LocationHelper", "Firebase error", it) }
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun getCurrentHour(): String {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.HOUR_OF_DAY).toString()
    }
}
