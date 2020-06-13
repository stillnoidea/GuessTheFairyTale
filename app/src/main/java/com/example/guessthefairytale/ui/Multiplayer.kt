package com.example.guessthefairytale.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import com.example.guessthefairytale.R
import com.example.guessthefairytale.database.FirebaseDatabaseManager
import com.example.guessthefairytale.database.dto.DatabaseCallback
import com.example.guessthefairytale.database.dto.GameDTO
import com.example.guessthefairytale.database.dto.User
import com.example.guessthefairytale.gamelogic.Round
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_game.*

class Multiplayer : GameActivity() {
    private var user = User("", "", "", 0)
    private var firstPlayerId = ""
    private var playerNo = ""
    private var sndPlayerNo = ""
    private val roundsNo: Int = 5
    private var gameData: GameDTO = GameDTO(0, 0, arrayListOf(), 0, 0, p1Ready = false, p2Ready = false)
    private var p1Score: Int = 0
    private var p2Score: Int = 0
    private var gameListener: Any? = null
    private var standbyListener: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer)
        setSupportActionBar(findViewById(R.id.main_toolbar))

        user = intent.getSerializableExtra("user") as User
        searchGame()

        game_activity_image_play_again.visibility = View.INVISIBLE
        game_activity_button_back.isClickable = false
        game_activity_button_back.visibility = View.INVISIBLE
        roundsLeft = roundsNo
        super.addAnswerButtons()
        for (button in buttons) {
            button.setOnClickListener(this)
        }
        super.buttonsVisibility(View.INVISIBLE)

        Log.i("Multi", "${this.hashCode()} onCreate")

    }

    private fun searchGame() {
        game_activity_text_round_result.text = getString(R.string.multiplayer_waiting_for_opponent)
        FirebaseDatabaseManager().findGameChallenge(user, object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                val challengeResult = value as String
                if (challengeResult != "") {
                    startGameAsSecondPlayer(challengeResult)
                } else {
                    FirebaseDatabaseManager().addGameChallenge(user, object : DatabaseCallback {
                        override fun onCallback(value: Any, sndValue: Any) {
                            if (value as Boolean) {
                                firstPlayerId = user.id
                                startGameAsFirstPlayer()
                            }
                        }
                    })
                }
            }
        })
    }

    private fun startGameAsSecondPlayer(playerOneId: String) {
        playerNo = "p2"
        sndPlayerNo = "p1"
        FirebaseDatabaseManager().listenForGameData(playerOneId, object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                gameData = value as GameDTO
                firstPlayerId = sndValue as String
            }
        })
        displayReadyButton()
    }

    private fun startGameAsFirstPlayer() {
        playerNo = "p1"
        sndPlayerNo = "p2"
        FirebaseDatabaseManager().uploadGameData(prepareGameData(), user.id)
        displayReadyButton()
    }

    private fun prepareGameData(): GameDTO {
        Log.i("Multi", "${this.hashCode()} prepareGameData")
        gameData = GameDTO(0, 0, arrayListOf(), 0, 0, p1Ready = false, p2Ready = false)
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
        gameData = GameDTO(roundsNo, roundsNo, rounds, 0, 0, p1Ready = false, p2Ready = false)
        return gameData
    }

    private fun displayReadyButton() {
        game_activity_text_round_result.text = getString(R.string.multiplayer_lets_start)
        buttons[2].visibility = VISIBLE
        buttons[2].text = getString(R.string.multiplayer_is_ready)
        buttons[2].background = resources.getDrawable(R.drawable.answers_buttons_diabled)
    }

    override fun onStop() {
        super.onStop()
        Log.i("Multi", "${this.hashCode()} onStop")
        releaseGame()
        if (gameListener != null) {
            FirebaseDatabaseManager().removeGameListener(gameListener as ValueEventListener, firstPlayerId)
        }
        if (standbyListener != null) {
            FirebaseDatabaseManager().removeGameListener(standbyListener as ValueEventListener, firstPlayerId)
        }
        FirebaseDatabaseManager().deletePlayerData(firstPlayerId)
    }

    override fun onDestroy() {
        gameData
        Log.i("Multi", "${this.hashCode()} onDestroy")
        super.onDestroy()
    }

    override fun onClick(v: View?) {
        if (v!!.id == R.id.game_activity_button_answer3 && game_activity_button_answer3.text == getString(R.string.multiplayer_is_ready)) {
            playerIsReady()
        } else {
            super.onClick(v)
        }
    }

    private fun playerIsReady() {
        game_activity_text_round_result.text =
            "${getString(R.string.multiplayer_lets_start)} \n ${getString(R.string.multiplayer_waiting_for_opponent)}"
        game_activity_button_answer3.isEnabled = false
        standbyListener = FirebaseDatabaseManager().listenForPlayersStandby(firstPlayerId, object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                Log.i("Multi", "${this.hashCode()} onDestroy")
                game_activity_text_round_result.text = getString(R.string.multiplayer_lets_start)
                if (value as Boolean) {
                    gameListener =
                        FirebaseDatabaseManager().listenForRoundChange(firstPlayerId, object : DatabaseCallback {
                            override fun onCallback(value: Any, sndValue: Any) {
                                onOpponentAnswerSelected(value as Int, sndValue as Int)
                            }
                        })
                    game_activity_button_answer3.isEnabled = true
                    startGame()
                }
            }
        })
        FirebaseDatabaseManager().notifyPlayerStandby(firstPlayerId, playerNo + "Ready", object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                if (!(value as Boolean)) {
                    endGameAsSndPlayerLeft()
                }
            }
        })
    }

    private fun onOpponentAnswerSelected(value: Int, sndValue: Int){
        if (p1Score != value || p2Score != sndValue || (p1Score == 0 && p2Score == 0)) {
            p1Score = value
            p2Score = sndValue
            isRound = false
            buttonsVisibility(View.VISIBLE)
            buttonsBlockade(true)
            releaseGame()
            setRoundResultText(getString(R.string.multiplayer_opponent_was_faster),R.color.colorBadAnswer)
            countTime(game.getBreakTime().toLong())
        }
    }

    override fun sumUpRound() {
        when (super.isAnswerCorrect()) {
            true -> {
                updatePoints(playerNo)
                FirebaseDatabaseManager().updateGamePoints(firstPlayerId, p1Score, p2Score, roundsLeft,
                    object : DatabaseCallback {
                        override fun onCallback(value: Any, sndValue: Any) {
                            endGameAsSndPlayerLeft()
                        }
                    })

                super.setRoundResultText(
                    getString(R.string.game_activity_answer_good) + " +1 dla ciebie", R.color.colorGoodAnswer
                )
                getSelectedButton()!!.background = resources.getDrawable(R.drawable.answers_buttons_good_answer, null)
            }
            false -> {
                updatePoints(sndPlayerNo)
                FirebaseDatabaseManager().updateGamePoints(firstPlayerId, p1Score, p2Score, roundsLeft,
                    object : DatabaseCallback {
                        override fun onCallback(value: Any, sndValue: Any) {
                            endGameAsSndPlayerLeft()
                        }
                    })

                setRoundResultText(
                    getString(R.string.game_activity_answer_bad) + " +1 dla przeciwnika", R.color.colorBadAnswer
                )
                getSelectedButton()!!.background = resources.getDrawable(R.drawable.answers_buttons_bad_answer, null)
                buttons.find { x -> x.text == game.getActualSong()!!.getFairyTale() }!!
                    .background = resources.getDrawable(R.drawable.answers_buttons_good_answer, null)
            }
            else -> {
                setRoundResultText(getString(R.string.multiplayer_nobody_answered), R.color.colorBadAnswer)
                buttons.find { x -> x.text == game.getActualSong()!!.getFairyTale() }!!
                    .background = resources.getDrawable(R.drawable.answers_buttons_good_answer, null)
            }
        }

    }

    private fun updatePoints(playerNoToAddPoints: String) {
        if (playerNoToAddPoints == "p1") {
            p1Score++
        } else {
            p2Score++
        }
    }

    private fun endGameAsSndPlayerLeft() {
        Log.i("Multi", "${this.hashCode()} endGameAsSndPlayerLeft")
        this.releaseGame()
        buttonsVisibility(View.INVISIBLE)
        game_activity_button_back.isClickable = true
        game_activity_button_back.visibility = VISIBLE
        game_activity_image_play_again.visibility = VISIBLE
        game_activity_text_time_counter.text = ""
        game_activity_text_round_result.textSize = 30F
        setRoundResultText(getString(R.string.multiplayer_second_player_left), R.color.colorBadAnswer)

    }

    override fun startRound() {
        val roundNo = roundsNo - roundsLeft - 1
        Log.i("Multi", "${this.hashCode()} startRound $roundNo")
        game.setActualSong(gameData.rounds[roundNo].currentSong)
        val name = gameData.rounds[roundNo].currentSong.getFilePath()
        isPlayerInitialized = true
        player = MediaPlayer.create(applicationContext, resources.getIdentifier(name, "raw", packageName))
        super.showAvailableAnswers(gameData.rounds[roundNo].answersList)
        super.startRound()
    }

    override fun endGame() {
        super.endGame()
        Log.i("Multi", "${this.hashCode()} endGame")
        game_activity_button_back.isClickable = true
        game_activity_button_back.visibility = VISIBLE
        game_activity_image_play_again.visibility = VISIBLE
    }

    override fun displayScore() {
        game_activity_text_time_counter.textSize = 40F
        game_activity_text_round_result.textSize = 40F
        if (playerNo == "p1") {
            when {
                p1Score > p2Score -> {
                    setResults(R.color.colorGoodAnswer, getString(R.string.result_you_won), p1Score, p2Score)
                }
                p2Score > p1Score -> {
                    setResults(R.color.colorBadAnswer, getString(R.string.result_you_lost), p1Score, p2Score)
                }
                else -> {
                    setResults(R.color.colorGoodAnswer, getString(R.string.result_draw), p1Score, p2Score)
                }
            }
        } else {
            when {
                p1Score > p2Score -> {
                    setResults(R.color.colorBadAnswer, getString(R.string.result_you_lost), p2Score, p1Score)
                }
                p2Score > p1Score -> {
                    setResults(R.color.colorGoodAnswer, getString(R.string.result_you_won), p2Score, p1Score)
                }
                else -> {
                    setResults(R.color.colorGoodAnswer, getString(R.string.result_draw), p2Score, p1Score)
                }
            }
        }
    }

    private fun setResults(color: Int, resultText: String, firstScore: Int, secondScore: Int) {
        super.changeCounterColor(color)
        game_activity_text_time_counter.text = resultText
        setRoundResultText(getString(R.string.results, firstScore, secondScore), color)
    }

    fun goBack(v: View) {
        super.onBackPressed()
        this.finish()
    }
}
