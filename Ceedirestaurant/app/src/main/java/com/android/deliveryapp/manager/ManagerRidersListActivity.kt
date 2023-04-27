package com.android.deliveryapp.manager

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityManagerRidersListBinding
import com.android.deliveryapp.manager.adapters.RiderListArrayAdapter
import com.android.deliveryapp.util.Keys.Companion.riderEmail
import com.android.deliveryapp.util.Keys.Companion.riderStatus
import com.android.deliveryapp.util.Keys.Companion.riders
import com.android.deliveryapp.util.RiderListItem
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

/**
 * Activity used by MANAGER
 */
class ManagerRidersListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerRidersListBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var riderList: Array<RiderListItem>

    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerRidersListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        firestore = FirebaseFirestore.getInstance()

        fetchRiderList(firestore)
    }

    override fun onStart() {
        super.onStart()

        binding.ridersList.setOnItemClickListener { _, _, i, _ ->
            if (!riderList[i].availability) { // if rider isn't available
                showUnavailabilityDialog()
            } else {
                val intent = Intent(
                    this@ManagerRidersListActivity,
                    ManagerRiderActivity::class.java
                )

                intent.putExtra(riderEmail, riderList[i].email)

                startActivity(intent)
            }
        }
    }

    private fun showUnavailabilityDialog() {
        val dialogView = LayoutInflater.from(this).inflate(
            R.layout.rider_unavailable_dialog, null
        )

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)

        val okBtn: ExtendedFloatingActionButton = dialogView.findViewById(R.id.okBtn)

        dialog = dialogBuilder.create()
        dialog!!.show()

        okBtn.setOnClickListener {
            dialog!!.dismiss()
        }
    }

    private fun fetchRiderList(firestore: FirebaseFirestore) {
        riderList = emptyArray()

        firestore.collection(riders).get()
            .addOnSuccessListener { result ->
                var email: String
                var isAvailable: Boolean

                for (document in result.documents) {
                    email = document.id
                    isAvailable = document.getBoolean(riderStatus) as Boolean

                    riderList = riderList.plus(RiderListItem(email, isAvailable))
                }

                updateView()
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_FIRESTORE", "Error getting riders data", e)

                Toast.makeText(
                    baseContext,
                    getString(R.string.error_getting_riders),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun updateView() {
        if (riderList.isNotEmpty()) {
            binding.ridersList.visibility = View.VISIBLE
            binding.empty.visibility = View.INVISIBLE

            binding.ridersList.adapter = RiderListArrayAdapter(
                this,
                R.layout.manager_rider_list_element,
                riderList
            )
        } else {
            binding.empty.visibility = View.VISIBLE
            binding.ridersList.visibility = View.INVISIBLE
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
        outState.putParcelable("listView", binding.ridersList.onSaveInstanceState())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.getBundle("dialog")?.let { dialog?.onRestoreInstanceState(it) }
        binding.ridersList.onRestoreInstanceState(savedInstanceState.getParcelable("listView"))
    }
}