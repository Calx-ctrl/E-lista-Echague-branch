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

        // Highlight Profile tab
        binding.bottomNavigationView.selectedItemId = R.id.nav_profile

        // Back button
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Dark Mode Toggle
        val prefs = getSharedPreferences("user_settings", MODE_PRIVATE)
        binding.switchDarkMode.isChecked = prefs.getBoolean("dark_mode", false)
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
        }

        // Logout button
        binding.logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, WelcomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        // Bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateTo(Home9Activity::class.java)
                R.id.nav_wallet -> navigateTo(Expenses12Activity::class.java)
                R.id.nav_camera_placeholder -> navigateTo(ReceiptScanUpload::class.java)
                R.id.nav_stats -> navigateTo(ChartDesign10Activity::class.java)
                R.id.nav_profile -> true
                else -> false
            }
        }

        // Navigate to separate activities
        binding.faqItem.setOnClickListener { startActivity(Intent(this, FaqActivity::class.java)) }
        binding.termsItem.setOnClickListener { startActivity(Intent(this, TermsActivity::class.java)) }
        binding.privacyItem.setOnClickListener { startActivity(Intent(this, PrivacyActivity::class.java)) }
        binding.aboutItem.setOnClickListener { startActivity(Intent(this, AboutUsActivity::class.java)) }
    }

    // Navigate helper for bottom nav
    private fun navigateTo(target: Class<*>): Boolean {
        if (this::class.java != target) {
            startActivity(Intent(this, target))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
        return true
    }
}
