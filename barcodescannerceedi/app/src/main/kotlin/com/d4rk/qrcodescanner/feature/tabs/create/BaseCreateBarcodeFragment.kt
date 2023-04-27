package com.d4rk.qrcodescanner.feature.tabs.create
import androidx.fragment.app.Fragment
import com.d4rk.qrcodescanner.extension.unsafeLazy
import com.d4rk.qrcodescanner.model.Contact
import com.d4rk.qrcodescanner.model.schema.Other
import com.d4rk.qrcodescanner.model.schema.Schema
abstract class BaseCreateBarcodeFragment : Fragment() {
    protected val parentActivity by unsafeLazy { requireActivity() as CreateBarcodeActivity }
    open val latitude: Double? = null
    open val longitude: Double? = null
    open fun getBarcodeSchema(): Schema = Other("")
    open fun showPhone(phone: String) {}
    open fun showContact(contact: Contact) {}
    open fun showLocation(latitude: Double?, longitude: Double?) {}
}