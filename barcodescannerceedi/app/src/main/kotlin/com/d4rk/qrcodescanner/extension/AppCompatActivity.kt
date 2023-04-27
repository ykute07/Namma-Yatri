package com.d4rk.qrcodescanner.extension
import androidx.appcompat.app.AppCompatActivity
import com.d4rk.qrcodescanner.feature.common.dialog.ErrorDialogFragment
fun AppCompatActivity.showError(error: Throwable?) {
    val errorDialog =
        ErrorDialogFragment.newInstance(
            this,
            error
        )
    errorDialog.show(supportFragmentManager, "")
}