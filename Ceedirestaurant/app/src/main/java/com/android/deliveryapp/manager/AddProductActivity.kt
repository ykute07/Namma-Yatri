package com.android.deliveryapp.manager

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import androidx.core.view.drawToBitmap
import coil.load
import coil.transform.CircleCropTransformation
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityAddProductBinding
import com.android.deliveryapp.util.Keys.Companion.productImages
import com.android.deliveryapp.util.Keys.Companion.productListFirebase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.*

class AddProductActivity : AppCompatActivity() {
    companion object {
        private const val IMAGE_CAPTURE_CODE = 1001
        private const val PERMISSION_CODE = 1000
        private const val IMAGE_GALLERY_CODE = 1
    }

    private lateinit var binding: ActivityAddProductBinding
    private var imageUri: Uri? = null
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkCameraPermissions()

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun checkCameraPermissions() {
        // check permissions to use camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED
        ) {
            // permission not enabled
            val permission = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            requestPermissions(permission, PERMISSION_CODE)
        } else {
            showDialog()
        }
    }

    private fun showDialog() {
        val dialog: AlertDialog?

        val dialogView =
            LayoutInflater.from(this).inflate(R.layout.manager_choose_image_from_dialog, null)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_select_image_title))

        val cameraBtn: FloatingActionButton = dialogView.findViewById(R.id.cameraBtn)
        val galleryBtn: FloatingActionButton = dialogView.findViewById(R.id.galleryBtn)

        dialog = dialogBuilder.create()
        dialog.show()

        cameraBtn.setOnClickListener { // CAMERA
            openCamera()
            dialog.dismiss()
        }

        galleryBtn.setOnClickListener { // GALLERY
            openGallery()
            dialog.dismiss()
        }
    }

    private fun openCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "New picture")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "From camera")
        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
    }

    private fun openGallery() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, getString(R.string.new_image_title))
        contentValues.put(
            MediaStore.Images.Media.DESCRIPTION,
            getString(R.string.new_image_desc_gallery)
        )
        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, IMAGE_GALLERY_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //called when user presses ALLOW or DENY from Permission Request Popup
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup was granted
                    showDialog()
                } else {
                    //permission from popup was denied
                    Toast.makeText(
                        this,
                        getString(R.string.camera_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // called when image was captured from camera intent
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_CAPTURE_CODE) {
                binding.imageView.load(imageUri) {
                    transformations(CircleCropTransformation())
                    crossfade(true)
                }
            }
            if (requestCode == IMAGE_GALLERY_CODE) {
                binding.imageView.load(data?.data) {
                    transformations(CircleCropTransformation())
                    crossfade(true)
                }
            }
        }

        database = FirebaseDatabase.getInstance()
        storage = FirebaseStorage.getInstance()

        val databaseRef = database.getReference(productListFirebase)

        val storageRef = storage.getReference(productImages)

        val today: Date = Calendar.getInstance().time
        var name =
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(today)

        name = name.replace(" ", "_") // remove spaces
        name = name.replace(",", "") // remove ","

        binding.addProductButton.setOnClickListener {
            if (isDataValid()) {
                uploadImage(storageRef, databaseRef, name)
            } else {
                printErrorToast()
            }
        }
    }

    /**
     * Upload product image from ImageView
     */
    private fun uploadImage(
        storageReference: StorageReference,
        reference: DatabaseReference,
        name: String
    ) {
        binding.imageView.isDrawingCacheEnabled = true
        binding.imageView.buildDrawingCache()

        val nameRef = storageReference.child("$name.jpg")

        val bitmap = binding.imageView.drawToBitmap()
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val data = baos.toByteArray()

        val uploadTask = nameRef.putBytes(data)
        uploadTask
            .addOnCompleteListener { task ->
                if (task.isComplete && task.isSuccessful) {

                    Log.d("FIREBASE_STORAGE", "Image uploaded with success")

                    storageReference.child("$name.jpg").downloadUrl
                        .addOnSuccessListener { url ->
                            // upload data
                            uploadData(reference, url)

                            // then return to home
                            val intent = Intent(
                                this@AddProductActivity,
                                ManagerHomeActivity::class.java
                            )

                            intent.putExtra("url", imageUri.toString())

                            startActivity(intent)
                        }
                        .addOnFailureListener { e ->
                            Log.d(
                                "FIREBASE_STORAGE",
                                "Error getting download url",
                                e
                            )

                            Toast.makeText(
                                baseContext,
                                getString(R.string.error_image_url),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_STORAGE", "Failed to upload image", e)

                Toast.makeText(
                    baseContext,
                    getString(R.string.image_upload_failure),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun uploadData(reference: DatabaseReference, imageUrl: Uri) {
        val entry = mapOf<String, Any?>(
            "title" to binding.productName.text.toString().toLowerCase(Locale.ROOT),
            "description" to binding.productDescription.text.toString(),
            "quantity" to binding.productQty.text.toString().toInt(),
            "price" to binding.productPrice.text.toString().toDouble(),
            "image" to imageUrl.toString()
        )

        reference.child(entry["title"] as String)
            .setValue(entry)
            .addOnSuccessListener {
                Toast.makeText(
                    baseContext,
                    getString(R.string.data_upload_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Log.w("FIREBASE_DATABASE", "Error uploading data", e)

                Toast.makeText(
                    baseContext,
                    getString(R.string.data_upload_failure),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun isDataValid(): Boolean {
        if (binding.productName.text.isNullOrEmpty()) {
            binding.productName.error = getString(R.string.please_fill)
            binding.productName.requestFocus()
            return false
        }
        if (binding.productDescription.text.isNullOrEmpty()) {
            binding.productDescription.error = getString(R.string.please_fill)
            binding.productDescription.requestFocus()
            return false

        }
        if (binding.productPrice.text.isNullOrEmpty()) {
            binding.productPrice.error = getString(R.string.please_fill)
            binding.productPrice.requestFocus()
            return false
        }
        if (binding.productQty.text.isNullOrEmpty()) {
            binding.productQty.error = getString(R.string.please_fill)
            binding.productQty.requestFocus()
            return false
        }
        try {
            binding.productPrice.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            binding.productPrice.error = getString(R.string.invalid_price)
            binding.productPrice.requestFocus()
            return false
        }
        if (binding.productPrice.text.toString().length > 1
            && binding.productPrice.text.toString().startsWith("0")
        ) {
            binding.productPrice.error = getString(R.string.invalid_price)
            binding.productPrice.requestFocus()
            return false
        }
        if ((binding.productQty.text.toString().length > 1
                    && binding.productQty.text.toString().startsWith("0"))
            || !binding.productQty.text.toString().isDigitsOnly()
        ) {
            binding.productPrice.error = getString(R.string.invalid_quantity)
            binding.productPrice.requestFocus()
            return false
        }
        return true
    }

    private fun printErrorToast() {
        Toast.makeText(
            baseContext, getString(R.string.invalid_values),
            Toast.LENGTH_LONG
        ).show()
    }

    // when the back button is pressed in actionbar, finish this activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(
                    Intent(
                        this@AddProductActivity,
                        ManagerHomeActivity::class.java
                    )
                )
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
}