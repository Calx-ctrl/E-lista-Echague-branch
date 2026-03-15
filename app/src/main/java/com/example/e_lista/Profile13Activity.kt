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

        binding.logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        val user = FirebaseAuth.getInstance().currentUser
        binding.emailText.text = user?.email ?: ""

        var isEmailPasswordUser = false

        user?.providerData?.forEach { profile ->
            if (profile.providerId == "password") {
                isEmailPasswordUser = true
            }
        }

        if (isEmailPasswordUser) {
            // Show the button only for Email/Password users
            binding.changePasswordItem.visibility = android.view.View.VISIBLE

            // Set click listener to show confirmation dialog
            binding.changePasswordItem.setOnClickListener {
                showPasswordResetDialog(user?.email)
            }
        }

        binding.faqItem.setOnClickListener { startActivity(Intent(this, FaqActivity::class.java)) }
        binding.termsItem.setOnClickListener { startActivity(Intent(this, TermsActivity::class.java)) }
        binding.privacyItem.setOnClickListener { startActivity(Intent(this, PrivacyActivity::class.java)) }
        binding.aboutItem.setOnClickListener { startActivity(Intent(this, AboutUsActivity::class.java)) }


        // ⚙️ Bottom Navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (this !is Home9Activity) {
                        startActivity(Intent(this, Home9Activity::class.java))
                        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_wallet -> {
                    if (this !is Expenses12Activity) {
                        startActivity(Intent(this, Expenses12Activity::class.java))
                        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_camera_placeholder -> {
                    if (this !is ReceiptScanUpload) {
                        startActivity(Intent(this, ReceiptScanUpload::class.java))
                        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    true
                }

                R.id.nav_stats -> {
                    if (this !is ChartDesign10Activity) {
                        startActivity(Intent(this, ChartDesign10Activity::class.java))
                        //overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                        overridePendingTransition(0, 0)
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
        binding.bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    private fun showPasswordResetDialog(email: String?) {
        if (email.isNullOrEmpty()) return

        // 1. Check if the cooldown period has passed (e.g., 5 minutes = 300,000 milliseconds)
        val sharedPref = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val lastResetTime = sharedPref.getLong("LAST_PASSWORD_RESET_TIME", 0)
        val currentTime = System.currentTimeMillis()
        val cooldownTime = 5 * 60 * 1000 // 5 minutes in milliseconds

        if (currentTime - lastResetTime < cooldownTime) {
            // Still in cooldown
            val timeLeftMillis = cooldownTime - (currentTime - lastResetTime)
            val minutesLeft = (timeLeftMillis / 1000) / 60
            val secondsLeft = (timeLeftMillis / 1000) % 60

            android.widget.Toast.makeText(
                this,
                "Please wait $minutesLeft min ${secondsLeft}s before requesting another link.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return // Stop the function here, don't show the dialog
        }

        // 2. If cooldown is over, show the confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Are you sure you want to send a password reset link to $email?")
            .setPositiveButton("Send") { _, _ ->

                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // 3. Save the current time because the email sent successfully
                            sharedPref.edit().putLong("LAST_PASSWORD_RESET_TIME", currentTime).apply()

                            android.widget.Toast.makeText(
                                this,
                                "Reset link sent! Please check your inbox.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                this,
                                "Error: ${task.exception?.localizedMessage}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}