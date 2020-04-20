package com.shaowei.streaming.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioRecordPlayground {
    var mIsRecording: Boolean = false
    private lateinit var mAudioRecord: AudioRecord
    private lateinit var mFileOutputStream: FileOutputStream
    private val TAG = AudioRecordPlayground::class.java.simpleName
    private val mBuffer = ByteArray(1024 * 5)

    fun startRecord(outputFile: File): Boolean {
        try {
            outputFile.parentFile?.mkdirs()
            outputFile.createNewFile()
            mFileOutputStream = FileOutputStream(outputFile)

            val audioSource = MediaRecorder.AudioSource.MIC
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize =
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            mAudioRecord = AudioRecord(
                audioSource, sampleRate, channelConfig,
                audioFormat, minBufferSize.coerceAtLeast(2048)
            )

            mAudioRecord.startRecording()
            while (mIsRecording) {
                val read: Int = mAudioRecord.read(mBuffer, 0, 2048)
                if (read > 0) {
                    mFileOutputStream.write(mBuffer, 0, read)
                } else {
                    return false
                }
            }
            return stopRecorder()

        } catch (exception: IOException) {
            Log.e(TAG, exception.toString())
            return false
        } finally {
            mAudioRecord.release()
        }
    }

    fun stopRecorder(): Boolean {
        try {


            mAudioRecord.stop()
            mAudioRecord.release()
            mFileOutputStream.close()

//            runOnUiThread(Runnable { textView.setText(textView.getText().toString() + "\nsuccess" + second + "S") })
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

}