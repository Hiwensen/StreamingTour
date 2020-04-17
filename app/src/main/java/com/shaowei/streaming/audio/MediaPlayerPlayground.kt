package com.shaowei.streaming.audio

import android.media.MediaPlayer
import android.util.Log
import java.io.IOException

private const val LOG_TAG = "MediaPlayerPlayground"

class MediaPlayerPlayground {
    private lateinit var mMediaPlayer: MediaPlayer

    fun startPlaying(fileName: String) {
        mMediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(fileName)
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    fun stopPlaying() {
        mMediaPlayer.release()
    }
}