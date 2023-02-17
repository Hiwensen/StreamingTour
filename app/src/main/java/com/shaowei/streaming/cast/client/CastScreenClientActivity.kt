package com.shaowei.streaming.cast.client

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

val SOCKET_PORT = 11000
val CODEC_DEQUEUE_TIMEOUT_US = 100000L

class CastScreenClientActivity : AppCompatActivity() {
    private val TAG = CastScreenClientActivity::class.java.simpleName
    private lateinit var mSurface: Surface
    private lateinit var mSocketLive: SocketLiveClient
    private lateinit var mMediaCodec: MediaCodec
    private val mSocketCallback = object : SocketLiveClient.SocketCallbackClient {
        override fun onMessage(data: ByteArray) {
            Log.d(TAG, "receive socket message:${data.size}")
            val index = mMediaCodec.dequeueInputBuffer(CODEC_DEQUEUE_TIMEOUT_US)
            if (index >= 0) {
                val inputBuffer = mMediaCodec.getInputBuffer(index)
                inputBuffer?.clear()
                inputBuffer?.put(data, 0, data.size)
                // Start to decode data
                mMediaCodec.queueInputBuffer(index, 0, data.size, System.currentTimeMillis(), 0)

                val bufferInfo = MediaCodec.BufferInfo()
                var outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, CODEC_DEQUEUE_TIMEOUT_US)
                while (outputBufferIndex > 0) {
                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, true)
                    outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,0)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast_client)
        val surfaceView = findViewById<SurfaceView>(R.id.cast_client_surface)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder) = Unit

            override fun surfaceCreated(holder: SurfaceHolder) {
                mSurface = holder.surface
                initSocket()
                initDecoder(mSurface)
            }
        })
    }

    override fun onStop() {
        super.onStop()
        mSocketLive.close()
    }

    private fun initSocket() {
        mSocketLive = SocketLiveClient(mSocketCallback, SOCKET_PORT)
        mSocketLive.start()
    }

    private fun initDecoder(surface: Surface) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, 720, 1280)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 720 * 1080)
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            mMediaCodec.configure(format, surface, null, 0)
            mMediaCodec.start()
        } catch (e: Exception) {
            Log.e(TAG, "initDecoder fail:$e")
        }
    }
}