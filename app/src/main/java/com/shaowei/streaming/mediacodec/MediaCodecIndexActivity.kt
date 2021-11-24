package com.shaowei.streaming.mediacodec

import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import kotlinx.android.synthetic.main.activity_media_codec.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val TAG = MediaCodecIndexActivity::class.java.simpleName

class MediaCodecIndexActivity : AppCompatActivity() {
    private lateinit var mVideoPlayer: VideoPlayer
    private lateinit var mSurface: Surface
    private var mSurfaceReady = false
    private val mMainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_codec)

        video_decode_async.setOnClickListener {
            if (mSurfaceReady) {
                if (this::mVideoPlayer.isInitialized) {
                    mVideoPlayer.stop()
                }
                mVideoPlayer = VideoPlayer()
                mVideoPlayer.playAsync(R.raw.sample, mSurface, this)
            } else {
                Toast.makeText(this, "surface is not ready", Toast.LENGTH_SHORT).show()
            }
        }

        video_decode_sync.setOnClickListener {
            if (mSurfaceReady) {
                if (this::mVideoPlayer.isInitialized) {
                    mVideoPlayer.stop()
                }

                mVideoPlayer = VideoPlayer()
                mMainScope.launch {
                    mVideoPlayer.playSync(R.raw.sample, mSurface, this@MediaCodecIndexActivity)
                }
            } else {
                Toast.makeText(this, "surface is not ready", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.play_mp4_video).setOnClickListener {
            if (mSurfaceReady) {
                if (this::mVideoPlayer.isInitialized) {
                    mVideoPlayer.stop()
                }

                //todo bugs when play big buck bunny file
                mVideoPlayer = VideoPlayer()
                mVideoPlayer.playMP4VideoAsync(R.raw.shariver, mSurface, this)
            } else {
                Toast.makeText(this, "surface is not ready", Toast.LENGTH_SHORT).show()
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

    override fun onPause() {
        super.onPause()
        mVideoPlayer.pause()

    }

    override fun onStop() {
        super.onStop()
        mSurface.release()
        mVideoPlayer.stop()
        mVideoPlayer.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMainScope.cancel()
    }

}