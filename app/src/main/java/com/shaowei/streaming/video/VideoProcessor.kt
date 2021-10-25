package com.shaowei.streaming.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class VideoProcessor {
    private val TAG = VideoProcessor::class.java.simpleName
    private val TRACK_UNKNOWN = -1
    private lateinit var mMediaCodec: MediaCodec

    fun clipAndMixVideo(
        context: Context, originalVideoPath: String, startPosition: Int, endPosition: Int,
        backgroundMusicPath: String, outputVideoPath: String, cacheDir: File
    ) {
        // Extract and decode original audio to PCM
        val originalAudioPCMFile = File(cacheDir, "originalAudio.pcm")
        decodeAudioFileToPCMAsync(
            context,
            originalVideoPath,
            startPosition,
            endPosition,
            originalAudioPCMFile.absolutePath
        )


        // Decode background music to PCM
        val backgroundMusicPCMFile = File(cacheDir, "backgroundAudio.pcm")


        // Mix original audio and background music PCM file

        // Merge mixed audio and original video

    }

    private fun decodeAudioFileToPCMAsync(
        context: Context,
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
            Log.e(TAG, "fail to get track")
            return
        }

        mediaExtractor.selectTrack(audioTrack)
        mediaExtractor.seekTo(startTimeUS.toLong(), MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val audioFormat = mediaExtractor.getTrackFormat(audioTrack)
        val maxBufferSize =
            if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            else 100 * 1000
        val byteBuffer = ByteBuffer.allocateDirect(maxBufferSize)

//        MediaCodec.createDecoderByType(audioFormat.getString(MediaFormat.KEY_MIME))
        val codecName = MediaCodecList(MediaCodecList.REGULAR_CODECS).findDecoderForFormat(audioFormat)
        try {
            mMediaCodec = MediaCodec.createByCodecName(codecName)
        } catch (e: Exception) {
            Toast.makeText(context, "Fail to create MediaCodec", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "fail to create codec:$e")
            return
        }

        mMediaCodec.configure(audioFormat, null, null, 0)
        mMediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {

            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {

            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {

            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {

            }

        })

        mMediaCodec.start()

        val outputPCMFile = File(outputPCMFilePath)
        val writeChannel = FileOutputStream(outputPCMFile).channel

    }

    private fun decodeAudioFileToPCMSync(
        context: Context,
        originalFilePath: String, startTimeUS: Int, endTimeUs: Int,
        outputPCMFilePath: String
    ) {
        
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