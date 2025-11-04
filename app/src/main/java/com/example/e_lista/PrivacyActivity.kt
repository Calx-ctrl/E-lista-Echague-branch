package com.example.e_lista

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityPrivacyBinding

class PrivacyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Set Privacy content
        binding.privacyText.text = """
            E-Lista values your privacy. We collect only the information needed to provide and improve our services — such as your entered expenses, account details, and basic device info.

            Your data is stored securely and never shared with third parties. You can delete your data anytime in the app or by emailing privacy@e-lista.app

            By using E-Lista, you agree to this policy and our commitment to keeping your information safe.
        """.trimIndent()
    }
}
