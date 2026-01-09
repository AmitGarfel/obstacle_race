package com.example.obstacle_race.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.obstacle_race.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    // If updateMap/resetToWorldView called before the map is ready
    private var pendingLocation: LatLng? = null
    private var pendingResetWorld: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment =
            (childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment)
                ?: SupportMapFragment.newInstance().also {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.map_container, it)
                        .commit()
                }

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        when {
            pendingLocation != null -> {
                showMarkerAndZoom(pendingLocation!!)
                pendingLocation = null
            }

            pendingResetWorld -> {
                pendingResetWorld = false
                resetToWorldView()
            }

            else -> {
                // Default: world view (neutral)
                resetToWorldView()
            }
        }
    }

    // Called when selecting a score with location
    fun updateMap(lat: Double, lng: Double) {
        val location = LatLng(lat, lng)
        val map = googleMap

        if (map == null) {
            pendingLocation = location
            pendingResetWorld = false
            return
        }

        showMarkerAndZoom(location)
    }

    // Called when selecting a score without location (optional)
    fun resetToWorldView() {
        val map = googleMap
        if (map == null) {
            pendingLocation = null
            pendingResetWorld = true
            return
        }

        map.clear()
        map.moveCamera(CameraUpdateFactory.zoomTo(2f))
    }

    private fun showMarkerAndZoom(location: LatLng) {
        val map = googleMap ?: return
        map.clear()
        map.addMarker(MarkerOptions().position(location).title("High Score"))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))
    }
}
