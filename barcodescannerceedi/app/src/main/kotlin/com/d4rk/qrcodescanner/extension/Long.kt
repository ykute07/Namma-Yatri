package com.d4rk.qrcodescanner.extension
fun Long?.orZero(): Long {
    return this ?: 0L
}