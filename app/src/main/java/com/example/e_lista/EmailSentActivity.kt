package com.example.e_lista

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class EmailSentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_sent_5)

        val messageTextView: TextView = findViewById(R.id.messageTextView)
        val nextButton: Button = findViewById(R.id.nextButton) // make sure this exists in your XML

        // Get the email passed from the previous activity
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: "your.email@example.com"

        // Create the full message string
        val fullMessage = "Check your email, we've sent a code to $userEmail. To verify your email, enter the six-digit passcode on the next page."

        // Make the email address bold and blue
        val spannableString = SpannableString(fullMessage)
        val emailStartIndex = fullMessage.indexOf(userEmail)
        val emailEndIndex = emailStartIndex + userEmail.length

        if (emailStartIndex != -1) {
            val blueColor = ContextCompat.getColor(this, android.R.color.holo_blue_dark)
            spannableString.setSpan(ForegroundColorSpan(blueColor), emailStartIndex, emailEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannableString.setSpan(StyleSpan(Typeface.BOLD), emailStartIndex, emailEndIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Set formatted text to TextView
        messageTextView.text = spannableString

        // 🟢 Handle Next Button → Go to OTP Activity
        nextButton.setOnClickListener {
            val intent = Intent(this, OtpActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
        }
    }
}
