package com.shaowei.streaming.video

import android.content.ContentResolver
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.jaygoo.widget.RangeSeekBar
import com.shaowei.streaming.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

@RequiresApi(Build.VERSION_CODES.N)
class VideoClipActivity : AppCompatActivity(), CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val TAG = VideoClipActivity::class.java.simpleName
    private lateinit var mVideoView: VideoView
    private lateinit var mRangeSeekBar: RangeSeekBar
    private lateinit var mOriginalAudioSeekBar: SeekBar
    private lateinit var mBackgroundMusicSeekBar: SeekBar
    private lateinit var mStartClipButton: Button
    private lateinit var mStartMixAudioVideoButton: Button
    private lateinit var mStartPlayNewVideo: Button
    private var mVideoStartPosition = 0f
    private var mVideoEndPosition = 0f
    private var mOriginalAudio = 0
    private var mBackgroundMusicAudio = 0
    private var mOriginalVideoDurationSecond = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_clip)

        mVideoView = findViewById(R.id.video_view)
        mRangeSeekBar = findViewById(R.id.range_seek_bar)
        mOriginalAudioSeekBar = findViewById(R.id.seek_bar_audio_original)
        mBackgroundMusicSeekBar = findViewById(R.id.seek_bar_audio_background_music)
        mStartClipButton = findViewById(R.id.start_clip)
        mStartPlayNewVideo = findViewById(R.id.play_new_video)

        initVideoView()

        mRangeSeekBar.setOnRangeChangedListener { rangeSeekBar: RangeSeekBar, min: Float, max: Float, fromUser: Boolean ->
            val currentRange = rangeSeekBar.currentRange
            Log.d(TAG, "range changed, currentRange:${currentRange[0]} ~ ${currentRange[1]}, min:$min, max:$max")
            mVideoStartPosition = min
            mVideoEndPosition = max
        }

        mOriginalAudioSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mOriginalAudio = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit

        })

        mBackgroundMusicSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mBackgroundMusicAudio = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        mStartClipButton.setOnClickListener {
            startClipVideo()
        }

        mStartPlayNewVideo.setOnClickListener { startPlayNewVideo() }
    }

    override fun onDestroy() {
        super.onDestroy()
        (this as CoroutineScope).cancel()
    }

    private fun initVideoView() {
        val videoUri = Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/raw/"
                    + "big_buck_bunny"
        )

        mVideoView.setVideoURI(videoUri)
        mVideoView.start()
        mVideoView.setOnPreparedListener { mediaPlayer: MediaPlayer ->
            mOriginalVideoDurationSecond = mediaPlayer.duration / 1000
            Log.d(TAG, "video view prepared, duration:$mOriginalVideoDurationSecond")
            mRangeSeekBar.setRange(0f, mOriginalVideoDurationSecond.toFloat())
            mRangeSeekBar.setValue(0f, mOriginalVideoDurationSecond.toFloat())
            mRangeSeekBar.isEnabled = true
            mRangeSeekBar.requestLayout()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun startClipVideo() {
        launch(Dispatchers.Main) {
            Log.d(TAG, "start to clip video, thread name:${Thread.currentThread().name}")
            VideoProcessor.clipAndMixVideo(
                resources.openRawResourceFd(R.raw.big_buck_bunny)
                , 20 * 1000000, 30 * 1000000,
                resources.openRawResourceFd(R.raw.beautifulday), cacheDir
            )
            startPlayNewVideo()
        }
    }

    fun mixAudioVideo(view: View) {
        launch {
            Log.d(TAG, "mixAudioVideo1, thread name:${Thread.currentThread().name}")
            val mixedVideo = File(cacheDir, "mixed.mp4")
            val mixedAudio = File(cacheDir, "mixed.mp3")
            VideoProcessor.mixVideoAndMusic(resources.openRawResourceFd(R.raw.big_buck_bunny), mixedVideo.absolutePath
                , 20 * 1000000, 30 * 1000000, mixedAudio.absolutePath, {})
            Log.d(TAG, "mixAudioVideo2, thread name:${Thread.currentThread().name}")
        }
    }

    private fun startPlayNewVideo() {
        Log.d(TAG, "mix audio success, thread name: ${Thread.currentThread().name}")
        Toast.makeText(this, "mix audio video success", Toast.LENGTH_SHORT).show()
//            mVideoView.stopPlayback()
//            val mixedMp4File = File(cacheDir, "mixed.mp4")
//            mVideoView.setVideoPath(mixedMp4File.absolutePath)
//            mVideoView.start()

    }

}