package com.d4rk.qrcodescanner.extension
fun Double?.orZero(): Double {
    return this ?: 0.0
}