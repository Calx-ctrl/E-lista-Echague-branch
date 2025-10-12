package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.e_lista.databinding.ActivityProfile13Binding

class Profile13Activity : AppCompatActivity() {

    private lateinit var binding: ActivityProfile13Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfile13Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 Highlight the "Profile" tab when this screen is open
        binding.bottomNavigationView.selectedItemId = R.id.nav_profile

        // 🔙 Back button
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 📸 Floating Camera button
        binding.fabCamera.setOnClickListener {
            val intent = Intent(this, Camera11Activity::class.java)
            startActivity(intent)
        }

        // 🌙 Dark Mode Toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // ⚙️ Bottom Navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, Home9Activity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_stats -> {
                    startActivity(Intent(this, ChartDesign10Activity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_wallet -> {
                    startActivity(Intent(this, Expenses12Activity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> true // already here
                else -> false
            }
        }
    }
}
