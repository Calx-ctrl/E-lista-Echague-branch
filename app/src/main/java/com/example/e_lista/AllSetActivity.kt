package com.example.e_lista

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class AllSetActivity : AppCompatActivity() {

    // Set the duration for how long this screen will be displayed (e.g., 2 seconds)
    private val SCREEN_DELAY: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_set_8)

        // Use a Handler to delay the navigation to the next screen
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an Intent to start the main activity of your app (HomeActivity)
            // Replace HomeActivity::class.java if your main screen has a different name
            val intent = Intent(this, Home9Activity::class.java)

            // These flags clear the back stack. This is important!
            // It prevents the user from pressing the "back" button and returning
            // to the login/signup flow after they are already logged in.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            // Close this AllSetActivity so it's also removed from the back stack
            finish()
        }, SCREEN_DELAY)
    }
}