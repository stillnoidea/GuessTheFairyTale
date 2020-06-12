package com.example.guessthefairytale.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import androidx.activity.OnBackPressedCallback
import com.example.guessthefairytale.R
import com.example.guessthefairytale.database.FirebaseDatabaseManager
import com.example.guessthefairytale.database.dto.DatabaseCallback
import com.example.guessthefairytale.database.dto.GameDTO
import com.example.guessthefairytale.database.dto.User
import com.example.guessthefairytale.gamelogic.Round
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_game.*

class Multiplayer(enabled: Boolean) : GameActivity() {
    private var user = User("", "", "", 0)
    private var firstPlayerId = ""
    private var playerNo = ""
    private var sndPlayerNo = ""
    private val roundsNo: Int = 5
    private var gameData: GameDTO = GameDTO(0, 0, arrayListOf(), 0, 0, p1Ready = false, p2Ready = false)
    private var p1Score: Int = 0
    private var p2Score: Int = 0
    private var gameListener: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiplayer)
        game_activity_image_play_again.visibility = View.INVISIBLE
        game_activity_button_back.isClickable = false
        game_activity_button_back.visibility = View.INVISIBLE
        roundsLeft = roundsNo
        super.addAnswerButtons()
        for (button in buttons) {
            button.setOnClickListener(this)
        }
        super.buttonsVisibility(View.INVISIBLE)

        setSupportActionBar(findViewById(R.id.main_toolbar))

        user = intent.getSerializableExtra("user") as User
        searchGame()

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                onStop()
                onDestroy()
            }
        })
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
        firstPlayerId = user.id
        playerNo = "p1"
        sndPlayerNo = "p2"
        FirebaseDatabaseManager().uploadGameData(prepareGameData(), user.id)
        displayReadyButton()
    }

    private fun prepareGameData(): GameDTO {
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

    override fun onPause() {
        super.onPause()
        onStop()
    }

    override fun onStop() {
        super.onStop()
        if (isPlayerInitialized) {
            player.stop()
            player.release()
            isPlayerInitialized = false
        }
        if (isCounterInitialized) {
            counter.cancel()
        }
        if (gameListener != null) {
            FirebaseDatabaseManager().removeGameListener(gameListener as ValueEventListener, firstPlayerId)
        }
        FirebaseDatabaseManager().deletePlayerData(firstPlayerId)
        finish()
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
        FirebaseDatabaseManager().listenForPlayersStandby(firstPlayerId, object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                game_activity_text_round_result.text = getString(R.string.multiplayer_lets_start)
                if (value as Boolean) {
                    gameListener =
                        FirebaseDatabaseManager().listenForRoundChange(firstPlayerId, object : DatabaseCallback {
                            override fun onCallback(value: Any, sndValue: Any) {
                                if (p1Score != value || p2Score != sndValue || (p1Score == 0 && p2Score == 0)) {
                                    p1Score = value as Int
                                    p2Score = sndValue as Int
                                    counter.cancel()
                                    isRound = false
                                    buttonsVisibility(View.VISIBLE)
                                    buttonsBlockade(true)
                                    if (isPlayerInitialized) {
                                        player.stop()
                                        player.release()
                                        isPlayerInitialized = false
                                    }
                                    setRoundResultText(
                                        getString(R.string.multiplayer_opponent_was_faster),
                                        R.color.colorBadAnswer
                                    )
                                    countTime(game.getBreakTime().toLong())
                                }
                            }
                        })
                    startGame()
                }
            }
        })
        FirebaseDatabaseManager().notifyPlayerStandby(firstPlayerId, playerNo + "Ready", object : DatabaseCallback {
            override fun onCallback(value: Any, sndValue: Any) {
                endGameAsSndPlayerLeft()
            }
        })
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
                    getString(R.string.game_activity_answer_good) + " +1 dla ciebie",
                    R.color.colorGoodAnswer
                )
                getSelectedButton()!!.background = resources.getDrawable(R.drawable.answers_buttons_good_answer, null)
            }
            false -> {
                updatePoints(sndPlayerNo)
                FirebaseDatabaseManager().updateGamePoints(
                    firstPlayerId,
                    p1Score,
                    p2Score,
                    roundsLeft,
                    object : DatabaseCallback {
                        override fun onCallback(value: Any, sndValue: Any) {
                            endGameAsSndPlayerLeft()
                        }
                    })

                setRoundResultText(
                    getString(R.string.game_activity_answer_bad) + " +1 dla przeciwnika",
                    R.color.colorBadAnswer
                )
                getSelectedButton()!!.background = resources.getDrawable(R.drawable.answers_buttons_bad_answer, null)
                buttons.find { x -> x.text == game.getActualSong()!!.getFairyTale() }!!
                    .background = resources.getDrawable(R.drawable.answers_buttons_good_answer, null)
            }
            else -> {
                setRoundResultText(
                    getString(R.string.game_activity_answer_bad),
                    R.color.colorBadAnswer
                )
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
        if (isCounterInitialized) {
            counter.cancel()
        }
        if (isPlayerInitialized) {
            player.stop()
            player.release()
            isPlayerInitialized = false
        }
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
        game.setActualSong(gameData.rounds[roundNo].currentSong)
        val name = gameData.rounds[roundNo].currentSong.getFilePath()
        isPlayerInitialized = true
        player = MediaPlayer.create(applicationContext, resources.getIdentifier(name, "raw", packageName))
        super.showAvailableAnswers(gameData.rounds[roundNo].answersList)
        super.startRound()
    }

    override fun endGame() {
        super.endGame()
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
        onBackPressed()
    }
}
