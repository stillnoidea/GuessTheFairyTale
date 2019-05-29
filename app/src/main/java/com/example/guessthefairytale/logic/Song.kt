package com.example.guessthefairytale.logic

import java.io.Serializable

class Song():Serializable {
    private var title: String = ""
    private var fairyTale: String = ""
    private var filePath: String = ""
    private var wasPlayed: Boolean = false

    constructor(newTitle: String, newFairy: String, newFilePath: String) : this() {
        title = newTitle
        fairyTale = newFairy
        filePath = newFilePath
        wasPlayed = false
    }

    constructor(played: Boolean) : this() {
        wasPlayed = played
    }

    fun getTitle(): String {
        return title
    }

    fun getFairyTale(): String {
        return fairyTale
    }

    fun getFilePath(): String {
        return filePath
    }

    fun wasPlayed(): Boolean {
        return wasPlayed
    }

    fun played() {
        wasPlayed = true
    }

}