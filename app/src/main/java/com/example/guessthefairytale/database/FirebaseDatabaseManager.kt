package com.example.guessthefairytale.database

import android.content.ContentValues.TAG
import android.util.Log
import com.example.guessthefairytale.database.dto.DatabaseCallback
import com.example.guessthefairytale.database.dto.GameChallenge
import com.example.guessthefairytale.database.dto.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
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

    fun findGameChallenge(user: User, callback: DatabaseCallback) {
        var result = false
        val challengeListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (gameChallenge in dataSnapshot.children) {
                    val gc = gameChallenge.value as HashMap<String, String>
                    if (gc["playerTwoId"] == "") {
                        val game = GameChallenge(gc["playerOneId"]!!, user.id)
                        database.child("gameRoom").child(gameChallenge.key!!).setValue(game)
                        result = true
                        break
                    }
                }
                callback.onCallback(result)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }

        val gameRoomListener = (object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.hasChild("gameRoom")) {
                    callback.onCallback(result)
                } else {
                    database.child("gameRoom").addListenerForSingleValueEvent(challengeListener)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })

        database.addListenerForSingleValueEvent(gameRoomListener)
    }

    fun addGameChallenge(user: User, callback: DatabaseCallback) {
        val gc = GameChallenge(user.id, "")
        database.child("gameRoom").child(user.id).setValue(gc)

        val secondPlayerListener = (object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val match = dataSnapshot.child(user.id).value as HashMap<String, String>
                if (match["playerTwoId"]!="") {
                    database.child("gameRoom").child(user.id).removeValue()
                    callback.onCallback(true)
                } else {
                    callback.onCallback(false)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })

        database.child("gameRoom").addListenerForSingleValueEvent(secondPlayerListener);
    }
}