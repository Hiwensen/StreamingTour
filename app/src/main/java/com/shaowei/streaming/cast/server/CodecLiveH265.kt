package com.shaowei.streaming.cast.server

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.*
import android.media.projection.MediaProjection
import android.os.Environment
import android.util.Log
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.experimental.and

class CodecLiveH265(private val socketLiveServer: SocketLiveServer, private val mediaProjection: MediaProjection) : Thread() {
    private val MEDIACODEC_TIMEOUT_US = 10000L
    private val TAG = CodecLiveH265::class.java.simpleName
    private lateinit var mMediaCodec: MediaCodec
    private val mWidth = 720
    private val mHeight = 1280
    private val FRAME_RATE = 20
    private val ENCODER_TYPE = "video/hevc"
    private val DISPLAY_NAME = "-display"
    private var mVirtualDisplay: VirtualDisplay? = null

    val NAL_I = 19
    val NAL_VPS = 32
    private lateinit var vps_sps_pps_buf: ByteArray


    fun startLive() {
        try {
            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_HEVC, mWidth, mHeight)
            videoFormat.apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(KEY_BIT_RATE, mWidth * mHeight)
                setInteger(KEY_FRAME_RATE, FRAME_RATE)
                setInteger(KEY_I_FRAME_INTERVAL, 1)
            }

            val mediaCodec = MediaCodec.createEncoderByType(ENCODER_TYPE)
            mediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = mediaCodec.createInputSurface()
            mediaProjection.createVirtualDisplay(
                DISPLAY_NAME,
                mWidth,
                mHeight,
                1,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface,
                null,
                null
            )
            mMediaCodec = mediaCodec
        } catch (e: IOException) {
            Log.e(TAG, e.toString())
        }

        start()
    }

    override fun run() {
        mMediaCodec.start()
        val bufferInfo = MediaCodec.BufferInfo()
        while (true) {
            try {
                val outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, MEDIACODEC_TIMEOUT_US)
                if (outputBufferIndex > 0) {
                    mMediaCodec.getOutputBuffer(outputBufferIndex)?.let {
                        handleFrame(it, bufferInfo)
                    }
//                    val outputData = ByteArray(bufferInfo.size)
//                    byteBuffer?.get(outputData)
//                    writeContent(outputData)
//                    writeBytes(outputData)

                    mMediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                }

            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                break
            }
        }
    }

    private fun handleFrame(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        var offset = 4
        if (byteBuffer[2].toInt() == 0x01) {
            offset = 3
        }
        val type: Int = (byteBuffer.get(offset) and 0x7E).toInt() shr 1
        when (type) {
            NAL_VPS -> {
                vps_sps_pps_buf = ByteArray(bufferInfo.size)
                byteBuffer.get(vps_sps_pps_buf)
            }
            NAL_I -> {
                val bytes = ByteArray(bufferInfo.size)
                byteBuffer.get(bytes)
                val newBuf = ByteArray(vps_sps_pps_buf.size + bytes.size)
                System.arraycopy(vps_sps_pps_buf, 0, newBuf, 0, vps_sps_pps_buf.size)
                System.arraycopy(bytes, 0, newBuf, vps_sps_pps_buf.size, bytes.size)
                socketLiveServer.sendData(newBuf)
            }
            else -> {
                val bytes = ByteArray(bufferInfo.size)
                byteBuffer.get(bytes)
                socketLiveServer.sendData(bytes)
                Log.d(TAG, "video data" + bytes.contentToString())
            }
        }
    }

    fun writeContent(array: ByteArray): String? {
        val HEX_CHAR_TABLE = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        )
        val sb = StringBuilder()
        for (b in array) {
            sb.append(HEX_CHAR_TABLE[(b and 0xf0.toByte()).toInt() shr 4])
            sb.append(HEX_CHAR_TABLE[(b and 0x0f).toInt()])
        }
        Log.d(TAG, "writeContent: $sb")
        var writer: FileWriter? = null
        try {
            writer = FileWriter(
                Environment.getExternalStorageDirectory().toString() + "/codecH265.txt",
                true
            )
            writer.write(sb.toString())
            writer.write("\n")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return sb.toString()
    }

    fun writeBytes(array: ByteArray) {
        var writer: FileOutputStream? = null
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/codec.h265", true)
            writer.write(array)
            writer.write('\n'.toInt())
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}