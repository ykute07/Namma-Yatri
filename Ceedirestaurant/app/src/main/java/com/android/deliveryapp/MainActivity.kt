package com.android.deliveryapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.android.deliveryapp.client.ClientHomeActivity
import com.android.deliveryapp.manager.ManagerHomeActivity
import com.android.deliveryapp.rider.RiderProfileActivity
import com.android.deliveryapp.util.Keys.Companion.CLIENT
import com.android.deliveryapp.util.Keys.Companion.MANAGER
import com.android.deliveryapp.util.Keys.Companion.RIDER
import com.android.deliveryapp.util.Keys.Companion.hasLocation
import com.android.deliveryapp.util.Keys.Companion.invalidUser
import com.android.deliveryapp.util.Keys.Companion.isLogged
import com.android.deliveryapp.util.Keys.Companion.isRegistered
import com.android.deliveryapp.util.Keys.Companion.pwd
import com.android.deliveryapp.util.Keys.Companion.themePref
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.deliveryapp.util.Keys.Companion.userType
import com.android.deliveryapp.util.Keys.Companion.username
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

/**
 * Splash screen activity
 */
class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide() // hide action bar

        auth = FirebaseAuth.getInstance()

        Handler(Looper.getMainLooper()).postDelayed({
            GlobalScope.launch {
                getUserData()
            }
        }, 1500) // wait 1.5 seconds, then show the activity

    }

    private fun getTheme(sharedPreferences: SharedPreferences) {
        // if has location > 10 km from market
        if (sharedPreferences.getBoolean(invalidUser, false)) {
            showErrorDialog()
        } else {
            // set theme
            when (sharedPreferences.getInt(themePref, -1)) {
                AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_NO
                )
                AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_YES
                )
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                )
            }
        }
    }

    private fun getUserData() {
        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        GlobalScope.launch {
            getTheme(sharedPreferences)
        }

        if (sharedPreferences.getBoolean(isRegistered, false)) {
            if (sharedPreferences.getBoolean(isLogged, false)) {
                val email = sharedPreferences.getString(username, null)
                val password = sharedPreferences.getString(pwd, null)

                // if client is already registered, so has already a location
                editor.putBoolean(hasLocation, true)
                editor.apply()

                auth.signInWithEmailAndPassword(email ?: "error", password ?: "error")
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d("FIREBASE_AUTH", "User logged successfully")
                            when (sharedPreferences.getString(userType, null)) {
                                CLIENT -> startActivity(
                                    Intent(
                                        this@MainActivity,
                                        ClientHomeActivity::class.java
                                    )
                                )
                                RIDER -> startActivity(
                                    Intent(
                                        this@MainActivity,
                                        RiderProfileActivity::class.java
                                    )
                                )
                                MANAGER -> startActivity(
                                    Intent(
                                        this@MainActivity,
                                        ManagerHomeActivity::class.java
                                    )
                                )
                                else -> startActivity(
                                    Intent(
                                        this@MainActivity,
                                        SelectUserTypeActivity::class.java
                                    )
                                )
                            }
                        } else {
                            Log.w(
                                "FIREBASE_AUTH", "Failed to log user",
                                task.exception
                            )

                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    LoginActivity::class.java
                                )
                            )
                        }
                    }
            } else {
                startActivity(
                    Intent(
                        this@MainActivity,
                        LoginActivity::class.java
                    )
                )
            }
        } else {
            editor.putBoolean(hasLocation, false)
            editor.apply()

            startActivity(
                Intent(
                    this@MainActivity,
                    SelectUserTypeActivity::class.java
                )
            )
        }
    }

    private fun showErrorDialog() {
        val dialogView =
            LayoutInflater.from(baseContext).inflate(R.layout.location_distance_error_dialog, null)

        val dialog: AlertDialog?

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(getString(R.string.too_far_title))

        val confirmButton: ExtendedFloatingActionButton =
            dialogView.findViewById(R.id.confirmButtonDialog)
        val fixLocationBtn: ExtendedFloatingActionButton =
            dialogView.findViewById(R.id.fixLocationBtn)

        dialog = dialogBuilder.create()
        dialog.show()

        fixLocationBtn.visibility = View.INVISIBLE // button not needed here

        confirmButton.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
            exitProcess(-1) // close the application entirely
        }
    }
}
