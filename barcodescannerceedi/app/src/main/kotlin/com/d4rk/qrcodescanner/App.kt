package com.d4rk.qrcodescanner
import androidx.multidex.MultiDexApplication
import com.d4rk.qrcodescanner.di.settings
class App : MultiDexApplication() {
    override fun onCreate() {
        applyTheme()
        super.onCreate()
    }
    private fun applyTheme() {
        settings.reapplyTheme()
    }
}