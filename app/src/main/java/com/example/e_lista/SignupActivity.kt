package com.example.e_lista // Make sure this matches your package name

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity


class SignupActivity : AppCompatActivity() {

    // 1. Declare variables for your UI elements
    private lateinit var emailEditText: EditText
    private lateinit var signUpButton: Button
    // Add other views like password, etc., if you need to interact with them

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_4)

        // 2. Link the variables to the views in your XML
        emailEditText = findViewById(R.id.emailEditText)
        signUpButton = findViewById(R.id.signUpButton)
        // Make sure the IDs match what's in your activity_sign_up.xml

        // 3. Set a click listener to handle the button press
        signUpButton.setOnClickListener {
            // This code will only run when the "Sign up" button is clicked
            handleSignUp()
        }
    }

    private fun handleSignUp() {
        // 4. Inside the listener, get the text from the EditText
        val userEmail = emailEditText.text.toString()

        // Check if email is not empty before proceeding
        if (userEmail.isNotEmpty()) {
            // This is the code from your screenshot, now placed correctly
            val intent = Intent(this, EmailSentActivity::class.java).apply {
                putExtra("USER_EMAIL", userEmail)
            }
            startActivity(intent)
        } else {
            // Optional: Show an error if the email field is empty
            emailEditText.error = "Email cannot be empty"
        }
    }
}