package com.example.guessthefairytale.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.guessthefairytale.R
import com.example.guessthefairytale.database.dto.User
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private var user = User("","","",0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_activity_button_single.setOnClickListener {
            showRoundNumbersDialog()
        }
        main_activity_button_multi.setOnClickListener {
            startMultiGame()
        }
        main_activity_button_tutorial.setOnClickListener {
            goToTutorial()
        }
        main_activity_button_logout.setOnClickListener {
            logout()
        }

        setSupportActionBar(findViewById(R.id.main_toolbar))
        user = intent.getSerializableExtra("user") as User

    }

    private fun startMultiGame() {
        val intent = Intent(this, Multiplayer::class.java)
        intent.putExtra("user", user)
        startActivity(intent)
    }

    private fun goToTutorial() {
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
    }

    private fun showRoundNumbersDialog() {
        lateinit var dialog: AlertDialog
        val array = arrayOf("5", "10", "15")
        val builder = AlertDialog.Builder(this)

        builder.setTitle("Wybierz liczbę rund")

        builder.setSingleChoiceItems(array, -1) { _, which ->
            dialog.dismiss()
            startSingleGame(array[which].toInt())
        }

        dialog = builder.create()

        dialog.show()
    }

    private fun startSingleGame(roundsNumber: Int) {
        val intent = Intent(this, Singleplayer::class.java)
        intent.putExtra(ROUNDS_NUMBER, roundsNumber)
        startActivity(intent)
    }

    private fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
    }

    companion object {
        const val ROUNDS_NUMBER = "ROUNDS_NUMBER"
    }
}
