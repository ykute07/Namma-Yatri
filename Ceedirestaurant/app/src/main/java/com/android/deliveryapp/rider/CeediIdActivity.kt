package com.android.deliveryapp.rider

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.android.deliveryapp.databinding.ActivityCeediIdBinding

class CeediIdActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCeediIdBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCeediIdBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}