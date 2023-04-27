package com.d4rk.qrcodescanner.feature
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.d4rk.qrcodescanner.di.rotationHelper
abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        rotationHelper.lockCurrentOrientationIfNeeded(this)
    }
}