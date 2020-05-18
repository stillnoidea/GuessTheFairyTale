package com.example.guessthefairytale.database.dto

import java.io.Serializable

data class User(
    val id: String,
    val username: String,
    val email: String,
    val score: Int
) : Serializable