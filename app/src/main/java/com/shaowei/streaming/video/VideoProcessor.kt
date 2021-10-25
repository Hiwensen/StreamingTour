package com.shaowei.streaming.video

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File

class VideoProcessor {
    private val TAG = VideoProcessor::class.java.simpleName
    private val TRACK_UNKNOWN = -1

    fun clipAndMixVideo(
        originalVideoPath: String, startPosition: Int, endPosition: Int,
        backgroundMusicPath: String, outputVideoPath: String, cacheDir: File
    ) {
        // Extract and decode original audio to PCM
        val originalAudioPCMFile = File(cacheDir, "originalAudio.pcm")
        decodeAudioFileToPCM(originalVideoPath, startPosition, endPosition, originalAudioPCMFile.absolutePath)


        // Decode background music to PCM
        val backgroundMusicPCMFile = File(cacheDir, "backgroundAudio.pcm")


        // Mix original audio and background music PCM file

        // Merge mixed audio and original video

    }

    private fun decodeAudioFileToPCM(
        originalFilePath: String, startTimeUS: Int, endTimeUs: Int,
        outputPCMFilePath: String
    ) {
        if (endTimeUs > startTimeUS) {
            Log.e(TAG, "decodeAudioFileToPCM, start time greater than end time")
        }
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(originalFilePath)
        val audioTrack = getTrack(mediaExtractor, true)
        if (audioTrack == TRACK_UNKNOWN) {
            Log.e(TAG,"fail to get track")
            return
        }

        mediaExtractor.selectTrack(audioTrack)

    }

    private fun getTrack(mediaExtractor: MediaExtractor, audio: Boolean): Int {
        val trackCount = mediaExtractor.trackCount
        for (i in 0 until trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("/video") == true && !audio) {
                return i
            }

            if (mime?.startsWith("/audio") == true && audio) {
                return i
            }
        }

        return TRACK_UNKNOWN
    }

}