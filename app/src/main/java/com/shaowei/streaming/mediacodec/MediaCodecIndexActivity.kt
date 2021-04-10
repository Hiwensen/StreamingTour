package com.shaowei.streaming.mediacodec

import android.media.MediaCodecList
import android.media.MediaCodecList.REGULAR_CODECS
import android.media.MediaFormat
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

private val TAG = MediaCodecIndexActivity::class.java.simpleName

class MediaCodecIndexActivity : AppCompatActivity() {
    private lateinit var mVideoFormat: MediaFormat
    private val mH264Player = H264Player()
    private lateinit var mSurface:Surface
    private var mSurfaceReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec)

        findViewById<Button>(R.id.video_decode_async).setOnClickListener{
            if (mSurfaceReady) {
                mH264Player.playAsync(R.raw.sample, mSurface, this)
            } else {
                Toast.makeText(this,"surface is not ready",Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.video_decode_sync).setOnClickListener{
            if (mSurfaceReady) {
                mH264Player.playSync(R.raw.sample, mSurface, this)
            } else {
                Toast.makeText(this,"surface is not ready",Toast.LENGTH_SHORT).show()
            }
        }

        val surfaceView = findViewById<SurfaceView>(R.id.surface_view)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                Log.d(TAG, "surfaceChanged")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                Log.d(TAG, "surfaceDestroyed")
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                mSurfaceReady = true
                mSurface = holder.surface
            }

        })
    }

    override fun onStop() {
        super.onStop()
        mH264Player.stop()
    }

}