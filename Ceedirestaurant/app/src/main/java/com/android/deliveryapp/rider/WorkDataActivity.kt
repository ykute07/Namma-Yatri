package com.android.deliveryapp.rider

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityWorkDataBinding
import com.android.deliveryapp.util.Keys
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.itextpdf.io.image.ImageData
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.HorizontalAlignment
import com.itextpdf.layout.property.TextAlignment
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

private lateinit var dates :MutableList<String>
class WorkDataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWorkDataBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var name: String
    private lateinit var noOfDeliverDriver: String
    private lateinit var earningDriver: String
    private lateinit var review: String
    private lateinit var userTypeofDriver: String
    private lateinit var userEmail: String

    var dates=ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user != null) {
            getNameandEmail(auth, firestore)
            getDeliveryHistory(auth, firestore)
            getReview(auth, firestore)
            getSSI()
            binding.downloadPdf.visibility = View.INVISIBLE
        }
    }

    private fun getNameandEmail(auth: FirebaseAuth, firestore: FirebaseFirestore) {
        val user = auth.currentUser
        userEmail = user?.email.toString()
        firestore.collection(Keys.users).document(user?.email!!).get()
            .addOnSuccessListener { result ->
                userTypeofDriver = result?.getString("userType").toString()
                name = result?.getString("Name").toString()
                binding.nameTextField.text = name
                binding.emailTextField.text = user.email
                binding.userTypeTextField.text = userTypeofDriver
            }
            .addOnFailureListener { e ->
                Log.w("FIRESTORE", "Failed to get data", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_user_data),
                    Toast.LENGTH_LONG
                ).show()
            }

    }

    private fun getDeliveryHistory(auth: FirebaseAuth, firestore: FirebaseFirestore) {
        val user = auth.currentUser
        firestore.collection(Keys.riders).document(user?.email!!).collection("deliveryHistory")
            .get().addOnSuccessListener { result ->
                var deliveryhistory = 0

                if (result.documents.lastIndex != -1) {

                    for (check in result) {

                        dates!!.add(check.id)
                    }

                    deliveryhistory = result.documents.lastIndex
                    binding.deliveryDateTextField.text = dates.toString().replace("[", "").replace("]", "").replace(",","\n")
                    binding.deliveryTextField.text = deliveryhistory.toString()
                    binding.earningTextField.text = (deliveryhistory * 100).toString()
                    noOfDeliverDriver = deliveryhistory.toString()
                    earningDriver = (deliveryhistory * 100).toString()
                } else {
                    binding.deliveryTextField.text = "0"
                    binding.earningTextField.text = "0"
                    noOfDeliverDriver = "0"
                    earningDriver = "0"
                    binding.deliveryDateTextField.text = "0"
                }
            }.addOnFailureListener { e ->
                Log.w("FIRESTORE", "Failed to get data", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_user_data),
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun getReview(auth: FirebaseAuth, firestore: FirebaseFirestore) {
        val user = auth.currentUser
        firestore.collection(Keys.riders).document(user?.email!!).collection("review")
            .get().addOnSuccessListener { result ->
                if (result.documents.lastIndex != -1) {
                    val document = result?.documents?.get(result.documents.lastIndex)
                    review = document?.getString("review").toString()!!
                    binding.reviewTextField.text = review
                } else {
                    review = "No Review"
                    binding.reviewTextField.text = review
                }

            }.addOnFailureListener { e ->
                Log.w("FIRESTORE", "Failed to get data", e)
                Toast.makeText(
                    baseContext,
                    getString(R.string.error_user_data),
                    Toast.LENGTH_LONG
                ).show()
            }

    }

    private fun getSSI() {
        Toast.makeText(baseContext, "Creating SSI ", Toast.LENGTH_SHORT).show()
        val stringRequest: StringRequest =
            object : StringRequest(Method.POST, "https://core.ssikit.walt.id/v1/key/gen",
                Response.Listener { response ->


                    try {
                        extractResponse(response)
                        Toast.makeText(
                            baseContext,
                            "Successfully Generated Key ${response}",
                            Toast.LENGTH_SHORT
                        ).show()


                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                }) {
                override fun getBody(): ByteArray {
                    val params2 = HashMap<String, String>()
                    params2.put("keyAlgorithm", "EdDSA_Ed25519")

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

    }

    private fun extractResponse(response: String) {
        val jsonobj = JSONObject(response)
        val did = jsonobj.get("id")

        generateDid(did.toString())

    }

    private fun generateDid(did: String) {
        val stringRequest: StringRequest =
            object : StringRequest(Method.POST, "https://core.ssikit.walt.id/v1/did/create",
                Response.Listener { response ->


                    try {
                        IssueLedger(response)
                        Toast.makeText(
                            baseContext,
                            "Successfully Generated DID ${response}",
                            Toast.LENGTH_SHORT
                        ).show()


                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                }) {
                override fun getBody(): ByteArray {
                    val params2 = HashMap<String, String>()
                    params2.put("method", "key")
                    params2.put("keyAlias", "${did}")

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

    }

    private fun verfiyLedger(Ledger: String) {
        val stringRequest: StringRequest =
            object : StringRequest(Method.POST, "https://auditor.ssikit.walt.id/v1/verify",
                Response.Listener { response ->


                    try {

                        Toast.makeText(baseContext, "Verified ${response}", Toast.LENGTH_LONG)
                            .show()

                        try {
                            val barcodeEncoder = BarcodeEncoder()
                            val bitmap =
                                barcodeEncoder.encodeBitmap(Ledger, BarcodeFormat.QR_CODE, 400, 400)
                            binding.downloadPdf.visibility = View.VISIBLE
                            binding.downloadPdf.setOnClickListener {
                                createPdf(bitmap)
                            }
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
                override fun getBody(): ByteArray {

                    return Ledger.toString().toByteArray()
                }

                override fun getHeaders(): Map<String, String> {
                    val params: MutableMap<String, String> = HashMap()
                    params["Content-Type"] = "application/json"

                    return params
                }

            }
        val requestQueue = Volley.newRequestQueue(baseContext)
        requestQueue.add(stringRequest)
    }

    private fun IssueLedger(did: String) {
        val stringRequest: StringRequest = object :
            StringRequest(Method.POST, "https://signatory.ssikit.walt.id/v1/credentials/issue",
                Response.Listener { response ->


                    try {

                        Toast.makeText(
                            baseContext,
                            "Successfully Issued Ledger",
                            Toast.LENGTH_SHORT
                        ).show()

                        verfiyLedger(response)

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(baseContext, error.toString(), Toast.LENGTH_LONG).show()
                }) {
            override fun getBody(): ByteArray {
                val params2 = JSONObject()
                val params3 = JSONObject()
                val params4 = JSONObject()
                val params5 = JSONObject()


                params5.put("firstName", name)

                params5.put("familyName", noOfDeliverDriver)
                params5.put("nameAndFamilyNameAtBirth", review)
                params5.put("placeOfBirth", userTypeofDriver)
                params5.put("gender", "MALE")
                params4.put("credentialSubject", params5)
                params3.put("issuerDid", "${did}")
                params3.put("subjectDid", "${did}")
                params2.put("templateId", "VerifiableId")
                params2.put("config", params3)
                params2.put("credentialData", params4)

                return params2.toString().toByteArray()
            }

            override fun getHeaders(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["Content-Type"] = "application/json"

                return params
            }

        }
        val requestQueue = Volley.newRequestQueue(baseContext)
        requestQueue.add(stringRequest)
    }

    private fun createPdf(bitmap: Bitmap?) {
        val pdfPath: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .toString();
        val file: File = File(pdfPath, "Invoice.pdf")
        val outputStream: OutputStream = FileOutputStream(file)

        val writer: PdfWriter = PdfWriter(file)
        val pdfDocument: PdfDocument = PdfDocument(writer)
        val document: Document = Document(pdfDocument)

        pdfDocument.defaultPageSize = PageSize.A4
        val user = auth.currentUser
        document.setMargins(0F, 0F, 0F, 0F)
        val ceediTicket: Paragraph = Paragraph("CEEDI WORK DATA").setBold().setFontSize(24F)
            .setTextAlignment(TextAlignment.CENTER)

        val table: Table = Table(floatArrayOf(150f, 150f))
        table.setHorizontalAlignment(HorizontalAlignment.CENTER)
        table.addCell(Cell().add(Paragraph("Name ")))
        table.addCell(Cell().add(Paragraph(name)))
        table.addCell(Cell().add(Paragraph("Email ")))
        table.addCell(Cell().add(Paragraph(userEmail)))
        table.addCell(Cell().add(Paragraph("Number Of Delivery ")))
        table.addCell(Cell().add(Paragraph(noOfDeliverDriver)))

        table.addCell(Cell().add(Paragraph("Delivery Date")))
        table.addCell(Cell().add(Paragraph(dates.toString().replace("[", "").replace("]", "").replace(",","\n"))))

        table.addCell(Cell().add(Paragraph("Earning ")))
        table.addCell(Cell().add(Paragraph(earningDriver)))

        table.addCell(Cell().add(Paragraph("Review")))
        table.addCell(Cell().add(Paragraph(review)))
        var stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val ImageData: ImageData = ImageDataFactory.create(stream.toByteArray())
        val image: Image = Image(ImageData)
        image.setHorizontalAlignment(HorizontalAlignment.CENTER)
        document.add(ceediTicket)
        document.add(table)
        document.add(image)
        document.close()
        Toast.makeText(this, "PDF Created ", Toast.LENGTH_LONG).show()

    }
}