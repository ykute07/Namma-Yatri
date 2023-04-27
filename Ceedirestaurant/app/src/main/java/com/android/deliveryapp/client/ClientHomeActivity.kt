package com.android.deliveryapp.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.ThemeActivity
import com.android.deliveryapp.client.adapters.ClientArrayAdapter
import com.android.deliveryapp.databinding.ActivityClientHomeBinding
import com.android.deliveryapp.util.Keys.Companion.chatCollection
import com.android.deliveryapp.util.Keys.Companion.clients
import com.android.deliveryapp.util.Keys.Companion.productListFirebase
import com.android.deliveryapp.util.Keys.Companion.shoppingCart
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.deliveryapp.util.Keys.Companion.users
import com.android.deliveryapp.util.ProductItem
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class ClientHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClientHomeBinding
    private lateinit var database: FirebaseDatabase // product names and prices
    private lateinit var firestore: FirebaseFirestore // shopping cart
    private lateinit var auth: FirebaseAuth
    private lateinit var productList: Array<ProductItem>

    private var dialog: AlertDialog? = null

    private var singleProductCount = 0

    private val channelID = "1"
    private val notificationID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()

        firestore = FirebaseFirestore.getInstance()

        val databaseRef = database.getReference(productListFirebase)
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        if (user != null) {
            fetchDatabase(databaseRef)
            firestore.collection(users).document(user.email!!)
                .get()
                .addOnSuccessListener { result ->
                    binding.welcomeLabel.text="Welcome ${result.get("Name")}"

                }
                .addOnFailureListener { e ->
                    Log.w("FIREBASEFIRESTORE", "Error Getting Name", e)
                    Toast.makeText(
                        baseContext,
                        "sorry",
                        Toast.LENGTH_LONG
                    ).show()
                }

            binding.shoppingCartButton.setOnClickListener {
                val stringRequest: StringRequest = object : StringRequest( Method.POST, "http://13.235.139.60/sandbox/bap/trigger/init",
                    Response.Listener { response ->


                        try {

                        Toast.makeText(baseContext,"Successfully init",Toast.LENGTH_SHORT).show()

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
                        params2.put("use_case", "init/adding_billing_and_shipping_details_during_checkout")
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
                startActivity(Intent(this@ClientHomeActivity, ShoppingCartActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val reference = database.getReference(productListFirebase)

        fetchDatabase(reference)
    }

    private fun fetchDatabase(reference: DatabaseReference) {
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                processItems(snapshot) // create the product list

                updateView()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FIREBASE_DATABASE", "Failed to retrieve items", error.toException())
            }
        })
    }

    private fun updateView() {
        if (productList.isNotEmpty()) {
            binding.emptyListClient.visibility = View.INVISIBLE
            binding.productListView.visibility = View.VISIBLE

            binding.productListView.adapter = ClientArrayAdapter(
                this@ClientHomeActivity, R.layout.list_element, productList
            )
        } else {
            binding.emptyListClient.visibility = View.VISIBLE
            binding.productListView.visibility = View.INVISIBLE
        }
    }

    override fun onStart() {
        super.onStart()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(
            this@ClientHomeActivity,
            ClientChatActivity::class.java
        )

        val pendingIntent = PendingIntent.getActivity(
            this@ClientHomeActivity,
            0,
            intent,
            0
        )

        val user = auth.currentUser

        /************************************* NOTIFICATIONS **************************************/

        if (user != null) {
            listenForDeliveryMessages(firestore, pendingIntent, notificationManager, user.email!!)
        }

        /***************************** ADD TO CART DIALOG ****************************************/

        binding.productListView.setOnItemClickListener { _, _, i, _ ->
            showAddToCartDialog(i, productList)
            val stringRequest: StringRequest = object : StringRequest( Method.POST, " http://13.235.139.60/sandbox/bap/trigger/select",
                Response.Listener { response ->


                    try {

                        Toast.makeText(baseContext,"Successfully select ",Toast.LENGTH_LONG).show()

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                }) {
                override fun getBody(): ByteArray {
                    val params2 = HashMap<String, String>()
                    params2.put("domain","delivery-0.9.2" )
                    params2.put("use_case", "select/add_item_from_a_single_provider")
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
        }
    }

    private fun showAddToCartDialog(i: Int, productList: Array<ProductItem>) {
        singleProductCount = 0
        val productTitle: String =
            productList[i].title.capitalize(Locale.ROOT) // capitalize first letter

        val dialogView = LayoutInflater.from(this).inflate(R.layout.client_product_dialog, null)

        val dialogImage: ImageView = dialogView.findViewById(R.id.productImageDialog)
        dialogImage.load(productList[i].imgUrl) {
            transformations(CircleCropTransformation())
            error(R.drawable.error_image)
            crossfade(true)
            build()
        }

        val dialogProductPrice: TextView = dialogView.findViewById(R.id.productPriceDialog)
        dialogProductPrice.text = String.format("%.2f ₹", productList[i].price)

        val productDesc: TextView = dialogView.findViewById(R.id.descriptionDialog)
        productDesc.text = productList[i].description

        val productQty: TextInputEditText = dialogView.findViewById(R.id.productQtyCounter)
        productQty.setText("$singleProductCount")
        productQty.keyListener = null // not editable with keyboard but visible

        val removeQty: FloatingActionButton = dialogView.findViewById(R.id.minusButton)

        val addQty: FloatingActionButton = dialogView.findViewById(R.id.plusButton)

        val addToCart: FloatingActionButton = dialogView.findViewById(R.id.addProductButton)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(productTitle)

        dialog = dialogBuilder.create()
        dialog!!.show()

        /************************ ALERT DIALOG BUTTONS ****************************************/

        removeQty.setOnClickListener {
            if (singleProductCount == 0) {
                dialog!!.dismiss()
            } else {
                productQty.setText((--singleProductCount).toString())
            }
        }

        addQty.setOnClickListener {
            // product desired by the user == quantity available
            if (singleProductCount == productList[i].quantity) {
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_product_quantity),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                productQty.setText((++singleProductCount).toString())
            }
        }

        addToCart.setOnClickListener {
            if (singleProductCount == 0) {
                Toast.makeText(
                    baseContext,
                    getString(R.string.please_add_quantity),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // add the new entry
                addToShoppingCart(
                    auth,
                    firestore,
                    productList[i].title,
                    productList[i].price,
                    singleProductCount
                )
                dialog!!.dismiss()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        productList = emptyArray()
        singleProductCount = 0
    }

    /**
     * @param snapshot the firebase realtime database snapshot
     * @return an array containing data related to the market products
     * if an item has quantity 0 it will not be shown at the user (CLIENT)
     */
    private fun processItems(snapshot: DataSnapshot) {
        var imageUrl = ""
        var title = ""
        var desc = ""
        var price = 0.00
        var qty: Long = 0

        productList = emptyArray()

        for (child in snapshot.children) {
            for (item in child.children) {
                when (item.key) {
                    "image" -> imageUrl = item.value as String
                    "title" -> title = item.value as String
                    "description" -> desc = item.value as String
                    "price" -> {
                        price = try {
                            item.value as Double
                        } catch (e: ClassCastException) {
                            val priceLong = item.value as Long
                            priceLong.toDouble()
                        }
                    }
                    "quantity" -> qty = item.value as Long
                }
            }
            if (qty.toInt() != 0) { // don't add items with qty 0
                productList =
                    productList.plus(ProductItem(imageUrl, title, desc, price, qty.toInt()))
            }
        }
    }

    /**
     * add a new entry to the database with a subcollection
     * "user.id/shoppingCart/product.title/"
     * @param auth firebase auth instance
     * @param firestore firestore instance
     * @param title product title
     * @param price product price
     * @param quantity product quantity
     */
    private fun addToShoppingCart(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        title: String,
        price: Double,
        quantity: Int
    ) {
        val user = auth.currentUser

        if (user != null) {
            val entry = mapOf(
                "title" to title,
                "price" to price,
                "qty" to quantity
            )

            // set entry under "user.orderEmail/shoppingCart/product.title"
            firestore.collection(clients).document(user.email!!)
                .collection(shoppingCart).document(title)
                .set(entry)
                .addOnSuccessListener { documentRef ->
                    Log.d("FIREBASEFIRESTORE", "Document added with id: $documentRef")
                    Toast.makeText(
                        baseContext,
                        getString(R.string.add_cart_success),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .addOnFailureListener { e ->
                    Log.w("FIREBASEFIRESTORE", "Error adding document", e)
                    Toast.makeText(
                        baseContext,
                        getString(R.string.error_shopping_cart),
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            auth.currentUser?.reload()
            Toast.makeText(
                baseContext,
                getString(R.string.error_shopping_cart),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Listen for rider messages on firestore chats
     * @param email client email
     */
    private fun listenForDeliveryMessages(
        firestore: FirebaseFirestore,
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager,
        email: String
    ) {
        firestore.collection(chatCollection).get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    if (document.id.contains(email) && document.getString("NAME") == "Rider") {
                        document.reference.addSnapshotListener { value, error ->
                            if (error != null) {
                                Log.w("FIREBASE_CHAT", "Listen failed", error)
                                return@addSnapshotListener
                            } else {
                                if (value != null) { // if message sent is from rider notify
                                    if (value.contains("NAME")
                                        && value.getString("NAME") as String == "Rider"
                                    ) {
                                        createNotification(pendingIntent, notificationManager)
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
        notificationManager: NotificationManager
    ) {
        val notification = Notification.Builder(this@ClientHomeActivity, channelID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.new_message_notification_title))
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.client_home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        auth = FirebaseAuth.getInstance()

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        return when (item.itemId) {
            R.id.clientProfile -> {
                startActivity(
                    Intent(
                        this@ClientHomeActivity,
                        ClientProfileActivity::class.java
                    )
                )
                true
            }
            R.id.orders -> {
                startActivity(
                    Intent(
                        this@ClientHomeActivity,
                        ClientOrdersActivity::class.java
                    )
                )
                true
            }
            R.id.shoppingCart -> {
                startActivity(
                    Intent(
                        this@ClientHomeActivity,
                        ShoppingCartActivity::class.java
                    )
                )
                true
            }
            R.id.clientReview -> {
                startActivity(
                    Intent(
                        this@ClientHomeActivity,
                        ClientReviewActivity::class.java
                    )
                )
                true
            }
            R.id.theme -> {
                startActivity(
                    Intent(
                        this@ClientHomeActivity,
                        ThemeActivity::class.java
                    )
                )
                true
            }
            R.id.logout -> {
                auth.signOut()
                editor.clear() // delete all shared preferences
                editor.apply()

                startActivity(
                    Intent(
                        this@ClientHomeActivity,
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

        outState.putBundle("dialog", dialog?.onSaveInstanceState())
        outState.putParcelable("listView", binding.productListView.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getBundle("dialog")?.let { dialog?.onRestoreInstanceState(it) }
        binding.productListView.onRestoreInstanceState(
            savedInstanceState
                .getParcelable("listView")
        )
    }
}