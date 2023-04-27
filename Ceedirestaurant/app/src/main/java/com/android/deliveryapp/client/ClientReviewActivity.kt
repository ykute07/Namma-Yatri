package com.android.deliveryapp.client

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.ThemeActivity
import com.android.deliveryapp.databinding.ActivityClientProfileBinding
import com.android.deliveryapp.databinding.ActivityClientReviewBinding
import com.android.deliveryapp.util.Keys
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ClientReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientReviewBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.reviewRestarurant.setOnClickListener{
            startActivity(
                Intent(
                    this@ClientReviewActivity,
                    ReviewRestaurantActivity::class.java
                )
            )
        }
        binding.reviewDriver.setOnClickListener{
            startActivity(
                Intent(
                    this@ClientReviewActivity,
                    ReviewRiderActivity::class.java
                )
            )
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
                startActivity(Intent(this@ClientReviewActivity, ClientHomeActivity::class.java))
                finish()
            }
            R.id.logout -> {
                auth.signOut()

                val sharedPreferences = getSharedPreferences(Keys.userInfo, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // delete all shared preferences
                editor.apply()

                startActivity(Intent(this@ClientReviewActivity, LoginActivity::class.java))
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}