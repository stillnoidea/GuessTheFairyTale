package com.example.guessthefairytale.database.dto

data class User(val id: String,
                val username: String,
                val email: String,
                val score: Int)