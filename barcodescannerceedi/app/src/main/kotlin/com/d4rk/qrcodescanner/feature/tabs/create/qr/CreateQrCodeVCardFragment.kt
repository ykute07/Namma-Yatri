package com.d4rk.qrcodescanner.feature.tabs.create.qr
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.d4rk.qrcodescanner.R
import com.d4rk.qrcodescanner.extension.textString
import com.d4rk.qrcodescanner.feature.tabs.create.BaseCreateBarcodeFragment
import com.d4rk.qrcodescanner.model.Contact
import com.d4rk.qrcodescanner.model.schema.Schema
import com.d4rk.qrcodescanner.model.schema.VCard
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_first_name
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_last_name
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_email
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_phone
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_job
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_organization
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_website
import kotlinx.android.synthetic.main.fragment_create_qr_code_vcard.edit_text_fax
class CreateQrCodeVCardFragment : BaseCreateBarcodeFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_qr_code_vcard, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        edit_text_first_name.requestFocus()
        parentActivity.isCreateBarcodeButtonEnabled = true
    }
    override fun getBarcodeSchema(): Schema {
       return VCard(
           firstName = edit_text_first_name.textString,
           lastName = edit_text_last_name.textString,
           organization = edit_text_organization.textString,
           title = edit_text_job.textString,
           email = edit_text_email.textString,
           phone = edit_text_phone.textString,
           secondaryPhone = edit_text_fax.textString,
           url = edit_text_website.textString
       )
    }
    override fun showContact(contact: Contact) {
        edit_text_first_name.setText(contact.firstName)
        edit_text_last_name.setText(contact.lastName)
        edit_text_email.setText(contact.email)
        edit_text_phone.setText(contact.phone)
    }
}