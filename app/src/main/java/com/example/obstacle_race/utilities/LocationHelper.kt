package com.example.obstacle_race.utilities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient

class LocationHelper(
    private val context: Context,
    private val fused: FusedLocationProviderClient
) {

    var lastLat: Double? = null
        private set

    var lastLng: Double? = null
        private set

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }

    fun fetchLastKnownLocation(onResult: (lat: Double?, lng: Double?) -> Unit) {
        if (!hasLocationPermission()) {
            onResult(null, null)
            return
        }

        try {
            fused.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        lastLat = loc.latitude
                        lastLng = loc.longitude
                        onResult(lastLat, lastLng)
                    } else {
                        onResult(null, null)
                    }
                }
                .addOnFailureListener { onResult(null, null) }
        } catch (_: SecurityException) {
            onResult(null, null)
        }
    }
}
