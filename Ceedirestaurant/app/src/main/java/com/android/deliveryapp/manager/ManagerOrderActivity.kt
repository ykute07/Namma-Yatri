package com.android.deliveryapp.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityManagerOrderBinding
import com.android.deliveryapp.manager.adapters.ManagerOrdersArrayAdapter
import com.android.deliveryapp.manager.adapters.OrderDetailAdapter
import com.android.deliveryapp.util.Keys.Companion.REJECTED
import com.android.deliveryapp.util.Keys.Companion.YET_TO_RESPOND
import com.android.deliveryapp.util.Keys.Companion.YET_TO_RESPOND_BY_RIDER
import com.android.deliveryapp.util.Keys.Companion.clientEmail
import com.android.deliveryapp.util.Keys.Companion.managerPref
import com.android.deliveryapp.util.Keys.Companion.orderDate
import com.android.deliveryapp.util.Keys.Companion.orders
import com.android.deliveryapp.util.ManagerOrderItem
import com.android.deliveryapp.util.ProductItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class ManagerOrderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerOrderBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var orderList: Array<ManagerOrderItem>
    private lateinit var products: Array<ProductItem>

    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        orderList = emptyArray()
        products = emptyArray()

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // get orders and updateview
        getOrders(firestore)

        binding.ordersList.setOnItemClickListener { _, _, i, _ ->
            if (orderList[i].outcome == YET_TO_RESPOND || orderList[i].outcome == REJECTED ||orderList[i].outcome == YET_TO_RESPOND_BY_RIDER) {
                showProductsDialog(i, firestore)
            }
        }
    }

    private fun showProductsDialog(i: Int, firestore: FirebaseFirestore) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.manager_order_list_dialog, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.order_info, orderList[i].email))
            .setView(dialogView)

        val riderBtn: FloatingActionButton = dialogView.findViewById(R.id.selectRidersBtn)

        val productListView: ListView = dialogView.findViewById(R.id.orderProductList)

        dialog = dialogBuilder.create()
        dialog!!.show()

        products = emptyArray()

        // get product list to show in dialog
        firestore.collection(orders).document(orderList[i].date)
            .get()
            .addOnSuccessListener { result ->
                var title = ""
                var price = 0.0
                var quantity: Long = 0

                for (item in result.get("products") as ArrayList<*>) {
                    for (field in item as Map<*, *>) {
                        when (field.key as String) {
                            "title" -> title = field.value as String
                            "price" -> price = field.value as Double
                            "quantity" -> quantity = field.value as Long
                        }
                    }
                    products = products.plus(
                        ProductItem(
                            "",
                            title,
                            "",
                            price,
                            quantity.toInt()
                        )
                    )
                }
                productListView.adapter = OrderDetailAdapter(
                    this,
                    R.layout.manager_order_detail_list_element,
                    products
                )
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Error getting data", e)

                Toast.makeText(
                    baseContext,
                    getString(R.string.failure_data),
                    Toast.LENGTH_LONG
                ).show()
            }

        riderBtn.setOnClickListener { // send order to rider
            val sharedPreferences = getSharedPreferences(managerPref, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            firestore.collection(orders).document(orderList[i].date).update("outcome",
                YET_TO_RESPOND_BY_RIDER).addOnSuccessListener { Toast.makeText(baseContext,"Succesfully Updated",Toast.LENGTH_SHORT) }.addOnFailureListener{e->
                    Toast.makeText(baseContext,"Failed to Update",Toast.LENGTH_SHORT)
            }

            editor.putString(clientEmail, orderList[i].email)
            editor.putString(orderDate, orderList[i].date)

            editor.apply()

            startActivity(
                Intent(
                    this@ManagerOrderActivity,
                    ManagerRidersListActivity::class.java
                )
            )
        }
    }

    private fun getOrders(firestore: FirebaseFirestore) {
        firestore.collection(orders)
            .get()
            .addOnSuccessListener { result ->
                for (document in result.documents) {
                    orderList = orderList.plus(
                        ManagerOrderItem(
                            document.getString("clientEmail") as String,
                            document.getString("date") as String,
                            document.getDouble("total") as Double,
                            document.getString("payment") as String,
                            document.getString("outcome") as String
                        )
                    )
                }
                orderList.reverse()
                updateView()
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Error getting data", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.failure_data),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateView() {
        if (orderList.isNotEmpty()) {
            binding.ordersList.visibility = View.VISIBLE
            binding.emptyOrdersLabel.visibility = View.INVISIBLE

            binding.ordersList.adapter = ManagerOrdersArrayAdapter(
                this,
                R.layout.manager_order_list_element,
                orderList
            )

        } else { // empty
            binding.ordersList.visibility = View.INVISIBLE
            binding.emptyOrdersLabel.visibility = View.VISIBLE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBundle("dialog", dialog?.onSaveInstanceState())
        outState.putParcelable("listView", binding.ordersList.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getBundle("dialog")?.let { dialog?.onRestoreInstanceState(it) }
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
}