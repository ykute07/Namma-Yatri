package com.d4rk.qrcodescanner.extension
fun Int?.orZero(): Int {
    return this ?: 0
}