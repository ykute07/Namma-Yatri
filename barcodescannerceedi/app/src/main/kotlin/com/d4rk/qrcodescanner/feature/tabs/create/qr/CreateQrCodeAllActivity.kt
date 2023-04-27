package com.d4rk.qrcodescanner.feature.tabs.create.qr
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.d4rk.qrcodescanner.R
import com.d4rk.qrcodescanner.extension.applySystemWindowInsets
import com.d4rk.qrcodescanner.feature.BaseActivity
import com.d4rk.qrcodescanner.feature.tabs.create.CreateBarcodeActivity
import com.d4rk.qrcodescanner.model.schema.BarcodeSchema
import com.google.zxing.BarcodeFormat
import kotlinx.android.synthetic.main.activity_create_qr_code_all.root_view
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_text
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_app
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_bookmark
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_url
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_otp
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_wifi
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_contact_vcard
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_contact_mecard
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_phone
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_sms
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_email
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_location
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_mms
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_cryptocurrency
import kotlinx.android.synthetic.main.activity_create_qr_code_all.toolbar
import kotlinx.android.synthetic.main.activity_create_qr_code_all.button_event
class CreateQrCodeAllActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CreateQrCodeAllActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_qr_code_all)
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
        button_text.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.OTHER) }
        button_url.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.URL) }
        button_wifi.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.WIFI) }
        button_location.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.GEO) }
        button_otp.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.OTP_AUTH) }
        button_contact_vcard.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.VCARD) }
        button_contact_mecard.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.MECARD) }
        button_event.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.VEVENT) }
        button_phone.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.PHONE) }
        button_email.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.EMAIL) }
        button_sms.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.SMS) }
        button_mms.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.MMS) }
        button_cryptocurrency.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.CRYPTOCURRENCY) }
        button_bookmark.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.BOOKMARK) }
        button_app.setOnClickListener { CreateBarcodeActivity.start(this, BarcodeFormat.QR_CODE, BarcodeSchema.APP) }
    }
}