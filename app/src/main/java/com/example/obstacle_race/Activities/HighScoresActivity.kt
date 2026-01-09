package com.example.obstacle_race.Activities

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.obstacle_race.Fragments.MapFragment
import com.example.obstacle_race.Fragments.ScoresFragment
import com.example.obstacle_race.R
import com.example.obstacle_race.utilities.LocationHelper
import com.example.obstacle_race.utilities.SharedPreferencesManager
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HighScoresActivity : AppCompatActivity(), ScoresFragment.OnScoreSelectedListener {

    private lateinit var mapFragment: MapFragment

    private val fusedLocation by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private lateinit var locationHelper: LocationHelper

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val prefs = SharedPreferencesManager.getInstance()

            if (!granted) {
                prefs.setLocationPreference(false)
                prefs.clearPendingScoreId()
                Toast.makeText(
                    this,
                    "Location not granted. Scores will be saved without location.",
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            prefs.setLocationPreference(true)

            val pendingId = prefs.consumePendingScoreId()

            locationHelper.fetchLastKnownLocation { lat, lng ->
                if (pendingId != null && lat != null && lng != null) {
                    prefs.updateHighScoreLocation(pendingId, lat, lng)

                    val scoresFrag = supportFragmentManager.findFragmentById(R.id.fragment_scores)
                    if (scoresFrag is ScoresFragment) {
                        scoresFrag.refreshScores()
                    }

                    Toast.makeText(
                        this,
                        "Location enabled ✅ Updated last score!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Location enabled ✅",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        SharedPreferencesManager.init(this)

        locationHelper = LocationHelper(
            context = this,
            fused = fusedLocation
        )

        if (savedInstanceState == null) {
            mapFragment = MapFragment()

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_scores, ScoresFragment())
                .replace(R.id.fragment_map, mapFragment)
                .commit()
        } else {
            mapFragment = supportFragmentManager.findFragmentById(R.id.fragment_map) as MapFragment
        }

        maybeAskLocationForScores()
    }

    // Ask only when needed (first time or "only this time" expired / permission removed).
    private fun maybeAskLocationForScores() {
        val prefs = SharedPreferencesManager.getInstance()

        val hasPermissionNow = locationHelper.hasLocationPermission()
        val prefSet = prefs.isLocationPrefSet()
        val userEnabled = prefs.shouldGetLocation()

        if (hasPermissionNow) return
        if (prefSet && !userEnabled) return

        MaterialAlertDialogBuilder(this)
            .setTitle("Enable location for high scores?")
            .setMessage(
                "If you allow location, the app can save location with high scores.\n" +
                        "This will also try to update your last saved score."
            )
            .setCancelable(true)
            .setPositiveButton("Enable") { _, _ ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            .setNegativeButton("Not now") { _, _ ->
                prefs.setLocationPreference(false)
                prefs.clearPendingScoreId()
            }
            .show()
    }

    override fun onScoreSelected(lat: Double, lng: Double) {
        mapFragment.updateMap(lat, lng)
    }

    override fun onScoreSelectedNoLocation() {
        Toast.makeText(this, "No location available for this score.", Toast.LENGTH_SHORT).show()
        mapFragment.resetToWorldView()
    }
}
