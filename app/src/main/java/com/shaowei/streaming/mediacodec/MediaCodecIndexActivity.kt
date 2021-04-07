package com.shaowei.streaming.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors

private val TAG = MediaCodecIndexActivity::class.java.simpleName

class MediaCodecIndexActivity : AppCompatActivity() {
    private var mIsRecording: Boolean = false
    private lateinit var mAudioFormat: MediaFormat
    private lateinit var mAudioCodec: MediaCodec
    private lateinit var mVideoFormat: MediaFormat
    private lateinit var mVideoCodec: MediaCodec
    private val mH264Player = H264AsyncPlayer()

    private var mOutputFormat: MediaFormat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec)
        findViewById<Button>(R.id.audio_encode).setOnClickListener {
            encodeAudio()
        }

        findViewById<Button>(R.id.audio_decode).setOnClickListener {
            decodeAudio()
        }

        findViewById<Button>(R.id.video_encode).setOnClickListener {
            encodeVideo()
        }

        val surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {

            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                decodeVideo(holder.surface)
            }

        })
    }

    override fun onResume() {
        super.onResume()
        initCodec()
    }

    override fun onStop() {
        super.onStop()
        mH264Player.stop()
    }

    private fun initCodec() {

    }

    private fun decodeAudio() {

    }

    private fun encodeAudio() {
        mAudioCodec.setCallback(object : MediaCodec.Callback() {
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                val outputBuffer = codec.getOutputBuffer(index)
                val bufferFormat = codec.getOutputFormat(index)
                // bufferFormat is equivalent to mOutputFormat
                // outputBuffer is ready to be processed or rendered.
                // ...

                //  codec.releaseOutputBuffer(index,)
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val inputBuffer = codec.getInputBuffer(index)
                // fill inputBuffer with valid data
                //...
                //codec.queueInputBuffer(index,)
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                // Subsequent data will conform to new format.
                // Can ignore if using getOutputFormat(outputBufferId)
                mOutputFormat = format; // option B
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.e(TAG, "mediaCodec error:$e")
            }

        })

        mAudioCodec.configure(mVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mOutputFormat = mAudioCodec.outputFormat
        mAudioCodec.start()
    }

    private fun decodeVideo(surface: Surface) {
        mH264Player.play("", surface, this)
    }

    private fun encodeVideo() {

    }

    private fun findCodecNameForFormat(encoder: Boolean, format: MediaFormat): String {
        val mime = format.getString(MediaFormat.KEY_MIME)
        val mediaCodecList = MediaCodecList(REGULAR_CODECS)
        val codecInfos = mediaCodecList.codecInfos
        for (codecInfo in codecInfos) {
            if (codecInfo.isEncoder != encoder) {
                continue
            }

            val capabilitiesForType = codecInfo.getCapabilitiesForType(mime)
            if (capabilitiesForType != null && capabilitiesForType.isFormatSupported(format)) {
                return codecInfo.name
            }
        }

        return ""
    }

}