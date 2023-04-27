package com.android.deliveryapp.rider

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.ThemeActivity
import com.android.deliveryapp.databinding.ActivityRiderHomeBinding
import com.android.deliveryapp.rider.adapters.RiderOrdersArrayAdapter
import com.android.deliveryapp.util.Keys
import com.android.deliveryapp.util.Keys.Companion.ACCEPTED
import com.android.deliveryapp.util.Keys.Companion.REJECTED
import com.android.deliveryapp.util.Keys.Companion.clientAddress
import com.android.deliveryapp.util.Keys.Companion.delivery
import com.android.deliveryapp.util.Keys.Companion.deliveryHistory
import com.android.deliveryapp.util.Keys.Companion.marketDocument
import com.android.deliveryapp.util.Keys.Companion.marketPosFirestore
import com.android.deliveryapp.util.Keys.Companion.newDelivery
import com.android.deliveryapp.util.Keys.Companion.riders
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.deliveryapp.util.RiderOrderItem
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import kotlin.math.*

class RiderHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderHomeBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var orders: Array<RiderOrderItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        orders = emptyArray()

        if (user != null) {
            getOrders(firestore, user.email!!)
        }
    }

    override fun onStart() {
        super.onStart()

        binding.riderOrdersList.setOnItemClickListener { _, _, i, _ ->
            val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)

            if (auth.currentUser != null) {
                if (sharedPreferences.getBoolean(newDelivery, false)) {
                    startActivity(
                        Intent(
                            this@RiderHomeActivity,
                            RiderDeliveryActivity::class.java
                        )
                    )
                } else {
                    showOrderDialog(i, auth.currentUser?.email!!)
                }
            }
        }
    }

    private fun showOrderDialog(i: Int, riderEmail: String) {
        val dialog: AlertDialog?

        val dialogView = LayoutInflater.from(this).inflate(R.layout.rider_order_dialog, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_title_rider))

        val mapBtn: ExtendedFloatingActionButton = dialogView.findViewById(R.id.viewMapBtn)
        val acceptBtn: ExtendedFloatingActionButton = dialogView.findViewById(R.id.acceptOrderBtn)
        val rejectBtn: ExtendedFloatingActionButton = dialogView.findViewById(R.id.rejectOrderBtn)

        dialog = dialogBuilder.create()
        dialog.show()

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        mapBtn.setOnClickListener {
            val intent = Intent(
                this@RiderHomeActivity,
                DeliveryMapActivity::class.java
            )
            intent.putExtra("clientLocation", orders[i].location)

            startActivity(intent)

            dialog.dismiss()
        }

        acceptBtn.setOnClickListener {
            uploadOnHistory(ACCEPTED, i, orders, firestore, riderEmail)

            editor.putBoolean(newDelivery, true)
            editor.apply()


            startActivity(
                Intent(
                    this@RiderHomeActivity,
                    RiderDeliveryActivity::class.java
                )
            )
            val stringRequest: StringRequest = object :
                StringRequest(Method.POST, "http://13.235.139.60/sandbox/bap/trigger/track",
                    Response.Listener { response ->


                        try {
                            Toast.makeText(
                                baseContext,
                                "successfully track call ",
                                Toast.LENGTH_LONG
                            ).show()

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    },
                    Response.ErrorListener { error ->
                        Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                    }) {
                override fun getBody(): ByteArray {
                    val params2 = HashMap<String, String>()
                    params2.put("domain", "delivery")
                    params2.put("use_case", "track/tracking_a_delivery_order")
                    params2.put("ttl", "10")
                    params2.put("bpp_uri", "http://13.235.139.60/sandbox/bpp1")
                    params2.put("transaction_id", "123871371289371983")
                    return JSONObject(params2 as Map<*, *>).toString().toByteArray()
                }

                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["Content-Type"] = "application/json"

                    return params
                }

            }
            val requestQueue = Volley.newRequestQueue(baseContext)
            requestQueue.add(stringRequest)
            dialog.dismiss()
        }

        rejectBtn.setOnClickListener {
            uploadOnHistory(REJECTED, i, orders, firestore, riderEmail) // delivery rejected
            val stringRequest: StringRequest = object :
                StringRequest(Method.POST, "http://13.235.139.60/sandbox/bap/trigger/cancel",
                    Response.Listener { response ->


                        try {
                            Toast.makeText(
                                baseContext,
                                "successfully cancel of order ",
                                Toast.LENGTH_LONG
                            ).show()

                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    },
                    Response.ErrorListener { error ->
                        Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                    }) {
                override fun getBody(): ByteArray {
                    val params2 = HashMap<String, String>()
                    params2.put("domain", "local-retail")
                    params2.put(
                        "use_case",
                        "cancel/cancellation_of_an_order_with_reason_for_cancellation"
                    )
                    params2.put("ttl", "10")
                    params2.put("bpp_uri", "http://13.235.139.60/sandbox/bpp1")
                    params2.put("transaction_id", "123871371289371983")
                    return JSONObject(params2 as Map<*, *>).toString().toByteArray()
                }

                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["Content-Type"] = "application/json"

                    return params
                }

            }
            val requestQueue = Volley.newRequestQueue(baseContext)
            requestQueue.add(stringRequest)
            dialog.dismiss()
        }
    }

    private fun uploadOnHistory(
        deliveryOutcome: String,
        i: Int,
        orderList: Array<RiderOrderItem>,
        firestore: FirebaseFirestore,
        riderEmail: String
    ) {

        val entry = mapOf(
            "location" to orderList[i].location,
            "date" to orderList[i].date,
            "clientEmail" to orderList[i].clientEmail,
            "outcome" to deliveryOutcome
        )

        firestore.collection(riders).document(riderEmail)
            .collection(deliveryHistory).document(orderList[i].date)
            .set(entry)
            .addOnSuccessListener {
                // update also orders for manager
                firestore.collection(Keys.orders).document(orderList[i].date)
                    .update("outcome", deliveryOutcome)
                    .addOnSuccessListener {

                        if (deliveryOutcome == REJECTED) {
                            firestore.collection(riders).document(riderEmail)
                                .collection(delivery).document(orderList[i].date)
                                .delete()
                                .addOnSuccessListener {
                                    Log.d(
                                        "FIREBASE_FIRESTORE",
                                        "Data saved with success"
                                    )

                                    val temp = orders
                                    val date = orders[i].date
                                    orders = emptyArray()

                                    // update the view
                                    for (item in temp) {
                                        if (date != item.date) {
                                            orders = orders.plus(item)
                                        }
                                    }
                                    updateView()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(
                                        "FIREBASE_FIRESTORE",
                                        "Failed to save data",
                                        e
                                    )
                                }
                        } else {
                            firestore.collection(riders).document(riderEmail)
                                .collection(delivery).document(orderList[i].date)
                                .update("outcome", deliveryOutcome)
                                .addOnSuccessListener {
                                    Log.d(
                                        "FIREBASE_FIRESTORE",
                                        "Data saved with success"
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Log.w(
                                        "FIREBASE_FIRESTORE",
                                        "Failed to save data",
                                        e
                                    )
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w("FIREBASE_FIRESTORE", "Failed to save data", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Failed to save data", e)
            }
    }

    private fun getOrders(firestore: FirebaseFirestore, email: String) {
        firestore.collection(riders).document(email)
            .collection(delivery)
            .get()
            .addOnSuccessListener { result ->
                var date: String
                var location = ""
                var locationGeoPoint: GeoPoint
                var distance: Double
                var marketPoint: GeoPoint
                var geocoder: List<Address>? = null
                var clientEmail: String

                for (document in result.documents) {
                    date = document.id
                    locationGeoPoint = document.getGeoPoint(clientAddress) as GeoPoint

                    clientEmail = document.getString("clientEmail") as String

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

                    // get market position
                    firestore.collection(marketPosFirestore).document(marketDocument)
                        .get()
                        .addOnSuccessListener { result2 ->

                            marketPoint = GeoPoint(19.1825, 73.1926)

                            distance = calculateDistanceFromMarket(marketPoint, locationGeoPoint)

                            orders = orders.plus(
                                RiderOrderItem(
                                    date,
                                    location,
                                    distance,
                                    clientEmail
                                )
                            )

                            updateView()
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

    private fun updateView() {
        if (orders.isNotEmpty()) {
            binding.empty.visibility = View.INVISIBLE
            binding.riderOrdersList.visibility = View.VISIBLE

            binding.riderOrdersList.adapter = RiderOrdersArrayAdapter(
                this,
                R.layout.rider_order_list_element,
                orders
            )
        } else { // empty
            binding.empty.visibility = View.VISIBLE
            binding.riderOrdersList.visibility = View.INVISIBLE
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.rider_home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        auth = FirebaseAuth.getInstance()

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)

        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.riderProfile -> {
                startActivity(
                    Intent(
                        this@RiderHomeActivity,
                        RiderProfileActivity::class.java
                    )
                )
                true
            }
            R.id.currentDelivery -> {
                if (sharedPreferences.getBoolean(newDelivery, false)) { // if rider has a delivery
                    startActivity(
                        Intent(
                            this@RiderHomeActivity,
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
                true
            }
            R.id.workdata -> {
                 //workdata
                    startActivity(
                        Intent(
                            this@RiderHomeActivity,
                            WorkDataActivity::class.java
                        )
                    )

                true
            }
            R.id.feedback -> {
                startActivity(
                    Intent(
                        this@RiderHomeActivity,
                        RiderFeedbackActivity::class.java
                    )
                )
                true
            }
            R.id.applyForLoan -> {
                startActivity(
                    Intent(
                        this@RiderHomeActivity,
                        ApplyLoanActivity::class.java
                    )
                )
                true
            }
            R.id.riderDeliveries -> { // history
                startActivity(
                    Intent(
                        this@RiderHomeActivity,
                        RiderDeliveryHistoryActivity::class.java
                    )
                )
                true
            }
            R.id.theme -> {
                startActivity(
                    Intent(
                        this@RiderHomeActivity,
                        ThemeActivity::class.java
                    )
                )
                true
            }
            R.id.logout -> {
                auth.signOut()
                startActivity(
                    Intent(
                        this@RiderHomeActivity,
                        LoginActivity::class.java
                    )
                )
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}