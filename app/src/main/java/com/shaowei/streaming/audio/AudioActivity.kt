package com.shaowei.streaming.audio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.shaowei.streaming.R

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class AudioActivity : AppCompatActivity() {

    private var fileName: String = ""

    private val mMediaRecorderPlayground = MediaRecorderPlayground()
    private val mMediaPlayerPlayground = MediaPlayerPlayground()
    private var mStartRecording = true
    private var mStartPlaying = true

    private lateinit var mRecordButton: Button
    private lateinit var mPlayButton: Button

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_audio)

        // Record to the external cache directory for visibility
        fileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        ActivityCompat.requestPermissions(
            this, permissions,
            REQUEST_RECORD_AUDIO_PERMISSION
        )

        mRecordButton = findViewById(R.id.media_recorder_record)
        mRecordButton.setOnClickListener {
            onRecord(mStartRecording)
            mRecordButton.text = when (mStartRecording) {
                true -> "MediaRecorder Stop recording"
                false -> "MediaRecorder Start recording"
            }
            mStartRecording = !mStartRecording
        }

        mPlayButton = findViewById(R.id.media_recorder_play)
        findViewById<Button>(R.id.media_recorder_play).setOnClickListener {
            onPlay(mStartPlaying)
            mPlayButton.text = when (mStartPlaying) {
                true -> "MediaPlayer Stop playing"
                false -> "MediaPlayer Start playing"
            }
            mStartPlaying = !mStartPlaying
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun onRecord(start: Boolean) = if (start) {
        mMediaRecorderPlayground.startRecording(fileName)
    } else {
        mMediaRecorderPlayground.stopRecording()
    }

    private fun onPlay(start: Boolean) = if (start) {
        mMediaPlayerPlayground.startPlaying(fileName)
    } else {
        mMediaPlayerPlayground.stopPlaying()
    }

}