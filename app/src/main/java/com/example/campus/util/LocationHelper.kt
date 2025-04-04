package com.example.campus.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.*
import android.os.CancellationSignal
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class LocationHelper(private val context: Context) {

    fun getLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Log.e("LocationHelper", "No location provider enabled")
            promptEnableGPS()
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("LocationHelper", "Location permission not granted")
            return
        }

        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location != null) {
            processLocation(location)
        } else {
            requestNewLocation(locationManager)
        }
    }

    private fun requestNewLocation(locationManager: LocationManager) {
        try {
            val executor = Executors.newSingleThreadExecutor()
            locationManager.getCurrentLocation(
                LocationManager.GPS_PROVIDER,
                CancellationSignal(),
                executor
            ) { location ->
                if (location != null) {
                    processLocation(location)
                } else {
                    Log.e("LocationHelper", "getCurrentLocation returned null")
                }
            }
        } catch (e: SecurityException) {
            Log.e("LocationHelper", "SecurityException: Location permission denied", e)
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error requesting current location", e)
        }
    }

    private fun processLocation(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        Log.d("LocationHelper", "Location retrieved - Latitude: $lat, Longitude: $lng")
        DataManipulator.saveLatitudeLongitudeToJson(context, lat, lng)
    }

    private fun promptEnableGPS() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        context.startActivity(intent)
    }
}
