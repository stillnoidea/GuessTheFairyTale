package com.example.guessthefairytale.ui

import android.os.Bundle
import android.view.View
import com.example.guessthefairytale.R
import com.example.guessthefairytale.database.FirebaseDatabaseManager
import com.example.guessthefairytale.database.dto.DatabaseCallback
import com.example.guessthefairytale.database.dto.GameDTO
import com.example.guessthefairytale.database.dto.User
import com.example.guessthefairytale.gamelogic.Round
import kotlinx.android.synthetic.main.activity_game.*

class Multiplayer : GameActivity() {
    private var user = User("", "", "", 0)
    private var firstPlayerId = ""
    private val roundsNo: Int = 5
    private var gameData: GameDTO = GameDTO(0, 0, arrayListOf(), 0, 0, p1Ready = false, p2Ready = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer)
        game_activity_image_play_again.visibility = View.INVISIBLE
        game_activity_button_back.isClickable = false
        game_activity_button_back.visibility = View.INVISIBLE
        setSupportActionBar(findViewById(R.id.main_toolbar))

        user = intent.getSerializableExtra("user") as User
        searchGame()
    }

    private fun searchGame() {
        FirebaseDatabaseManager().findGameChallenge(user, object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                val challengeResult = value as String
                if (challengeResult != "") {
                    startGameAsSecondPlayer(challengeResult)
                } else {
                    FirebaseDatabaseManager().addGameChallenge(user, object : DatabaseCallback {
                        override fun onCallback(value: Any, sndValue: Any) {
                            if (value as Boolean) {
                                startGameAsFirstPlayer()
                            }
                        }
                    })
                }
            }
        })
    }

    private fun startGameAsSecondPlayer(playerOneId: String) {
        FirebaseDatabaseManager().listenForGameData(playerOneId, object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                gameData = value as GameDTO
                firstPlayerId = sndValue as String
            }
        })

    }

    private fun startGameAsFirstPlayer() {
        FirebaseDatabaseManager().uploadGameData(prepareGameData(), user.id)

    }

    private fun prepareGameData(): GameDTO {
        val rounds: ArrayList<Round> = arrayListOf()
        rounds.ensureCapacity(5)
        game.initializeLib(this)
        for (no in 0 until roundsNo) {
            rounds.add(Round())
            rounds[no].answersList = game.initializeRound()
            rounds[no].currentSong = game.getActualSong()!!
            for (answer in rounds[no].answersList) {
                if (answer == game.getActualSong()!!.getFairyTale()) {
                    rounds[no].correctAnswer = rounds[no].answersList.indexOf(answer)
                    break
                }
            }
        }
        gameData = GameDTO(roundsNo, 1, rounds, 0, 0, p1Ready = false, p2Ready = false)
        return gameData
    }


    fun goBack(v: View) {
        onBackPressed()
    }
}
