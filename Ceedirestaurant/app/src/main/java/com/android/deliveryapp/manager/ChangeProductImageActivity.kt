package com.android.deliveryapp.manager

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import coil.load
import coil.transform.CircleCropTransformation
import com.android.deliveryapp.R
import com.android.deliveryapp.databinding.ActivityChangeProductImageBinding
import com.android.deliveryapp.util.Keys.Companion.productImages
import com.android.deliveryapp.util.Keys.Companion.productListFirebase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.util.*


class ChangeProductImageActivity : AppCompatActivity() {
    companion object {
        private const val IMAGE_CAPTURE_CODE = 1001
        private const val PERMISSION_CODE = 1000
        private const val IMAGE_GALLERY_CODE = 1
    }

    private lateinit var binding: ActivityChangeProductImageBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangeProductImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        checkCameraPermissions()
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

    private fun openCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, getString(R.string.new_image_title))
        contentValues.put(
            MediaStore.Images.Media.DESCRIPTION,
            getString(R.string.new_image_desc_camera)
        )
        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE)
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
                    openCamera()
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
                binding.imageCaptured.load(imageUri) {
                    transformations(CircleCropTransformation())
                    crossfade(true)
                }
            } else if (requestCode == IMAGE_GALLERY_CODE) {
                binding.imageCaptured.load(data?.data) {
                    transformations(CircleCropTransformation())
                    crossfade(true)
                }
            }
            database = FirebaseDatabase.getInstance()

            storage = FirebaseStorage.getInstance()

            val storageRef = storage.getReference(productImages)
            val reference = database.getReference(productListFirebase)

            val name = intent.getStringExtra("name")

            binding.cancelBtn.setOnClickListener { // manager isn't satisfied with taken image
                showDialog()
            }

            binding.acceptBtn.setOnClickListener { // manager is satisfied with taken image
                uploadImage(reference, storageRef, name!!)
            }
        }
    }

    /**
     * Upload product image from ImageView
     */
    private fun uploadImage(
        reference: DatabaseReference,
        storageReference: StorageReference,
        productName: String
    ) {
        binding.imageCaptured.isDrawingCacheEnabled = true
        binding.imageCaptured.buildDrawingCache()

        val nameRef = storageReference.child("$productName.jpg")

        val bitmap = binding.imageCaptured.drawToBitmap()
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

        val data = baos.toByteArray()

        val uploadTask = nameRef.putBytes(data)
        uploadTask
            .addOnCompleteListener { task ->
                if (task.isComplete && task.isSuccessful) {

                    storageReference.child("$productName.jpg").downloadUrl
                        .addOnSuccessListener { url ->
                            Log.d("FIREBASE_STORAGE", "Image uploaded with success")

                            intent.putExtra("url", url.toString())

                            // update url on firebase database
                            reference.child(productName.toLowerCase(Locale.ROOT))
                                .child("image").setValue(url.toString())
                                .addOnSuccessListener {
                                    Log.d(
                                        "FIREBASE_DATABASE",
                                        "Data uploaded with success"
                                    )

                                    // then return to home
                                    val intent = Intent(
                                        this@ChangeProductImageActivity,
                                        ManagerHomeActivity::class.java
                                    )

                                    startActivity(intent)

                                    Toast.makeText(
                                        baseContext,
                                        getString(R.string.image_upload_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w("FIREBASE_DATABASE", "Error uploading data", e)

                                    Toast.makeText(
                                        baseContext,
                                        getString(R.string.data_upload_failure),
                                        Toast.LENGTH_LONG
                                    ).show()

                                }
                        }
                        .addOnFailureListener { e ->
                            Log.w(
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

    // when the back button is pressed in actionbar, finish this activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                startActivity(
                    Intent(
                        this@ChangeProductImageActivity,
                        ManagerHomeActivity::class.java
                    )
                )
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}