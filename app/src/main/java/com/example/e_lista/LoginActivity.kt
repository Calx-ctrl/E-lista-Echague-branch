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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    // UI
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginBtn: Button
    private lateinit var signupBtn: TextView
    private lateinit var googleButton: LinearLayout
    private lateinit var facebookButton: LinearLayout

    // Firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Dialog
    private var progressDialog: ProgressDialog? = null

    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in_3)

        // Firebase
        mAuth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginBtn = findViewById(R.id.loginBtn)
        signupBtn = findViewById(R.id.signupBtn)
        googleButton = findViewById(R.id.googleButton)
        facebookButton = findViewById(R.id.facebookButton)

        progressDialog = ProgressDialog(this)

        // 🔹 Email Login
        loginBtn.setOnClickListener {
            performEmailLogin()
        }

        // 🔹 Go to Signup
        signupBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // 🔹 Google Login
        googleButton.setOnClickListener {
            // ✅ Force Google to forget the previous sign-in
            googleSignInClient.revokeAccess().addOnCompleteListener {
                // After revoking, always show the chooser
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        // 🔹 Facebook Login
        facebookButton.setOnClickListener {
        }
    }

    private fun performEmailLogin() {
        val email = emailEditText.text.toString().trim()
        val pass = passwordEditText.text.toString().trim()

        if (email.isEmpty()) {
            emailEditText.error = "Email Required"
            return
        }
        if (pass.isEmpty()) {
            passwordEditText.error = "Password Required"
            return
        }

        progressDialog?.setMessage("Logging in...")
        progressDialog?.show()

        mAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                progressDialog?.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Home9Activity::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Login failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }


    // 🔹 Google Sign-In result
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                handleGoogleSignIn(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount) {
        val idToken = account.idToken ?: return
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val result = task.result
                    val isNewUser = result?.additionalUserInfo?.isNewUser == true
                    val user = mAuth.currentUser

                    if (isNewUser) {
                        // ❌ User doesn’t exist — delete the auto-created account
                        user?.delete()
                        mAuth.signOut()
                        googleSignInClient.signOut()
                        Toast.makeText(
                            this,
                            "This Google account is not associated with an E-Lista account.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // ✅ Existing account — log in
                        Toast.makeText(this, "Welcome back, ${user?.email}", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Home9Activity::class.java))
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Google Sign-In failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
