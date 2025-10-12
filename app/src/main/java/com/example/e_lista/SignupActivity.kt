package com.example.e_lista

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.android.material.snackbar.Snackbar

class SignupActivity : AppCompatActivity() {

    // UI elements
    private lateinit var mEmail: EditText
    private lateinit var mPass: EditText
    private lateinit var confirmmPass: EditText
    private lateinit var signUpButton: Button
    private lateinit var googleButton: LinearLayout
    private lateinit var facebookButton: LinearLayout

    // Dialog
    private var mDialog: ProgressDialog? = null

    // Firebase
    private lateinit var mAuth: FirebaseAuth

    // Google sign-in
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_4)

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize views
        mEmail = findViewById(R.id.emailEditText)
        mPass = findViewById(R.id.passwordEditText)
        confirmmPass = findViewById(R.id.confirmPasswordEditText)
        signUpButton = findViewById(R.id.signUpButton)
        googleButton = findViewById(R.id.googleButton)
        facebookButton = findViewById(R.id.facebookButton)
        mDialog = ProgressDialog(this)

        // Handle sign-up button click
        signUpButton.setOnClickListener { performEmailSignUp() }

        // Handle Google sign-up
        googleButton.setOnClickListener {
            Snackbar.make(it, "Google Sign-Up coming soon!", Snackbar.LENGTH_SHORT).show()
            // TODO: Replace with startGoogleSignIn() when implementing full sign-in flow
        }

        // Handle Facebook sign-up
        facebookButton.setOnClickListener {
            Snackbar.make(it, "Facebook Sign-Up coming soon!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun performEmailSignUp() {
        val email = mEmail.text.toString().trim()
        val pass = mPass.text.toString().trim()
        val confirmpass = confirmmPass.text.toString().trim()

        // Validation
        when {
            email.isEmpty() -> {
                mEmail.error = "Email Required"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                mEmail.error = "Enter a valid email address"
                return
            }
            pass.isEmpty() -> {
                mPass.error = "Password Required"
                return
            }
            !isValidPassword(pass) -> {
                mPass.error = "Password must be at least 8 chars, with upper, lower, and number"
                return
            }
            confirmpass != pass -> {
                confirmmPass.error = "Passwords don't match"
                return
            }
        }

        // Proceed with Firebase sign-up
        mDialog?.setMessage("Processing...")
        mDialog?.show()

        mAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                mDialog?.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Registration Complete", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, EmailSentActivity::class.java).apply {
                        putExtra("USER_EMAIL", email)
                    })
                } else {
                    try {
                        throw task.exception ?: Exception("Unknown error")
                    } catch (e: FirebaseAuthUserCollisionException) {
                        mEmail.error = "This email is already registered"
                    } catch (e: Exception) {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun isValidPassword(pass: String): Boolean {
        // At least 8 characters, 1 uppercase, 1 lowercase, 1 digit
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$")
        return passwordRegex.matches(pass)
    }
}
