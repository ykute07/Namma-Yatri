package com.android.deliveryapp.rider

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.databinding.ActivityRiderChatBinding
import com.android.deliveryapp.util.Keys.Companion.chatCollection
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class RiderChatActivity : AppCompatActivity() {
    companion object {
        const val NAME = "NAME"
        const val TEXT = "TEXT"
    }

    private lateinit var binding: ActivityRiderChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRiderChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val riderEmail = intent.getStringExtra("riderEmail")
        val recipientEmail = intent.getStringExtra("recipientEmail")

        if (recipientEmail.isNullOrEmpty())
            Toast.makeText(baseContext, "EMPTY", Toast.LENGTH_SHORT).show()

        val firestoreChat by lazy {
            FirebaseFirestore.getInstance().collection(chatCollection)
                .document("$riderEmail|$recipientEmail")
        }

        updateChat(firestoreChat)

        binding.sendMsgBtn.setOnClickListener {
            sendMessage(firestoreChat)
            binding.message.text?.clear()
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

    private fun sendMessage(reference: DocumentReference) {
        val newMessage = mapOf(
            NAME to "Rider",
            TEXT to binding.message.text.toString()
        )

        reference.set(newMessage)
            .addOnSuccessListener {
                Log.d("FIRESTORE_CHAT", "Message sent")
            }
            .addOnFailureListener { e ->
                Log.e("ERROR", e.message.toString())
            }
    }

    private fun updateChat(reference: DocumentReference) {
        reference.addSnapshotListener { value, error ->
            when {
                error != null -> Log.e("ERROR", error.message.toString())
                value != null && value.exists() -> {
                    with(value) {
                        binding.messageTextView.append("${data?.get(NAME)}:${data?.get(TEXT)}\n")
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("messageTextView", binding.messageTextView.text.toString())
        outState.putString("message", binding.message.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        binding.messageTextView.text = savedInstanceState.getString("messageTextView")
        binding.message.setText(savedInstanceState.getString("message"))
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