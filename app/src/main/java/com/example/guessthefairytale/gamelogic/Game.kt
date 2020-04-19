package com.example.guessthefairytale.gamelogic

import android.content.Context
import kotlin.random.Random

class Game {
    private var roundsNumber: Int = 0
    private var roundTime: Int = 30000
    private var breakTime: Int = 3000
    private var score: Double = 0.0
    private var answersNumber: Int = 4
    private val songsLib: SongsLibrary = SongsLibrary()
    private var actualSong: Song = Song(true)

    fun getRoundTime(): Int {
        return roundTime
    }

    fun getBreakTime(): Int {
        return breakTime
    }

    fun getScore(): Double {
        return score
    }

    fun getActualSong(): Song? {
        return actualSong
    }

    fun setRoundsNumber(number: Int) {
        roundsNumber = number
    }

    fun addPoints(points: Double) {
        if (score < roundsNumber) {
            score += points
        }
    }

    fun initializeLib(context: Context) {
        songsLib.readLibraryFromFile(context)
    }

    fun initializeRound(): ArrayList<String> {
        initializeRoundSong()
        return getAnswers()
    }

    private fun initializeRoundSong() {
        var index = 0

        while (actualSong.wasPlayed()) {
            index = Random.nextInt(0, songsLib.libSize)
            val song = songsLib.getSongsList()[index]
            if (!song.wasPlayed()) {
                actualSong = song
            }
        }
        songsLib.getSongsList()[index].played()
    }

    private fun getAnswers(): ArrayList<String> {
        val result: ArrayList<String> = arrayListOf()

        while (result.size < answersNumber) {
            val song = songsLib.getSongsList()[Random.nextInt(0, songsLib.libSize)]
            if (!result.contains(song.getFairyTale()) && !song.wasPlayed())
                result.add(song.getFairyTale())
        }

        result[Random.nextInt(0, answersNumber)] = actualSong.getFairyTale()
        return result
    }
}
