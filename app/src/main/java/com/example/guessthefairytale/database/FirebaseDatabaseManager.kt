package com.example.guessthefairytale.database

import android.util.Log
import com.example.guessthefairytale.database.dto.User
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseDatabaseManager {
    private val database = Firebase.database
    private val KEY_USER = "user"
    private val KEY_GAME = "game"

    fun createUser(id: String, name: String, email: String) {
        val user = User(id, name, email, 0)
        val res = database
            .reference
            .child(KEY_USER)
            .child(id).setValue(user)
        Log.e("result: ", res.result.toString())
    }

    fun addReference() {
        database.reference.child(KEY_USER)
    }
}