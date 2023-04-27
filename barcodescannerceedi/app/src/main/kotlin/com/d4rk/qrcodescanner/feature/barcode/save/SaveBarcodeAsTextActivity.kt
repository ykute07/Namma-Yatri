package com.d4rk.qrcodescanner.feature.barcode.save
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import com.d4rk.qrcodescanner.R
import com.d4rk.qrcodescanner.di.barcodeSaver
import com.d4rk.qrcodescanner.di.permissionsHelper
import com.d4rk.qrcodescanner.extension.applySystemWindowInsets
import com.d4rk.qrcodescanner.extension.showError
import com.d4rk.qrcodescanner.extension.unsafeLazy
import com.d4rk.qrcodescanner.feature.BaseActivity
import com.d4rk.qrcodescanner.model.Barcode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_save_barcode_as_text.root_view
import kotlinx.android.synthetic.main.activity_save_barcode_as_text.toolbar
import kotlinx.android.synthetic.main.activity_save_barcode_as_text.button_save
import kotlinx.android.synthetic.main.activity_save_barcode_as_text.spinner_save_as
import kotlinx.android.synthetic.main.activity_save_barcode_as_text.scroll_view
import kotlinx.android.synthetic.main.activity_save_barcode_as_text.progress_bar_loading
class SaveBarcodeAsTextActivity : BaseActivity() {
    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 101
        private val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        private const val BARCODE_KEY = "BARCODE_KEY"
        fun start(context: Context, barcode: Barcode) {
            val intent = Intent(context, SaveBarcodeAsTextActivity::class.java).apply {
                putExtra(BARCODE_KEY, barcode)
            }
            context.startActivity(intent)
        }
    }
    private val barcode by unsafeLazy {
        intent?.getSerializableExtra(BARCODE_KEY) as? Barcode ?: throw IllegalArgumentException("No barcode passed")
    }
    private val disposable = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_barcode_as_text)
        supportEdgeToEdge()
        initToolbar()
        initFormatSpinner()
        initSaveButton()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsHelper.areAllPermissionsGranted(grantResults)) {
            saveBarcode()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }
    private fun supportEdgeToEdge() {
        root_view.applySystemWindowInsets(applyTop = true, applyBottom = true)
    }
    private fun initToolbar() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    private fun initFormatSpinner() {
        spinner_save_as.adapter = ArrayAdapter.createFromResource(
            this, R.array.activity_save_barcode_as_text_formats, R.layout.item_spinner
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
    }
    private fun initSaveButton() {
        button_save.setOnClickListener {
            requestPermissions()
        }
    }
    private fun requestPermissions() {
        permissionsHelper.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS_CODE)
    }
    private fun saveBarcode() {
        val saveFunc = when (spinner_save_as.selectedItemPosition) {
            0 -> barcodeSaver::saveBarcodeAsCsv
            1 -> barcodeSaver::saveBarcodeAsJson
            else -> return
        }
        showLoading(true)
        saveFunc(this, barcode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { showBarcodeSaved() },
                { error ->
                    showLoading(false)
                    showError(error)
                }
            )
            .addTo(disposable)
    }
    private fun showLoading(isLoading: Boolean) {
        progress_bar_loading.isVisible = isLoading
        scroll_view.isVisible = isLoading.not()
    }
    private fun showBarcodeSaved() {
        Toast.makeText(this, R.string.activity_save_barcode_as_text_file_name_saved, Toast.LENGTH_LONG).show()
        finish()
    }
}