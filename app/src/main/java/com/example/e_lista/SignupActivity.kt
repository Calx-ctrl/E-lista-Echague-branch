package com.example.e_lista

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthProvider
import com.example.e_lista.databinding.ActivitySignUp4Binding

class SignupActivity : AppCompatActivity() {

    // Binding variable for the layout
    private lateinit var binding: ActivitySignUp4Binding

    // Dialog
    private var mDialog: ProgressDialog? = null

    // Firebase
    private lateinit var mAuth: FirebaseAuth

    // Google sign-in
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivitySignUp4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔙 Back button
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize ProgressDialog
        mDialog = ProgressDialog(this)

        // Handle sign-up button click
        binding.signUpButton.setOnClickListener { performEmailSignUp() }

        // Handle Google sign-up
        binding.googleButton.setOnClickListener {
            // ✅ Force Google to forget the previous sign-in
            googleSignInClient.revokeAccess().addOnCompleteListener {
                // After revoking, always show the chooser
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        // Handle Facebook sign-up
        binding.facebookButton.setOnClickListener {
            startFacebookSignUp()
            Snackbar.make(it, "Facebook Sign-Up coming soon!", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun startFacebookSignUp() {
        TODO("Not yet implemented")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                handleGoogleAccount(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ✅ This handles both sign-up and sign-in automatically:
     * - If the email exists → sign in
     * - If it's new → sign up
     */
    private fun handleGoogleAccount(account: GoogleSignInAccount) {
        val idToken = account.idToken ?: return
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val isNewUser = task.result?.additionalUserInfo?.isNewUser == true
                    val user = mAuth.currentUser

                    if (isNewUser) {
                        Toast.makeText(this, "Welcome, new user: ${user?.email}", Toast.LENGTH_SHORT).show()
                        // ✅ Optional: save user to Firestore or Realtime Database here
                    } else {
                        Toast.makeText(this, "Welcome back, ${user?.email}", Toast.LENGTH_SHORT).show()
                    }

                    startActivity(Intent(this, Home9Activity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Firebase Auth failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun performEmailSignUp() {
        val email = binding.emailEditText.text.toString().trim()
        val pass = binding.passwordEditText.text.toString().trim()
        val confirmpass = binding.confirmPasswordEditText.text.toString().trim()

        // Validation
        when {
            email.isEmpty() -> {
                binding.emailEditText.error = "Email Required"
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailEditText.error = "Enter a valid email address"
                return
            }
            pass.isEmpty() -> {
                binding.passwordEditText.error = "Password Required"
                return
            }
            !isValidPassword(pass) -> {
                binding.passwordEditText.error = "Password must be at least 8 chars, with upper, lower, and number"
                return
            }
            confirmpass != pass -> {
                binding.confirmPasswordEditText.error = "Passwords don't match"
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
                        binding.emailEditText.error = "This email is already registered"
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
