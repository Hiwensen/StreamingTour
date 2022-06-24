package com.shaowei.streaming.ffmpeg

class FFMpegPlayer {
    private lateinit var mPrepareListener : PrepareListener

    fun setPrepareListener(prepareListener: PrepareListener) {

    }

}

interface PrepareListener{
    fun onPrepared()
}
