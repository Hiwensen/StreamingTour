package com.shaowei.streaming.mediacodec

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.nio.ByteBuffer

class H264Player {
    private val TAG = H264Player::class.java.simpleName
    private lateinit var mMediaCodec: MediaCodec
    private val VIDEO_TYPE_H264 = "video/avc"
    private val FIND_NEXT_FRAME_OFFSET = 1
    private val BUFFER_CAPACITY = 500 * 1024 //500kb
    private var mOutputFormat: MediaFormat? = null
    private val mContentWidth = 368
    private val mContentHeight = 384
    private val CODEC_DEQUEUE_TIMEOUT_US = 10000L
    private var mCodecStatus = CodecStatus.IDLE

    fun playAsync(rawFileId: Int, targetSurface: Surface, context: Context) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(VIDEO_TYPE_H264)
        } catch (e: Exception) {
            Toast.makeText(context,"fail to create codec",Toast.LENGTH_SHORT).show()
            return
        }

        mCodecStatus = CodecStatus.WORKING
        val bytes = getBytes(context, rawFileId)
        val sourceFileSize = bytes.size
        Log.d(TAG, "sourceFileSize:$sourceFileSize")
        var startIndex = 0

        mMediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                if (index >= 0) {
                    codec.releaseOutputBuffer(index, true)
                }
                if (info.flags == BUFFER_FLAG_END_OF_STREAM) {
                    mCodecStatus = CodecStatus.IDLE
                    Log.d(TAG, "codec finish")
                }
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                Log.d(TAG, "onInputBufferAvailable,index:$index")
                codec.getInputBuffer(index)?.let {
                    it.clear()
                    val nextFrameStartPosition =
                        findNextFrameStartPosition(bytes, startIndex + FIND_NEXT_FRAME_OFFSET, sourceFileSize)
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

    fun playMP4Video(rawFileId: Int, targetSurface: Surface, context: Context) {
        mCodecStatus = CodecStatus.WORKING
        val bytes = getVideoBytesFromMp4(context, rawFileId)
        val videoFormat = getVideoFormat(context, rawFileId)
        val mime = videoFormat.getString(MediaFormat.KEY_MIME)
        Log.d(TAG, "playMP4Video, mime:$mime")
        if (mime.isNullOrEmpty()) {
            Toast.makeText(context, "fail to get mime", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            mMediaCodec = MediaCodec.createDecoderByType(mime)
        } catch (e: Exception) {
            Toast.makeText(context,"fail to create codec",Toast.LENGTH_SHORT).show()
            return
        }

        val sourceFileSize = bytes.size
        Log.d(TAG, "sourceFileSize:$sourceFileSize")
        var startIndex = 0

        mMediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                if (index >= 0) {
                    codec.releaseOutputBuffer(index, true)
                }
                if (info.flags == BUFFER_FLAG_END_OF_STREAM) {
                    mCodecStatus = CodecStatus.FINISHED
                    Log.d(TAG, "decode finish")
                }
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                codec.getInputBuffer(index)?.let {
                    it.clear()
                    val nextFrameStartPosition =
                        findNextFrameStartPosition(bytes, startIndex + FIND_NEXT_FRAME_OFFSET, sourceFileSize)
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

        mMediaCodec.configure(videoFormat, targetSurface, null, 0)
        mOutputFormat = mMediaCodec.outputFormat
        mMediaCodec.start()
    }

    fun playSync(rawFileId: Int, targetSurface: Surface, context: Context) {
        try {
            mMediaCodec = MediaCodec.createDecoderByType(VIDEO_TYPE_H264)
        } catch (e: Exception) {
            Toast.makeText(context,"fail to create codec",Toast.LENGTH_SHORT).show()
            return
        }

        mCodecStatus = CodecStatus.WORKING
        val bytes = getBytes(context, rawFileId)
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
                val nextFrameStartPosition =
                    findNextFrameStartPosition(bytes, startIndex + FIND_NEXT_FRAME_OFFSET, sourceFileSize)
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
    }

    fun release() {
        mMediaCodec.release()
    }

    @Throws(IOException::class)
    private fun getBytes(context: Context, rawFileId: Int): ByteArray {
        val `is`: InputStream = context.resources.openRawResource(rawFileId)
        var len: Int
        val size = 1024
        val byteArray = ByteArray(size)
        val byteArrayOutputStream = ByteArrayOutputStream()
        while (`is`.read(byteArray, 0, size).also { len = it } != -1) byteArrayOutputStream.write(byteArray, 0, len)
        return byteArrayOutputStream.toByteArray()
    }

    @Throws(IOException::class)
    private fun getVideoBytesFromMp4(context: Context, rawFileId: Int): ByteArray {
        // the mediaExtractor must be recreated,
        // Otherwise crash happen if extractor.setDataSource() be executed multiple times
        val extractor = MediaExtractor()

        val rawResourceFd = context.resources.openRawResourceFd(rawFileId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extractor.setDataSource(rawResourceFd)
        }
        val trackCount = extractor.trackCount
        var videoTrackIndex = -1
        for (i in 0 until trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val formatString = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "format string:$formatString")

            if (formatString?.startsWith("video/") == true) {
                videoTrackIndex = i
                Log.d(TAG, "video track:$videoTrackIndex")
            }
        }

        val inputBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
        val mVideoOutputStream = ByteArrayOutputStream()
        try {
            // select video track and write video stream
            extractor.selectTrack(videoTrackIndex)
            while (true) {
                val readSampleData = extractor.readSampleData(inputBuffer, 0)
                if (readSampleData < 0) {
                    break
                }

                val byteArray = ByteArray(readSampleData)
                inputBuffer.get(byteArray)
                mVideoOutputStream.write(byteArray)
                inputBuffer.clear()
                extractor.advance()
            }
            mVideoOutputStream.close()
        } catch (ioException: IOException) {
            Log.e(TAG, "extractRawFileNPlay,${ioException}")
        } finally {
            extractor.release()
        }

        val `is`: InputStream = context.resources.openRawResource(rawFileId)
        var len: Int
        val size = 1024
        val byteArray = ByteArray(size)
//        val byteArrayOutputStream = ByteArrayOutputStream()
        while (`is`.read(byteArray, 0, size).also { len = it } != -1) mVideoOutputStream.write(byteArray, 0, len)
        return mVideoOutputStream.toByteArray()
    }

    private fun getVideoFormat(context: Context, rawFileId: Int): MediaFormat {
        val extractor = MediaExtractor()

        val rawResourceFd = context.resources.openRawResourceFd(rawFileId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extractor.setDataSource(rawResourceFd)
        }
        val trackCount = extractor.trackCount
        var videoTrackIndex: Int
        var videoFormat = MediaFormat()
        for (i in 0 until trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val formatString = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "format string:$formatString")

            if (formatString?.startsWith("video/") == true) {
                videoTrackIndex = i
                videoFormat = mediaFormat
                Log.d(TAG, "video track:$videoTrackIndex")
            }
        }

        return videoFormat
    }


    private fun findNextFrameStartPosition(bytes: ByteArray, start: Int, totalSize: Int): Int {
        // In the h264 stream, the separator between each frame is 0x00 00 00 01
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

    private fun findCodecNameForFormat(encoder: Boolean, format: MediaFormat): String {
        val mime = format.getString(MediaFormat.KEY_MIME)
        val mediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codecInfos = mediaCodecList.codecInfos
        for (codecInfo in codecInfos) {
            if (codecInfo.isEncoder != encoder) {
                continue
            }

            val capabilitiesForType = codecInfo.getCapabilitiesForType(mime)
            if (capabilitiesForType != null && capabilitiesForType.isFormatSupported(format)) {
                return codecInfo.name
            }
        }

        return ""
    }

    enum class CodecStatus {
        IDLE,
        WORKING,
        FINISHED
    }
}