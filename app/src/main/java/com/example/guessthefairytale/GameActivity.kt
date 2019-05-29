package com.example.guessthefairytale

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
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

class GameActivity : AppCompatActivity(), View.OnClickListener, SensorEventListener, ShakeDetector.Listener {
    private val game: Game = Game()
    private lateinit var counter: CountDownTimer
    private var buttons: ArrayList<Button> = arrayListOf()
    private var player: MediaPlayer = MediaPlayer()
    private lateinit var sensorManager: SensorManager
    private var shakeDetector: ShakeDetector? = null
    private var proximity: Sensor? = null
    private val countDownInterval: Long = 1000
    private var pauseMoment: Long = 33
    private var actualTime: Long = 0
    private var resumeTime: Long = 0
    private var roundsLeft = 0
    private var isPaused: Boolean = false
    private var wasShaken: Boolean = false
    private var isRound = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        buttons.add(game_activity_button_answer1)
        buttons.add(game_activity_button_answer2)
        buttons.add(game_activity_button_answer3)
        buttons.add(game_activity_button_answer4)

        val roundsNumber = intent.getIntExtra(ROUNDS_NUMBER, 10)
        game.setRoundsNumber(roundsNumber)
        roundsLeft = roundsNumber

        for (button in buttons) {
            button.setOnClickListener(this)
        }
        game.initializeLib(this)
        game_activity_button_back.isClickable = false
        game_activity_button_back.isVisible = false

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        shakeDetector = ShakeDetector(this)

    }

    override fun onStart() {
        startGame()
        super.onStart()
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

    private fun displayRoundAnswer() {
        for (button in buttons) {
            if (button.isSelected) {
                button.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorAccentDark))
            }
            if (button.text == game.getActualSong()!!.getFairyTale()) {
                button.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
            }
        }
    }

    private fun checkAnswer() {
        displayRoundAnswer()
        customAnswerTextView(getString(R.string.game_avtivity_answer_bad), R.color.colorAccentDark)
        var point = 1.0
        if (wasShaken) {
            point = 0.5
        }

        for (button in buttons) {
            if (button.isSelected && button.text == game.getActualSong()!!.getFairyTale()) {

                game.addPoints(point)
                customAnswerTextView(getString(R.string.game_avtivity_text_good_answer), R.color.colorAccent)

            }
        }
    }

    private fun startGame() {
        countTime(game.getBreakTime().toLong())
        visibilityButtons(false)
    }

    private fun countTime(time: Long) {
        counter = object : CountDownTimer(time, countDownInterval) {

            override fun onTick(millisUntilFinished: Long) {

                if (isPaused) {
                    resumeTime = millisUntilFinished
                    pauseMoment = resumeTime / 1000
                    cancel()

                } else {
                    val timeLeft = millisUntilFinished / 1000
                    actualTime = timeLeft
                    game_activity_text_time_counter.text = timeLeft.toString()

                    if (isRound && timeLeft == 10.toLong()) {
                        changeCounterColor(R.color.colorAccentDark)
                    }
                    if (!isRound) {
                        changeCounterColor(R.color.colorAccentDarkest)
                    }
                }
            }

            override fun onFinish() {
                counter.cancel()
                visibilityButtons(true)
                runNextRoundOrEndGame()
            }
        }
        counter.start()
    }

    private fun runNextRoundOrEndGame() {
        if (roundsLeft == 0) {
            player.stop()
            endGame()
        } else {
            runRoundOrPause()
        }
    }

    private fun runRoundOrPause() {
        if (isRound) {
            isRound = false
            startBreak()
        } else {
            isRound = true
            roundsLeft--
            startRound()
        }
    }

    override fun onClick(v: View?) {
        for (button in buttons) {
            if (button.id == v!!.id) {
                button.isSelected = true
            }
        }
        counter.cancel()
        isRound = false
        startBreak()
    }


    private fun endGame() {
        shakeDetector!!.stop()
        visibilityButtons(false)
        changeCounterColor(R.color.colorAccent)
        game_activity_text_time_counter.text = getString(R.string.game_activity_text_score)
        game_activity_text_time_counter.textSize = 40F
        game_activity_text_answer_correctness.textSize = 130F
        customAnswerTextView(game.getScore().toString(), R.color.colorAccent)
        game_activity_button_back.isClickable = true
        game_activity_button_back.isVisible = true
    }

    private fun startBreak() {
        shakeDetector!!.stop()
        blockingButtons(true)
        player.stop()
        checkAnswer()
        countTime(game.getBreakTime().toLong())
    }

    private fun startRound() {
        pauseMoment = 33
        shakeDetector!!.start(sensorManager)
        wasShaken = false

        clearView()
        blockingButtons(false)
        val answers = game.initializeRound()
        val name = game.getActualSong()!!.getFilePath()

        player = MediaPlayer.create(applicationContext, resources.getIdentifier(name, "raw", packageName))

        showButtonsAnswers(answers)

        player.start()
        countTime(game.getRoundTime().toLong())
    }

    private fun pauseGame() {
        isPaused = true
        player.pause()
        blockingButtons(true)
    }

    private fun resumeGame() {
        if (isPaused) {
            countTime(resumeTime)
        }
        player.start()
        isPaused = false
        if (isRound) {
            blockingButtons(false)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val sensor = event.sensor

        if (sensor.type == Sensor.TYPE_PROXIMITY && isRound) {
            val distance = event.values[0]
            if (distance < 0.5) {
                if (actualTime > 3 && pauseMoment - 2 > actualTime) pauseGame()
            } else {
                resumeGame()
            }
        }
    }


    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Sensor accuracy shouldn't change
    }

    override fun hearShake() {
        if (!wasShaken) {
            wasShaken = true
            var iteration = 0
            while (iteration < 2) {
                val index = Random.nextInt(4)
                val button = buttons[index]
                if (button.text != game.getActualSong()!!.getFairyTale() && button.isVisible) {
                    button.text = ""
                    button.isVisible = false
                    iteration++
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        proximity?.also { proximity ->
            sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun blockingButtons(isBlocked: Boolean) {
        for (button in buttons) {
            button.isClickable = !isBlocked
        }
    }

    private fun showButtonsAnswers(answers: ArrayList<String>) {
        for (i in 0..3) {
            buttons[i].text = answers[i]
        }
    }

    private fun changeCounterColor(id: Int) {
        game_activity_text_time_counter.setTextColor(ContextCompat.getColor(applicationContext, id))
    }

    private fun customAnswerTextView(text: String, colorId: Int) {
        game_activity_text_answer_correctness.text = text
        game_activity_text_answer_correctness.setTextColor(ContextCompat.getColor(applicationContext, colorId))
    }

    private fun clearView() {
        for (button in buttons) {
            button.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorPrimaryDark))
            button.isSelected = false
        }
        game_activity_text_answer_correctness.text = ""
    }

    private fun visibilityButtons(isVisible: Boolean) {
        for (button in buttons) {
            button.isVisible = isVisible
        }
    }

}
