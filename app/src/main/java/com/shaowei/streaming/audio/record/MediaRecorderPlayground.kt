package com.shaowei.streaming.audio.record

import android.media.MediaRecorder
import android.util.Log
import java.io.IOException

private const val LOG_TAG = "MediaRecorderPlayground"

class MediaRecorderPlayground {
    private lateinit var recorder: MediaRecorder

    fun startRecording(fileName: String) {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    fun stopRecording() {
        recorder.apply {
            stop()
            release()
        }
    }


}