package com.android.deliveryapp.client

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityClientProfileBinding
import com.android.deliveryapp.util.Keys.Companion.chatCollection
import com.android.deliveryapp.util.Keys.Companion.clientAddress
import com.android.deliveryapp.util.Keys.Companion.clients
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.HashMap

class ClientProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firestore: FirebaseFirestore

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        supportActionBar?.title = getString(R.string.client)

        var geocoder: List<Address>? = null

        val user = auth.currentUser

        if (user != null) {
            binding.userEmail.setText(user.email) // show orderEmail at the user
            binding.userEmail.keyListener = null // not editable by user, but still visible
            binding.location.keyListener = null
        }

        sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        binding.chatWithRiderBtn?.visibility = View.VISIBLE
        binding.chatWithRiderBtnLand?.visibility = View.VISIBLE

        if (user != null) {

            /******************** USER ADDRESS FROM CLOUD INTO VIEW ************************/

            firestore.collection(clients).document(user.email!!)
                .get()
                .addOnSuccessListener { result ->
                    val clientGeoPoint = result.getGeoPoint(clientAddress)

                    if (clientGeoPoint != null) {
                        try {
                            geocoder = Geocoder(this).getFromLocation(
                                clientGeoPoint.latitude,
                                clientGeoPoint.longitude,
                                1
                            )
                        } catch (e: IOException) {
                            Log.w("Geocoder", e.message.toString())
                        }

                        if (geocoder != null) {
                            binding.location.setText(
                                "${geocoder!![0].getAddressLine(0)}, " +
                                        "${geocoder!![0].adminArea}, " +
                                        geocoder!![0].postalCode
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        baseContext,
                        getString(R.string.error_user_data),
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            auth.currentUser?.reload()
            Toast.makeText(
                baseContext,
                getString(R.string.error_user_data),
                Toast.LENGTH_LONG
            ).show()
        }

        /******************** LOCATION **************************/
        binding.setLocationBtn?.setOnClickListener {
            val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/search",
                Response.Listener { response ->


                    try {

                        Toast.makeText(baseContext,"Successfully search ",Toast.LENGTH_LONG).show()

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
                    params2.put("use_case", "search/searching_by_pickup_and_drop_location_of_a_package")
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
            startLocationActivity()
        }
        binding.setLocationBtnLand?.setOnClickListener {
            startLocationActivity()
        }

        /********************* HOME *****************************/
        binding.homepageButton?.setOnClickListener {
            val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/search",
                Response.Listener { response ->


                    try {

                        Toast.makeText(baseContext,"Successfully on_search ",Toast.LENGTH_SHORT).show()

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(baseContext, error.toString(), Toast.LENGTH_SHORT).show()
                }) {
                override fun getBody(): ByteArray {
                    val params2 = HashMap<String, String>()
                    params2.put("domain","delivery" )
                    params2.put("use_case", "on_search/sending_a_list_of_delivery_service_options")
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
            startHomeActivity()
        }
        binding.homepageButtonLand?.setOnClickListener {
            startHomeActivity()
        }

        /********************* CHAT RIDER *************************/
        binding.chatWithRiderBtn?.setOnClickListener {
            if (user != null) {
                startChatActivity(firestore, user.email!!)
            }
        }
        binding.chatWithRiderBtnLand?.setOnClickListener {
            if (user != null) {
                startChatActivity(firestore, user.email!!)
            }
        }
    }

    private fun startChatActivity(firestore: FirebaseFirestore, email: String) {
        // look if there are chats
        firestore.collection(chatCollection)
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    if (document.id.contains(email)) {
                        startActivity(
                            Intent(
                                this@ClientProfileActivity,
                                ClientChatActivity::class.java
                            )
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_CHAT", "Error getting chats", e)

                Toast.makeText(
                    baseContext,
                    getString(R.string.no_chat_found),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun startLocationActivity() {
        startActivity(
            Intent(
                this@ClientProfileActivity,
                ClientLocationActivity::class.java
            )
        )
    }

    private fun startHomeActivity() {
        startActivity(
            Intent(
                this@ClientProfileActivity,
                ClientHomeActivity::class.java
            )
        )
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        auth = FirebaseAuth.getInstance()
        when (item.itemId) {
            R.id.homePage -> {
                startActivity(Intent(this@ClientProfileActivity, ClientHomeActivity::class.java))
                finish()
            }
            R.id.logout -> {
                auth.signOut()

                val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // delete all shared preferences
                editor.apply()

                startActivity(Intent(this@ClientProfileActivity, LoginActivity::class.java))
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // hide keyboard when user clicks outside EditText
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("clientEmail", binding.userEmail.text.toString())
        outState.putString("clientLocation", binding.location.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.userEmail.setText(savedInstanceState.getString("clientEmail"))
        binding.location.setText(savedInstanceState.getString("clientLocation"))
    }
}