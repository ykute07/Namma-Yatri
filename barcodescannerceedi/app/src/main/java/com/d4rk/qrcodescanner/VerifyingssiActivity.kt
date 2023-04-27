package com.d4rk.qrcodescanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.d4rk.qrcodescanner.databinding.ActivityVerifyingssiBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.json.JSONException
import org.json.JSONObject
import java.io.StringReader
import java.util.*
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.crypto.ECDSAVerifier

import org.jsoup.helper.Validate.fail

import com.nimbusds.jose.jwk.JWK
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.jsoup.helper.Validate
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey


class VerifyingssiActivity : AppCompatActivity() {
    private val LEDGER = "LEDGER"
    private lateinit var binding : ActivityVerifyingssiBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyingssiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val did : String = "did:key:zQ3shv378PvkMuRrYMGFV9a3MtKpJkteqb2dUbQMEMvtWc2tE"
        val host_port_url:String = "https://verifier-sandbox.circle.com/verifications"

        val stringRequest_challenge_url: StringRequest = object : StringRequest( Method.POST, host_port_url,
            Response.Listener { response ->


                try {

                    Toast.makeText(baseContext,"${response}", Toast.LENGTH_LONG).show()
                    val challenge_url_json = JSONObject(response)
                    val challenge_url_token = challenge_url_json.get("challengeTokenUrl")
                    Toast.makeText(baseContext,"${challenge_url_token}",Toast.LENGTH_LONG).show()
                    //Get Request

                    val stringRequestVerite: StringRequest = object : StringRequest( Method.GET, challenge_url_token.toString(),
                        Response.Listener { response ->


                            try {
                                val verification_json = JSONObject(response)
                                val nonce = verification_json.getJSONObject("body").get("challenge")
                                val defination_id = verification_json.getJSONObject("body").getJSONObject("presentation_definition").get("id")
                                Toast.makeText(baseContext,"${intent.getStringExtra(LEDGER).toString()}",Toast.LENGTH_LONG).show()

                                val stringRequest: StringRequest = object : StringRequest(Method.POST, "https://encrypt112.herokuapp.com/",
                                    Response.Listener { response ->

                                        try {

                                            val response_check = JSONObject(response)
                                            val status = response_check.get("status")
                                            val credential = response_check.getJSONObject("verificationResult")
                                            val subject = credential.get("subject")

                                            Toast.makeText(
                                                baseContext,
                                                "${response}",
                                                Toast.LENGTH_SHORT
                                            ).show()



                                            binding.nametage.visibility = View.VISIBLE
                                            binding.nameText.visibility = View.VISIBLE
                                            binding.loadingView.visibility =View.INVISIBLE
                                            binding.right.visibility=View.VISIBLE
                                            binding.desription.text ="Status  ${status}"
                                            binding.nameText.text= subject.toString()

                                        } catch (e: JSONException) {
                                            e.printStackTrace()
                                        }
                                    },
                                    Response.ErrorListener { error ->

                                        Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                                    }) {
                                    override fun getBody(): ByteArray? {
                                        val params2 = HashMap<String, Any>()

                                        params2.put("nonce",nonce.toString())
                                        params2.put("sub","did:key:zQ3shv378PvkMuRrYMGFV9a3MtKpJkteqb2dUbQMEMvtWc2tE" )
                                        params2.put("verifiableCredential",intent.getStringExtra(LEDGER).toString())
                                        params2.put("definition_id",defination_id.toString())
                                        params2.put("challenge_url",challenge_url_token)

                                        return JSONObject(params2 as Map<*, *>).toString().toByteArray()
                                    }

                                    override fun getHeaders(): Map<String, String> {
                                        val params: MutableMap<String, String> = HashMap()
                                        params["Content-Type"] = "application/json"

                                        return params
                                    }

                                }
                                val requestQueue = Volley.newRequestQueue(baseContext)
                                requestQueue.add(stringRequest)


                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        },
                        Response.ErrorListener { error ->
                            Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                        }) {


                    }
                    val requestQueueVerite = Volley.newRequestQueue(baseContext)
                    requestQueueVerite.add(stringRequestVerite)



                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
            }) {
            override fun getBody(): ByteArray {

                val params2 = HashMap<String, Any>()
                params2.put("network","ethereum" )
                params2.put("subject","0xf39fd6e51aad88f6f4ce6ab8827279cfffb92266")
                params2.put("chainId",1337)
                return JSONObject(params2 as Map<*, *>).toString().toByteArray()
            }
            override fun getHeaders() : Map<String,String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/json"

                return params
            }

        }
        val requestQueue_challenge_url = Volley.newRequestQueue(baseContext)
        requestQueue_challenge_url.add(stringRequest_challenge_url)


    }

}

