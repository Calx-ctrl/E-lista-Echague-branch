package com.example.e_lista

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
    //google signin
    private val RC_SIGN_IN = 1001

    //FB sign up
    private lateinit var callbackManager: CallbackManager

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

        //Config FB sign-in
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)

        callbackManager = CallbackManager.Factory.create()

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
            startFacebookSignIn()
        }
    }

    private fun startFacebookSignIn() {
        // Always show account chooser (forces re-login)
        LoginManager.getInstance().logOut()

        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookSignInToken(result.accessToken)
                }

                override fun onCancel() {
                    Toast.makeText(
                        this@LoginActivity,
                        "Facebook sign-in cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(error: FacebookException) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Facebook sign-in failed: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun handleFacebookSignInToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)

        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this@LoginActivity) { task ->
                if (task.isSuccessful) {
                    val result = task.result
                    val isNewUser = result?.additionalUserInfo?.isNewUser == true
                    val user = mAuth.currentUser

                    if (isNewUser) {
                        // ❌ Account doesn’t exist — delete immediately
                        user?.delete()
                        mAuth.signOut()
                        LoginManager.getInstance().logOut()
                        Toast.makeText(
                            this@LoginActivity,
                            "This Facebook account is not associated with an E-Lista account.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // ✅ Existing account — proceed
                        Toast.makeText(
                            this@LoginActivity,
                            "Welcome back, ${user?.displayName}",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(Intent(this@LoginActivity, Home9Activity::class.java))
                        finish()
                    }

                } else {
                    // 🔍 Handle specific Firebase exceptions
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthUserCollisionException -> {
                            // ⚠️ Email already used by another provider
                            Toast.makeText(
                                this@LoginActivity,
                                "Facebook account’s email is registered with another sign-in method.",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.w("FacebookAuth", "Collision: ${exception.message}")
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "Invalid Facebook credentials. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("FacebookAuth", "Invalid credentials", exception)
                        }
                        else -> {
                            Toast.makeText(
                                this@LoginActivity,
                                "Sign-in failed: ${exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("FacebookAuth", "Unknown error", exception)
                        }
                    }
                }
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
                    val exception = task.exception
                    Log.w("EmailLogin", "Sign-in failed", exception)

                    when (exception) {
                        is FirebaseNetworkException -> {
                            Toast.makeText(
                                this,
                                "Network error. Please check your connection.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is FirebaseAuthInvalidCredentialsException,
                        is FirebaseAuthInvalidUserException -> {
                            // ✅ Covers wrong password OR non-existent email
                            Toast.makeText(
                                this,
                                "Invalid email or password.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Toast.makeText(
                                this,
                                "Login failed: ${exception?.localizedMessage ?: "Unknown error"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
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
                when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS,
                    12501 -> {
                        // 👇 User cancelled the sign-in, ignore silently
                        Log.i("GoogleSignIn", "User cancelled Google Sign-In")
                    }
                    GoogleSignInStatusCodes.NETWORK_ERROR,
                    7 -> {
                        // 👇 No internet connection or network issue
                        Toast.makeText(this, "Network error. Please check your connection", Toast.LENGTH_SHORT).show()
                        Log.w("GoogleSignIn", "Network error during sign-in", e)
                    }
                    else -> {
                        // 👇 Other real errors
                        Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("GoogleSignIn", "Error during sign-in", e)
                    }
                }
            }
        } else {
            // ✅ Pass other results (like Facebook) to their handler
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleGoogleSignIn(account: GoogleSignInAccount) {
        val idToken = account.idToken ?: return
        val email = account.email ?: return

        // Step 1: Check if email exists and what provider was used
        mAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { methodTask ->
                if (methodTask.isSuccessful) {
                    val methods = methodTask.result?.signInMethods ?: emptyList()

                    if (methods.isEmpty()) {
                        // ❌ No account found with this email
                        Toast.makeText(
                            this,
                            "This Google account is not associated with an E-Lista account.",
                            Toast.LENGTH_LONG
                        ).show()
                        googleSignInClient.signOut()
                        mAuth.signOut()
                        return@addOnCompleteListener
                    }

                    if (!methods.contains("google.com")) {
                        // ❌ Email exists, but registered via another method
                        Toast.makeText(
                            this,
                            "This email is registered with a different sign-in method.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.w("GoogleAuth", "Existing provider: $methods")
                        googleSignInClient.signOut()
                        mAuth.signOut()
                        return@addOnCompleteListener
                    }

                    // ✅ Email exists and registered via Google → proceed
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val user = mAuth.currentUser
                                Toast.makeText(
                                    this,
                                    "Welcome back, ${user?.email}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(this, Home9Activity::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Google Sign-In failed: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("GoogleAuth", "Sign-In failed", task.exception)
                            }
                        }
                } else {
                    Toast.makeText(
                        this,
                        "Failed to verify email: ${methodTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("GoogleAuth", "fetchSignInMethodsForEmail failed", methodTask.exception)
                }
            }
    }


}
