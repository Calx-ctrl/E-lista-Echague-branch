package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The theme is already set to SplashTheme in AndroidManifest.xml,
        // so no need for setContentView()

        // Simulate loading or initialization
        lifecycleScope.launch {
            delay(2000) // 2-second splash delay

            // TODO: Replace LoginActivity with HomeActivity if user is already logged in
            val intent = Intent(this@SplashActivity, WelcomeActivity::class.java)
            startActivity(intent)
        }
    }
}
