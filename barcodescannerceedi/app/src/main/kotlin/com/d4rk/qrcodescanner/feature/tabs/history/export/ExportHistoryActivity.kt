package com.d4rk.qrcodescanner.feature.tabs.history.export
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.d4rk.qrcodescanner.R
import com.d4rk.qrcodescanner.di.barcodeDatabase
import com.d4rk.qrcodescanner.di.barcodeSaver
import com.d4rk.qrcodescanner.di.permissionsHelper
import com.d4rk.qrcodescanner.extension.applySystemWindowInsets
import com.d4rk.qrcodescanner.extension.isNotBlank
import com.d4rk.qrcodescanner.extension.showError
import com.d4rk.qrcodescanner.extension.textString
import com.d4rk.qrcodescanner.feature.BaseActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_export_history.root_view
import kotlinx.android.synthetic.main.activity_export_history.toolbar
import kotlinx.android.synthetic.main.activity_export_history.spinner_export_as
import kotlinx.android.synthetic.main.activity_export_history.edit_text_file_name
import kotlinx.android.synthetic.main.activity_export_history.button_export
import kotlinx.android.synthetic.main.activity_export_history.scroll_view
import kotlinx.android.synthetic.main.activity_export_history.progress_bar_loading
class ExportHistoryActivity : BaseActivity() {
    private val disposable = CompositeDisposable()
    companion object {
        private const val REQUEST_PERMISSIONS_CODE = 101
        private val PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        fun start(context: Context) {
            val intent = Intent(context, ExportHistoryActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_history)
        supportEdgeToEdge()
        initToolbar()
        initExportTypeSpinner()
        initFileNameEditText()
        initExportButton()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsHelper.areAllPermissionsGranted(grantResults)) {
            exportHistory()
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
    private fun initExportTypeSpinner() {
        spinner_export_as.adapter = ArrayAdapter.createFromResource(
            this, R.array.activity_export_history_types, R.layout.item_spinner
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
    }
    private fun initFileNameEditText() {
        edit_text_file_name.addTextChangedListener {
            button_export.isEnabled = edit_text_file_name.isNotBlank()
        }
    }
    private fun initExportButton() {
        button_export.setOnClickListener {
            requestPermissions()
        }
    }
    private fun requestPermissions() {
        permissionsHelper.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS_CODE)
    }
    private fun exportHistory() {
        val fileName = edit_text_file_name.textString
        val saveFunc = when (spinner_export_as.selectedItemPosition) {
            0 -> barcodeSaver::saveBarcodeHistoryAsCsv
            1 -> barcodeSaver::saveBarcodeHistoryAsJson
            else -> return
        }
        showLoading(true)
        barcodeDatabase
            .getAllForExport()
            .flatMapCompletable { barcodes ->
                saveFunc(this, fileName, barcodes)
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    showHistoryExported()
                },
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
    private fun showHistoryExported() {
        Toast.makeText(this, R.string.activity_export_history_exported, Toast.LENGTH_LONG).show()
        finish()
    }
}