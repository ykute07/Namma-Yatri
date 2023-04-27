package com.android.deliveryapp.rider

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.deliveryapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.io.IOException

class DeliveryMapActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val LOCATION_REQUEST_CODE = 101
    }

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near India , Maharashtra
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permission == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, LOCATION_REQUEST_CODE)
        }

        /******************************** MAP SETTINGS ****************************************/

        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL

        val mapSettings = mMap.uiSettings
        mapSettings?.isZoomControlsEnabled = false
        mapSettings?.isZoomGesturesEnabled = true
        mapSettings?.isScrollGesturesEnabled = true
        mapSettings?.isTiltGesturesEnabled = true
        mapSettings?.isRotateGesturesEnabled = true

        /***************************************************************************************/

        var geocoder: List<Address>? = null

        val clientLocation = intent.getStringExtra("clientLocation") ?: ""

        val clientPoint: LatLng

        try {
            geocoder = Geocoder(this).getFromLocationName(clientLocation, 1)
        } catch (e: IOException) {
            Log.w("Geocoder", e.message.toString())
        }

        if (geocoder != null) {
            clientPoint = LatLng(geocoder[0].latitude, geocoder[0].longitude)

            mMap.addMarker(
                MarkerOptions()
                    .position(clientPoint)
                    .title(getString(R.string.client_marker))
            )

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clientPoint, 12.0F))
        }
    }

    /**
     * Request current location permission
     */
    private fun requestPermission(permissionType: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionType), requestCode)
    }

    // when the back button is pressed in actionbar, finish this activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}