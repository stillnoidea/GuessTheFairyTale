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
            // Show the single choice list items on an alert dialog
            showDialog()
        }
        main_activity_button_tutorial.setOnClickListener {
            // Show the single choice list items on an alert dialog
            goToTutorial()
        }
    }

    private fun goToTutorial() {
        val intent = Intent(this, TutorialActivity::class.java)
        startActivity(intent)
    }


    // Method to show an alert dialog with single choice list items
    private fun showDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog

        // Initialize an array of colors
        val array = arrayOf("5", "10", "15")

        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this)

        // Set a title for alert dialog
        builder.setTitle("Wybierz liczbę rund")

        builder.setSingleChoiceItems(array, -1) { _, which ->


            dialog.dismiss()
            play(array[which].toInt())
        }


        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }


    fun play(number: Int) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra(ROUNDS_NUMBER, number)
        startActivity(intent)
    }

    companion object {
        const val ROUNDS_NUMBER = "ROUNDS_NUMBER"
    }
}
