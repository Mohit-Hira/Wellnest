package com.example.wellnest.activities


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wellnest.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore


class SplashScreen : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    lateinit var auth: FirebaseAuth
    private lateinit var name: String
    private lateinit var email: String
    lateinit var mGoogleSignInClient: GoogleSignInClient
    val RC_SIGN_IN: Int = 1
    lateinit var gso:GoogleSignInOptions

    private fun createRequest() {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                email=account.email.toString()
                name=account.displayName.toString()
                firebaseAuthWithGoogle(account)
            }
            catch (e: ApiException) {
                Toast.makeText(this, "$e Login Failed", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        var found=false
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("Users")
                        .get()
                        .addOnCompleteListener {
                            val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            if (it.isSuccessful) {
                                for (document in it.result!!) {
                                    if (document.data.getValue("email").toString() == account.email) {
                                        found = true
                                        name = "${document.data.getValue("name")}"
                                        val editor = sharedPreferences.edit()
                                        editor.putString("email", account.email.toString())
                                        editor.putString("name", name)
                                        editor.apply()
                                     }
                                }
                            }
                            if (found == true) {
                                val editor = sharedPreferences.edit()
                                editor.putBoolean("isLoggedIn", true)
                                editor.apply()


                             startTabActivity()

                            }
                            else {
                                val editor = sharedPreferences.edit()
                                editor.putString("email", account.email.toString())
                                editor.putString("name", account.displayName.toString())
                                editor.apply()
                                createAccount()

                            }
                        }
                } else {
                    Toast.makeText(this, "Login Failed: 2 ", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun createAccount()
    {

        val editor = sharedPreferences.edit()
        editor.putString("email", email)
        editor.putString("name", name)
        editor.putBoolean("isLoggedIn", true)
        editor.apply()

        val db = FirebaseFirestore.getInstance()
        val dataAdd = HashMap<String,Any>()

        dataAdd["name"] = name
        dataAdd["email"]=email
        dataAdd["totalTime"]="0"
        val collection=db.collection("Users")
        collection
            .document(email) // Set the document ID
            .set(dataAdd)
            .addOnSuccessListener {
            }
            .addOnFailureListener {exception ->
                Log.e("FirebaseError", "Error adding data: $exception")
                Toast.makeText(this," Data not added ",Toast.LENGTH_LONG).show()
            }
        startTabActivity()

    }
    fun startTabActivity() {

            val intent = Intent(applicationContext, PlayGames::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)

    }
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        val animFadein: Animation = AnimationUtils.loadAnimation(applicationContext, R.anim.fade_in)
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        val logo = findViewById<ImageView>(R.id.logo)
        val loginButton = findViewById<Button>(R.id.login_btn)
        auth = FirebaseAuth.getInstance()
        createRequest()
        loginButton.setOnClickListener {
            loginButton.startAnimation(animFadein)
            signOut()
            signIn()
        }
        loginButton.visibility = View.GONE
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        val slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        logoAnimation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                if (sharedPreferences.getBoolean("isLoggedIn", false)) {

                    val rootView: View = findViewById(R.id.rootView)
                    sharedPreferences.getString("email", "")
                        ?.let {
                            startTabActivity()
                        }
                }
                else {
                    loginButton.visibility = View.VISIBLE
                    loginButton.startAnimation(slideUpAnimation)
                }
            }
            override fun onAnimationRepeat(animation: Animation) {}
        })
        logo.startAnimation(logoAnimation)
    }
}