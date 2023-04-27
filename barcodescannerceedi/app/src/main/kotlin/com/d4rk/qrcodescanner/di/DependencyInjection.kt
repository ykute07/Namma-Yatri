package com.d4rk.qrcodescanner.di
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.d4rk.qrcodescanner.usecase.BarcodeImageScanner
import com.d4rk.qrcodescanner.usecase.BarcodeSaver
import com.d4rk.qrcodescanner.usecase.BarcodeImageGenerator
import com.d4rk.qrcodescanner.usecase.BarcodeImageSaver
import com.d4rk.qrcodescanner.usecase.BarcodeParser
import com.d4rk.qrcodescanner.usecase.WifiConnector
import com.d4rk.qrcodescanner.usecase.OTPGenerator
import com.d4rk.qrcodescanner.usecase.Settings
import com.d4rk.qrcodescanner.usecase.BarcodeDatabase
import com.d4rk.qrcodescanner.usecase.ContactHelper
import com.d4rk.qrcodescanner.usecase.PermissionsHelper
import com.d4rk.qrcodescanner.usecase.RotationHelper
import com.d4rk.qrcodescanner.usecase.ScannerCameraHelper
val com.d4rk.qrcodescanner.App.settings get() = Settings.getInstance(applicationContext)
val AppCompatActivity.barcodeParser get() = BarcodeParser
val AppCompatActivity.barcodeImageScanner get() = BarcodeImageScanner
val AppCompatActivity.barcodeImageGenerator get() = BarcodeImageGenerator
val AppCompatActivity.barcodeSaver get() = BarcodeSaver
val AppCompatActivity.barcodeImageSaver get() = BarcodeImageSaver
val AppCompatActivity.wifiConnector get() = WifiConnector
val AppCompatActivity.otpGenerator get() = OTPGenerator
val AppCompatActivity.barcodeDatabase get() = BarcodeDatabase.getInstance(this)
val AppCompatActivity.settings get() = Settings.getInstance(this)
val AppCompatActivity.contactHelper get() = ContactHelper
val AppCompatActivity.permissionsHelper get() = PermissionsHelper
val AppCompatActivity.rotationHelper get() = RotationHelper
val Fragment.scannerCameraHelper get() = ScannerCameraHelper
val Fragment.barcodeParser get() = BarcodeParser
val Fragment.barcodeDatabase get() = BarcodeDatabase.getInstance(requireContext())
val Fragment.settings get() = Settings.getInstance(requireContext())
val Fragment.permissionsHelper get() = PermissionsHelper