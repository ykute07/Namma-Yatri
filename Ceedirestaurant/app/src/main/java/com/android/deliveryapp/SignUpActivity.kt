package com.android.deliveryapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.client.ClientProfileActivity
import com.android.deliveryapp.databinding.ActivitySignUpBinding
import com.android.deliveryapp.manager.ManagerHomeActivity
import com.android.deliveryapp.rider.RiderProfileActivity
import com.android.deliveryapp.util.Keys.Companion.CLIENT
import com.android.deliveryapp.util.Keys.Companion.MANAGER
import com.android.deliveryapp.util.Keys.Companion.RIDER
import com.android.deliveryapp.util.Keys.Companion.isRegistered
import com.android.deliveryapp.util.Keys.Companion.userInfo
import com.android.deliveryapp.util.Keys.Companion.userType
import com.android.deliveryapp.util.Keys.Companion.users
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "EmailPassword"
    }

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var sharedPreferences :SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
         sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)

        when (sharedPreferences.getString(userType, null)) {
            MANAGER -> {
                binding.name?.hint ="Restaurant Name"
            }
            CLIENT ->{
                binding.name?.hint ="Name"
            }
            RIDER ->{
                binding.name?.hint = "Name"
            }
        }
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance() // saves user data in cloud

        binding.nextButton.setOnClickListener {
            checkEmailAndPassword(binding.email, binding.password, binding.confirmPassword)
        }
    }

    /**
     * @param email given by the user
     * @param password given by the user
     * @param confirmPwd must be the same as password in order to confirm
     */
    private fun checkEmailAndPassword(
        email: TextInputEditText,
        password: TextInputEditText,
        confirmPwd: TextInputEditText
    ) {
        if (email.text.isNullOrEmpty()) {
            email.error = getString(R.string.empty_email)
            email.requestFocus()
            return
        }
        if (password.text.isNullOrEmpty()) {
            password.error = getString(R.string.empty_password)
            password.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            email.error = getString(R.string.invalid_email)
            email.requestFocus()
            return
        }
        if (password.text!!.length < 6) {
            password.error = getString(R.string.invalid_password)
            password.requestFocus()
            return
        }
        if (password.text.toString() != confirmPwd.text.toString()) {
            confirmPwd.error = getString(R.string.password_not_match)
            confirmPwd.requestFocus()
            return
        }



        createUser(auth, sharedPreferences, email.text.toString(), password.text.toString())

        return
    }

    private fun createUser(
        auth: FirebaseAuth,
        sharedPreferences: SharedPreferences,
        email: String,
        password: String
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail: SUCCESS")


                    val editor = sharedPreferences.edit()
                    editor.putBoolean(isRegistered, true) // user flagged as registered
                    editor.apply()

                    when (sharedPreferences.getString(userType, null)) {
                        CLIENT -> {
                            saveUserInfo(
                                sharedPreferences.getString(userType, null),
                                firestore,
                                email
                            )
                            startActivity(
                                Intent(
                                    this@SignUpActivity,
                                    ClientProfileActivity::class.java
                                )
                            )
                        }
                        RIDER -> {
                            saveUserInfo(
                                sharedPreferences.getString(userType, null),
                                firestore,
                                email
                            )
                            startActivity(
                                Intent(
                                    this@SignUpActivity,
                                    RiderProfileActivity::class.java
                                )
                            )
                        }
                        MANAGER -> {
                            saveUserInfo(
                                sharedPreferences.getString(userType, null),
                                firestore,
                                email
                            )
                            startActivity(
                                Intent(
                                    this@SignUpActivity,
                                    ManagerHomeActivity::class.java
                                )
                            )
                        }
                    }
                    finish()
                } else {
                    Log.w(TAG, "createUserWithEmail: FAILURE", task.exception)
                    Toast.makeText(
                        baseContext,
                        getString(R.string.sign_up_failure),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun saveUserInfo(userType: String?, firestore: FirebaseFirestore, email: String) {
        val entry = hashMapOf(
            "userType" to userType,
            "Name" to binding.name?.text.toString()
        )

        firestore.collection(users).document(email)
            .set(entry)
            .addOnSuccessListener {
                Log.d("FIRESTORE", "Document added with success")
            }
            .addOnFailureListener { e ->
                Log.w("FIRESTORE", "Failed to add document", e)
            }


    }

    // hide keyboard when user clicks outside EditText
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("orderEmail", binding.email.text.toString())
        outState.putString("pwd", binding.password.text.toString())
        outState.putString("confirmPwd", binding.confirmPassword.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.email.setText(savedInstanceState.getString("orderEmail"))
        binding.password.setText(savedInstanceState.getString("pwd"))
        binding.confirmPassword.setText(savedInstanceState.getString("confirmPwd"))
    }
}


