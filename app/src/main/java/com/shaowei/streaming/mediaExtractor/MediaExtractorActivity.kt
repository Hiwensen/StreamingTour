package com.shaowei.streaming.mediaExtractor

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class MediaExtractorActivity : AppCompatActivity() {
    private lateinit var extractFile: Button
    private val mExtractor = MediaExtractor()
    private val BUFFER_CAPACITY = 1000
    private var mFilePath = Uri.parse("android.resource://com.shaowei.streaming/raw/shariver").toString()
    private val mVideoPath = "/sdcard/extractor/video.mp4"
    private val mAudioPath = "/sdcard/extractor/audio.pcm"
    private val mVideoOutputStream = FileOutputStream(File(mVideoPath))
    private val mAudioOutputStream = FileOutputStream(File(mAudioPath))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_extractor)
        extractFile = findViewById(R.id.extract_mp4)
        Log.e("chang",mFilePath)
        extractFile.setOnClickListener { extract(mFilePath) }
    }

    private fun extract(filePath: String) {
        val weAreInterestedInThisTrack = false
        mExtractor.setDataSource(filePath)
        val trackCount = mExtractor.trackCount
        for (i in 1 until trackCount) {
            val mediaFormat = mExtractor.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            if (weAreInterestedInThisTrack) {
                mExtractor.selectTrack(i)
            }
        }

        val inputBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
        while (mExtractor.readSampleData(inputBuffer, 100) >= 0) {
            val sampleTrackIndex = mExtractor.sampleTrackIndex
            val presentationTimeUs = mExtractor.sampleTime
            //...
            mExtractor.advance()
        }
    }

    override fun onStop() {
        super.onStop()
        mExtractor.release()
    }

}