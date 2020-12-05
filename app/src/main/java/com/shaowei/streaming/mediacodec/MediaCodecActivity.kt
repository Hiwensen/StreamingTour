package com.shaowei.streaming.mediacodec

import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.io.File

private val TAG = MediaCodecActivity::class.java.simpleName

class MediaCodecActivity : AppCompatActivity() {
    private lateinit var mAudioFormat: MediaFormat
    private lateinit var mAudioCodec: MediaCodec
    private lateinit var mVideoFormat: MediaFormat
    private lateinit var mVideoCodec: MediaCodec

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

        findViewById<Button>(R.id.video_decode).setOnClickListener {
            decodeVideo()
        }

        findViewById<SurfaceView>(R.id.surface_view).holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {

            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                val surface = holder.surface
                H264Player().play("", surface)
            }

        })
    }

    override fun onResume() {
        super.onResume()
        getMediaFormat("")
        initCodec()
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

    private fun decodeVideo() {

    }

    private fun encodeVideo() {

    }

    private fun createMediaCoded() {
        // createDecoderByType(type:String)
        // createEncoderByType(type:String)
        // createByCodecName(name:String)

    }

    private fun getMediaFormat(filePath: String) {
        val extractor = MediaExtractor()

        val file = File(filePath)
        extractor.setDataSource(file.absolutePath)
        val trackCount = extractor.trackCount

        for (i in 0 until trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "mime:$mime")
            if (mime?.startsWith("audio/") == true) {
                val audioCodecName = findCodecNameForFormat(true, mediaFormat)
                mAudioFormat = mediaFormat
                mAudioCodec = MediaCodec.createByCodecName(audioCodecName)
            }

            if (mime?.startsWith("video/") == true) {
                val videoCodecName = findCodecNameForFormat(true, mediaFormat)
                mVideoFormat = mediaFormat
                mVideoCodec = MediaCodec.createByCodecName(videoCodecName)
            }

        }

        extractor.release()
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