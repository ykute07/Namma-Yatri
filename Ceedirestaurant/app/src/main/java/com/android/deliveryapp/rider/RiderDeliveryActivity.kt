package com.android.deliveryapp.rider

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityRiderDeliveryBinding
import com.android.deliveryapp.util.Keys.Companion.ACCEPTED
import com.android.deliveryapp.util.Keys.Companion.DELIVERED
import com.android.deliveryapp.util.Keys.Companion.DELIVERY_FAILED
import com.android.deliveryapp.util.Keys.Companion.MANAGER
import com.android.deliveryapp.util.Keys.Companion.REJECTED
import com.android.deliveryapp.util.Keys.Companion.START
import com.android.deliveryapp.util.Keys.Companion.chatCollection
import com.android.deliveryapp.util.Keys.Companion.delivery
import com.android.deliveryapp.util.Keys.Companion.deliveryHistory
import com.android.deliveryapp.util.Keys.Companion.newDelivery
import com.android.deliveryapp.util.Keys.Companion.orders
import com.android.deliveryapp.util.Keys.Companion.riderEmail
import com.android.deliveryapp.util.Keys.Companion.riderStatus
import com.android.deliveryapp.util.Keys.Companion.riders
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class RiderDeliveryActivity : AppCompatActivity() {
    companion object {
        private const val LOCATION_REQUEST_CODE = 101
        private const val TAG = "FIRESTORE_CHAT";
    }

    private lateinit var binding: ActivityRiderDeliveryBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var clientEmail: String

    private val channelID = "1"
    private val notificationID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderDeliveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        val intent = Intent(
            this@RiderDeliveryActivity,
            RiderChatActivity::class.java
        )

        var fusedLocation: FusedLocationProviderClient

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (user != null) {
            var date = ""
            var location = ""

            /******************************** GET OUTCOMES ************************************/

            firestore.collection(riders).document(user.email!!)
                .collection(deliveryHistory)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result.documents) {
                        // update view with outcome
                        date = document.getString("date") as String
                        location = document.getString("location") as String
                        updateView(document.getString("outcome") as String)
                    }
                    getData(firestore, date, location)
                }
                .addOnFailureListener { e ->
                    Log.w("FIREBASE_FIRESTORE", "Failed to get data", e)
                }

            /************************ START DELIVERY ***********************************/

            binding.startDeliveryBtn.setOnClickListener {
                updateView(START)

                uploadData(firestore, date, user.email!!, START)

                sendMessageToClient(user.email!!, clientEmail)
            }

            /**************************** SHARE LOCATION ******************************/

            binding.shareLocationBtn.setOnClickListener {
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    requestPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_REQUEST_CODE
                    )
                } else {
                    fusedLocation = LocationServices.getFusedLocationProviderClient(baseContext)

                    fusedLocation.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                updateLocation(
                                    firestore,
                                    GeoPoint(location.latitude, location.longitude),
                                    user.email!!
                                )
                            }
                        }
                }
            }

            /************************** VIEW MAP ***********************************/

            binding.deliveryMap.setOnClickListener {
                val mapIntent = Intent(
                    this@RiderDeliveryActivity,
                    DeliveryMapActivity::class.java
                )
                mapIntent.putExtra("clientLocation", location)
                startActivity(mapIntent)
            }

            /********************** CHAT WITH CLIENT ******************************/

            binding.riderChatClientBtn.setOnClickListener {
                intent.putExtra("recipientEmail", clientEmail)
                intent.putExtra("riderEmail", user.email)
                startActivity(intent)
            }

            /********************** CHAT WITH MANAGER ***************************/

            binding.riderChatManagerBtn.setOnClickListener {
                intent.putExtra("riderEmail", user.email)

                intent.putExtra("recipientEmail", MANAGER)

                startActivity(intent)
            }

            /********************* END DELIVERY SUCCESS **********************/

            binding.endDeliverySuccessBtn.setOnClickListener {
                updateView(DELIVERED)

                uploadData(firestore, date, user.email!!, DELIVERED)

                editor.putBoolean(newDelivery, false)
                editor.apply()

                val geopoint = GeoPoint(0.0, 0.0)

                // stop sharing and put 0.0, 0.0
                updateLocation(firestore, geopoint, user.email!!)

                removeClientChat(user.email!!, clientEmail)

                startActivity(
                    Intent(
                        this@RiderDeliveryActivity,
                        RiderDeliveryHistoryActivity::class.java
                    )
                )
            }

            /************************ END DELIVERY FAILURE ***********************/

            binding.endDeliveryFailureBtn.setOnClickListener {
                updateView(DELIVERY_FAILED)

                uploadData(firestore, date, user.email!!, DELIVERY_FAILED)

                editor.putBoolean(newDelivery, false)
                editor.apply()

                val geopoint = GeoPoint(0.0, 0.0)

                // stop sharing and put 0.0, 0.0
                updateLocation(firestore, geopoint, user.email!!)

                removeClientChat(user.email!!, clientEmail)

                startActivity(
                    Intent(
                        this@RiderDeliveryActivity,
                        RiderDeliveryHistoryActivity::class.java
                    )
                )
            }
        }
    }

    private fun removeClientChat(riderEmail: String, clientEmail: String) {
        val reference = FirebaseFirestore.getInstance().collection(chatCollection)

        reference.document("$riderEmail|$clientEmail").delete()
            .addOnSuccessListener {
                Log.d(TAG, "Chat deleted with success")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to remove chat", e)
            }
    }

    override fun onStart() {
        super.onStart()

        val user = auth.currentUser

        if (user != null) {

            /*************************** NOTIFICATIONS ********************************/

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            val intent = Intent(
                this@RiderDeliveryActivity,
                RiderChatActivity::class.java
            )

            intent.putExtra(riderEmail, user.email)

            listenForClientMessages(firestore, notificationManager, user.email!!)

            listenForManagerMessages(firestore, notificationManager, user.email!!, intent)
        }
    }

    private fun listenForClientMessages(
        firestore: FirebaseFirestore,
        notificationManager: NotificationManager,
        riderEmail: String
    ) {

        firestore.collection(chatCollection).get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    if (document.id.contains(MANAGER)) {
                        document.reference.addSnapshotListener { value, error ->
                            if (error != null) {
                                Log.w("FIREBASE_CHAT", "Listen failed", error)
                                return@addSnapshotListener
                            } else {
                                if (value != null) { // if message sent is from rider notify
                                    if (value.contains("NAME")
                                        && value.getString("NAME") as String == "Rider"
                                        && value.id.contains("$riderEmail|$clientEmail")
                                    ) {

                                        intent.putExtra("recipientEmail", clientEmail)

                                        val pendingIntent = PendingIntent.getActivity(
                                            this@RiderDeliveryActivity,
                                            0,
                                            intent,
                                            0
                                        )

                                        createNotification(
                                            pendingIntent,
                                            notificationManager,
                                            getString(R.string.new_message_from_client)
                                        )
                                        createNotificationChannel(
                                            channelID,
                                            getString(R.string.app_name),
                                            getString(R.string.notification_channel_desc),
                                            notificationManager
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Error getting documents", e)
            }
    }

    private fun listenForManagerMessages(
        firestore: FirebaseFirestore,
        notificationManager: NotificationManager,
        riderEmail: String, intent: Intent
    ) {
        firestore.collection(chatCollection).get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    if (document.id.contains(MANAGER)) {
                        document.reference.addSnapshotListener { value, error ->
                            if (error != null) {
                                Log.w("FIREBASE_CHAT", "Listen failed", error)
                                return@addSnapshotListener
                            } else {
                                if (value != null) { // if message sent is from rider notify
                                    if (value.contains("NAME")
                                        && value.getString("NAME") as String == "Rider"
                                        && value.id.contains("$riderEmail|$MANAGER")
                                    ) {

                                        intent.putExtra("recipientEmail", MANAGER)

                                        val pendingIntent = PendingIntent.getActivity(
                                            this@RiderDeliveryActivity,
                                            0,
                                            intent,
                                            0
                                        )

                                        createNotification(
                                            pendingIntent,
                                            notificationManager,
                                            getString(R.string.new_message_from_manager)
                                        )
                                        createNotificationChannel(
                                            channelID,
                                            getString(R.string.app_name),
                                            getString(R.string.notification_channel_desc),
                                            notificationManager
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Error getting documents", e)
            }
    }

    private fun createNotification(
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
        title: String
    ) {
        val notification = Notification.Builder(this@RiderDeliveryActivity, channelID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(title)
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

    private fun updateLocation(firestore: FirebaseFirestore, geopoint: GeoPoint, email: String) {
        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)

        val entry = mapOf(
            "riderPosition" to geopoint,
            riderStatus to sharedPreferences.getBoolean(riderStatus, false)
        )

        firestore.collection(riders).document(email)
            .set(entry)
            .addOnSuccessListener {
                Log.d("FIREBASE_FIRESTORE", "Location updated with success")

                Toast.makeText(
                    baseContext,
                    getString(R.string.location_update_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Error updating position", e)

                Toast.makeText(
                    baseContext,
                    getString(R.string.location_update_failure),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Request current location permission
     */
    private fun requestPermission(permissionType: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permissionType), requestCode)
    }

    private fun updateView(outcome: String) {
        when (outcome) {
            ACCEPTED -> {
                binding.riderChatManagerBtn.visibility = View.VISIBLE
                binding.startDeliveryBtn.visibility = View.VISIBLE
                binding.deliveryMap.visibility = View.VISIBLE
                binding.shareLocationBtn.visibility = View.INVISIBLE
                binding.riderChatClientBtn.visibility = View.INVISIBLE
                binding.endDeliverySuccessBtn.visibility = View.INVISIBLE
                binding.endDeliveryFailureBtn.visibility = View.INVISIBLE
            }
            START -> {
                binding.riderChatManagerBtn.visibility = View.VISIBLE
                binding.startDeliveryBtn.visibility = View.INVISIBLE
                binding.deliveryMap.visibility = View.VISIBLE
                binding.shareLocationBtn.visibility = View.VISIBLE
                binding.riderChatClientBtn.visibility = View.VISIBLE
                binding.endDeliverySuccessBtn.visibility = View.VISIBLE
                binding.endDeliveryFailureBtn.visibility = View.VISIBLE
            }
            REJECTED -> {
                binding.riderChatManagerBtn.visibility = View.INVISIBLE
                binding.startDeliveryBtn.visibility = View.INVISIBLE
                binding.deliveryMap.visibility = View.VISIBLE
                binding.shareLocationBtn.visibility = View.INVISIBLE
                binding.riderChatClientBtn.visibility = View.INVISIBLE
                binding.endDeliverySuccessBtn.visibility = View.INVISIBLE
                binding.endDeliveryFailureBtn.visibility = View.INVISIBLE
            }
            DELIVERED -> {
                binding.riderChatManagerBtn.visibility = View.VISIBLE
                binding.startDeliveryBtn.visibility = View.INVISIBLE
                binding.deliveryMap.visibility = View.INVISIBLE
                binding.shareLocationBtn.visibility = View.INVISIBLE
                binding.riderChatClientBtn.visibility = View.INVISIBLE
                binding.endDeliverySuccessBtn.visibility = View.INVISIBLE
                binding.endDeliveryFailureBtn.visibility = View.INVISIBLE
            }
            DELIVERY_FAILED -> {
                binding.riderChatManagerBtn.visibility = View.VISIBLE
                binding.startDeliveryBtn.visibility = View.INVISIBLE
                binding.deliveryMap.visibility = View.VISIBLE
                binding.shareLocationBtn.visibility = View.INVISIBLE
                binding.riderChatClientBtn.visibility = View.VISIBLE
                binding.endDeliverySuccessBtn.visibility = View.INVISIBLE
                binding.endDeliveryFailureBtn.visibility = View.INVISIBLE
            }
        }
    }

    private fun uploadData(
        firestore: FirebaseFirestore,
        date: String,
        riderEmail: String,
        outcome: String
    ) {
        firestore.collection(riders).document(riderEmail)
            .collection(deliveryHistory).document(date)
            .update("outcome", outcome)
            .addOnSuccessListener {
                // update also in orders
                firestore.collection(orders).document(date)
                    .update("outcome", outcome)
                    .addOnSuccessListener {
                        Log.d("FIREBASE_FIRESTORE", "Data updated with success")
                        if (outcome != START && outcome != ACCEPTED) {
                            // delete entry in rider.email/delivery
                            firestore.collection(riders).document(riderEmail)
                                .collection(delivery).document(date)
                                .delete()
                                .addOnSuccessListener {
                                    Log.d(
                                        "FIREBASE_FIRESTORE",
                                        "Document deleted with success"
                                    )

                                    Toast.makeText(
                                        baseContext,
                                        getString(R.string.data_update_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(
                                        "FIREBASE_FIRESTORE",
                                        "Failed to update data",
                                        e
                                    )

                                    Toast.makeText(
                                        baseContext,
                                        getString(R.string.error_updating_database),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("FIREBASE_FIRESTORE", "Failed to update data", e)

                        Toast.makeText(
                            baseContext,
                            getString(R.string.error_updating_database),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Failed to update data", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_updating_database),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun sendMessageToClient(riderEmail: String, clientEmail: String) {
        val reference = FirebaseFirestore.getInstance().collection(chatCollection)
            .document("$riderEmail|$clientEmail")

        val automaticMessage = mapOf(
            "NAME" to "Rider",
            "TEXT" to getString(R.string.delivery_start_auto_msg)
        )

        reference.set(automaticMessage)
            .addOnSuccessListener {
                Log.d("FIRESTORE_CHAT", "Message sent")
            }
            .addOnFailureListener { e ->
                Log.e("ERROR", e.message.toString())
            }
    }

    private fun getData(firestore: FirebaseFirestore, date: String, location: String) {
        val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/track",
            Response.Listener { response ->


                try {

                    binding.state.text = "State: ${extractJSON(response)}"

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
            }) {
            override fun getBody(): ByteArray {
                val params2 = HashMap<String, String>()
                params2.put("domain","delivery" )
                params2.put("use_case", "on_track/sending_tracking_information_of_a_delivery_order")
                params2.put("ttl", "10")
                params2.put("bpp_uri","http://13.235.139.60/sandbox/bpp1")
                params2.put("transaction_id","123871371289371983")
                return JSONObject(params2 as Map<*, *>).toString().toByteArray()
            }
            override fun getHeaders() : Map<String,String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/json"

                return params
            }

        }
        val requestQueue = Volley.newRequestQueue(baseContext)
        requestQueue.add(stringRequest)
        firestore.collection(orders).document(date)
            .get()
            .addOnSuccessListener { result ->
                binding.deliveryTotalPrice.text = getString(
                    R.string.total_price_delivery,
                    String.format("%.2f ₹", result.getDouble("total") as Double)
                )
                binding.dateDelivery.text = getString(R.string.delivery_date, date)
                binding.deliveryPaymentType.text = getString(
                    R.string.delivery_payment_type,
                    result.getString("payment")
                )
                binding.locationDelivery.text = getString(R.string.delivery_location, location)
                clientEmail = result.getString("clientEmail") as String
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Error getting data", e)
            }

    }
    private fun extractJSON(response:String): String {
        val jsonArray = JSONArray(response)
        val firstIndex = jsonArray.getJSONObject(0)
        val message = firstIndex.getJSONObject("message")
        val order = message.getJSONObject("tracking")
        val state = order.get("status")


        return state.toString()

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