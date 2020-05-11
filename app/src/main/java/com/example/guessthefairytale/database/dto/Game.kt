package com.example.guessthefairytale.database.dto

import com.example.guessthefairytale.gamelogic.Song

data class Game(
    val id: String,
    val roundsNo: String,
    val actualRound: String,
    val songs: List<Song>,
    val actualSong: Song,
    val p1Score: Int,
    val p2Score: Int
)