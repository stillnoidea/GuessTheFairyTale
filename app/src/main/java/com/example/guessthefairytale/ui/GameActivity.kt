package com.example.guessthefairytale.ui

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
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
    private lateinit var counter: CountDownTimer
    var buttons: ArrayList<Button> = arrayListOf()
    var player: MediaPlayer = MediaPlayer()
    private val countDownInterval: Long = 1000
    private var roundsLeft = 0
    private var isRound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

//        addAnswerButtons()
//        for (button in buttons) {
//            button.setOnClickListener(this)
//        }
//        initializeRoundsNumber()
//        game.initializeLib(this)
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

    override fun onStart() {
        startGame()
        super.onStart()
    }

    private fun startGame() {
        countTime(game.getBreakTime().toLong())
        buttonsVisibility(View.INVISIBLE)
    }

    fun countTime(time: Long) {
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

    private fun changeCounterColor(id: Int) {
        game_activity_text_time_counter.setTextColor(ContextCompat.getColor(applicationContext, id))
    }

    fun buttonsVisibility(isVisible: Int) {
        for (button in buttons) {
            button.visibility = isVisible
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

    open fun endGame() {
        buttonsVisibility(View.INVISIBLE)
//        game_activity_button_back.isClickable = true
//        game_activity_button_back.visibility = VISIBLE
//        game_activity_image_play_again.visibility = VISIBLE
        displayScore()
    }

    fun displayScore() {
        changeCounterColor(R.color.colorGoodAnswer)
        game_activity_text_time_counter.text = getString(R.string.game_activity_text_score)
        game_activity_text_time_counter.textSize = 40F
        game_activity_text_round_result.textSize = 120F
        setRoundResultText(game.getScore().toString(),
            R.color.colorGoodAnswer
        )
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

    open fun startBreak() {
        buttonsVisibility(VISIBLE)
        buttonsBlockade(true)
        player.stop()
        sumUpRound()
        countTime(game.getBreakTime().toLong())
    }

    fun buttonsBlockade(isBlocked: Boolean) {
        for (button in buttons) {
            button.isClickable = !isBlocked
        }
    }

    fun sumUpRound() {
        val isAnswerCorrect = isAnswerCorrect()
        if (isAnswerCorrect==true) {
            game.addPoints(getPointsAmount())
            setRoundResultText(getString(R.string.game_avtivity_answer_good),
                R.color.colorGoodAnswer
            )
            getSelectedButton()!!.background = resources.getDrawable(R.drawable.answers_buttons_good_answer,null)
        } else if (isAnswerCorrect==false){
            setRoundResultText(getString(R.string.game_avtivity_answer_bad),
                R.color.colorBadAnswer
            )
            getSelectedButton()!!.background = resources.getDrawable(R.drawable.answers_buttons_bad_answer,null)
            buttons.find { x -> x.text == game.getActualSong()!!.getFairyTale() }!!
                .background = resources.getDrawable(R.drawable.answers_buttons_good_answer,null)
        } else {
            setRoundResultText(getString(R.string.game_avtivity_answer_bad),
                R.color.colorBadAnswer
            )
            buttons.find { x -> x.text == game.getActualSong()!!.getFairyTale() }!!
                .background = resources.getDrawable(R.drawable.answers_buttons_good_answer,null)
        }
    }

    private fun isAnswerCorrect(): Boolean? {
        val selectedButton =getSelectedButton()
        return if(selectedButton==null){
            null
        }else{
            selectedButton.text == game.getActualSong()!!.getFairyTale()
        }
    }

    open fun getPointsAmount(): Double {
        return 1.0
    }

    private fun getSelectedButton(): Button? {
        return buttons.find { x -> x.isSelected }
    }

    open fun startRound() {
        prepareButtonAndTextsAfterBreak()
        buttonsBlockade(false)
        buttonsVisibility(VISIBLE)

        initializeRound()
        player.start()
        countTime(game.getRoundTime().toLong())
    }

    fun prepareButtonAndTextsAfterBreak() {
        for (button in buttons) {
            button.background = resources.getDrawable(R.drawable.answers_buttons,null)
            button.isSelected = false
        }
        game_activity_text_round_result.text = ""
    }

    fun initializeRound() {
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

    override fun onClick(v: View?) {
        buttons.find { x -> x.id == v!!.id }!!.isSelected = true
        counter.cancel()
        isRound = false
        startBreak()
    }

}