package com.android.deliveryapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.databinding.ActivitySsiWalletBinding
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

import org.json.JSONException
import org.json.JSONObject
import java.nio.file.Paths
import java.util.*

import kotlin.collections.HashMap
import com.auth0.jwt.interfaces.DecodedJWT

import com.android.deliveryapp.util.PemUtils
import com.auth0.jwt.interfaces.ECDSAKeyProvider
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import io.fusionauth.jwt.Verifier
import io.fusionauth.jwt.domain.JWT as JWTS
import java.security.interfaces.ECKey
import io.fusionauth.jwt.ec.ECVerifier
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jwt.*;

import com.nimbusds.jwt.SignedJWT
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.StringReader

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWK.parseFromPEMEncodedObjects
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import com.auth0.jwt.exceptions.JWTCreationException
import com.nimbusds.jwt.JWTClaimsSet








class SsiWallet : AppCompatActivity() {

    private lateinit var binding: ActivitySsiWalletBinding

    companion object {

        const val PRIVATE_KEY_FILE_256 =
            "DeliveryApp//app//src//main//java//com//android//deliveryapp//issuerr-public.pem"
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivitySsiWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createssibutton.setOnClickListener {
            binding.animatedProgressBar.visibility = View.VISIBLE
            binding.createssibutton.visibility = View.INVISIBLE
            Toast.makeText(baseContext, "Creating Verite Id", Toast.LENGTH_SHORT).show()
            binding.animatedProgressBar.setProgress(25)

            val stringRequest: StringRequest = object : StringRequest( Method.GET, "https://issuer-sandbox.circle.com/api/v1/issuance/manifest/kyc/test-string",
                Response.Listener { response ->


                    try {
                        extractVeriteResponse(response)
                        Toast.makeText(baseContext,"${response}",Toast.LENGTH_SHORT).show()
                        binding.animatedProgressBar.setProgress(25)

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                }) {

                override fun getHeaders() : Map<String,String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["Content-Type"] = "application/json"

                    return params
                }

            }
            val requestQueue = Volley.newRequestQueue(baseContext)
            requestQueue.add(stringRequest)


        }

    }

    private fun extractVeriteResponse(response: String) {

        val jsonobjeVerite = JSONObject(response)
        val jsonbody = jsonobjeVerite.getJSONObject("body")
        val challenge = jsonbody.get("challenge")
        val id = jsonobjeVerite.get("id")
        val reply_url = jsonobjeVerite.get("reply_url")
        binding.animatedProgressBar.setProgress(50)

        val params: MutableMap<String, Any> = HashMap()
        params["id"] = "${id}"
        params["manifest_id"] = "KYCAMLAttestation"
        val params1: MutableMap<String, Any> = HashMap()
        val params2: MutableMap<String, Any> = HashMap()
        params2["alg"] = arrayListOf("ES256K")
        params1["jwt_vp"] = params2
        params["format"] = params1

        val presentation_submission: MutableMap<String, Any> = HashMap()
        presentation_submission["id"] = "b4f43310-1d6b-425d-84c6-f8afac3fe244"

        presentation_submission["definition_id"] = "ProofOfControlPresentationDefinition"

        val descriptors_map: MutableMap<String, Any> = HashMap()
        descriptors_map["id"] = "proofOfIdentifierControlVP"
        descriptors_map["format"] = "jwt_vp"
        descriptors_map["path"] = "$.presentation"

        presentation_submission["descriptor_map"] = arrayListOf(descriptors_map)

//
//
        val params20: MutableMap<String, Any> = HashMap()
        params20["@context"] = arrayListOf("https://www.w3.org/2018/credentials/v1")
        params20["type"] = arrayListOf("VerifiablePresentation", "CredentialFulfillment")
        params20["holder"] = "did:web:circle.com"
        params20["verifiableCredential"] =
            arrayListOf("eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9....7wwi-YRX")



        val algorithm = Algorithm.HMAC256("${challenge}")
        val token = JWT.create()
            .withClaim("sub", "did:key:zQ3shv378PvkMuRrYMGFV9a3MtKpJkteqb2dUbQMEMvtWc2tE")
            .withClaim("iss", "did:key:zQ3shv378PvkMuRrYMGFV9a3MtKpJkteqb2dUbQMEMvtWc2tE")
            .withClaim("credential_application", params)
            .withClaim("presentation_submission", presentation_submission)
            .withClaim("vp", params20)
            .sign(algorithm)
        Toast.makeText(baseContext, "${token}", Toast.LENGTH_LONG).show()
        val stringRequest: StringRequest = object : StringRequest(Method.POST, reply_url.toString(),
            Response.Listener { response ->
                try {

                    Toast.makeText(
                        baseContext,
                        "Response in JWT ${response}",
                        Toast.LENGTH_SHORT
                    ).show()


                    val privateKeyString="-----BEGIN PUBLIC KEY-----\n" +
                            "MFYwEAYHKoZIzj0CAQYFK4EEAAoDQgAEwolZiHX84B7UAXqYKeEZqy3ubWulNASv\n" +
                            "kpBK/MERm9KTNQgkTOLAHubAA1EYBu7M6ukHjlHRj2eAnN3aj/IS1A==\n" +
                            "-----END PUBLIC KEY-----"
                    val jwk = JWK.parseFromPEMEncodedObjects(privateKeyString) as com.nimbusds.jose.jwk.ECKey

                    val check =  SignedJWT.parse(response)
                        .verify(ECDSAVerifier(jwk))
                    val recheck =  SignedJWT.parse(response).getJWTClaimsSet().toJSONObject()

                    val vp = JSONObject(recheck).getJSONObject("vp")
                    val verfication_cred = vp.getJSONArray("verifiableCredential")

                    val real_verification_cred = verfication_cred.get(0)
                    Toast.makeText(baseContext,"${real_verification_cred}",Toast.LENGTH_LONG).show()



                    binding.animatedProgressBar.setProgress(150)
                    binding.createaccount.visibility=View.VISIBLE
                    binding.createaccount.setOnClickListener {
                        startActivity(Intent(this@SsiWallet, SignUpActivity::class.java))
                    }
                    try {
                        val barcodeEncoder = BarcodeEncoder()
                        val bitmap =
                            barcodeEncoder.encodeBitmap(real_verification_cred.toString(), BarcodeFormat.QR_CODE, 400, 400)

                        binding.ivOutput.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
            }) {
            override fun getBody(): ByteArray? {
                val params2 = "${token}"

                return params2.toByteArray()
            }

            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "text/plain"

                return params
            }

        }
        val requestQueue = Volley.newRequestQueue(baseContext)
        requestQueue.add(stringRequest)


    }

}