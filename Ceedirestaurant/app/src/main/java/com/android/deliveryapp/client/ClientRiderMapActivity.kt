package com.android.deliveryapp.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.deliveryapp.R
import com.android.deliveryapp.util.Keys.Companion.clientAddress
import com.android.deliveryapp.util.Keys.Companion.clients
import com.android.deliveryapp.util.Keys.Companion.riderEmail
import com.android.deliveryapp.util.Keys.Companion.riderPosition
import com.android.deliveryapp.util.Keys.Companion.riders
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class ClientRiderMapActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val LOCATION_REQUEST_CODE = 101
    }

    private lateinit var mMap: GoogleMap
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_rider_map)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
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

        val user = auth.currentUser

        if (user != null) {
            getMyLocation(firestore, user.email!!)

            val riderEmail = intent.getStringExtra(riderEmail)

            getRiderPosition(firestore, riderEmail!!)
        }

    }

    private fun getRiderPosition(firestore: FirebaseFirestore, riderEmail: String) {
        firestore.collection(riders).document(riderEmail)
                .get()
                .addOnSuccessListener { result ->
                    val geoPoint = result.getGeoPoint(riderPosition) as GeoPoint

                    mMap.addMarker(
                            MarkerOptions()
                                    .title(getString(R.string.rider))
                                    .snippet(getString(R.string.rider_is_delivering))
                                    .position(LatLng(geoPoint.latitude, geoPoint.longitude))
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

    private fun getMyLocation(firestore: FirebaseFirestore, email: String) {
        firestore.collection(clients).document(email)
                .get()
                .addOnSuccessListener { result ->
                    val clientPoint = result.getGeoPoint(clientAddress) as GeoPoint

                    mMap.addMarker(
                            MarkerOptions()
                                    .title(getString(R.string.client_position))
                                    .position(LatLng(clientPoint.latitude, clientPoint.longitude))
                                    .snippet(getString(R.string.client_pos_snippet))
                    )
                }
                .addOnFailureListener { e ->
                    Log.w("FIREBASE_FIRESTORE", "Error getting client position", e)
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