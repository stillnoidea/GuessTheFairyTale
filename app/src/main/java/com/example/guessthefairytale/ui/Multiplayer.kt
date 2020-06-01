package com.example.guessthefairytale.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.guessthefairytale.R
import com.example.guessthefairytale.database.FirebaseDatabaseManager
import com.example.guessthefairytale.database.dto.DatabaseCallback
import com.example.guessthefairytale.database.dto.User

class Multiplayer : AppCompatActivity() {
    private var user = User("", "", "", 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer)

        setSupportActionBar(findViewById(R.id.main_toolbar))
        user = intent.getSerializableExtra("user") as User
        searchGame()
    }

    private fun searchGame() {
        var isChallengeFound: Boolean
        FirebaseDatabaseManager().findGameChallenge(user, object : DatabaseCallback {
            override fun onCallback(value: Any) {
                isChallengeFound = value as Boolean
                if (isChallengeFound) {
                    startGame()
                } else {
                    FirebaseDatabaseManager().addGameChallenge(user, object : DatabaseCallback {
                        override fun onCallback(value: Any) {
                            startGame()
                        }
                    })
                }
            }
        })
    }

    private fun startGame() {
        TODO("implement multiplayer game logic")
    }
}
