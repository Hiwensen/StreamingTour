package com.shaowei.streaming.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.shaowei.streaming.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.max

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class AudioActivity : AppCompatActivity() {
    private val TAG = AudioActivity::class.java.simpleName

    private lateinit var mAudioRecord: AudioRecord
    private lateinit var mFileOutputStream: FileOutputStream
    private val mBuffer = ByteArray(2048)
    private val mExecutorService = Executors.newSingleThreadExecutor()

    private var mIsRecording = false
    private var mIsPlaying = false
    private var m3GPFileName: String = ""
    private var mPCMFileName: String = ""

    private val mMediaRecorderPlayground = MediaRecorderPlayground()
    private val mMediaPlayerPlayground = MediaPlayerPlayground()
    private var mStartRecording = true
    private var mStartPlaying = true

    private lateinit var mMediaRecorderRecord: Button
    private lateinit var mMediaPlayerPlay: Button
    private lateinit var mAudioRecordRecord: Button
    private lateinit var mAudioTrackPlay: Button

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)
        setContentView(R.layout.activity_audio)

        m3GPFileName = "${externalCacheDir?.absolutePath}/audiorecordtest.3gp"
        mPCMFileName = "${externalCacheDir?.absolutePath}/RecorderTest/audiorecordtest.pcm"

        ActivityCompat.requestPermissions(
            this, permissions,
            REQUEST_RECORD_AUDIO_PERMISSION
        )
        initView()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun initView() {
        // Record to the external cache directory for visibility
        mMediaRecorderRecord = findViewById(R.id.media_recorder_record)
        mMediaRecorderRecord.setOnClickListener {
            onRecordWithMediaRecorder(mStartRecording)
            mMediaRecorderRecord.text = when (mStartRecording) {
                true -> "MediaRecorder Stop recording"
                false -> "MediaRecorder Start recording"
            }
            mStartRecording = !mStartRecording
        }

        mMediaPlayerPlay = findViewById(R.id.media_player_play)
        mMediaPlayerPlay.setOnClickListener {
            onPlayWithMediaPlayer(mStartPlaying)
            mMediaPlayerPlay.text = when (mStartPlaying) {
                true -> "MediaPlayer Stop playing"
                false -> "MediaPlayer Start playing"
            }
            mStartPlaying = !mStartPlaying
        }

        mAudioRecordRecord = findViewById(R.id.audio_record_record)
        mAudioRecordRecord.setOnClickListener {
            if (mIsRecording) {
                mIsRecording = false
                mAudioRecordRecord.text = "audiorecord start record"
            } else {
                mIsRecording = true
                mExecutorService.submit {
                    if (!startAudioRecord(File(mPCMFileName))) {
                        recordFail()
                    }
                }
                mAudioRecordRecord.text = "audiorecord stop record"
            }
        }

        mAudioTrackPlay = findViewById(R.id.audio_track_play)
        mAudioTrackPlay.setOnClickListener {
            if (!mIsPlaying) {
                mIsPlaying = true
                mExecutorService.submit {
                    audioTrackPlay(File(mPCMFileName))
                }
            }
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

    private fun onRecordWithMediaRecorder(start: Boolean) = if (start) {
        mMediaRecorderPlayground.startRecording(m3GPFileName)
    } else {
        mMediaRecorderPlayground.stopRecording()
    }

    private fun onPlayWithMediaPlayer(start: Boolean) = if (start) {
        mMediaPlayerPlayground.startPlaying(m3GPFileName)
    } else {
        mMediaPlayerPlayground.stopPlaying()
    }

    private fun startAudioRecord(audioFile: File): Boolean {
        return try {
            audioFile.parentFile?.mkdirs()
            audioFile.createNewFile()
            mFileOutputStream = FileOutputStream(audioFile)
            val audioSource = MediaRecorder.AudioSource.MIC
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize =
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            mAudioRecord = AudioRecord(
                audioSource, sampleRate, channelConfig,
                audioFormat, max(minBufferSize, 2048)
            )
            mAudioRecord.startRecording()
            while (mIsRecording) {
                val read = mAudioRecord.read(mBuffer, 0, 2048)
                if (read > 0) {
                    mFileOutputStream.write(mBuffer, 0, read)
                } else {
                    return false
                }
            }
            stopAudioRecord()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
            false
        } catch (e: java.lang.RuntimeException) {
            Log.e(TAG, e.toString())
            false
        } finally {
            mAudioRecord.release()
        }
    }

    private fun stopAudioRecord(): Boolean {
        try {
            mIsRecording = false
            mAudioRecord.stop()
            mAudioRecord.release()
            mFileOutputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
            return false
        }
        return true
    }

    private fun recordFail() {
        mIsRecording = false
        runOnUiThread {
            Toast.makeText(this, "record fail", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun audioTrackPlay(audioFile: File) {
        val streamType = AudioManager.STREAM_MUSIC
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val mode = AudioTrack.MODE_STREAM
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

//        val audioTrackOld = AudioTrack(
//            streamType, sampleRate, channelConfig, audioFormat,
//            max(minBufferSize, 2048), mode
//        )

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setLegacyStreamType(streamType)
                .build(),
            AudioFormat.Builder()
                .setChannelMask(channelConfig)
                .setEncoding(audioFormat)
                .setSampleRate(sampleRate)
                .build(),
            max(minBufferSize, 2048),
            mode, AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        var mFileInputStream: FileInputStream? = null
        try {
            mFileInputStream = FileInputStream(audioFile)
            audioTrack.play()
            var read: Int
            while (mFileInputStream.read(mBuffer).also { read = it } > 0) {
                when (audioTrack.write(mBuffer, 0, read)) {
                    AudioTrack.ERROR_BAD_VALUE, AudioTrack.ERROR_INVALID_OPERATION, AudioManager.ERROR_DEAD_OBJECT -> audioTrackPlayFail()
                    else -> {
                    }
                }
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, e.toString())
            audioTrackPlayFail()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
            audioTrackPlayFail()
        } finally {
            mIsPlaying = false
            mFileInputStream?.let { closeQuietly(it) }
            audioTrack.stop()
            audioTrack.release()
        }
    }

    private fun closeQuietly(mFileInputStream: FileInputStream) {
        try {
            mFileInputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }
    }

    private fun audioTrackPlayFail() {
        runOnUiThread { Toast.makeText(this, "play fail", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mExecutorService.shutdownNow()
    }
}
