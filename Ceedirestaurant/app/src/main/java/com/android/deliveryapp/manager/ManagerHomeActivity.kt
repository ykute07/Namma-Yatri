package com.android.deliveryapp.manager

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.ThemeActivity
import com.android.deliveryapp.databinding.ActivityManagerHomeBinding
import com.android.deliveryapp.manager.adapters.ManagerArrayAdapter
import com.android.deliveryapp.util.Keys.Companion.YET_TO_RESPOND
import com.android.deliveryapp.util.Keys.Companion.orders
import com.android.deliveryapp.util.Keys.Companion.productListFirebase
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.deliveryapp.util.Keys.Companion.userType
import com.android.deliveryapp.util.ProductItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class ManagerHomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerHomeBinding
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var productList: Array<ProductItem>
    private lateinit var firestore: FirebaseFirestore

    private var dialog: AlertDialog? = null

    private val channelID = "1"
    private val notificationID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance()
        val databaseRef = database.getReference(productListFirebase)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchDatabase(databaseRef)

        binding.addProductButton.setOnClickListener {
            startActivity(
                Intent(
                    this@ManagerHomeActivity,
                    AddProductActivity::class.java
                )
            )
        }

        binding.ridersMapBtn.setOnClickListener {
            startActivity(
                Intent(
                    this@ManagerHomeActivity,
                    RidersMapActivity::class.java
                )
            )
        }
    }

    private fun fetchDatabase(reference: DatabaseReference) {
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList = processItems(snapshot)

                updateView()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FIREBASE_DATABASE", "Failed to retrieve items", error.toException())
            }
        })
    }

    override fun onResume() {
        super.onResume()

        val databaseRef = database.getReference(productListFirebase)

        fetchDatabase(databaseRef)
    }

    override fun onStart() {
        super.onStart()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(this@ManagerHomeActivity, ManagerOrderActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this@ManagerHomeActivity,
            0,
            intent,
            0
        )

        listenForNewOrders(firestore, pendingIntent, notificationManager)

        val databaseRef = database.getReference(productListFirebase)

        binding.productListView.setOnItemClickListener { _, _, i, _ ->
            showItemDialog(i, databaseRef)

            // update list view even if there are no changes
            fetchDatabase(databaseRef)
        }
    }

    private fun showItemDialog(i: Int, reference: DatabaseReference) {
        val productTitle: String =
            productList[i].title.capitalize(Locale.ROOT) // capitalize first letter

        val dialogView = LayoutInflater.from(this).inflate(R.layout.manager_product_dialog, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(productTitle)

        val image: ImageView = dialogView.findViewById(R.id.managerImageDialog)

        image.load(intent.getStringExtra("url") ?: productList[i].imgUrl) {
            transformations(CircleCropTransformation())
            error(R.drawable.error_image)
            crossfade(true)
            build()
        }

        val productName: TextInputEditText = dialogView.findViewById(R.id.managerProductTitleDialog)
        val productDesc: TextInputEditText = dialogView.findViewById(R.id.managerDescriptionDialog)
        val productPrice: TextInputEditText = dialogView.findViewById(R.id.managerPriceDialog)
        val productQty: TextInputEditText =
            dialogView.findViewById(R.id.managerProductCounterDialog)

        val doneBtn: FloatingActionButton = dialogView.findViewById(R.id.editDoneBtn)

        productName.setText(productList[i].title)
        productDesc.setText(productList[i].description)
        productPrice.setText(String.format("%.2f", productList[i].price))
        productQty.setText(productList[i].quantity.toString())

        dialog = dialogBuilder.create()
        dialog!!.show()

        val intent = Intent(
            this@ManagerHomeActivity,
            ChangeProductImageActivity::class.java
        )
        intent.putExtra("name", productName.text.toString())

        image.setOnClickListener { // manager wants to modify product image
            startActivity(intent)
            dialog!!.dismiss()
        }

        doneBtn.setOnClickListener {
            // if extra is null/"" take it from the list
            val imageUrl = intent.getStringExtra("url") ?: productList[i].imgUrl

            val price: Double = if (productPrice.text.toString().length == 1) {
                "${productPrice.text.toString()}.00".toDouble()
            } else {
                productPrice.text.toString().toDouble()
            }

            if (productPrice.text.toString()
                    .startsWith("0") && productPrice.text.toString().length == 1
            ) {
                productPrice.error = getString(R.string.error_invalid_price)
            } else {
                updateItemValues(
                    reference,
                    imageUrl,
                    productName.text.toString().toLowerCase(Locale.ROOT),
                    productDesc.text.toString(),
                    price,
                    productQty.text.toString().toInt(),
                    productTitle
                )

                dialog!!.dismiss()
            }
        }
    }

    private fun updateItemValues(
        reference: DatabaseReference,
        imgUrl: String,
        title: String,
        desc: String,
        price: Double,
        quantity: Int,
        oldTitle: String
    ) {
        reference.child(oldTitle).removeValue() // remove the old entry
            .addOnSuccessListener {
                Log.d("FIREBASE_DATABASE", "Data removed with success")

                val entry = mapOf<String, Any?>(
                    "description" to desc,
                    "image" to imgUrl,
                    "price" to price,
                    "title" to title,
                    "quantity" to quantity
                )

                reference.child(title)
                    .updateChildren(entry)
                    .addOnSuccessListener { // add the new entry with updated values
                        Log.d("FIREBASE_DATABASE", "Data uploaded with success")
                        Toast.makeText(
                            baseContext,
                            getString(R.string.data_update_success),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_DATABASE", "Error removing data", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_updating_database),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateView() {
        if (productList.isNotEmpty()) {
            binding.emptyList.visibility = View.INVISIBLE
            binding.productListView.visibility = View.VISIBLE

            binding.productListView.adapter = ManagerArrayAdapter(
                this@ManagerHomeActivity, R.layout.list_element, productList
            )
        } else {
            binding.emptyList.visibility = View.VISIBLE
            binding.productListView.visibility = View.INVISIBLE
        }
    }

    private fun processItems(snapshot: DataSnapshot): Array<ProductItem> {
        var imageUrl = ""
        var title = ""
        var desc = ""
        var price = 0.00
        var qty: Long = 0

        var array = emptyArray<ProductItem>()

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
            array = array.plus(ProductItem(imageUrl, title, desc, price, qty.toInt()))
        }
        return array
    }

    /**
     * Listen for new orders and sends a notification
     * @param firestore firestore instance
     */
    private fun listenForNewOrders(
        firestore: FirebaseFirestore,
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager
    ) {
        firestore.collection(orders).addSnapshotListener { value, error ->
            if (error != null) {
                Log.w("FIREBASE_FIRESTORE", "Listen failed", error)
                return@addSnapshotListener
            } else {
                if (value != null) {
                    for (document in value.documents) {
                        if (document.contains("outcome")
                            && document.getString("outcome") as String == YET_TO_RESPOND
                        ) { // if there is a new order
                            createNotification(pendingIntent, notificationManager)
                            createNotificationChannel(
                                channelID,
                                getString(R.string.app_name),
                                getString(R.string.new_order_notification_msg),
                                notificationManager
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotification(
        pendingIntent: PendingIntent,
        notificationManager: NotificationManager
    ) {
        val notification = Notification.Builder(this@ManagerHomeActivity, channelID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.new_order_notification_msg))
            .setContentText(getString(R.string.new_order_description))
            .setChannelId(channelID)
            .setAutoCancel(true)
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
        inflater.inflate(R.menu.manager_home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        auth = FirebaseAuth.getInstance()
        deleteSharedPreferences(userType)
        return when (item.itemId) {
            R.id.managerProfile -> {
                startActivity(
                    Intent(
                        this@ManagerHomeActivity,
                        ManagerProfileActivity::class.java
                    )
                )
                true
            }
            R.id.feedback -> {
                startActivity(
                    Intent(
                        this@ManagerHomeActivity,
                        ManagerFeedbackActivity::class.java
                    )
                )
                true
            }
            R.id.ridersList -> {
                startActivity(
                    Intent(
                        this@ManagerHomeActivity,
                        ManagerRidersListActivity::class.java
                    )
                )
                true
            }
            R.id.orders -> {
                startActivity(
                    Intent(
                        this@ManagerHomeActivity,
                        ManagerOrderActivity::class.java
                    )
                )
                true
            }
            R.id.theme -> {
                startActivity(
                    Intent(
                        this@ManagerHomeActivity,
                        ThemeActivity::class.java
                    )
                )
                true
            }
            R.id.logout -> {
                auth.signOut()
                val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // delete all shared preferences
                editor.apply()

                startActivity(
                    Intent(
                        this@ManagerHomeActivity,
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
        binding.productListView.onRestoreInstanceState(savedInstanceState.getParcelable("listView"))
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