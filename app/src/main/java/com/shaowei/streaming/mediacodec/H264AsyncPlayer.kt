package com.shaowei.streaming.mediacodec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import com.shaowei.streaming.R
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class H264AsyncPlayer {
    private val mMediaCodec: MediaCodec
    private val VIDEO_TYPE_H264 = "video/avc"
    private var mOutputFormat: MediaFormat? = null
    private val TAG = H264AsyncPlayer::class.java.simpleName

    init {
        mMediaCodec = MediaCodec.createDecoderByType(VIDEO_TYPE_H264)
    }

    fun play(filePath: String, targetSurface: Surface, context: Context) {
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
                    val nextFrameStartPosition = findNextFrameStartPosition(bytes, startIndex + 2, sourceFileSize)
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

        val videoFormat = MediaFormat.createVideoFormat(VIDEO_TYPE_H264, 368, 384)
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        mMediaCodec.configure(videoFormat, targetSurface, null, 0)
        mOutputFormat = mMediaCodec.outputFormat
        mMediaCodec.start()
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