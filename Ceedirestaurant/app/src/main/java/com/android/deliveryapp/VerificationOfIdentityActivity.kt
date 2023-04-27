package com.android.deliveryapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.android.deliveryapp.databinding.ActivityVerificationOfIdentityBinding
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.util.HashMap

class VerificationOfIdentityActivity : AppCompatActivity() {
    private val LEDGER="LEDGER"
    private lateinit var binding: ActivityVerificationOfIdentityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationOfIdentityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val stringRequest: StringRequest = object : StringRequest( Method.POST, "https://auditor.ssikit.walt.id/v1/verify",
            Response.Listener { response ->


                try {

                    Toast.makeText(baseContext,"${response}", Toast.LENGTH_LONG).show()
                    extractResponse(response)



                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
            }) {
            override fun getBody(): ByteArray {

                return intent.getStringExtra(LEDGER).toString().toByteArray()
            }
            override fun getHeaders() : Map<String,String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/json"

                return params
            }

        }
        val requestQueue = Volley.newRequestQueue(baseContext)
        requestQueue.add(stringRequest)
        binding.CeediButton.setOnClickListener{
            startActivity(Intent(this, SsiWallet::class.java))
        }
}
    private fun extractResponse(response: String){
        val jsonobj = JSONObject(response)
        val did = jsonobj.get("valid")

        if(did.toString()=="true"){
            binding.loadingView.visibility = View.INVISIBLE
            binding.right.visibility= View.VISIBLE
            binding.desription.text ="Verified User"
            binding.CeediButton.visibility=View.VISIBLE
        }
        else{
            binding.loadingView.visibility = View.INVISIBLE
            binding.wrong.visibility= View.VISIBLE
            binding.desription.text ="Not a Verified User"
        }


    }
}