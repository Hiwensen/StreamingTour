package com.shaowei.streaming.ffmpeg

import android.util.Log

class FFMpegPlayer {
    private val TAG = FFMpegPlayer::class.java.simpleName
    private lateinit var mPrepareListener: PrepareListener
    private var mUri: String = ""

    fun setPrepareListener(prepareListener: PrepareListener) {
        mPrepareListener = prepareListener
    }

    suspend fun prepare(uri: String) {
        mUri = uri
        if (mUri.isEmpty()) {
            Log.e(TAG, "uri should not be empty")
        } else {
            nativePrepare(uri)
        }
    }

    suspend fun start() {
        if (mUri.isEmpty()) {
            Log.e(TAG, "uri should not be empty")
        } else {
            nativeStart()
        }
    }

    /**
     * Callback function from native c++
     */
    fun onPrepared() {
        mPrepareListener.onPrepared()
    }

    private external fun nativePrepare(mUri: String)
    private external fun nativeStart()

}

interface PrepareListener {
    fun onPrepared()
}
