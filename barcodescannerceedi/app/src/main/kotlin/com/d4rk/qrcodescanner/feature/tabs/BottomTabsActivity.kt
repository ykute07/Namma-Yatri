package com.d4rk.qrcodescanner.feature.tabs
import android.os.Bundle
import android.view.MenuItem
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.multidex.BuildConfig
import com.d4rk.qrcodescanner.R
import com.d4rk.qrcodescanner.extension.applySystemWindowInsets
import com.d4rk.qrcodescanner.feature.BaseActivity
import com.d4rk.qrcodescanner.feature.tabs.create.CreateBarcodeFragment
import com.d4rk.qrcodescanner.feature.tabs.history.BarcodeHistoryFragment
import com.d4rk.qrcodescanner.feature.tabs.scan.ScanBarcodeFromCameraFragment
import com.d4rk.qrcodescanner.feature.tabs.settings.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_bottom_tabs.bottom_navigation_view
class BottomTabsActivity : BaseActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val ACTION_CREATE_BARCODE = "${BuildConfig.APPLICATION_ID}.CREATE_BARCODE"
        private const val ACTION_HISTORY = "${BuildConfig.APPLICATION_ID}.HISTORY"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        setContentView(R.layout.activity_bottom_tabs)
        supportEdgeToEdge()
        initBottomNavigationView()
        if (savedInstanceState == null) {
            showInitialFragment()
        }
    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == bottom_navigation_view.selectedItemId) {
            return false
        }
        showFragment(item.itemId)
        return true
    }
    override fun onBackPressed() {
        if (bottom_navigation_view.selectedItemId == R.id.item_scan) {
            super.onBackPressed()
        } else {
            bottom_navigation_view.selectedItemId = R.id.item_scan
        }
    }
    private fun supportEdgeToEdge() {
        bottom_navigation_view.applySystemWindowInsets(applyBottom = true)
    }
    private fun initBottomNavigationView() {
        bottom_navigation_view.apply {
            setOnNavigationItemSelectedListener(this@BottomTabsActivity)
        }
    }
    private fun showInitialFragment() {
        when (intent?.action) {
            ACTION_CREATE_BARCODE -> bottom_navigation_view.selectedItemId = R.id.item_create
            ACTION_HISTORY -> bottom_navigation_view.selectedItemId = R.id.item_history
            else -> showFragment(R.id.item_scan)
        }
    }
    private fun showFragment(bottomItemId: Int) {
        val fragment = when (bottomItemId) {
            R.id.item_scan -> ScanBarcodeFromCameraFragment()
            R.id.item_create -> CreateBarcodeFragment()
            R.id.item_history -> BarcodeHistoryFragment()
            R.id.item_settings -> SettingsFragment()
            else -> null
        }
        fragment?.apply(::replaceFragment)
    }
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.layout_fragment_container, fragment)
            .setReorderingAllowed(true)
            .commit()
    }
}