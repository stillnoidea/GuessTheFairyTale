package com.example.guessthefairytale.database.dto

import com.example.guessthefairytale.gamelogic.Round

data class GameDTO(
    val roundsNo: Int,
    var roundsLeft: Int,
    var rounds: List<Round>,
    var p1Score: Int,
    var p2Score: Int,
    var p1Ready: Boolean,
    var p2Ready: Boolean
) {
    constructor() : this(0, 0, arrayListOf<Round>(), 0, 0, false, false)
}