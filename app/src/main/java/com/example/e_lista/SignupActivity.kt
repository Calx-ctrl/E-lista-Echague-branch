package com.example.e_lista // Make sure this matches your package name

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
//e2 binagu

class SignupActivity : AppCompatActivity() {

    // 1. Declare variables for your UI elements
    private lateinit var mEmail: EditText
    private lateinit var mPass: EditText
    private lateinit var confirmmPass: EditText
    private lateinit var signUpButton: Button
    //val googleButton = findViewById<Button>(R.id.googleSignUpButton)
    //Dialog
    private var mDialog: ProgressDialog ?= null

    //Firebase
    private lateinit var mAuth: FirebaseAuth

    //google signup
    private lateinit var googleSignInClient: GoogleSignInClient
    // Unique request code for Google Sign In intent
    private val RC_SIGN_IN = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up_4)

        mAuth= FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // from google-services.json
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        mDialog = ProgressDialog(this)

        handleSignUp()
    }

    private fun handleSignUp() {
        // 4. Inside the listener, get the text from the EditText
        mEmail = findViewById(R.id.emailEditText)
        mPass = findViewById(R.id.passwordEditText)
        confirmmPass = findViewById(R.id.confirmPasswordEditText)
        signUpButton = findViewById(R.id.signUpButton)

        signUpButton.setOnClickListener {
            val email = mEmail.text.toString().trim()
            val pass = mPass.text.toString().trim()
            val confirmpass = confirmmPass.text.toString().trim()

            if (email.isEmpty()) {
                mEmail.setError("Email Required")
                return@setOnClickListener
            }
            //check if vaild email
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            if (pass.isEmpty()) {
                mPass.setError("Password Required")
                return@setOnClickListener
            }

            if (!isValidPassword(pass)) {
                mPass.error = "Password must be at least 8 characters, include uppercase, lowercase and a number"
                return@setOnClickListener
            }


            if(confirmpass != pass){
                confirmmPass.setError("Passwords don't match")
                return@setOnClickListener
            }

            mDialog?.setMessage("Processing...")
            mDialog?.show()

            mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        mDialog?.dismiss()
                        Toast.makeText(applicationContext, "Registration Complete", Toast.LENGTH_SHORT).show()

                        //startActivity(Intent(applicationContext, Home9Activity::class.java))
                        startActivity(Intent(this, EmailSentActivity::class.java).apply {
                            putExtra("USER_EMAIL", email)
                        })

                    }else {
                        try {
                            throw task.exception ?: Exception("Unknown error")
                        } catch (e: FirebaseAuthUserCollisionException) {
                            // 🔹 This exception means the email already exists
                            mEmail.error = "This email is already registered"
                        } catch (e: Exception) {
                            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        mDialog?.dismiss()
                        //Toast.makeText(applicationContext, "Registration Failed", Toast.LENGTH_SHORT).show()

                    }
                    try{
                        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener {
                            if (task.isSuccessful) {
                                Toast.makeText(applicationContext, "Registration Complete", Toast.LENGTH_SHORT).show()
                            }else {
                                Toast.makeText(applicationContext, "Registration Failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }catch (e: FirebaseAuthUserCollisionException) {
                        // 🔹 This exception means the email already exists
                        mEmail.error = "This email is already registered"
                    }

                }

        //val userEmail = mEmail.text.toString()
        /*
        // Check if email is not empty before proceeding
        if (userEmail.isNotEmpty()) {
            // This is the code from your screenshot, now placed correctly
            val intent = Intent(this, EmailSentActivity::class.java).apply {
                putExtra("USER_EMAIL", userEmail)
            }
            startActivity(intent)
        } else {
            // Optional: Show an error if the email field is empty
            mEmail.error = "Email cannot be empty"
        }*/
        }
    }

    private fun SignupActivity.isValidPassword(pass: String): Boolean {
        // At least 8 characters, 1 uppercase, 1 lowercase, 1 digit, 1 special char
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}$")
        return passwordRegex.matches(pass)
    }
}
