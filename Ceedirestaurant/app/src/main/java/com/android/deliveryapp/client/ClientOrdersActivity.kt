package com.android.deliveryapp.client

import android.content.ContentValues.TAG
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.client.adapters.ClientOrdersArrayAdapter
import com.android.deliveryapp.databinding.ActivityClientOrdersBinding
import com.android.deliveryapp.util.ClientOrderItem
import com.android.deliveryapp.util.Keys.Companion.clients
import com.android.deliveryapp.util.Keys.Companion.orders
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONException
import org.json.JSONObject
import com.android.volley.Response

class ClientOrdersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientOrdersBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var orderList: Array<ClientOrderItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchOrders()

    }

    /**
     * Fetch the order list
     */

    private fun fetchOrders() {

        orderList = emptyArray()

        val user = auth.currentUser

        if (user != null) {
            firestore.collection(clients).document(user.email!!)
                .collection(orders)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result.documents) {
                        orderList = orderList.plus(
                            ClientOrderItem(
                                document.getString("date") as String,
                                document.getDouble("total") as Double,
                                document.getString("payment") as String
                            )
                        )
                    }

                    orderList.reverse() // order by the last order

                    updateView(orderList)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        baseContext,
                        getString(R.string.error_user_data),
                        Toast.LENGTH_LONG
                    ).show()
                    Log.w("FIREBASE_FIRESTORE", "Error fetching documents", e)
                }
        }
    }

    private fun updateView(orderList: Array<ClientOrderItem>) {
        // client has orders
        if (orderList.isNotEmpty()) {
            binding.ordersList.visibility = View.VISIBLE
            binding.emptyOrdersLabel.visibility = View.INVISIBLE

            binding.ordersList.adapter = ClientOrdersArrayAdapter(
                this,
                R.layout.list_element_order,
                orderList
            )
        } else {
            binding.emptyOrdersLabel.visibility = View.VISIBLE
            binding.ordersList.visibility = View.INVISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable("listView", binding.ordersList.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.ordersList.onRestoreInstanceState(savedInstanceState.getParcelable("listView"))
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

    override fun onDestroy() {
        super.onDestroy()

        orderList = emptyArray()
    }
}