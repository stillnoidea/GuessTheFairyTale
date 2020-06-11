package com.example.guessthefairytale.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.guessthefairytale.R
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)

        game_activity_button_back.setOnClickListener {
            onBackPressed()
        }
    }
}
