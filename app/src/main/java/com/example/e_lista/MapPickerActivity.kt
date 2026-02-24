package com.example.e_lista

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedAddress: String? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker) // You need to create this layout

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.btnConfirmLocation).setOnClickListener {
            if (selectedAddress != null) {
                val intent = Intent()
                intent.putExtra("SELECTED_ADDRESS", selectedAddress)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Check if an address was passed
        val currentAddress = intent.getStringExtra("CURRENT_ADDRESS")

        if (!currentAddress.isNullOrEmpty()) {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                // Limit to 1 result
                val addresses = geocoder.getFromLocationName(currentAddress, 1)

                if (!addresses.isNullOrEmpty()) {
                    val location = addresses[0]
                    val latLng = LatLng(location.latitude, location.longitude)

                    // Add marker and move camera
                    mMap.addMarker(MarkerOptions().position(latLng).title("Current Selection"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) // Zoom level 15 is street view
                    selectedAddress = currentAddress // Keep the selected address string
                } else {
                    // Fallback if geocoding fails
                    moveToDefaultLocation()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                moveToDefaultLocation()
            }
        } else {
            moveToDefaultLocation()
        }

        // ... (keep your existing setOnMapClickListener logic here) ...
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()
            mMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))

            // Convert LatLng to Address
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    selectedAddress = addresses[0].getAddressLine(0)
                    Toast.makeText(this, selectedAddress, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun moveToDefaultLocation() {
        // Default to Philippines (Manila)
        val defaultLoc = LatLng(14.5995, 120.9842)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLoc, 10f))
    }
}