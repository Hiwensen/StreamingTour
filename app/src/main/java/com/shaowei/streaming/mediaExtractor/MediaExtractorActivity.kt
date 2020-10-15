package com.shaowei.streaming.mediaExtractor

import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class MediaExtractorActivity : AppCompatActivity() {
    private lateinit var extractFile: Button
    private val mExtractor = MediaExtractor()
    private val BUFFER_CAPACITY = 500 * 1024
    private lateinit var mVideoOutputStream: FileOutputStream
    private lateinit var mAudioOutputStream: FileOutputStream

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_extractor)
        extractFile = findViewById(R.id.extract_mp4)
        extractFile.setOnClickListener { extractRawFile(R.raw.shariver) }

        if (!(hasWriteStoragePermission(this) && hasReadStoragePermission(this))) {
            requestReadWriteStoragePermission(this)
        } else {
            prepareOutputStream()
        }
    }

    /**
     * The extracted file lack of some head info so can't be played
     */
    private fun extractRawFile(rawFileId: Int) {
        val rawResourceFd = resources.openRawResourceFd(rawFileId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mExtractor.setDataSource(rawResourceFd)
        }
        val trackCount = mExtractor.trackCount
        var audioTrackIndex = -1
        var videoTrackIndex = -1
        for (i in 0 until trackCount) {
            val mediaFormat = mExtractor.getTrackFormat(i)
            val formatString = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "format string:$formatString")

            if (formatString?.startsWith("video/") == true) {
                videoTrackIndex = i
                Log.d(TAG, "video track:$videoTrackIndex")
            }

            if (formatString?.startsWith("audio/") == true) {
                audioTrackIndex = i
                Log.d(TAG, "audio track:$audioTrackIndex")
            }
        }

        val inputBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
        try {
            // select video track and write video stream
            mExtractor.selectTrack(videoTrackIndex)
            while (true) {
                // val sampleTrackIndex = mExtractor.sampleTrackIndex
                // val presentationTimeUs = mExtractor.sampleTime

                val readSampleData = mExtractor.readSampleData(inputBuffer, 0)
                if (readSampleData <= 0) {
                    break
                }

                val byteArray = ByteArray(readSampleData)
                inputBuffer.get(byteArray)
                mVideoOutputStream.write(byteArray)
                inputBuffer.clear()
                mExtractor.advance()
            }
            mVideoOutputStream.close()

            // select audio track and write audio stream
            mExtractor.selectTrack(audioTrackIndex)
            while (true) {
                val readSampleData = mExtractor.readSampleData(inputBuffer, 0)
                if (readSampleData <= 0) {
                    break
                }

                val byteArray = ByteArray(readSampleData)
                inputBuffer.get(byteArray)
                mAudioOutputStream.write(byteArray)
                inputBuffer.clear()
                mExtractor.advance()
            }
            mAudioOutputStream.close()

        } catch (ioException: IOException) {
            Log.e(TAG, ioException.toString())
        } finally {
            mExtractor.release()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!(hasWriteStoragePermission(this) && hasReadStoragePermission(this))) {
            Toast.makeText(
                this,
                "Reading and Writing to external storage permission is needed to run this application",
                Toast.LENGTH_LONG
            ).show()
            launchPermissionSettings(this)
        } else {
            prepareOutputStream()
        }
    }

    private fun prepareOutputStream() {
        val videoFile = File(this.filesDir, "video.mp4")
        if (!videoFile.exists()) {
            val createNewFile = videoFile.createNewFile()
            Log.d(TAG, "create video file: $createNewFile, file path: ${videoFile.path}, abs path:${videoFile.absolutePath}")
        } else {
            Log.d(TAG, "video file path: ${videoFile.path}, abs path:${videoFile.absolutePath}")
        }

        val audioFile = File(this.filesDir, "audio.pcm")
        if (!audioFile.exists()) {
            val createAudioFile = audioFile.createNewFile()
            Log.d(TAG, "create video file: $createAudioFile, file path: ${audioFile.path}, abs path:${audioFile.absolutePath}")
        } else {
            Log.d(TAG, "audio file path: ${audioFile.path}, abs path:${audioFile.absolutePath}")
        }

        mVideoOutputStream = FileOutputStream(videoFile)
        mAudioOutputStream = FileOutputStream(audioFile)
    }

    override fun onStop() {
        super.onStop()
        mExtractor.release()
    }

    companion object {
        private val TAG = MediaExtractorActivity::class.java.simpleName
    }

}