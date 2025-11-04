package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.e_lista.databinding.ActivityProfile13Binding
import com.google.firebase.auth.FirebaseAuth

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
        //binding.fabCamera.setOnClickListener {
        //    val intent = Intent(this, Camera11Activity::class.java)
        //    startActivity(intent)
        //}

        binding.logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
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
                    if (this !is Home9Activity) {
                        startActivity(Intent(this, Home9Activity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }

                R.id.nav_wallet -> {
                    if (this !is Expenses12Activity) {
                        startActivity(Intent(this, Expenses12Activity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }

                R.id.nav_camera_placeholder -> {
                    if (this !is ReceiptScanUpload) {
                        startActivity(Intent(this, ReceiptScanUpload::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }

                R.id.nav_stats -> {
                    if (this !is ChartDesign10Activity) {
                        startActivity(Intent(this, ChartDesign10Activity::class.java))
                        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        finish()
                    }
                    true
                }

                R.id.nav_profile -> {
                    true
                }

                else -> false
            }
        }
        binding.bottomNavigationView.selectedItemId = R.id.nav_home
    }
}