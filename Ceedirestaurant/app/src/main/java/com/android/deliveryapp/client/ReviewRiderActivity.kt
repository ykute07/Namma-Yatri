package com.android.deliveryapp.client

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.ThemeActivity
import com.android.deliveryapp.databinding.ActivityReviewRiderBinding
import com.android.deliveryapp.util.Keys
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.util.*

class ReviewRiderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReviewRiderBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var reviewbyuser :String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReviewRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firestore = FirebaseFirestore.getInstance()

        binding.button.setOnClickListener {
            if (binding.loginEmail?.text.isNullOrEmpty() && binding.reviewMessage.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Email or Review is Empty", Toast.LENGTH_SHORT).show()
            } else {
                reviewbyuser = binding.reviewMessage.text.toString()


                val entry = mapOf(
                    "review" to reviewbyuser

                )

                firestore.collection(Keys.riders)
                    .document(binding.loginEmail.text.toString()).collection(Keys.review)
                    .document(getDate())
                    .set(entry)
                    .addOnSuccessListener {
                        Toast.makeText(
                            baseContext,
                            "Review Added Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Log.w("FIREBASE_DATABASE", "Failed to upload data", e)
                        Toast.makeText(
                            baseContext,
                            getString(R.string.order_failure),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                startActivity(Intent(this@ReviewRiderActivity, ClientHomeActivity::class.java))
                finish()
            }

        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.client_home_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        auth = FirebaseAuth.getInstance()
        when (item.itemId) {
            R.id.homePage -> {
                startActivity(Intent(this@ReviewRiderActivity, ClientHomeActivity::class.java))
                finish()
            }
            R.id.logout -> {
                auth.signOut()

                val sharedPreferences = getSharedPreferences(Keys.userInfo, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // delete all shared preferences
                editor.apply()

                startActivity(Intent(this@ReviewRiderActivity, LoginActivity::class.java))
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun getDate(): String {
        val today: Date = Calendar.getInstance().time

        return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM).format(today)
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        return super.dispatchTouchEvent(event)
    }
}