package com.example.e_lista

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.e_lista.databinding.ActivityAboutUsBinding

class AboutUsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutUsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Set About Us content
        binding.aboutText.text = """
            At E-Lista, we believe that financial clarity should be simple, smart, and stress-free.
            Born from the idea that managing expenses shouldn’t feel like a chore, E-Lista helps you take full control of your spending—one list at a time.

            Our mission is to empower individuals to make confident financial decisions by giving them the tools to track, organize, and understand where their money goes.

            With an intuitive design, insightful analytics, and customizable categories, E-Lista transforms your expense tracking into a clear picture of your financial habits.
        """.trimIndent()
    }
}
