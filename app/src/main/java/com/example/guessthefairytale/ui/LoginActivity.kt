package com.example.guessthefairytale.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guessthefairytale.R
import com.example.guessthefairytale.database.FirebaseDatabaseManager
import com.example.guessthefairytale.database.dto.User
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    val RC_SIGN_IN = 200

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setSupportActionBar(findViewById(R.id.toolbar))

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            val user = User(currentUser.uid, currentUser.displayName!!, currentUser.email!!,0)
            intent.putExtra("user", user)
            startActivity(intent)
        }

        login_activity_signin_button.setOnClickListener {
            startSigningIn()
        }

    }

    public override fun onStart() {
        super.onStart()
    }

    private fun startSigningIn() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            when {
                resultCode == Activity.RESULT_OK -> {

                    auth.currentUser.let{
                        val currentUser = User(it!!.uid, it.displayName!!, it.email!!,0)
                        if (response!!.isNewUser) {
                            FirebaseDatabaseManager().createUser(it.uid, it.displayName!!, it.email!!)
                        }
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("user", currentUser)
                        startActivity(intent)
                        finish()
                    }
                }
                response == null -> {
                    Toast.makeText(this, "Signing in cancelled", Toast.LENGTH_LONG).show()
                }
                else -> {
                    Log.e("logging error", response.error!!.errorCode.toString())
                    Toast.makeText(this, "Something went wrong please try again", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

}
