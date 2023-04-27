package com.android.deliveryapp.client

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityClientLocationBinding
import com.android.deliveryapp.util.Keys.Companion.clientAddress
import com.android.deliveryapp.util.Keys.Companion.clients
import com.android.deliveryapp.util.Keys.Companion.fieldPosition
import com.android.deliveryapp.util.Keys.Companion.hasLocation
import com.android.deliveryapp.util.Keys.Companion.invalidUser
import com.android.deliveryapp.util.Keys.Companion.marketPosFirestore
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.*
import kotlin.system.exitProcess

/**
 * Client set his home location
 */
class ClientLocationActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private const val LOCATION_REQUEST_CODE = 101
        private const val TAG = "GoogleMaps"
        private const val FIREBASEFIRESTORE = "FIREBASEFIRESTORE"
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityClientLocationBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
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

        auth = FirebaseAuth.getInstance()
        database = FirebaseFirestore.getInstance()

        var geocoder: List<Address>? = null
        var clientPosition = LatLng(0.0, 0.0)
        var searchPosition = LatLng(0.0, 0.0)

        var markers = emptyArray<Marker>()

        val isLocationEnabled: Boolean

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

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        /**************************** GET MARKET POSITION ***************************************/
        var marketPos = GeoPoint(19.1825, 73.1926)

        database.collection(marketPosFirestore)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    marketPos = document.getGeoPoint(fieldPosition) as GeoPoint
                }

                mMap.addMarker(
                    MarkerOptions() // put a marker
                        .position(LatLng(marketPos.latitude, marketPos.longitude))
                        .title(getString(R.string.market_position))
                        .snippet(getString(R.string.market_snippet))
                )
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error getting documents", exception)
            }

        /******************************** CLIENT LOCATION *****************************************/

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (isLocationEnabled) { // if user has enabled location services
            val locationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            locationProviderClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        clientPosition = LatLng(location.latitude, location.longitude)

                        showUserLocation(markers, mMap, clientPosition)
                    }
                }
        }

        /******************************** MAP BUTTONS ******************************************/

        mMap.setOnMyLocationButtonClickListener { // when user click on location button on map
            if (clientPosition != LatLng(0.0, 0.0)) {
                for (marker in markers) { // if there are previous markers, delete it
                    marker.remove()
                }
                markers = emptyArray()

                showUserLocation(markers, mMap, clientPosition) // update the new marker
                true
            } else {
                Toast.makeText(
                    baseContext,
                    getString(R.string.location_error),
                    Toast.LENGTH_SHORT
                ).show()
                false
            }
        }

        // if user press search button
        binding.searchLocationBtn.setOnClickListener {
            // clear all markers if user wants to correct/search location
            for (marker in markers) {
                marker.remove()
            }
            markers = emptyArray()

            try {
                geocoder = Geocoder(this).getFromLocationName(
                    binding.searchLocation.text.toString(),
                    1
                )

                if (geocoder != null) {

                    searchPosition = LatLng(geocoder!![0].latitude, geocoder!![0].longitude)

                    markers.plus(
                        mMap.addMarker( // add marker on map and to the markers array
                            MarkerOptions()
                                .position(searchPosition)
                        )
                    )

                    // animate on the new searched position
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(searchPosition, 12.0F))
                }
            } catch (e: IOException) {
                Log.w(TAG, e.message.toString())
            }
        }

        // if user wants to save location
        binding.saveLocationBtn.setOnClickListener {
            val clientGeoPoint: GeoPoint =
                if (binding.searchLocation.text.isNullOrEmpty()) { // if live location is correct
                    GeoPoint(clientPosition.latitude, clientPosition.longitude)
                } else { // if user has searched for a location
                    GeoPoint(searchPosition.latitude, searchPosition.longitude)
                }

            if (calculateDistanceFromMarket(marketPos, clientGeoPoint) >0) {
                try {
                    geocoder = Geocoder(this).getFromLocation(
                        clientPosition.latitude,
                        clientPosition.longitude, 1
                    )
                } catch (e: IOException) {
                    Log.w(TAG, e.message.toString())
                }
                GlobalScope.launch {
                    if (geocoder != null) {
                        val user = auth.currentUser

                        if (user != null) {
                            val entry = hashMapOf(
                                clientAddress to clientGeoPoint
                            )

                            // adds a document with user email
                            database.collection(clients).document(user.email!!)
                                .set(entry)
                                .addOnSuccessListener { documentRef ->
                                    Log.d(
                                        FIREBASEFIRESTORE,
                                        "DocumentSnapshot added with id $documentRef"
                                    )

                                    editor.putBoolean(hasLocation, true) // set preference
                                    editor.apply()

                                    startActivity(
                                        Intent(
                                            this@ClientLocationActivity,
                                            ClientProfileActivity::class.java
                                        )
                                    )
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(FIREBASEFIRESTORE, "Error adding document", e)
                                    Toast.makeText(
                                        baseContext,
                                        getString(R.string.save_location_failed),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                }
            } else {
                showErrorDialog(editor)
            }
        }
    }

    private fun showErrorDialog(editor: SharedPreferences.Editor) {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.location_distance_error_dialog, null)

        val dialog: AlertDialog?

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(getString(R.string.too_far_title))

        val confirmButton: ExtendedFloatingActionButton =
            dialogView.findViewById(R.id.confirmButtonDialog)
        val fixLocationBtn: ExtendedFloatingActionButton =
            dialogView.findViewById(R.id.fixLocationBtn)

        dialog = dialogBuilder.create()
        dialog.show()

        confirmButton.setOnClickListener {
            editor.putBoolean(invalidUser, true)
            editor.apply()
            dialog.dismiss()
            finishAffinity()
            exitProcess(-1) // close the application entirely
        }
        fixLocationBtn.setOnClickListener {
            editor.putBoolean(invalidUser, false)
            editor.apply()
            dialog.dismiss()
        }
    }

    private fun calculateDistanceFromMarket(market: GeoPoint, clientGeoPoint: GeoPoint): Double {
        val lon1: Double = Math.toRadians(market.longitude)
        val lat1: Double = Math.toRadians(market.latitude)

        val lon2: Double = Math.toRadians(clientGeoPoint.longitude)
        val lat2: Double = Math.toRadians(clientGeoPoint.latitude)

        val distanceLng: Double = lon2 - lon1
        val distanceLat: Double = lat2 - lat1

        val a: Double =
            sin(distanceLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(distanceLng / 2).pow(2.0)
        val c = 2 * asin(sqrt(a))

        return (6367 * c)
    }

    /**
     * Show the current user location in the map
     * @param markers an array of markers, to keep track of them
     * @param googleMap the google map of this activity
     * @param clientPosition the LatLng user position
     */
    private fun showUserLocation(
        markers: Array<Marker>,
        googleMap: GoogleMap,
        clientPosition: LatLng
    ) {
        markers.plus(
            googleMap.addMarker( // add marker to the array
                MarkerOptions()
                    .position(clientPosition)
                    .title(getString(R.string.client_position))
                    .snippet(getString(R.string.client_pos_snippet))
            )
        )

        // animate on current position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clientPosition, 12.0F))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_REQUEST_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this, "Unable to show location - permission required",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val mapFragment =
                        supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                    mapFragment.getMapAsync(this)
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

    // hide keyboard when user clicks outside EditText
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        return super.dispatchTouchEvent(event)
    }
}
