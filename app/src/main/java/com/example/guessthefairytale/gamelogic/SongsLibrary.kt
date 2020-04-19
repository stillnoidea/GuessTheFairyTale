package com.example.guessthefairytale.gamelogic

import android.content.Context
import java.nio.charset.Charset


class SongsLibrary {
    private var songsList: ArrayList<Song> = arrayListOf()
    var libSize: Int = 0

    fun getSongsList(): ArrayList<Song> {
        return songsList
    }

    fun readLibraryFromFile(context: Context) {
        val fileName = "music.txt"
        val stream = context.assets.open(fileName)
        stream.reader(Charset.defaultCharset())
        val list = stream.bufferedReader(Charset.defaultCharset()).use {
            it.readLines()
        }

        for (line in list) {
            val text = line.split(",")
            val song = Song(text[0], text[1], text[2])
            songsList.add(song)
        }
        libSize = songsList.size
    }


}
