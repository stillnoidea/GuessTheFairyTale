package com.example.guessthefairytale.ui

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import com.example.guessthefairytale.R
import com.squareup.seismic.ShakeDetector
import kotlinx.android.synthetic.main.activity_game.*
import kotlin.random.Random

class Singleplayer : GameActivity(),ShakeDetector.Listener {
    private lateinit var sensorManager: SensorManager
    private var shakeDetector: ShakeDetector? = null
    private var wasShaken = false

    override fun onCreate(savedInstanceState: Bundle?) {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector(this)
        super.onCreate(savedInstanceState)
        setSupportActionBar(findViewById(R.id.game_toolbar))
        super.setContentView(R.layout.activity_singleplayer)
        game_activity_image_play_again.visibility = View.INVISIBLE
        game_activity_button_back.isClickable = false
        game_activity_button_back.visibility = View.INVISIBLE

        super.addAnswerButtons()
        for (button in buttons) {
            button.setOnClickListener(this)
        }
        super.initializeRoundsNumber()
        super.game.initializeLib(this)
    }


    override fun endGame() {
        shakeDetector!!.stop()
        super.buttonsVisibility(View.INVISIBLE)
        game_activity_button_back.isClickable = true
        game_activity_button_back.visibility = View.VISIBLE
        game_activity_image_play_again.visibility = View.VISIBLE
        super.displayScore()
    }

    override fun startBreak() {
        shakeDetector!!.stop()
        super.buttonsVisibility(View.VISIBLE)
        super.buttonsBlockade(true)
        super.player.stop()
        super.sumUpRound()
        super.countTime(game.getBreakTime().toLong())
    }

    override fun startRound() {
        super.prepareButtonAndTextsAfterBreak()
        shakeDetector!!.start(sensorManager)
        wasShaken = false
        super.buttonsBlockade(false)
        super.buttonsVisibility(View.VISIBLE)

        super.initializeRound()
        super.player.start()
        super.countTime(game.getRoundTime().toLong())
    }

    override fun getPointsAmount(): Double {
        var point = 1.0
        if (wasShaken) {
            point = 0.5
        }
        return point
    }

    fun goBack(v: View) {
        onBackPressed()
    }

    override fun hearShake() {
        if (!wasShaken) {
            wasShaken = true
            var iteration = 0

            while (iteration < 2) {
                val index = Random.nextInt(4)
                val button = super.buttons[index]
                if (button.text != game.getActualSong()!!.getFairyTale() && button.isClickable) {
                    button.isClickable = false
                    button.background = resources.getDrawable(R.drawable.answers_buttons_diabled,null)
                    iteration++
                }
            }
        }
    }
}
