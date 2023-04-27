package com.android.deliveryapp.manager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.LoginActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityManagerProfileBinding
import com.android.deliveryapp.util.Keys
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ManagerProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        database = FirebaseFirestore.getInstance()

        supportActionBar?.title = getString(R.string.manager)

        val user = auth.currentUser

        if (user != null) {
            binding.managerEmail.setText(user.email) // show orderEmail at the user
            binding.managerEmail.keyListener = null // not editable by user, but still visible

        }

        binding.homepageButton.setOnClickListener {
            startActivity(
                Intent(
                    this@ManagerProfileActivity,
                    ManagerHomeActivity::class.java
                )
            )
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.homePage -> {
                startActivity(Intent(this@ManagerProfileActivity, ManagerHomeActivity::class.java))
                finish()
                true
            }
            R.id.logout -> {
                auth.signOut()
                val sharedPreferences = getSharedPreferences(Keys.userInfo, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.clear() // delete all shared preferences
                editor.apply()

                startActivity(Intent(this@ManagerProfileActivity, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("managerEmail", binding.managerEmail.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.managerEmail.setText(savedInstanceState.getString("managerEmail"))
    }
}