package com.example.e_lista

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FacebookAuthProvider
import java.util.Locale


class SignupActivity : AppCompatActivity() {

    // Binding variable for the layout
    private lateinit var binding: ActivitySignUp4Binding
    // Dialog
    private var mDialog: ProgressDialog? = null

    // Firebase
    private lateinit var mAuth: FirebaseAuth

    // Google sign-up
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001

    //FB sign up
    private lateinit var callbackManager: CallbackManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUp4Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase
        mAuth = FirebaseAuth.getInstance()

        // Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Facebook Sign-In
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
        callbackManager = CallbackManager.Factory.create()

        mDialog = ProgressDialog(this)

        binding.signUpButton.setOnClickListener { performEmailSignUp() }

        binding.signinBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Google sign-up
        binding.googleButton.setOnClickListener {
            googleSignInClient.revokeAccess().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        // Facebook sign-up
        binding.facebookButton.setOnClickListener {
            startFacebookSignUp()
        }
    }

    private fun startFacebookSignUp() {
        // ✅ Always show account chooser (forces re-login)
        LoginManager.getInstance().logOut()

        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )

        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookAccessToken(result.accessToken)
                }

                override fun onCancel() {
                    Toast.makeText(
                        this@SignupActivity,
                        "Facebook signup cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(error: FacebookException) {
                    val message = error.message ?: "Unknown error"

                    // ✅ Network error handling
                    if (message.contains("CONNECTION_FAILURE", true) ||
                        message.contains("NETWORK_ERROR", true) ||
                        message.contains("Failed to connect", true)
                    ) {
                        Toast.makeText(
                            this@SignupActivity,
                            "Network error. Please check your connection.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@SignupActivity,
                            "Facebook signup failed: $message",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)

        // ✅ Step 1: Get the email associated with the Facebook account
        val request = GraphRequest.newMeRequest(token) { obj, _ ->
            val email = obj?.getString("email")

            if (email == null) {
                Toast.makeText(
                    this@SignupActivity,
                    "Unable to retrieve Facebook email. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
                return@newMeRequest
            }

            // ✅ Step 2: Check if the email already exists in Firebase Auth
            mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener { methodTask ->
                    if (methodTask.isSuccessful) {
                        val methods = methodTask.result?.signInMethods ?: emptyList()

                        when {
                            // ⚠️ Already registered via Facebook
                            methods.contains("facebook.com") -> {
                                Toast.makeText(
                                    this@SignupActivity,
                                    "This Facebook account is already registered.",
                                    Toast.LENGTH_LONG
                                ).show()
                                LoginManager.getInstance().logOut()
                            }

                            // ⚠️ Registered via another provider (Google/Email)
                            methods.isNotEmpty() -> {
                                Toast.makeText(
                                    this@SignupActivity,
                                    "This Facebook email is already registered with another sign-in method.",
                                    Toast.LENGTH_LONG
                                ).show()
                                Log.w("FacebookAuth", "Email already exists with $methods")
                                LoginManager.getInstance().logOut()
                            }

                            // ✅ Not registered yet → proceed with Facebook sign-up
                            else -> {
                                mAuth.signInWithCredential(credential)
                                    .addOnCompleteListener(this@SignupActivity) { task ->
                                        if (task.isSuccessful) {
                                            val user = mAuth.currentUser
                                            Toast.makeText(
                                                this@SignupActivity,
                                                "Welcome new user: ${user?.displayName}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            startActivity(Intent(this@SignupActivity,
                                                AllSetActivity::class.java))
                                            finish()
                                        } else {
                                            Toast.makeText(
                                                this@SignupActivity,
                                                "Firebase Auth failed: ${task.exception?.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            Log.e("FacebookAuth", "Auth failed", task.exception)
                                        }
                                    }
                            }
                        }
                    } else {
                        Toast.makeText(
                            this@SignupActivity,
                            "Failed to verify Facebook email: ${methodTask.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("FacebookAuth", "Email check failed", methodTask.exception)
                    }
                }
        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name,email")
        request.parameters = parameters
        request.executeAsync()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                handleGoogleSignUp(account)
            } catch (e: ApiException) {
                when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED,
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS,
                    12501 -> {
                        // 👇 User cancelled the sign-in, ignore silently
                        Log.i("GoogleSignUp", "User cancelled Google Sign-up")
                    }
                    GoogleSignInStatusCodes.NETWORK_ERROR,
                    7 -> {
                        // 👇 No internet connection or network issue
                        Toast.makeText(this, "Network error. Please check your connection", Toast.LENGTH_SHORT).show()
                        Log.w("GoogleSignUp", "Network error during sign-up", e)
                    }
                    else -> {
                        // 👇 Other real errors
                        Toast.makeText(this, "Google Sign-Up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("GoogleSignIn", "Error during sign-in", e)
                    }
                }
            }
        } else {
            // ✅ Pass other results (like Facebook) to their handler
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleGoogleSignUp(account: GoogleSignInAccount) {
        val idToken = account.idToken ?: return
        val email = account.email ?: return

        // 🔹 Step 1: Check if the email already exists in Firebase Auth
        mAuth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { methodTask ->
                if (methodTask.isSuccessful) {
                    val methods = methodTask.result?.signInMethods ?: emptyList()

                    if (methods.isNotEmpty()) {
                        // ✅ Email already registered — don't create or sign in
                        Toast.makeText(
                            this,
                            "This email is already registered.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.w("GoogleAuth", "Email already exists: $email")

                        // Force Google to forget previous sign-in
                        googleSignInClient.signOut()
                        mAuth.signOut()
                        return@addOnCompleteListener
                    }

                    // 🔹 Step 2: Proceed with Google sign-up
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this) { authTask ->
                            if (authTask.isSuccessful) {
                                val user = mAuth.currentUser
                                Toast.makeText(
                                    this,
                                    "Welcome, new user: ${user?.email}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                startActivity(Intent(this, AllSetActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Firebase Auth failed: ${authTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("GoogleAuth", "Auth failed", authTask.exception)
                            }
                        }
                } else {
                    Toast.makeText(
                        this,
                        "Failed to check email: ${methodTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("GoogleAuth", "Email check failed", methodTask.exception)
                }
            }
    }



    private fun performEmailSignUp() {
        val email = binding.emailEditText.text.toString().trim().lowercase()
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
        mDialog?.setMessage("Creating account...")
        mDialog?.show()

        mAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                mDialog?.dismiss()
                if (task.isSuccessful) {
                    val user = mAuth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) {
                                //Toast.makeText(
                                 //   this,
                                //    "Verification email sent to $email. Please check your inbox.",
                                //    Toast.LENGTH_LONG
                                //).show()

                                val intent = Intent(this, EmailSentActivity::class.java)
                                intent.putExtra("USER_EMAIL", user.email)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Failed to send verification email: ${verifyTask.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    try {
                        throw task.exception ?: Exception("Unknown error")
                    } catch (e: FirebaseAuthUserCollisionException) {
                        binding.emailEditText.error = "This email is already registered"
                    } catch (e: FirebaseNetworkException) {
                        Toast.makeText(this, "Network error. Please check your connection", Toast.LENGTH_LONG).show()
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
