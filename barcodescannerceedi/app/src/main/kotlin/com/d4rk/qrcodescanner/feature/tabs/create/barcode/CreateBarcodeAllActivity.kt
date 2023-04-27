package com.d4rk.qrcodescanner.feature.tabs.create.barcode
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.d4rk.qrcodescanner.R
import com.d4rk.qrcodescanner.extension.applySystemWindowInsets
import com.d4rk.qrcodescanner.feature.BaseActivity
import com.d4rk.qrcodescanner.feature.tabs.create.CreateBarcodeActivity
import com.google.zxing.BarcodeFormat
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_aztec
import kotlinx.android.synthetic.main.activity_create_barcode_all.root_view
import kotlinx.android.synthetic.main.activity_create_barcode_all.toolbar
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_codabar
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_code_128
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_code_39
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_code_93
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_data_matrix
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_pdf_417
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_ean_13
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_upc_a
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_itf_14
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_upc_e
import kotlinx.android.synthetic.main.activity_create_barcode_all.button_ean_8
class CreateBarcodeAllActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CreateBarcodeAllActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_barcode_all)
        supportEdgeToEdge()
        handleToolbarBackClicked()
        handleButtonsClicked()
    }
    private fun supportEdgeToEdge() {
        root_view.applySystemWindowInsets(applyTop = true, applyBottom = true)
    }
    private fun handleToolbarBackClicked() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    private fun handleButtonsClicked() {
        button_data_matrix.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.DATA_MATRIX) }
        button_aztec.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.AZTEC) }
        button_pdf_417.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.PDF_417) }
        button_codabar.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.CODABAR) }
        button_code_39.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.CODE_39) }
        button_code_93.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.CODE_93) }
        button_code_128.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.CODE_128) }
        button_ean_8.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.EAN_8) }
        button_ean_13.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.EAN_13) }
        button_itf_14.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.ITF) }
        button_upc_a.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.UPC_A) }
        button_upc_e.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.UPC_E) }
    }
}