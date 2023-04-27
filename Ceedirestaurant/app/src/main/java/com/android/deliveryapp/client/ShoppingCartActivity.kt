package com.android.deliveryapp.client

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.client.adapters.ShoppingCartArrayAdapter
import com.android.deliveryapp.databinding.ActivityShoppingCartBinding
import com.android.deliveryapp.util.Keys.Companion.YET_TO_RESPOND
import com.android.deliveryapp.util.Keys.Companion.clients
import com.android.deliveryapp.util.Keys.Companion.hasLocation
import com.android.deliveryapp.util.Keys.Companion.orders
import com.android.deliveryapp.util.Keys.Companion.productListFirebase
import com.android.deliveryapp.util.Keys.Companion.shoppingCart
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.deliveryapp.util.ProductItem

import com.android.volley.toolbox.StringRequest
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.util.*
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.firebase.firestore.GeoPoint
import org.json.JSONException
import org.json.JSONObject

class ShoppingCartActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShoppingCartBinding
    private lateinit var firestore: FirebaseFirestore // shopping cart
    private lateinit var database: FirebaseDatabase // orders
    private lateinit var auth: FirebaseAuth
    private lateinit var products: Array<ProductItem>
    private lateinit var address: GeoPoint

    private var dialog: AlertDialog? = null

    private var total: Double = 0.00

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShoppingCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        database = FirebaseDatabase.getInstance()

        auth = FirebaseAuth.getInstance()

        fetchItemsFromCloud()

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()

        binding.shoppingListView.setOnItemClickListener { _, _, i, _ -> // remove from cart dialog
            val dialog: AlertDialog?

            val dialogView = LayoutInflater.from(this).inflate(
                R.layout.remove_from_cart_dialog,
                null
            )

            val dialogBuilder = AlertDialog.Builder(this)
                .setTitle(getString(R.string.remove_from_shopping_cart_title, products[i].title))
                .setView(dialogView)

            val removeBtn: FloatingActionButton = dialogView.findViewById(R.id.removeFromCartBtn)

            dialog = dialogBuilder.create()
            dialog.show()

            removeBtn.setOnClickListener {
                removeFromShoppingCart(auth, firestore, products[i].title)
                dialog.dismiss()
                updateView(products)
            }
        }

        binding.checkoutBtn.setOnClickListener {
            // check if user has set a location
            val user = auth.currentUser
            val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)

            // has location
            if (sharedPreferences.getBoolean(hasLocation, false) && user != null) {
                showCheckOutDialog(user)
            } else {
                showLocationErrorDialog()
            }
        }
    }

    private fun showLocationErrorDialog() {
        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.location_error_dialog, null)

        val dialog: AlertDialog?

        val errorButton: ExtendedFloatingActionButton =
            dialogView.findViewById(R.id.errorButton)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(getString(R.string.error))

        dialog = dialogBuilder.create()
        dialog.show()

        // return to profile so client can set his location
        errorButton.setOnClickListener {
            dialog.dismiss()
            startActivity(
                Intent(
                    this@ShoppingCartActivity,
                    ClientProfileActivity::class.java
                )
            )
        }
    }

    private fun showCheckOutDialog(user: FirebaseUser) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.checkout_dialog, null)

        // radio group has "credit cart" as set default checked
        var paymentType = ""

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(getString(R.string.place_order))

        val totalPrice: TextView = dialogView.findViewById(R.id.totalPriceDialog)
        totalPrice.text = binding.totalPriceLabel.text

        val paymentRadioGroup: RadioGroup = dialogView.findViewById(R.id.paymentOptions)

        val creditCardRadioButton: RadioButton = dialogView.findViewById(R.id.creditCard)
        val cashRadioButton: RadioButton = dialogView.findViewById(R.id.cash)

        val placeOrderBtn: ExtendedFloatingActionButton =
            dialogView.findViewById(R.id.placeOrderBtn)

        dialog = dialogBuilder.create()
        dialog!!.show()

        paymentRadioGroup.setOnCheckedChangeListener { _, i ->
            if (creditCardRadioButton.isChecked && creditCardRadioButton.id == i) {
                paymentType = getString(R.string.credit_card)
            } else if (cashRadioButton.isChecked && cashRadioButton.id == i) {
                paymentType = getString(R.string.cash)
            }
        }

        placeOrderBtn.setOnClickListener {
            if(paymentType=="Credit Card"){
                confirmPostpaidBeckn()
            }
            if (paymentType=="Cash"){
                confirmPrepaidBeckn()
            }
            if (paymentType.isNotEmpty()) {
                val reference = database.getReference(productListFirebase)
                firestore.collection(clients).document(user.email!!).get().addOnSuccessListener { result->
                    address=result.getGeoPoint("address") as GeoPoint
                    createOrder(firestore, reference, user, paymentType,address)
                    dialog!!.dismiss()
                }



                startActivity(
                    Intent(
                        this@ShoppingCartActivity,
                        ClientOrdersActivity::class.java
                    )
                )
                finish()
            } else {
                Toast.makeText(
                    baseContext,
                    getString(R.string.please_select_payment),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Remove the document from the sub-collection "shoppingCart"
     * @param auth firebase auth instance
     * @param firestore firestore instance
     * @param title product title to be removed
     */
    private fun removeFromShoppingCart(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        title: String
    ) {
        val user = auth.currentUser

        val temp = products
        products = emptyArray()

        for (item in temp) {
            if (item.title != title) {
                products = products.plus(item)
            }
        }

        if (user != null) {
            firestore.collection(clients).document(user.email!!)
                .collection(shoppingCart).document(title)
                .get()
                .addOnSuccessListener { result ->
                    if (result.exists()) { // if it exists then remove it, otherwise, do nothing
                        result.reference.delete()
                        Log.d("FIREBASE_FIRESTORE", "Product removed with success")
                        Toast.makeText(
                            baseContext,
                            getString(R.string.product_removed_from_cart_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Log.w("FIREBASE_FIRESTORE", "Error removing product from cart")
                    Toast.makeText(
                        baseContext,
                        getString(R.string.error_removing_from_cart),
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
    }

    private fun getDate(): String {
        val today: Date = Calendar.getInstance().time

        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(today)
    }
    private fun confirmPostpaidBeckn(){
        val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/confirm",
            Response.Listener { response ->


                try {
                    Toast.makeText(baseContext,"Succesfully confirm",Toast.LENGTH_LONG).show()

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
                params2.put("use_case", "confirm/confirmation_of_a_postpaid_order")
                params2.put("ttl", "2000")
                params2.put("bpp_uri", "http://13.235.139.60/sandbox/bpp1")
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
    }
    private fun confirmPrepaidBeckn(){
        val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/confirm",
            Response.Listener { response ->


                try {
                    Toast.makeText(baseContext,"Succesfully Confirm",Toast.LENGTH_LONG).show()

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
                params2.put("use_case", "confirm/confirmation_of_a_postpaid_order")
                params2.put("ttl", "2000")
                params2.put("bpp_uri", "http://13.235.139.60/sandbox/bpp1")
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
    }
    private fun createOrder(
        firestore: FirebaseFirestore,
        reference: DatabaseReference,
        user: FirebaseUser,
        paymentType: String,
        address :GeoPoint
    ) {

        val today = getDate()

        val entry = mapOf(
            "total" to total,
            "payment" to paymentType,
            "date" to today,
            "products" to products.toList()
        )

        val orderEntryManager = mapOf(
            "address" to address,
            "total" to total,
            "payment" to paymentType,
            "date" to today,
            "products" to products.toList(),
            "clientEmail" to user.email!!,
            "outcome" to YET_TO_RESPOND
        )

        firestore.collection(clients).document(user.email!!)
            .collection(orders).document(today)
            .set(entry)
            .addOnSuccessListener {
                // set user order in firestore so manager can see them
                firestore.collection(orders).document(today)
                    .set(orderEntryManager)
                    .addOnSuccessListener {
                        for (item in products) {
                            updateProductQuantity(reference, item.title, item.quantity)
                        }

                        // empty the shopping cart
                        emptyShoppingCart(firestore, user.email!!)
                        products = emptyArray()

                        // update view
                        updateView(products)
                    }
                    .addOnFailureListener { e ->
                        Log.w("FIREBASE_DATABASE", "Failed to upload data", e)
                        Toast.makeText(
                            baseContext,
                            getString(R.string.order_failure),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Failed to upload data", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.order_failure),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /**
     * Update the product quantity on firebase firestore
     */
    private fun updateProductQuantity(
        reference: DatabaseReference,
        productTitle: String,
        quantity: Int
    ) {
        reference.child(productTitle).child("quantity")
            .get()
            .addOnSuccessListener { result ->
                val oldQuantity = result.value as Long

                reference.child(productTitle).child("quantity")
                    .setValue((oldQuantity - quantity))
                    .addOnSuccessListener {
                        Log.d(
                            "FIREBASE_DATABASE",
                            "Data uploaded with success"
                        )
                        Toast.makeText(
                            baseContext,
                            getString(R.string.order_success),
                            Toast.LENGTH_LONG
                        ).show()

                    }
                    .addOnFailureListener { e ->
                        Log.w(
                            "FIREBASE_DATABASE",
                            "Failed to upload data",
                            e
                        )
                        Toast.makeText(
                            baseContext,
                            getString(R.string.order_failure),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun emptyShoppingCart(firestore: FirebaseFirestore, userEmail: String) {
        firestore.collection(clients).document(userEmail)
            .collection(shoppingCart)
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    document.reference.delete()
                }
                Log.d("FIREBASE_FIRESTORE", "Documents deleted")
            }
            .addOnFailureListener {
                Log.w("FIREBASE_FIRESTORE", "Error deleting documents")
            }
    }

    /**
     * Get the total price
     */
    private fun getTotalPrice(): Double {
        for (item in products) {
            total += (item.price * item.quantity)

        }
        return total // 2 decimals
    }

    /**
     * Fetch the shopping cart items from cloud
     */
    private fun fetchItemsFromCloud() {
        total = 0.00
        products = emptyArray()

        val user = auth.currentUser

        if (user != null) {
            firestore.collection(clients).document(user.email!!)
                .collection(shoppingCart)
                .get()
                .addOnSuccessListener { result ->
                    for (document in result.documents) {
                        products = products.plus(
                            ProductItem(
                                "",
                                document.getString("title") as String,
                                "",
                                document.getDouble("price") as Double,
                                document.getLong("qty")!!.toInt()
                            )
                        )
                    }

                    // update view
                    updateView(products)
                }
                .addOnFailureListener { e ->
                    Log.w("FIREBASE_FIRESTORE", "Error fetching data", e)
                    Toast.makeText(
                        baseContext,
                        getString(R.string.error_user_data),
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            Toast.makeText(
                baseContext,
                getString(R.string.error_user_data),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Update the view: if there are products in the shopping cart, then show them, otherwise
     * show "empty cart"
     */
    private fun updateView(products: Array<ProductItem>) {
        // if client has put some items in shopping cart then he can place the order
        if (products.isNotEmpty()) {
            binding.emptyCartLabel.visibility = View.INVISIBLE
            binding.checkoutBtn.visibility = View.VISIBLE
            binding.shoppingListView.visibility = View.VISIBLE

            binding.shoppingListView.adapter = ShoppingCartArrayAdapter(
                this,
                R.layout.list_element_shopping_cart,
                products
            )
            binding.totalPriceLabel.text =
                String.format("%s %.2f", getString(R.string.total_price), getTotalPrice())

        } else { // empty cart
            binding.emptyCartLabel.visibility = View.VISIBLE
            binding.checkoutBtn.visibility = View.INVISIBLE
            binding.shoppingListView.visibility = View.INVISIBLE

            binding.totalPriceLabel.text = ("${getString(R.string.total_price)} 0.00 ₹")
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBundle("dialog", dialog?.onSaveInstanceState())
        outState.putParcelable("listView", binding.shoppingListView.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getBundle("dialog")?.let { dialog?.onRestoreInstanceState(it) }
        binding.shoppingListView.onRestoreInstanceState(savedInstanceState.getParcelable("listView"))
    }

    override fun onDestroy() {
        super.onDestroy()
        products = emptyArray()
        total = 0.00
    }
}