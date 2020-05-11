package com.example.guessthefairytale.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.guessthefairytale.R
import com.example.guessthefairytale.database.FirebaseDatabaseManager
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    val RC_SIGN_IN = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setSupportActionBar(findViewById(R.id.toolbar))

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("user", currentUser)
            startActivity(intent)
        }

        login_activity_signin_button.setOnClickListener {
            startSigningIn()
        }

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

                    val user = FirebaseAuth.getInstance().currentUser
                    if (response!!.isNewUser) {
                        Toast.makeText(this, user!!.displayName, Toast.LENGTH_LONG).show()
                        FirebaseDatabaseManager().createUser(user.uid, user.displayName!!, user.email!!)
                    }

                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("user", user)
                    startActivity(intent)
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
