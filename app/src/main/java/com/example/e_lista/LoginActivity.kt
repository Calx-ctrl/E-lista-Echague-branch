package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityLogIn3Binding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogIn3Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogIn3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigate to SignupActivity when Sign Up button is clicked
        binding.signupBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // ✅ Navigate to Home9Activity when Login button is clicked
        binding.loginBtn.setOnClickListener {
            val intent = Intent(this, Home9Activity::class.java)
            startActivity(intent)
        }
    }
}
