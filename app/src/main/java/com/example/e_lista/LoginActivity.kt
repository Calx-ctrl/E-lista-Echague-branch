package com.example.e_lista

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    // UI
    private lateinit var emailEditText: EditText

    private lateinit var passwordEditText: EditText
    private lateinit var forgotPasswordText: TextView
    private lateinit var loginBtn: Button
    private lateinit var signupBtn: TextView
    private lateinit var googleButton: LinearLayout
    private lateinit var facebookButton: LinearLayout
    private lateinit var resendVerificationBtn: Button
    private lateinit var verifyNoticeText: TextView

    // Firebase
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Dialog
    private var progressDialog: ProgressDialog? = null
    //google signin
    private val RC_SIGN_IN = 1001

    //FB sign up
    private lateinit var callbackManager: CallbackManager

    //para sa verificatoi
    private var unverifiedUserEmail: String? = null

    // Timer
    private var resendTimer: CountDownTimer? = null
    private var canResend = true
    private val RESEND_PREFS = "ResendPrefs"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in_3)

        mAuth = FirebaseAuth.getInstance()

        // 🔹 Add these two UI elements in your XML
        resendVerificationBtn = findViewById(R.id.resendVerificationBtn)
        verifyNoticeText = findViewById(R.id.verifyNoticeText)

        // Hide initially
        resendVerificationBtn.visibility = View.GONE
        verifyNoticeText.visibility = View.GONE


        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                // ✅ Email verified, skip login screen
                startActivity(Intent(this, Home9Activity::class.java))
                finish()
                return
            } else {
                // ⚠️ Email not verified, show verification UI
                unverifiedUserEmail = currentUser.email
                mAuth.signOut()
            }
        }

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
        forgotPasswordText = findViewById(R.id.forgotPasswordText)

        // Hide "Forgot Password" if they start typing again
        emailEditText.addTextChangedListener {
            if (resendVerificationBtn.visibility == View.VISIBLE) hideVerificationSection()
            forgotPasswordText.visibility = View.GONE // <-- ADD THIS
        }
        passwordEditText.addTextChangedListener {
            forgotPasswordText.visibility = View.GONE // <-- ADD THIS
        }

        forgotPasswordText.setOnClickListener {
            handleForgotPassword()
        }

        val passedEmail = intent.getStringExtra("USER_EMAIL")
        if (!passedEmail.isNullOrEmpty()) {
            emailEditText.setText(passedEmail)
            passwordEditText.requestFocus()
        }
        loginBtn = findViewById(R.id.loginBtn)
        signupBtn = findViewById(R.id.signupBtn)
        googleButton = findViewById(R.id.googleButton)
        facebookButton = findViewById(R.id.facebookButton)
        progressDialog = ProgressDialog(this)

        loginBtn.setOnClickListener {
            performEmailLogin()
        }

        signupBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        googleButton.setOnClickListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        facebookButton.setOnClickListener {
            startFacebookSignIn()
        }

        resendVerificationBtn.setOnClickListener {
            resendVerificationEmail()
        }

        // When user changes email → hide verification section
        emailEditText.addTextChangedListener {
            if (resendVerificationBtn.visibility == View.VISIBLE)hideVerificationSection()
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
                            "Welcome back",
                            Toast.LENGTH_SHORT
                        ).show()
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
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
                    val user = mAuth.currentUser

                    if (user != null && user.isEmailVerified) {
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, Home9Activity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                        getSharedPreferences(RESEND_PREFS, MODE_PRIVATE).edit().clear().apply()
                    } else {
                        // ❌ Not verified yet
                        unverifiedUserEmail = user?.email
                        showVerificationSection(user)
                        mAuth.signOut()
                    }

                } else {
                    val exception = task.exception
                    Log.w("EmailLogin", "Sign-in failed", exception)

                    //Show Forgot Password text on failed attempt
                    forgotPasswordText.visibility = View.VISIBLE

                    when (exception) {
                        is FirebaseNetworkException -> {
                            Toast.makeText(this, "Network error. Please check your connection.", Toast.LENGTH_SHORT).show()
                        }
                        is FirebaseAuthInvalidCredentialsException,
                        is FirebaseAuthInvalidUserException -> {
                            Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Login failed: ${exception?.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

    private fun handleForgotPassword() {
        val email = emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            emailEditText.error = "Please enter your email here first"
            emailEditText.requestFocus()
            return
        }

        progressDialog?.setMessage("Checking account...")
        progressDialog?.show()

        // 1. Check if the email is actually registered in Firebase
        mAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                progressDialog?.dismiss()

                if (task.isSuccessful) {
                    val methods = task.result?.signInMethods ?: emptyList()

                    if (methods.isEmpty()) {
                        // ❌ No account exists
                        Toast.makeText(this, "No account exists with this email address.", Toast.LENGTH_LONG).show()
                    } else if (!methods.contains("password")) {
                        // ❌ Account exists, but they used Google or Facebook
                        Toast.makeText(this, "This email is registered using a social login (Google/Facebook).", Toast.LENGTH_LONG).show()
                    } else {
                        // ✅ Account exists and uses a password! Show the dialog.
                        showPasswordResetDialog(email)
                    }
                } else {
                    Toast.makeText(this, "Error checking email: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showPasswordResetDialog(email: String) {
        // 1. Check if the cooldown period has passed (5 minutes = 300,000 milliseconds)
        val sharedPref = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        val lastResetTime = sharedPref.getLong("LAST_PASSWORD_RESET_TIME", 0)
        val currentTime = System.currentTimeMillis()
        val cooldownTime = 5 * 60 * 1000 // 5 minutes in milliseconds

        if (currentTime - lastResetTime < cooldownTime) {
            // Still in cooldown
            val timeLeftMillis = cooldownTime - (currentTime - lastResetTime)
            val minutesLeft = (timeLeftMillis / 1000) / 60
            val secondsLeft = (timeLeftMillis / 1000) % 60

            Toast.makeText(
                this,
                "Please wait $minutesLeft min ${secondsLeft}s before requesting another link.",
                Toast.LENGTH_LONG
            ).show()
            return // Stop the function here, don't show the dialog
        }

        // 2. If cooldown is over, show the confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Are you sure you want to send a password reset link to $email?")
            .setPositiveButton("Send") { _, _ ->

                progressDialog?.setMessage("Sending link...")
                progressDialog?.show()

                mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        progressDialog?.dismiss()
                        if (task.isSuccessful) {
                            // 3. Save the current time because the email sent successfully
                            sharedPref.edit().putLong("LAST_PASSWORD_RESET_TIME", currentTime).apply()

                            Toast.makeText(
                                this,
                                "Reset link sent! Please check your inbox.",
                                Toast.LENGTH_LONG
                            ).show()

                            // Hide the button again after success
                            forgotPasswordText.visibility = View.GONE
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to send reset email: ${task.exception?.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun startResendTimer(durationMillis: Long) {
        resendVerificationBtn.isEnabled = false

        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                resendVerificationBtn.text = "Resend in ${seconds}s"
            }

            override fun onFinish() {
                resendVerificationBtn.text = "Resend verification email"
                resendVerificationBtn.isEnabled = true

                // Clear timer state from SharedPreferences
                getSharedPreferences(RESEND_PREFS, MODE_PRIVATE).edit().clear().apply()
            }
        }.start()
    }

    private fun saveResendTimer(email: String) {
        val prefs = getSharedPreferences(RESEND_PREFS, MODE_PRIVATE)
        val expireTime = System.currentTimeMillis() + 3 * 60 * 1000 // 3 minutes
        prefs.edit()
            .putString("lastEmail", email)
            .putLong("expireTime", expireTime)
            .apply()
    }

    private fun checkAndResumeResendTimer(email: String) {
        val prefs = getSharedPreferences(RESEND_PREFS, MODE_PRIVATE)
        val lastEmail = prefs.getString("lastEmail", null)
        val expireTime = prefs.getLong("expireTime", 0)
        val currentTime = System.currentTimeMillis()

        if (lastEmail == email && currentTime < expireTime) {
            val millisLeft = expireTime - currentTime
            startResendTimer(millisLeft)
        } else {
            resendVerificationBtn.text = "Resend verification email"
            resendVerificationBtn.isEnabled = true
        }
    }


    private fun showVerificationSection(user: FirebaseUser?) {
        verifyNoticeText.visibility = View.VISIBLE
        verifyNoticeText.text = "Your email is not verified. Please verify to continue."
        resendVerificationBtn.visibility = View.VISIBLE

        val email = user?.email ?: emailEditText.text.toString().trim()

        // Remember which account the cooldown belongs to
        unverifiedUserEmail = email
        checkAndResumeResendTimer(email)

        resendVerificationBtn.setOnClickListener {
            resendVerificationEmail()
        }
    }

    private fun hideVerificationSection() {
        verifyNoticeText.visibility = View.GONE
        resendVerificationBtn.visibility = View.GONE
        resendVerificationBtn.text = "Resend Verification Email"
        resendTimer?.cancel()
        canResend = true
    }

    private fun resendVerificationEmail() {
        val email = unverifiedUserEmail ?: emailEditText.text.toString().trim()
        val pass = passwordEditText.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Enter email and password first", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences(RESEND_PREFS, MODE_PRIVATE)
        val lastEmail = prefs.getString("lastEmail", null)
        val expireTime = prefs.getLong("expireTime", 0)

        // 🕒 Check if timer still active for this email
        if (lastEmail == email && System.currentTimeMillis() < expireTime) {
            val millisLeft = expireTime - System.currentTimeMillis()
            startResendTimer(millisLeft)
            return
        }

        // 🔐 Silent sign-in (ensures user is available)
        mAuth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                val user = mAuth.currentUser
                if (user != null && !user.isEmailVerified) {
                    user.sendEmailVerification()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Verification email sent to $email", Toast.LENGTH_SHORT).show()
                                saveResendTimer(email)
                                startResendTimer(3 * 60 * 1000)
                            } else {
                                Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(this, "Your email is already verified.", Toast.LENGTH_SHORT).show()
                }

                // Sign out again after sending, so login state stays clean
                mAuth.signOut()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Re-authentication failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
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
                                    "Welcome back",
                                    Toast.LENGTH_SHORT
                                ).show()
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
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
