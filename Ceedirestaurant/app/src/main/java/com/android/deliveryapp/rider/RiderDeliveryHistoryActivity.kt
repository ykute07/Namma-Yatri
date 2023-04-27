package com.android.deliveryapp.rider

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityRiderDeliveryHistoryBinding
import com.android.deliveryapp.rider.adapters.RiderHistoryArrayAdapter
import com.android.deliveryapp.util.Keys.Companion.deliveryHistory
import com.android.deliveryapp.util.Keys.Companion.riders
import com.android.deliveryapp.util.RiderHistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RiderDeliveryHistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiderDeliveryHistoryBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderDeliveryHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser

        if (user != null) {
            getDeliveryHistory(firestore, user.email!!)
        }

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun getDeliveryHistory(firestore: FirebaseFirestore, email: String) {
        firestore.collection(riders).document(email)
            .collection(deliveryHistory)
            .get()
            .addOnSuccessListener { result ->
                var deliveryHistory: Array<RiderHistoryItem> = emptyArray()

                for (document in result.documents) {
                    deliveryHistory = deliveryHistory.plus(
                        RiderHistoryItem(
                            document.getString("date") as String,
                            document.getString("location") as String,
                            document.getString("outcome") as String
                        )
                    )
                }
                deliveryHistory.reverse()
                updateView(deliveryHistory)
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

    private fun updateView(deliveries: Array<RiderHistoryItem>) {
        if (deliveries.isNullOrEmpty()) {
            binding.empty.visibility = View.VISIBLE
            binding.deliveryHistoryListView.visibility = View.INVISIBLE
        } else {
            binding.empty.visibility = View.INVISIBLE
            binding.deliveryHistoryListView.visibility = View.VISIBLE

            binding.deliveryHistoryListView.adapter = RiderHistoryArrayAdapter(
                this,
                R.layout.rider_history_list_element,
                deliveries
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(
            "listView",
            binding.deliveryHistoryListView.onSaveInstanceState()
        )
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.deliveryHistoryListView
            .onRestoreInstanceState(savedInstanceState.getParcelable("listView"))
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