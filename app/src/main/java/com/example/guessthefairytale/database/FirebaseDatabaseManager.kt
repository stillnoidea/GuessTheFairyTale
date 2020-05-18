package com.example.guessthefairytale.database

import android.util.Log
import com.example.guessthefairytale.database.dto.User
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class FirebaseDatabaseManager {
    private val KEY_USER = "user"
    private val KEY_GAME = "game"
    private val database = Firebase.database.reference

    fun createUser(id: String, name: String, email: String) {
        val user = User(id, name, email, 0)

        database.child("users").child(user.id).setValue(user)
            .addOnSuccessListener {
                Log.i("Firebase", "Success")
            }
            .addOnFailureListener {
                Log.i("Firebase", "Error")
            }
    }
}