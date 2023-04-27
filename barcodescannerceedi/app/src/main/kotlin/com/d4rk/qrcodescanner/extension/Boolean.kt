package com.d4rk.qrcodescanner.extension
fun Boolean?.orFalse(): Boolean {
    return this ?: false
}