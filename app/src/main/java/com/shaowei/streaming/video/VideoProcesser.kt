package com.shaowei.streaming.video

import android.util.Log
import java.io.File

class VideoProcessor {
    private val TAG = VideoProcessor::class.java.simpleName

    fun clipAndMixVideo(originalVideoPath: String, videoStartPosition: Float, videoEndPosition: Float,
        backgroundMusicPath: String,outputVideoPath: String, cacheDir: File ) {
        // Extract and decode original audio to PCM
        val originalAudioPCMFile = File(cacheDir, "originalAudio.pcm")


        // Decode background music to PCM
        val backgroundMusicPCMFile = File(cacheDir, "backgroundAudio.pcm")


        // Mix original audio and background music PCM file

        // Merge mixed audio and original video

    }

    private fun decodeAudioFileToPCM(originalFilePath:String, startTimeUS:Int, endTimeUs:Int,
                                        outputPCMFilePath:String) {
        if (endTimeUs > startTimeUS) {
            Log.e(TAG, "decodeAudioFileToPCM, start time greater than end time")
        }


    }

}