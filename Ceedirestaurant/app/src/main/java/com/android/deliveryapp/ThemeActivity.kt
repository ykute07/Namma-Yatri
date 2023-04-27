package com.android.deliveryapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.android.deliveryapp.databinding.ActivityThemeBinding
import com.android.deliveryapp.util.Keys.Companion.themePref
import com.android.deliveryapp.util.Keys.Companion.userInfo

class ThemeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityThemeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // show a back arrow button in actionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val sharedPreferences = getSharedPreferences(userInfo, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        updateView()

        binding.radioGroup.setOnCheckedChangeListener { _, _ ->
            if (binding.lightTheme.isChecked) { // LIGHT THEME
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putInt(themePref, AppCompatDelegate.MODE_NIGHT_NO)
                editor.apply()
            }
            if (binding.darkTheme.isChecked) { // DARK THEME
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putInt(themePref, AppCompatDelegate.MODE_NIGHT_YES)
                editor.apply()
            }
            if (binding.systemDefault.isChecked) { // SYSTEM DEFAULT
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                editor.putInt(themePref, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                editor.apply()
            }
        }
    }

    @SuppressLint("SwitchIntDef")
    private fun updateView() {
        // update radiobuttons
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> {
                binding.lightTheme.isChecked = true
                binding.darkTheme.isChecked = false
                binding.systemDefault.isChecked = false
            }
            AppCompatDelegate.MODE_NIGHT_YES -> {
                binding.lightTheme.isChecked = false
                binding.darkTheme.isChecked = true
                binding.systemDefault.isChecked = false
            }
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                binding.lightTheme.isChecked = false
                binding.darkTheme.isChecked = false
                binding.systemDefault.isChecked = true
            }
        }
    }

    // when the back button is pressed in actionbar, finish this activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}