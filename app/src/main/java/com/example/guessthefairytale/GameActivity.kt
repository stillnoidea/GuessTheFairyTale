package com.example.guessthefairytale

import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.guessthefairytale.MainActivity.Companion.ROUNDS_NUMBER
import com.example.guessthefairytale.logic.Game
import com.squareup.seismic.ShakeDetector
import kotlinx.android.synthetic.main.activity_game.*
import kotlin.random.Random

class GameActivity : AppCompatActivity(), View.OnClickListener, ShakeDetector.Listener {
    private val game: Game = Game()
    private lateinit var counter: CountDownTimer
    private var buttons: ArrayList<Button> = arrayListOf()
    private var player: MediaPlayer = MediaPlayer()
    private lateinit var sensorManager: SensorManager
    private var shakeDetector: ShakeDetector? = null
    private val countDownInterval: Long = 1000
    private var roundsLeft = 0
    private var wasShaken = false
    private var isRound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(this)

        game_activity_button_back.isClickable = false
        game_activity_button_back.isVisible = false

        addAnswerButtons()
        for (button in buttons) {
            button.setOnClickListener(this)

        }
        initializeRoundsNumber()
        game.initializeLib(this)
    }

    private fun initializeRoundsNumber() {
        val roundsNumber = intent.getIntExtra(ROUNDS_NUMBER, 10)
        game.setRoundsNumber(roundsNumber)
        roundsLeft = roundsNumber
    }

    private fun addAnswerButtons() {
        buttons.add(game_activity_button_answer1)
        buttons.add(game_activity_button_answer2)
        buttons.add(game_activity_button_answer3)
        buttons.add(game_activity_button_answer4)
    }

    override fun onStart() {
        startGame()
        super.onStart()
    }

    private fun startGame() {
        countTime(game.getBreakTime().toLong())
        buttonsVisibility(false)
    }

    private fun countTime(time: Long) {
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
            changeCounterColor(R.color.colorAccentDark)
        } else if (!isRound) {
            changeCounterColor(R.color.colorAccentDarkest)
        }
    }

    private fun changeCounterColor(id: Int) {
        game_activity_text_time_counter.setTextColor(ContextCompat.getColor(applicationContext, id))
    }

    private fun buttonsVisibility(isVisible: Boolean) {
        for (button in buttons) {
            button.isVisible = isVisible
        }
    }

    private fun runNextRoundOrEndGame() {
        if (roundsLeft == 0) {
            player.stop()
            endGame()
        } else {
            runNextStage()
        }
    }

    private fun endGame() {
        shakeDetector!!.stop()
        buttonsVisibility(false)
        game_activity_button_back.isClickable = true
        game_activity_button_back.isVisible = true
        displayScore()
    }

    private fun displayScore() {
        changeCounterColor(R.color.colorAccent)
        game_activity_text_time_counter.text = getString(R.string.game_activity_text_score)
        game_activity_text_time_counter.textSize = 40F
        game_activity_text_round_result.textSize = 130F
        setRoundResultText(game.getScore().toString(), R.color.colorAccent)
    }

    private fun setRoundResultText(text: String, colorId: Int) {
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

    private fun startBreak() {
        shakeDetector!!.stop()
        buttonsVisibility(true)
        buttonsBlockade(true)
        player.stop()
        sumUpRound()
        countTime(game.getBreakTime().toLong())
    }

    private fun buttonsBlockade(isBlocked: Boolean) {
        for (button in buttons) {
            button.isClickable = !isBlocked
        }
    }

    private fun sumUpRound() {
        if (isAnswerCorrect()) {
            game.addPoints(getPointsAmount())
            setRoundResultText(getString(R.string.game_avtivity_answer_good), R.color.colorAccent)
            getSelectedButton().setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
        } else {
            setRoundResultText(getString(R.string.game_avtivity_answer_bad), R.color.colorAccentDark)
            getSelectedButton().setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorAccentDark))
            buttons.find { x -> x.text == game.getActualSong()!!.getFairyTale() }!!
                .setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
        }
    }

    private fun isAnswerCorrect(): Boolean {
        return getSelectedButton().text == game.getActualSong()!!.getFairyTale()
    }

    private fun getPointsAmount(): Double {
        var point = 1.0
        if (wasShaken) {
            point = 0.5
        }
        return point
    }

    private fun getSelectedButton(): Button {
        return buttons.find { x -> x.isSelected }!!
    }

    private fun startRound() {
        prepareButtonAndTextsAfterBreak()
        shakeDetector!!.start(sensorManager)
        wasShaken = false
        buttonsBlockade(false)
        buttonsVisibility(true)

        initializeRound()
        player.start()
        countTime(game.getRoundTime().toLong())
    }

    private fun prepareButtonAndTextsAfterBreak() {
        for (button in buttons) {
            button.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorPrimaryDark))
            button.isSelected = false
        }
        game_activity_text_round_result.text = ""
    }

    private fun initializeRound() {
        val answers = game.initializeRound()
        val name = game.getActualSong()!!.getFilePath()
        player = MediaPlayer.create(applicationContext, resources.getIdentifier(name, "raw", packageName))
        showAvailableAnswers(answers)
    }

    private fun showAvailableAnswers(answers: ArrayList<String>) {
        for (i in 0..3) {
            buttons[i].text = answers[i]
        }
    }

    override fun onStop() {
        player.stop()
        counter.cancel()
        super.onStop()
    }

    override fun onRestart() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        super.onRestart()
    }

    fun goBack(v: View) {
        onBackPressed()
    }

    override fun onClick(v: View?) {
        buttons.find { x -> x.id == v!!.id }!!.isSelected = true
        counter.cancel()
        isRound = false
        startBreak()
    }

    override fun hearShake() {
        if (!wasShaken) {
            wasShaken = true
            var iteration = 0

            while (iteration < 2) {
                val index = Random.nextInt(4)
                val button = buttons[index]
                if (button.text != game.getActualSong()!!.getFairyTale() && button.isClickable) {
                    button.isClickable = false
                    button.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorButtonInactive))
                    iteration++
                }
            }
        }
    }
}