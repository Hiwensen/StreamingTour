package com.shaowei.streaming.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.max

class AudioTrackPlayground {
    var mBuffer: ByteArray = ByteArray(2048)

    fun play(audioFile: File): Boolean {
        val streamType = AudioManager.STREAM_MUSIC
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val mode = AudioTrack.MODE_STREAM

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val audioTrack = AudioTrack(
            streamType, sampleRate, channelConfig, audioFormat,
            max(minBufferSize, 2048), mode
        )

        var mFileInputStream: FileInputStream? = null
        try {
            mFileInputStream = FileInputStream(audioFile)
            var read: Int
            audioTrack.play()
            while (mFileInputStream.read(mBuffer).also { read = it } > 0) {
                val ret: Int = audioTrack.write(mBuffer, 0, read)
                return when (ret) {
                    AudioTrack.ERROR_BAD_VALUE, AudioTrack.ERROR_INVALID_OPERATION, AudioManager.ERROR_DEAD_OBJECT -> {
                        palyFaile()
                        false
                    }
                    else -> {
                        true
                    }
                }
            }
            return true
        } catch (e: RuntimeException) {
            e.printStackTrace()
//            palyFaile()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
//            palyFaile()
            return false
        } finally {
            mFileInputStream?.let { closeQuietly(it) }
            audioTrack.stop()
            audioTrack.release()
        }
    }

    private fun closeQuietly(mFileInputStream: FileInputStream) {
        try {
            mFileInputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun palyFaile() {

    }

}