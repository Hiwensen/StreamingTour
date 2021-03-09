package com.shaowei.streaming.mediacodec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class H264Player {
    private val TAG = H264Player::class.java.simpleName

    private val mExcutor: ExecutorService = Executors.newFixedThreadPool(3)
    private lateinit var mMediaCodec: MediaCodec
    private lateinit var mFilePath: String
    private var mWidth = 0
    private var mHeight = 0
    private val TIMEOUT_MICRO_SECONDS = 10000L

    init {
        try {
            mMediaCodec = MediaCodec.createDecoderByType("video/avc")
        } catch (exception: Exception) {
            Log.e(TAG, "create media codec failed:$exception")
        }
    }

    fun play(filePath: String, surface: Surface) {
        mFilePath = filePath
        val mediaFormat = MediaFormat.createVideoFormat("video/avc", mWidth, mHeight)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        mMediaCodec.configure(mediaFormat, surface, null, 0)
        mMediaCodec.start()
        mExcutor.execute(DecodeH264(filePath))
    }

    inner class DecodeH264(private val filePath: String) : Runnable {
        override fun run() {
            decodeH264(filePath)
        }

        private fun decodeH264(filePath: String) {
            val fileBytes = getFileBytes(filePath)
            val totalSize = fileBytes.size
            var startIndex = 0

            while (true) {
                if (totalSize == 0 || startIndex >= totalSize) {
                    break
                }

                val nextFrameStart = findFrameStart(fileBytes, totalSize, startIndex + 2)
                val availableIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_MICRO_SECONDS)
                if (availableIndex > 0) {
                    // Found available input buffer, load it to mediaCodec
                    val inputBuffer = mMediaCodec.getInputBuffer(availableIndex)
                    inputBuffer?.run {
                        inputBuffer.clear()
                        inputBuffer.put(fileBytes, startIndex, (nextFrameStart - startIndex))
                        mMediaCodec.queueInputBuffer(availableIndex, startIndex, nextFrameStart - startIndex, 0, 0)
                        startIndex = nextFrameStart
                    }
                } else {
                    continue
                }
            }

            // Get output data from mediaCodec
            val bufferInfo = MediaCodec.BufferInfo()
            val outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_MICRO_SECONDS)
            if (outputBufferIndex >= 0) {
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, true)
            }
        }

        private fun findFrameStart(fileBytes: ByteArray, totalSize: Int, startIndex: Int): Int {
            for (i in startIndex until totalSize - 4) {
                if (fileBytes[i].toInt() == 0x00 && fileBytes[i + 1].toInt() == 0x00 && fileBytes[i + 2].toInt() ==
                    0x00 && fileBytes[i + 3].toInt() == 0x01) {
                    return i
                }
            }
            return -1
        }

        private fun getFileBytes(filePath: String): ByteArray {
            val inputStream = DataInputStream(FileInputStream(File(filePath)))
            val byteArrayOutputStream = ByteArrayOutputStream()

            val size = 1024
            var length: Int
            val byteArray = ByteArray(size)
            while (true) {
                length = inputStream.read(byteArray, 0, size)
                if (length == -1) {
                    break
                }
                byteArrayOutputStream.write(byteArray, 0, length)
            }

            return byteArrayOutputStream.toByteArray()
        }

    }


}