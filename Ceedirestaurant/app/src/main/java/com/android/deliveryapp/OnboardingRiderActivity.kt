package com.android.deliveryapp

import android.R.attr
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.databinding.ActivityOnboardingRiderBinding
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import android.R.attr.data





class OnboardingRiderActivity : AppCompatActivity() {
    private lateinit var binding : ActivityOnboardingRiderBinding
    companion object{
        private const val PICK_FILE =1
        private const val LEDGER = "LEDGER"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingRiderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.idUploadButton.setOnClickListener{
            openTextFile()
        }
    }

    private fun openTextFile(){
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/plain"
        startActivityForResult(intent, PICK_FILE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode === PICK_FILE) {
            if (resultCode === RESULT_OK) {
                val uri: Uri = data?.getData() ?: return
                val fileContent: String = readTextFile(uri).toString()

                val intent = Intent(this, VerificationOfIdentityActivity::class.java)
                intent.putExtra(LEDGER,fileContent)
                startActivity(intent)
                            }
        }
    }
    private fun readTextFile(uri: Uri): String {
        var reader: BufferedReader? = null
        val builder = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(contentResolver.openInputStream(uri)))
            var line: String? = ""
            while (reader.readLine().also { line = it } != null) {
                builder.append(line)
            }
            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return builder.toString()
    }
}