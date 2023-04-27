package com.d4rk.qrcodescanner.extension
import com.d4rk.qrcodescanner.model.Barcode
import com.google.zxing.Result
fun Result.equalTo(barcode: Barcode?): Boolean {
    return barcodeFormat == barcode?.format && text == barcode?.text
}