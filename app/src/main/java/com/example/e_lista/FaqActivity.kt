package com.example.e_lista

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityFaqBinding

class FaqActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaqBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Optional: set FAQ text programmatically if needed
        binding.faqText.text = """
            1. What is E-Lista?
            E-Lista is an easy-to-use expense tracker that helps you record, organize, and understand your daily spending.

            2. Is it free?
            Yes! E-Lista is free to use, with premium features coming soon.

            3. Does it work offline?
            Yes. You can log expenses anytime—your data syncs when you’re back online.

            4. Is my data safe?
            Your privacy matters. E-Lista encrypts your data and keeps it secure.

            5. How can I contact support?
            Email us at elistaapp2025@gmail.com — we’re happy to help!
        """.trimIndent()
    }
}
