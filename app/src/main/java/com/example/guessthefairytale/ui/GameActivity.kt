package com.example.guessthefairytale.ui

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.guessthefairytale.R
import com.example.guessthefairytale.gamelogic.Game
import com.example.guessthefairytale.ui.MainActivity.Companion.ROUNDS_NUMBER
import kotlinx.android.synthetic.main.activity_game.*


abstract class GameActivity : AppCompatActivity(), View.OnClickListener {
    val game: Game = Game()
    lateinit var counter: CountDownTimer
    var buttons: ArrayList<Button> = arrayListOf()
    lateinit var player: MediaPlayer
    private val countDownInterval: Long = 1000
    var roundsLeft = 0
    var isRound = false
    var isCounterInitialized: Boolean = false
    var isPlayerInitialized: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("Multi", "${this.hashCode()} [GameAct] onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
    }

    fun releaseGame() {
        if (isCounterInitialized) {
            counter.cancel()
        }
        if (isPlayerInitialized) {
            player.stop()
            player.release()
            isPlayerInitialized = false
        }
    }

    fun initializeRoundsNumber() {
        val roundsNumber = intent.getIntExtra(ROUNDS_NUMBER, 10)
        game.setRoundsNumber(roundsNumber)
        roundsLeft = roundsNumber
    }

    fun addAnswerButtons() {
        buttons.add(game_activity_button_answer1)
        buttons.add(game_activity_button_answer2)
        buttons.add(game_activity_button_answer3)
        buttons.add(game_activity_button_answer4)
    }

    fun startGame() {
        countTime(game.getBreakTime().toLong())
        buttonsVisibility(View.INVISIBLE)
    }

    fun countTime(time: Long) {
        isCounterInitialized = true
        counter = object : CountDownTimer(time, countDownInterval) {

            override fun onTick(millisUntilFinished: Long) {
                updateCounter(millisUntilFinished / 1000)
            }

            override fun onFinish() {
                counter.cancel()
                runNextRoundOrEndGame()
            }
        }
        counter.start()
    }

    private fun updateCounter(timeLeft: Long) {
        game_activity_text_time_counter.text = timeLeft.toString()

        if (isRound && timeLeft == 10.toLong()) {
            changeCounterColor(R.color.colorBadAnswer)
        } else if (!isRound) {
            changeCounterColor(R.color.colorFontDark)
        }
    }

    fun changeCounterColor(id: Int) {
        game_activity_text_time_counter.setTextColor(ContextCompat.getColor(applicationContext, id))
    }

    fun buttonsVisibility(isVisible: Int) {
        for (button in buttons) {
            button.visibility = isVisible
        }
    }

    private fun runNextRoundOrEndGame() {
        Log.i("Multi", "${this.hashCode()} [GameAct] runNextRoundOrEndGame")
        if (roundsLeft == 0) {
            if (isPlayerInitialized) {
                player.stop()
                player.release()
                isPlayerInitialized = false
            }
            endGame()
        } else {
            runNextStage()
        }
    }

    open fun endGame() {
        buttonsVisibility(View.INVISIBLE)
        displayScore()
    }

    open fun displayScore() {
        changeCounterColor(R.color.colorGoodAnswer)
        game_activity_text_time_counter.text = getString(R.string.game_activity_text_score)
        game_activity_text_time_counter.textSize = 40F
        game_activity_text_round_result.textSize = 120F
        setRoundResultText(game.getScore().toString(), R.color.colorGoodAnswer)
    }

    fun setRoundResultText(text: String, colorId: Int) {
        game_activity_text_round_result.text = text
        game_activity_text_round_result.setTextColor(ContextCompat.getColor(applicationContext, colorId))
    }

    private fun runNextStage() {
        if (isRound) {
            isRound = false
            startBreak()
        } else {
            isRound = true
            roundsLeft--
            startRound()
        }
    }

    open fun startBreak() {
        sumUpRound()
        buttonsVisibility(VISIBLE)
        buttonsBlockade(true)
        if (isPlayerInitialized) {
            player.stop()
            player.release()
            isPlayerInitialized = false
        }
        countTime(game.getBreakTime().toLong())
    }

    fun buttonsBlockade(isBlocked: Boolean) {
        for (button in buttons) {
            button.isClickable = !isBlocked
        }
    }

    open fun sumUpRound() {
        when (isAnswerCorrect()) {
            true -> {
                game.addPoints(getPointsAmount())
                setRoundResultText(
                    getString(R.string.game_activity_answer_good),
                    R.color.colorGoodAnswer
                )
                getSelectedButton()!!.background = resources.getDrawable(R.drawable.answers_buttons_good_answer, null)
            }
            false -> {
                setRoundResultText(
                    getString(R.string.game_activity_answer_bad),
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

    fun isAnswerCorrect(): Boolean? {
        val selectedButton = getSelectedButton()
        return if (selectedButton == null) {
            null
        } else {
            selectedButton.text == game.getActualSong()!!.getFairyTale()
        }
    }

    open fun getPointsAmount(): Double {
        return 1.0
    }

    fun getSelectedButton(): Button? {
        return buttons.find { x -> x.isSelected }
    }

    open fun startRound() {
        prepareButtonAndTextsAfterBreak()
        buttonsBlockade(false)
        buttonsVisibility(VISIBLE)

        player.start()
        isPlayerInitialized = true
        countTime(game.getRoundTime().toLong())
    }

    override fun onDestroy() {
        Log.i("Multi", "${this.hashCode()} [GameAct] onDestroy")
        isPlayerInitialized
        super.onDestroy()
    }

    fun prepareButtonAndTextsAfterBreak() {
        for (button in buttons) {
            button.background = resources.getDrawable(R.drawable.answers_buttons, null)
            button.isSelected = false
        }
        game_activity_text_round_result.text = ""
    }

    fun initializeRound() {
        val answers = game.initializeRound()
        val name = game.getActualSong()!!.getFilePath()
        isPlayerInitialized = true
        player = MediaPlayer.create(applicationContext, resources.getIdentifier(name, "raw", packageName))
        showAvailableAnswers(answers)
    }

    fun showAvailableAnswers(answers: ArrayList<String>) {
        for (i in 0..3) {
            buttons[i].text = answers[i]
        }
    }

    override fun onStop() {
        Log.i("Multi", "${this.hashCode()} [GameAct] onStop")
        if (isPlayerInitialized) {
            player.stop()
            player.release()
            isPlayerInitialized = false
        }
        if (isCounterInitialized) {
            counter.cancel()
        }
        super.onStop()
    }

    override fun onRestart() {
        Log.i("Multi", "${this.hashCode()} [GameAct] onRestart")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        super.onRestart()
    }

    override fun onClick(v: View?) {
        buttons.find { x -> x.id == v!!.id }!!.isSelected = true
        counter.cancel()
        isRound = false
        startBreak()
    }

}