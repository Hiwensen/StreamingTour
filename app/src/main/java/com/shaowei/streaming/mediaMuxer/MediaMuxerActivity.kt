package com.shaowei.streaming.mediaMuxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.nio.ByteBuffer

class MediaMuxerActivity : AppCompatActivity() {
    private val outPutPath = "temp.mp4"
    private val mMediaMuxer = MediaMuxer(outPutPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_muxer)
        findViewById<Button>(R.id.mux_mp4).setOnClickListener {
            muxVideoAudio()
        }
    }

    private fun muxVideoAudio() {
        // Setup Metadata track
        val audioFormat = MediaFormat()
        val videoFormat = MediaFormat()
        // More often, the MediaFormat will be retrieved from MediaCodec.getOutputFormat()
        // or MediaExtractor.getTrackFormat().

        val audioTrackIndex = mMediaMuxer.addTrack(audioFormat)
        val videoTrackIndex = mMediaMuxer.addTrack(videoFormat)
        val inputBuffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE)
        var finished = false
        var isAudioSample = false

        val bufferInfo = MediaCodec.BufferInfo()

        mMediaMuxer.start()
        while (!finished) {
            // getInputBuffer() will fill the inputBuffer with one frame of encoded
            // sample from either MediaCodec or MediaExtractor, set isAudioSample to
            // true when the sample is audio data, set up all the fields of bufferInfo,
            // and return true if there are no more samples.
            finished = getInputBuffer(inputBuffer, isAudioSample, bufferInfo)
            if (!finished) {
                val currentTrackIndex = if (isAudioSample) {
                    audioTrackIndex
                } else {
                    videoTrackIndex
                }

                mMediaMuxer.writeSampleData(currentTrackIndex, inputBuffer, bufferInfo)
            }
        }

        mMediaMuxer.stop()
    }

    private fun getInputBuffer(
        inputBuffer: ByteBuffer,
        audioSample: Boolean,
        bufferInfo: MediaCodec.BufferInfo
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        mMediaMuxer.release()
    }

}