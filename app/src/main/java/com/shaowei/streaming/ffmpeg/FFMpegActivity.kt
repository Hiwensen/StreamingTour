package com.shaowei.streaming.ffmpeg

import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.io.File

class FFMpegActivity : AppCompatActivity() {
    private val mUrl = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mSurface: Surface
    private lateinit var mPlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg)
        findViewById<TextView>(R.id.jni_string).text = stringFromJNI()
        findViewById<SurfaceView>(R.id.ffmpeg_surface_view).run {
            mSurfaceView = this
            this.holder.addCallback(object:SurfaceHolder.Callback{
                override fun surfaceCreated(holder: SurfaceHolder) {
                    mSurface = holder.surface
                }

                override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) = Unit

                override fun surfaceDestroyed(holder: SurfaceHolder?) = Unit

            })
        }

        // Need to put the video to the cache dir
        val video = File(cacheDir, "big_buck_bunny.mp4")

        mPlay = findViewById(R.id.ffmpeg_play_1)
        mPlay.setOnClickListener{
            if (!video.exists()) {
                Toast.makeText(this,"Can't find the video", Toast.LENGTH_SHORT).show()
            } else {
                playRemoteVideoWithFFMpeg1(video.absolutePath, mSurface)
            }
        }

        findViewById<Button>(R.id.ffmpeg_play_2).setOnClickListener {
            if (!video.exists()) {
                Toast.makeText(this,"Can't find the video", Toast.LENGTH_SHORT).show()
            } else {
                playLocalVideoWithFFMpeg2(video.absolutePath, mSurface)
            }
        }
    }

    /**
     * A native method that is implemented by the 'streaming' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String

    private external fun playRemoteVideoWithFFMpeg1(url: String, surface: Surface)

    private external fun playLocalVideoWithFFMpeg2(path: String, surface: Surface):Int

    companion object {
        // Used to load the 'ffmpegplayground' library on application startup.
        init {
            System.loadLibrary("streaming")
        }
    }
}