package com.android.deliveryapp.manager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.deliveryapp.R
import com.android.deliveryapp.util.Keys.Companion.riderPosition
import com.android.deliveryapp.util.Keys.Companion.riders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class RidersMapActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val LOCATION_REQUEST_CODE = 101
    }

    private lateinit var mMap: GoogleMap
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_riders_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firestore = FirebaseFirestore.getInstance()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
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

        // listen for riders position change

        firestore.collection(riders)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("FIREBASE_FIRESTORE", "Failed to listen", error)
                } else {
                    if (value != null) {
                        getRidersPositions(firestore)
                    }
                }
            }
    }

    private fun getRidersPositions(firestore: FirebaseFirestore) {
        firestore.collection(riders).get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    if (document.contains(riderPosition)) {
                        val geoPoint = document.getGeoPoint(riderPosition) as GeoPoint

                        // 0.0, 0.0 means rider has finished delivering and stopped sharing location
                        if (geoPoint.latitude != 0.0 && geoPoint.longitude != 0.0) {
                            mMap.addMarker(
                                MarkerOptions()
                                    .title(document.id)
                                    .position(LatLng(geoPoint.latitude, geoPoint.longitude))
                                    .snippet(getString(R.string.rider_is_delivering))
                            )

                            mMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        geoPoint.latitude,
                                        geoPoint.longitude
                                    ), 10.0F
                                )
                            )
                        }
                    }
                }
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