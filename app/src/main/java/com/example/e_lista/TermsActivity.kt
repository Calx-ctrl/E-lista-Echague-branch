package com.example.e_lista

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityTermsOfUseBinding

class TermsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTermsOfUseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsOfUseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Set Terms content
        binding.termsText.text = """
            Welcome to E-Lista! By using our app, you agree to follow these simple terms:

            - Use E-Lista only for personal and lawful purposes.
            - You are responsible for the accuracy of the information you enter.
            - Don’t attempt to copy, modify, or misuse the app’s features or data.
            - We may update or change the app at any time to improve your experience.
            - E-Lista is not responsible for any loss caused by inaccurate or incomplete data you provide.

            By continuing to use E-Lista, you accept these terms. If you have any questions, contact support@e-lista.app
        """.trimIndent()
    }
}
