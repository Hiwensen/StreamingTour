package com.shaowei.streaming.audio.clip

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.shaowei.streaming.audio.PcmToWavUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

class AudioClipper {
    private val TAG = AudioClipper::class.java.simpleName
    private val AUDIO_FORMAT_PREFIX = "audio/"
    private val AUDIO_TRACK_INDEX_UNFOUND = -1
    private val DEQUEUE_ENQUE_TIME_OUT_US = 100000L
    private val SAMPLE_RATE = 44100
    private val SAMPLE_TIME_OFFSET_MICRO_SECONDS = 100000 // 0.1s
    private var mQuitPlayback = false

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("WrongConstant")
    suspend fun clipSync(
        sourceFilePath: AssetFileDescriptor,
        desParentFilePath: String,
        startTimeUs: Long,
        endTimeUs: Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (endTimeUs < startTimeUs) {
            Log.e(TAG, "endTime should greater than startTime")
            return@withContext false
        }

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(sourceFilePath)
        val audioTrackIndex = getAudioTrackIndex(mediaExtractor)
        if (audioTrackIndex == AUDIO_TRACK_INDEX_UNFOUND) {
            Log.e(TAG, "failed to find audio track index")
            return@withContext false
        }

        mediaExtractor.selectTrack(audioTrackIndex)
        mediaExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val oriAudioTrackFormat = mediaExtractor.getTrackFormat(audioTrackIndex)
        var maxBufferSize = 100 * 1000 // 100k
        if (oriAudioTrackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = oriAudioTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        }

        val byteBuffer = ByteBuffer.allocateDirect(maxBufferSize)

        val mediaCodec = MediaCodec.createDecoderByType(oriAudioTrackFormat.getString(MediaFormat.KEY_MIME) ?: "")
        // Pass CONFIGURE_FLAG_ENCODE to flags if the mediaCodec is used as an encoder
        mediaCodec.configure(oriAudioTrackFormat, null, null, 0)
        val pcmFile = File(desParentFilePath, "output.pcm")
        val fileChannel = FileOutputStream(pcmFile).channel
        mediaCodec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        var outputBufferIndex: Int
        while (true) {
            val dequeueInputIndex = mediaCodec.dequeueInputBuffer(DEQUEUE_ENQUE_TIME_OUT_US)
            if (dequeueInputIndex >= 0) {
                val sampleTimeUs = mediaExtractor.sampleTime
                if (sampleTimeUs == -1L) {
                    break
                } else if (sampleTimeUs < startTimeUs) {
                    // discard data
                    mediaExtractor.advance()
                    continue
                } else if (sampleTimeUs > endTimeUs) {
                    break
                }

                // Get the data between startTime and endTime
                bufferInfo.size = mediaExtractor.readSampleData(byteBuffer, 0)
                bufferInfo.presentationTimeUs = sampleTimeUs
                bufferInfo.flags = mediaExtractor.sampleFlags

                val content = ByteArray(byteBuffer.remaining())
                byteBuffer.get(content)

                // Load the data to mediaCodec
                val inputBuffer = mediaCodec.getInputBuffer(dequeueInputIndex)
                inputBuffer?.put(content)
                mediaCodec.queueInputBuffer(
                    dequeueInputIndex, 0, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags
                )
                mediaExtractor.advance()
            }

            // Get the decoded data
            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_ENQUE_TIME_OUT_US)
            while (outputBufferIndex >= 0) {
                val decodeOutputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex)
                fileChannel.write(decodeOutputBuffer)
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DEQUEUE_ENQUE_TIME_OUT_US)
            }
        }

        fileChannel.close()
        mediaExtractor.release()
        mediaCodec.stop()
        mediaCodec.release()

        // pcm -> mp3
        val wavFile = File(desParentFilePath, "output.mp3")
        PcmToWavUtil(
            SAMPLE_RATE, channelConfig = AudioFormat.CHANNEL_IN_STEREO, audioFormat = AudioFormat
                .ENCODING_PCM_16BIT
        ).pcmToWav(pcmFile.absolutePath, wavFile.absolutePath)

        Log.d(TAG, "clip audio success,file path:${wavFile.absolutePath}")
        return@withContext true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun clipAsync(
        context: Context, sourceFilePath: AssetFileDescriptor, desParentFilePath: String, startTimeUs: Long,
        endTimeUs: Long
    ): Boolean {
        if (endTimeUs < startTimeUs) {
            Log.e(TAG, "endTime should greater than startTime")
            return false
        }

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(sourceFilePath)
        val audioTrackIndex = getAudioTrackIndex(mediaExtractor)
        if (audioTrackIndex == AUDIO_TRACK_INDEX_UNFOUND) {
            Log.e(TAG, "failed to find audio track index")
            return false
        }

        mediaExtractor.selectTrack(audioTrackIndex)
        mediaExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        val oriAudioTrackFormat = mediaExtractor.getTrackFormat(audioTrackIndex)
        var maxBufferSize = 100 * 1000 // 100k
        if (oriAudioTrackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            maxBufferSize = oriAudioTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        }

        val byteBuffer = ByteBuffer.allocateDirect(maxBufferSize)

        val mediaCodec = MediaCodec.createDecoderByType(oriAudioTrackFormat.getString(MediaFormat.KEY_MIME) ?: "")
        // Pass CONFIGURE_FLAG_ENCODE to flags if the mediaCodec is used as an encoder
        mediaCodec.configure(oriAudioTrackFormat, null, null, 0)

        val pcmFile = File(desParentFilePath, "output.pcm")
        val fileChannel = FileOutputStream(pcmFile).channel

        mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                if (mQuitPlayback) return

                Log.d(TAG, "onOutputBufferAvailable, index:$index, flags:${info.flags}")
                if (index >= 0) {
                    val decodeOutputBuffer = codec.getOutputBuffer(index)
                    fileChannel.write(decodeOutputBuffer)
                    codec.releaseOutputBuffer(index, false)
                }

                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.d(TAG, "onOutputBufferAvailable, end of stream")
                    fileChannel.close()
                    mediaExtractor.release()

                    // pcm -> mp3
                    val wavFile = File(desParentFilePath, "output.mp3")
                    PcmToWavUtil(
                        SAMPLE_RATE, channelConfig = AudioFormat.CHANNEL_IN_STEREO, audioFormat = AudioFormat
                            .ENCODING_PCM_16BIT
                    ).pcmToWav(pcmFile.absolutePath, wavFile.absolutePath)

                    Toast.makeText(context, "clip video success", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "clip audio success,file path:${wavFile.absolutePath}")
                }
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                val sampleTimeUs = mediaExtractor.sampleTime
                when {
                    sampleTimeUs == -1L -> {
                        return
                    }
                    sampleTimeUs < startTimeUs -> {
                        // discard data
                        mediaExtractor.advance()
                        return
                    }
                    sampleTimeUs > endTimeUs -> {
                        return
                    }

                    // Get the data between startTime and endTime
                    // Load the data to mediaCodec
                    else -> {
                        codec.getInputBuffer(index)?.let {
                            it.clear()

                            val size = mediaExtractor.readSampleData(byteBuffer, 0)
                            val flags = if (abs(sampleTimeUs - endTimeUs) < SAMPLE_TIME_OFFSET_MICRO_SECONDS) {
                                Log.d(TAG, "set end of stream flag")
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            } else {
                                mediaExtractor.sampleFlags
                            }

                            val content = ByteArray(byteBuffer.remaining())
                            byteBuffer.get(content)
                            it.put(content)

                            Log.d(
                                TAG, "onInputBufferAvailable, index:$index, size:$size, " +
                                        "sampleTime:${sampleTimeUs}, flags:$flags"
                            )
                            codec.queueInputBuffer(index, 0, size, sampleTimeUs, flags)
                            mediaExtractor.advance()
                        }
                    }
                }

            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "onOutputFormatChanged:${format.getString(MediaFormat.KEY_MIME)}")
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                Log.e(TAG, "mediaCodec error:$e")
            }

        })

        mediaCodec.start()
        return true
    }

    private fun getAudioTrackIndex(mediaExtractor: MediaExtractor): Int {
        for (i in 0 until mediaExtractor.trackCount) {
            val mediaFormat = mediaExtractor.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "mime:$mime")
            if (mime?.startsWith(AUDIO_FORMAT_PREFIX) == true) {
                return i
            }
        }
        return AUDIO_TRACK_INDEX_UNFOUND
    }

}