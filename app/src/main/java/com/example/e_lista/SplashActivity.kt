package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Theme is already set to SplashTheme in the manifest

        lifecycleScope.launch {
            delay(2000) // 2-second splash delay

            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser

            if (currentUser != null && currentUser.isEmailVerified) {
                // ✅ User is logged in and verified → go straight to Home
                val intent = Intent(this@SplashActivity, Home9Activity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                // 🚪 Not logged in or not verified → go to Welcome screen
                val intent = Intent(this@SplashActivity, WelcomeActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }
}