package com.android.deliveryapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.databinding.ActivitySelectUserTypeBinding
import com.android.deliveryapp.util.Keys.Companion.CLIENT
import com.android.deliveryapp.util.Keys.Companion.MANAGER
import com.android.deliveryapp.util.Keys.Companion.RIDER
import com.android.deliveryapp.util.Keys.Companion.hasLocation
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.deliveryapp.util.Keys.Companion.userType
import com.android.deliveryapp.util.Keys.Companion.users
import com.google.firebase.firestore.FirebaseFirestore

class SelectUserTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectUserTypeBinding
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectUserTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        binding.radioGroup.setOnCheckedChangeListener { _, _ ->
            // check if manager already exists
            if (binding.manager.isChecked) {
                firestore.collection(users).get()
                    .addOnSuccessListener { result ->
                        for (document in result.documents) {
                            if (document.getString(userType) as String == MANAGER) { // if manager already exists
                                updateView()
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            baseContext,
                            getString(R.string.failure_data),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        binding.confirmButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            if (binding.client.isChecked) {
                editor.putString(userType, CLIENT)
                editor.putBoolean(hasLocation, false)

                editor.apply()

                startActivity(Intent(this@SelectUserTypeActivity, SignUpActivity::class.java))
            }
            if (binding.rider.isChecked) {

                editor.putString(userType, RIDER)
                editor.putBoolean(hasLocation, false)
                editor.apply()

                startActivity(Intent(this@SelectUserTypeActivity,OnboardingRiderActivity::class.java))


            }
            if (binding.manager.isChecked) {
                editor.putString(userType, MANAGER)
                editor.putBoolean(hasLocation, false)

                editor.apply()

                startActivity(Intent(this@SelectUserTypeActivity, SignUpActivity::class.java))
            }


            finish()
        }

        binding.hasAccount.setOnClickListener {
            startActivity(Intent(this@SelectUserTypeActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun updateView() {
        Toast.makeText(
            baseContext,
            getString(R.string.manager_existence_error),
            Toast.LENGTH_LONG
        ).show()

        binding.client.isChecked = true
        binding.manager.isChecked = false
        binding.manager.isClickable = false // user cannot check manager anymore
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("clientRadioButton", binding.client.isChecked)
        outState.putBoolean("riderRadioButton", binding.rider.isChecked)
        outState.putBoolean("managerRadioButton", binding.manager.isChecked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.client.isChecked = savedInstanceState.getBoolean("clientRadioButton")
        binding.rider.isChecked = savedInstanceState.getBoolean("riderRadioButton")
        binding.manager.isChecked = savedInstanceState.getBoolean("managerRadioButton")
    }
}