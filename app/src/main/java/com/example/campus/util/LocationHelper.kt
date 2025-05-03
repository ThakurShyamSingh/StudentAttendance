package com.example.campus.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest: LocationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L
    ).apply {
        setMinUpdateIntervalMillis(500L)
        setMaxUpdateDelayMillis(5000L)
    }.build()

    @SuppressLint("MissingPermission")
    fun getLocation(callback: (latitude: Double, longitude: Double) -> Unit) {
        Log.d("LocationHelper", "getLocation called")

        if (!isLocationPermissionGranted()) {
            Log.e("LocationHelper", "Location permission not granted")
            return
        }

        if (!isLocationEnabled()) {
            Log.e("LocationHelper", "Location services not enabled")
            promptEnableGPS()
            return
        }

        Log.d("LocationHelper", "Permission and GPS are OK. Starting location updates...")

        var bestLocation: Location? = null

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("LocationHelper", "onLocationResult triggered with ${locationResult.locations.size} locations")

                for (location in locationResult.locations) {
                    Log.d("LocationHelper", "Fused location: $location (accuracy: ${location.accuracy} meters)")

                    if (location.accuracy <= 1f) {
                        Log.d("LocationHelper", "Good accuracy. Stopping updates.")
                        fusedLocationClient.removeLocationUpdates(this)
                        callback(location.latitude, location.longitude)
                        return
                    }

                    if (bestLocation == null || location.accuracy < bestLocation!!.accuracy) {
                        bestLocation = location
                        Log.d("LocationHelper", "Updated bestLocation to: $bestLocation")
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        // Timeout after 20 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("LocationHelper", "Timeout reached. Removing location updates.")
            fusedLocationClient.removeLocationUpdates(locationCallback)

            bestLocation?.let {
                Log.d("LocationHelper", "Timeout fallback to best current location: $it")
                callback(it.latitude, it.longitude)
            } ?: run {
                Log.e("LocationHelper", "No good current location within timeout.")
                // Nothing is returned if no current location is received.
            }
        }, 60000)
    }

    private fun isLocationPermissionGranted(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d("LocationHelper", "Permission granted: $granted")
        return granted
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        Log.d("LocationHelper", "Location enabled: $enabled")
        return enabled
    }

    private fun promptEnableGPS() {
        Log.d("LocationHelper", "Prompting user to enable GPS...")
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
