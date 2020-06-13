package com.example.guessthefairytale.database

import android.content.ContentValues.TAG
import android.util.Log
import com.example.guessthefairytale.database.dto.DatabaseCallback
import com.example.guessthefairytale.database.dto.GameChallenge
import com.example.guessthefairytale.database.dto.GameDTO
import com.example.guessthefairytale.database.dto.User
import com.example.guessthefairytale.gamelogic.Round
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class FirebaseDatabaseManager {
    private val KEY_USER = "user"
    private val KEY_GAME = "game"
    private val database = Firebase.database.reference

    fun createUser(id: String, name: String, email: String) {
        val user = User(id, name, email, 0)
        database.child("users").child(user.id).setValue(user)
    }

    fun findGameChallenge(user: User, callback: DatabaseCallback) {
        var result: String?
        val challengeListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                result = iterateThrowGames(user, dataSnapshot)
                if (result == null) {
                    callback.onCallback("")
                } else {
                    callback.onCallback(result!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }

        val gameRoomListener = (object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.hasChild("gameRoom")) {
                    callback.onCallback("")
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

    private fun iterateThrowGames(user: User, dataSnapshot: DataSnapshot): String? {
        for (gameChallenge in dataSnapshot.children) {
            val gc = gameChallenge.value as HashMap<String, String>
            if (gc["playerTwoId"] == "") {
                val game = GameChallenge(gc["playerOneId"]!!, user.id)
                database.child("gameRoom").child(gameChallenge.key!!).setValue(game)
                return gc["playerOneId"]!!
            }
        }
        return null
    }

    fun addGameChallenge(user: User, callback: DatabaseCallback) {
        val gc = GameChallenge(user.id, "")

        database.child("gameRoom").child(user.id).setValue(gc).addOnCompleteListener {
            database.child("gameRoom").child(user.id).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.value != null) {
                        val match = dataSnapshot.value as HashMap<String, String>
                        if (match["playerTwoId"] != "") {
                            database.child("gameRoom").child(user.id).removeValue()
                            database.child("gameRoom").child(user.id).removeEventListener(this)
                            callback.onCallback(true)
                        } else {
                            callback.onCallback(false)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                }
            })
        }
    }

    fun uploadGameData(gameData: GameDTO, userID: String) {
        database.child("game").child(userID).setValue(gameData)
    }

    fun listenForGameData(playerOneId: String, callback: DatabaseCallback) {
        val gameDataListener = (object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    Log.i("Multi","[Firebase] listenForGameData")
                    print(dataSnapshot)
                    val dbData = dataSnapshot.getValue<GameDTO>()
                    val round = dataSnapshot.child("rounds").getValue<ArrayList<Round>>()
                    dbData!!.rounds = round!!
                    database.child("game").child(dataSnapshot.key!!).removeEventListener(this)
                    callback.onCallback(dbData, dataSnapshot.key!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })
        database.child("game").child(playerOneId).addValueEventListener(gameDataListener)
    }

    fun deletePlayerData(firstPlayerId: String) {
        database.child("game").child(firstPlayerId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    database.child("gameRoom").child(firstPlayerId).removeValue()
                    database.child("game").child(firstPlayerId).removeValue()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    fun listenForPlayersStandby(playerOneId: String, callback: DatabaseCallback): ValueEventListener {
        val playersListener = (object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {
                    Log.i("Multi","[Firebase] listenForPlayersStandby")
                    val gameData = dataSnapshot.getValue<GameDTO>()
                    if (gameData!!.p1Ready && gameData.p2Ready) {
                        database.child("game").child(playerOneId).removeEventListener(this)
                        callback.onCallback(true)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })
        database.child("game").child(playerOneId).addValueEventListener(playersListener)
        return playersListener
    }

    fun notifyPlayerStandby(playerOneId: String, playerNoID: String, callback: DatabaseCallback) {
        database.child("game").child(playerOneId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.i("Multi","[Firebase] notifyPlayerStandby")
                if (dataSnapshot.value == null) {
                    callback.onCallback(false)
                } else {
                    callback.onCallback(true)
                    database.child("game").child(playerOneId).child(playerNoID).setValue(true)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    fun listenForRoundChange(playerOneId: String, callback: DatabaseCallback): ValueEventListener {
        var noOfChange = 0
        val roundListener = (object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.i("Multi","[Firebase] listenForRoundChange")
                if (noOfChange > 0) {
                    if (dataSnapshot.value != null) {
                        val gameData = dataSnapshot.getValue<GameDTO>()
                        if (gameData!!.roundsLeft == 0) {
                            database.child("game").child(playerOneId).removeEventListener(this)
                        }
                        callback.onCallback(gameData.p1Score, gameData.p2Score)
                    }
                }
                noOfChange++
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })
        database.child("game").child(playerOneId).addValueEventListener(roundListener)
        return roundListener
    }

    fun updateGamePoints(playerOneId: String, p1Pnt: Int, p2Pnt: Int, roundsLeft: Int, callback: DatabaseCallback) {
        database.child("game").child(playerOneId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value == null) {
                    callback.onCallback(false)
                } else {
                    Log.i("Multi","[Firebase] updateGamePoints")
                    val childUpdates = HashMap<String, Int>()
                    childUpdates["p1Score"] = p1Pnt
                    childUpdates["p2Score"] = p2Pnt
                    childUpdates["roundsLeft"] = roundsLeft

                    database.child("game").child(playerOneId).updateChildren(childUpdates as Map<String, Any>)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
            }
        })
    }

    fun removeGameListener(listener: ValueEventListener, playerOneId: String) {
        database.child("game").child(playerOneId).removeEventListener(listener)
    }
}