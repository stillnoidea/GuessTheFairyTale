package com.example.guessthefairytale

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        main_activity_button_play.setOnClickListener {
            showRoundNumbersDialog()
        }
        main_activity_button_tutorial.setOnClickListener {
            goToTutorial()
        }
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
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(ROUNDS_NUMBER, roundsNumber)
        startActivity(intent)
    }

    companion object {
        const val ROUNDS_NUMBER = "ROUNDS_NUMBER"
    }
}
