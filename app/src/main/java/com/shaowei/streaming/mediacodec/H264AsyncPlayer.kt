package com.shaowei.streaming.mediacodec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import android.widget.Toast
import com.shaowei.streaming.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class H264AsyncPlayer {
    private val TAG = H264AsyncPlayer::class.java.simpleName
    private val mMediaCodec: MediaCodec
    private val VIDEO_TYPE_H264 = "video/avc"
    private var mOutputFormat: MediaFormat? = null
    private val mContentWidth = 368
    private val mContentHeight = 384
    private val CODEC_DEQUEUE_TIMEOUT_US = 10000L

    init {
        mMediaCodec = MediaCodec.createDecoderByType(VIDEO_TYPE_H264)
    }

    fun playAsync(filePath: String, targetSurface: Surface, context: Context) {
        val bytes = getBytes(context)
        val sourceFileSize = bytes.size
        Log.d(TAG, "sourceFileSize:$sourceFileSize")
        var startIndex = 0

        mMediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                if (index >= 0) {
                    codec.releaseOutputBuffer(index, true)
                }
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                Log.d(TAG, "onInputBufferAvailable,index:$index")
                codec.getInputBuffer(index)?.let {
                    it.clear()
                    val nextFrameStartPosition = findNextFrameStartPosition(bytes, startIndex + 1, sourceFileSize)
                    if (sourceFileSize == 0 || nextFrameStartPosition < 0 || startIndex >= sourceFileSize) {
                        return
                    }

                    it.put(bytes, startIndex, nextFrameStartPosition - startIndex)
                    codec.queueInputBuffer(index, 0, nextFrameStartPosition - startIndex, 0, 0)
                    startIndex = nextFrameStartPosition
                }
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                mOutputFormat = format
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.e(TAG, "MediaCodec error:$e")
            }

        })

        val videoFormat = MediaFormat.createVideoFormat(VIDEO_TYPE_H264, mContentWidth, mContentHeight)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        mMediaCodec.configure(videoFormat, targetSurface, null, 0)
        mOutputFormat = mMediaCodec.outputFormat
        mMediaCodec.start()
    }

    fun playSync(filePath: String, targetSurface: Surface, context: Context) {
        val bytes = getBytes(context)
        val sourceFileSize = bytes.size
        if (sourceFileSize == 0) {
            Toast.makeText(context, "fail to get srouce data", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "sourceFileSize:$sourceFileSize")
        var startIndex = 0

        val mediaFormat = MediaFormat.createVideoFormat(VIDEO_TYPE_H264, mContentWidth, mContentHeight)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        mMediaCodec.configure(mediaFormat, targetSurface, null, 0)
        mOutputFormat = mMediaCodec.outputFormat
        mMediaCodec.start()

        Thread {
            while (true) {
                val nextFrameStartPosition = findNextFrameStartPosition(bytes, startIndex + 1, sourceFileSize)
                if (nextFrameStartPosition < 0) {
                    Log.e(TAG, "fail to get next frame start position")
                    break
                }

                if (nextFrameStartPosition >= sourceFileSize) {
                    Log.d(TAG, "decode file end")
                    break
                }

                // Load data into mediaCoded
                val inputBufferIndex = mMediaCodec.dequeueInputBuffer(CODEC_DEQUEUE_TIMEOUT_US)
                if (inputBufferIndex >= 0) {
                    mMediaCodec.getInputBuffer(inputBufferIndex)?.let {
                        it.clear()
                        val dataSize = nextFrameStartPosition - startIndex
                        it.put(bytes, startIndex, dataSize)
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, dataSize, 0, 0)
                        startIndex = nextFrameStartPosition
                    }
                } else {
                    continue
                }

                // Get output data
                val bufferInfo = MediaCodec.BufferInfo()
                val outputIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, CODEC_DEQUEUE_TIMEOUT_US)
                if (outputIndex >= 0) {
                    mMediaCodec.releaseOutputBuffer(outputIndex, true)
                }
            }
        }.start()

    }

    fun stop() {
        mMediaCodec.stop()
        mMediaCodec.release()
    }

    @Throws(IOException::class)
    fun getBytes(context: Context): ByteArray {
        val `is`: InputStream = context.resources.openRawResource(R.raw.sample_new)
        //        InputStream is =   new DataInputStream(new FileInputStream(new File(path)));
        var len: Int
        val size = 1024
        var buf: ByteArray
        val bos = ByteArrayOutputStream()
        buf = ByteArray(size)
        while (`is`.read(buf, 0, size).also { len = it } != -1) bos.write(buf, 0, len)
        buf = bos.toByteArray()
        return buf
    }

    private fun findNextFrameStartPosition(bytes: ByteArray, start: Int, totalSize: Int): Int {
        for (i in start until totalSize - 4) {
            if (bytes[i].toInt() == 0x00 && bytes[i + 1].toInt() == 0x00 && bytes[i + 2].toInt() == 0x00 && bytes[i +
                        3].toInt() == 0x01
            ) {
                Log.d(TAG, "findNextFrameStartPosition:$i")
                return i
            }
        }
        return -1
    }
}