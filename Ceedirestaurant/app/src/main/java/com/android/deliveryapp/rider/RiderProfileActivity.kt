package com.android.deliveryapp.rider

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityRiderProfileBinding
import com.android.deliveryapp.util.Keys
import com.android.deliveryapp.util.Keys.Companion.YET_TO_RESPOND
import com.android.deliveryapp.util.Keys.Companion.clientAddress
import com.android.deliveryapp.util.Keys.Companion.delivery
import com.android.deliveryapp.util.Keys.Companion.marketDocument
import com.android.deliveryapp.util.Keys.Companion.marketPosFirestore
import com.android.deliveryapp.util.Keys.Companion.newDelivery
import com.android.deliveryapp.util.Keys.Companion.riderStatus
import com.android.deliveryapp.util.Keys.Companion.riders
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.io.IOException
import kotlin.math.*

class RiderProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val channelID = "1"
    private val notificationID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        firestore = FirebaseFirestore.getInstance()

        supportActionBar?.title = getString(R.string.rider)

        val user = auth.currentUser

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (sharedPreferences.getBoolean(newDelivery, false)) {
            binding.currentDeliveryBtn.visibility = View.VISIBLE
        } else {
            binding.currentDeliveryBtn.visibility = View.INVISIBLE
        }

        if (user != null) {
            binding.riderEmail.setText(user.email) // show orderEmail at the user
            binding.riderEmail.keyListener = null // not editable by user, but still visible

            getAvailability(firestore, user.email!!, sharedPreferences, editor)

            binding.riderStatus.setOnCheckedChangeListener { _, isChecked ->
                uploadToCloud(firestore, user, isChecked)
                editor.putBoolean(riderStatus, isChecked)
                editor.apply()
            }
            firestore.collection(Keys.users).document(user.email!!)
                .get()
                .addOnSuccessListener { result ->
                    binding.welcomeLabel?.text="Welcome ${result.get("Name")}"

                }
                .addOnFailureListener { e ->
                    Log.w("FIREBASEFIRESTORE", "Error Getting Name", e)
                    Toast.makeText(
                        baseContext,
                        "sorry",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

        binding.homepageBtn.setOnClickListener {
            startActivity(
                Intent(
                    this@RiderProfileActivity,
                    RiderHomeActivity::class.java
                )
            )
            finish()
        }

        binding.currentDeliveryBtn.setOnClickListener {
            if (sharedPreferences.getBoolean(newDelivery, false)) {
                startActivity(
                    Intent(
                        this@RiderProfileActivity,
                        RiderDeliveryActivity::class.java
                    )
                )
            } else {
                Toast.makeText(
                    baseContext,
                    getString(R.string.no_current_delivery),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()

        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this@RiderProfileActivity, RiderHomeActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this@RiderProfileActivity,
            0,
            intent,
            0
        )
        if (user != null) {
            listenForNewOrders(firestore, pendingIntent, notificationManager, user.email!!)
        }
    }

    private fun getAvailability(
        firestore: FirebaseFirestore,
        email: String,
        sharedPreferences: SharedPreferences,
        editor: SharedPreferences.Editor
    ) {
        firestore.collection(riders).document(email)
            .get()
            .addOnSuccessListener { result ->
                // if cannot get from server, get it from preferences
                if (result.getBoolean(riderStatus) == null) {
                    binding.riderStatus.isChecked = sharedPreferences.getBoolean(riderStatus, false)
                } else {
                    binding.riderStatus.isChecked = result.getBoolean(riderStatus) as Boolean
                    editor.putBoolean(riderStatus, binding.riderStatus.isChecked)
                    editor.apply()
                }
            }
    }

    private fun listenForNewOrders(
        firestore: FirebaseFirestore,
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
        email: String
    ) {
        firestore.collection(riders).document(email)
            .collection(delivery)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.w("FIREBASE_FIRESTORE", "Listen failed", error)
                    return@addSnapshotListener
                } else {
                    if (value != null) {
                        for (document in value.documents) {
                            if (document.contains("outcome")
                                && document.getString("outcome") == YET_TO_RESPOND
                            ) {
                                sendDataToNotification(
                                    firestore,
                                    pendingIntent,
                                    notificationManager,
                                    email
                                )
                            }
                        }
                    }
                }
            }
    }

    private fun sendDataToNotification(
        firestore: FirebaseFirestore,
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
        email: String
    ) {
        firestore.collection(riders).document(email)
            .collection(delivery)
            .get()
            .addOnSuccessListener { result ->
                var location = ""
                var locationGeoPoint: GeoPoint
                var distance: Double
                var marketPoint: GeoPoint
                var geocoder: List<Address>? = null

                for (document in result.documents) {
                    locationGeoPoint = document.getGeoPoint(clientAddress) as GeoPoint

                    try {
                        geocoder = Geocoder(this).getFromLocation(
                            locationGeoPoint.latitude,
                            locationGeoPoint.longitude,
                            1
                        )
                    } catch (e: IOException) {
                        Log.w("Geocoder", e.message.toString())
                    }

                    if (geocoder != null) {
                        location = "${geocoder[0].getAddressLine(0)}, " +
                                "${geocoder[0].adminArea}, " +
                                geocoder[0].postalCode
                    }

                    firestore.collection(marketPosFirestore).document(marketDocument)
                        .get()
                        .addOnSuccessListener { result2 ->
                            marketPoint =  GeoPoint(19.1825, 73.1926)

                            distance = calculateDistanceFromMarket(
                                marketPoint,
                                locationGeoPoint
                            )

                            createNotification(
                                pendingIntent,
                                notificationManager,
                                location,
                                distance
                            )
                            createNotificationChannel(
                                channelID,
                                getString(R.string.app_name),
                                getString(R.string.new_delivery_title),
                                notificationManager
                            )
                        }
                        .addOnFailureListener { exception ->
                            Log.w("Firestore", "Error getting documents", exception)
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_user_data),
                    Toast.LENGTH_SHORT
                ).show()

                Log.w(
                    "FIREBASE_FIRESTORE",
                    "Error getting orders",
                    e
                )
            }
    }

    private fun createNotification(
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
        location: String,
        distance: Double
    ) {
        val notification = Notification.Builder(this@RiderProfileActivity, channelID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.new_delivery_title))
            .setContentText(
                "${getString(R.string.notification_desc_address, location)} " +
                        getString(
                            R.string.notification_desc_distance,
                            String.format("%.2f", distance)
                        )
            )
            .setAutoCancel(true)
            .setChannelId(channelID)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationID, notification)
    }

    private fun createNotificationChannel(
        id: String,
        name: String,
        description: String,
        notificationManager: NotificationManager
    ) {
        val priority = NotificationManager.IMPORTANCE_HIGH

        val channel = NotificationChannel(id, name, priority)

        channel.description = description

        notificationManager.createNotificationChannel(channel)
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
     * Upload data to cloud
     */
    private fun uploadToCloud(database: FirebaseFirestore, user: FirebaseUser, isChecked: Boolean) {
        val entry = hashMapOf(riderStatus to isChecked)

        database.collection(riders)
            .document(user.email!!)
            .set(entry)
            .addOnSuccessListener { documentRef ->
                Log.d("FIREBASEFIRESTORE", "DocumentSnapshot added with $documentRef")
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASEFIRESTORE", "Error adding document", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.rider_status_save_failure),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (auth.currentUser != null) {
            // in case checkbox hasn't been checked at all
            uploadToCloud(firestore, auth.currentUser!!, binding.riderStatus.isChecked)

            editor.putBoolean(riderStatus, binding.riderStatus.isChecked)
            editor.apply()
        }

        return when (item.itemId) {
            R.id.homePage -> {
                startActivity(
                    Intent(
                        this@RiderProfileActivity,
                        RiderHomeActivity::class.java
                    )
                )
                finish()
                true
            }
            R.id.logout -> {
                auth.signOut()
                startActivity(
                    Intent(
                        this@RiderProfileActivity,
                        LoginActivity::class.java
                    )
                )
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("riderEmail", binding.riderEmail.text.toString())
        outState.putBoolean("riderStatus", binding.riderStatus.isChecked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.riderEmail.setText(savedInstanceState.getString("riderEmail"))
        binding.riderStatus.isChecked = savedInstanceState.getBoolean("riderStatus")
    }
}