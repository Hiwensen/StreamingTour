package com.shaowei.streaming.ffmpeg

import android.Manifest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shaowei.streaming.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FFMpegActivity : AppCompatActivity() {
    private val TAG = FFMpegActivity::class.java.simpleName
    private val mMp4Url = "http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4"
    private val mMp3Url = "http://mpge.5nd.com/2015/2015-11-26/69708/1.mp3"
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mSurface: Surface
    private lateinit var mPlay: Button
    private lateinit var mAudioTrack: AudioTrack
    private val mFFMpegPlayer = FFMpegPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ffmpeg)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 5)
        }

        findViewById<TextView>(R.id.jni_string).text = stringFromJNI()
        findViewById<SurfaceView>(R.id.ffmpeg_surface_view).run {
            mSurfaceView = this
            this.holder.addCallback(object : SurfaceHolder.Callback {
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
        mPlay.setOnClickListener {
            if (!video.exists()) {
                Toast.makeText(this, "Can't find the video", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    playRemoteVideoWithFFMpeg1(video.absolutePath, mSurface)
                }
            }
        }

        findViewById<Button>(R.id.ffmpeg_play_2).setOnClickListener {
            if (!video.exists()) {
                Toast.makeText(this, "Can't find the video", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    playLocalVideoWithFFMpeg2(video.absolutePath, mSurface)
                }
            }
        }

        val audioFile = File(cacheDir, "beautifulday.mp3")

        findViewById<Button>(R.id.ffmpeg_play_mp3_with_audio_track).setOnClickListener {
            if (!audioFile.exists()) {
                Toast.makeText(this, "Can't find the audio file", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    playAudioWithFFMpeg(audioFile.absolutePath)
                }
            }
        }

        mFFMpegPlayer.setPrepareListener(object : PrepareListener {
            override fun onPrepared() {
                Log.d(TAG, "onPrepared")
                lifecycleScope.launch {
                    mFFMpegPlayer.start()
                }

            }
        })

        findViewById<Button>(R.id.ffmpeg_play_mp3_with_opensl).setOnClickListener {
            lifecycleScope.launch {
                mFFMpegPlayer.prepare(mMp3Url)
            }
        }

    }

    fun createTrack(sampleRateInHz: Int, numberOfTracks: Int) {
        val channelConfig = when (numberOfTracks) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_MONO
        }

        val bufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        mAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig,
            AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM
        )
        mAudioTrack.play()
    }

    fun playTrack(buffer: ByteArray, length: Int) {
        if (mAudioTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
            mAudioTrack.write(buffer, 0, length)
        }
    }

    /**
     * A native method that is implemented by the 'streaming' native library,
     * which is packaged with this application.
     */
    private external fun stringFromJNI(): String

    private external fun playRemoteVideoWithFFMpeg1(url: String, surface: Surface)

    private external fun playLocalVideoWithFFMpeg2(path: String, surface: Surface): Int

    private external fun playAudioWithFFMpeg(path: String)

    companion object {
        // Used to load the 'ffmpegplayground' library on application startup.
        init {
            System.loadLibrary("streaming")
        }
    }
}