package com.shaowei.streaming.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.shaowei.streaming.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class AudioActivity : AppCompatActivity() {
    private lateinit var mAudioRecorder: AudioRecord
    private lateinit var mFileOutputStream: FileOutputStream
    private lateinit var mAudioFile: File
    private val mBuffer = ByteArray(2048)
    private val mExecutorService = java.util.concurrent.Executors.newSingleThreadExecutor()


    private var mIsRecording = false
    private var m3GPFileName: String = ""
    private var mPCMFileName: String = ""

    private val mMediaRecorderPlayground = MediaRecorderPlayground()
    private val mMediaPlayerPlayground = MediaPlayerPlayground()
    private var mStartRecording = true
    private var mStartPlaying = true

    private val mAudioRecordPlayground = AudioRecordPlayground()
    private val mAudioTrackPlayerPlayground = AudioTrackPlayground()

    private lateinit var mMediaRecorderRecord: Button
    private lateinit var mMediaPlayerPlay: Button
    private lateinit var mAudioRecordRecord: Button
    private lateinit var mAudioTrackPlay: Button

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

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

            if (mAudioRecordPlayground.mIsRecording) {
                mAudioRecordRecord.text = "audiorecord start record"
                mAudioRecordPlayground.stopRecorder()
                mAudioRecordPlayground.mIsRecording = false
            } else {
                mAudioRecordRecord.text = "audiorecord stop record"
                mAudioRecordPlayground.startRecord(File(mPCMFileName))
                mAudioRecordPlayground.mIsRecording = true
            }
        }

        mAudioRecordRecord.setOnClickListener(View.OnClickListener {
            if (mIsRecording) {
                mAudioRecordRecord.setText("audiorecord start record")
                mIsRecording = false
            } else {
                mAudioRecordRecord.setText("audiorecord stop record")
                mIsRecording = true
                mExecutorService.submit(Runnable {
                    if (!startRecorder()) {
                        recoderFail()
                    }
                })
            }
        })


        mAudioTrackPlay = findViewById(R.id.audio_track_play)
        mAudioTrackPlay.setOnClickListener {
            val playResult = mAudioTrackPlayerPlayground.play(File(mPCMFileName))
            if (!playResult) {
                mAudioTrackPlay.text = "audiotrack play fail"
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

    private fun startRecorder(): Boolean {
        return try {
            mAudioFile = File(
                Environment.getExternalStorageDirectory().absolutePath + "/RecorderTest/" +
                        System.currentTimeMillis() + ".pcm"
            )
            mAudioFile.getParentFile().mkdirs()
            mAudioFile.createNewFile()
            mFileOutputStream = FileOutputStream(mAudioFile)
            val audioSource = MediaRecorder.AudioSource.MIC
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize =
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            mAudioRecorder = AudioRecord(
                audioSource, sampleRate, channelConfig,
                audioFormat, Math.max(minBufferSize, 2048)
            )
            mAudioRecorder.startRecording()
            while (mIsRecording) {
                val read: Int = mAudioRecorder.read(mBuffer, 0, 2048)
                if (read > 0) {
                    mFileOutputStream.write(mBuffer, 0, read)
                } else {
                    return false
                }
            }
            stopRecorder()
        } catch (e: IOException) {
            e.printStackTrace()
            false
        } catch (e: RuntimeException) {
            e.printStackTrace()
            false
        } finally {
            mAudioRecorder.release()
        }
    }

    private fun stopRecorder(): Boolean {
        try {
            mAudioRecorder.stop()
            mAudioRecorder.release()
            mFileOutputStream.close()

            runOnUiThread { mAudioRecordRecord.text = "audiorecord stop record" }

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun recoderFail() {
        mIsRecording = false
        runOnUiThread { mAudioRecordRecord.text = "audiorecord record fail" }
    }

}