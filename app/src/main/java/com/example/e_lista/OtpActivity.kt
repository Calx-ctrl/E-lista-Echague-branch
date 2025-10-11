package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OtpActivity : AppCompatActivity() {

    private lateinit var otpBoxes: List<EditText>
    private lateinit var countdownTextView: TextView
    private lateinit var resendOtpTextView: TextView
    private lateinit var verifyButton: Button

    private var otpTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 5 * 60 * 1000 // 5 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_6)

        // 🧩 Initialize views
        otpBoxes = listOf(
            findViewById(R.id.otpBox1),
            findViewById(R.id.otpBox2),
            findViewById(R.id.otpBox3),
            findViewById(R.id.otpBox4),
            findViewById(R.id.otpBox5),
            findViewById(R.id.otpBox6)
        )

        countdownTextView = findViewById(R.id.countdownTextView)
        resendOtpTextView = findViewById(R.id.resendOtpTextView)
        verifyButton = findViewById(R.id.verifyButton)

        // 🕒 Start countdown
        startTimer()

        // ✉️ Handle resend click
        resendOtpTextView.setOnClickListener {
            resetTimer()
            Toast.makeText(this, "OTP resent!", Toast.LENGTH_SHORT).show()
        }

        // ⌨️ Auto-focus next box
        setupOtpInputs()

        // ✅ Verify any 6-digit OTP
        verifyButton.setOnClickListener {
            val enteredOtp = otpBoxes.joinToString("") { it.text.toString() }

            if (enteredOtp.length == 6) {
                Toast.makeText(this, "OTP Verified Successfully!", Toast.LENGTH_SHORT).show()

                // 🚀 Go to AllSetActivity
                val intent = Intent(this, AllSetActivity::class.java)
                startActivity(intent)
                finish() // optional, disables back press returning here
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                Toast.makeText(this, "Please enter all 6 digits", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTimer() {
        otpTimer?.cancel()
        otpTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                countdownTextView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                countdownTextView.text = "00:00"
                resendOtpTextView.isEnabled = true
                resendOtpTextView.setTextColor(resources.getColor(android.R.color.holo_blue_dark))
            }
        }.start()
    }

    private fun resetTimer() {
        timeLeftInMillis = 5 * 60 * 1000
        resendOtpTextView.isEnabled = false
        resendOtpTextView.setTextColor(resources.getColor(android.R.color.darker_gray))
        startTimer()
    }

    private fun setupOtpInputs() {
        for (i in otpBoxes.indices) {
            otpBoxes[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1 && i < otpBoxes.size - 1) {
                        otpBoxes[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        otpTimer?.cancel()
    }
}
