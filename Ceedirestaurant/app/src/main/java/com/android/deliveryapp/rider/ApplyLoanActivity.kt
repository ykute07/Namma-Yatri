package com.android.deliveryapp.rider

import android.R
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.databinding.ActivityApplyLoanBinding


class ApplyLoanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityApplyLoanBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApplyLoanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.thorughOurPortal.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("APPLY FOR LOAN THROUGH OUR PORTAL")
                .setMessage("Your work data will be shared with the portal bank") // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(
                    R.string.yes,
                    DialogInterface.OnClickListener { dialog, which ->
                        // Continue with delete operation
                    }) // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.no, null)
                .setIcon(R.drawable.ic_dialog_alert)
                .show()
        }
        binding.thorughOurBank.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("APPLY FOR LOAN THROUGH OUR BANK")
                .setMessage("Your work data will be shared with the portal bank") // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(
                    R.string.yes,
                    DialogInterface.OnClickListener { dialog, which ->
                        // Continue with delete operation
                        val uri: Uri =
                            Uri.parse("https://www.bankbazaar.com/personal-loan.html") // missing 'http://' will cause crashed

                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        startActivity(intent)
                    }) // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(R.string.no, null)
                .setIcon(R.drawable.ic_dialog_alert)
                .show()
        }
    }
}