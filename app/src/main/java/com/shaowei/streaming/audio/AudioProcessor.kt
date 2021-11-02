package com.shaowei.streaming.audio

import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.N)
object AudioProcessor {
    private val TAG = AudioProcessor::class.java.simpleName
    private val TRACK_INDEX_UNFOUND = -1
    private val DEQUEUE_ENQUE_TIME_OUT_US = 100000L
    private val AUDIO_FORMAT_PREFIX = "audio/"
    private val SAMPLE_TIME_OFFSET_MICRO_SECONDS = 100000 // 0.1s
    private val SAMPLE_RATE = 44100

    fun decodeToPCMSync(
        fileDescriptor: AssetFileDescriptor, pcmFilePath: String, startTimeUs: Long, endTimeUs: Long
    ): Boolean {
        if (endTimeUs < startTimeUs) {
            Log.e(TAG, "endTime should greater than startTime")
            return false
        }

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(fileDescriptor)
        val audioTrackIndex = getAudioTrackIndex(mediaExtractor)
        if (audioTrackIndex == TRACK_INDEX_UNFOUND) {
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
        val pcmFile = File(pcmFilePath)
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

        Log.d(TAG, "decode pcm file success")
        return true
    }

    fun decodeToPCMAsync(
        sourceFilePath: AssetFileDescriptor,
        pcmFilePath: String,
        startTimeUs: Long,
        endTimeUs: Long
    ): Boolean {
        if (endTimeUs < startTimeUs) {
            Log.e(TAG, "endTime should greater than startTime")
            return false
        }

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(sourceFilePath)
        val audioTrackIndex = getAudioTrackIndex(mediaExtractor)
        if (audioTrackIndex == TRACK_INDEX_UNFOUND) {
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

        val pcmFile = File(pcmFilePath)
        val fileChannel = FileOutputStream(pcmFile).channel

        mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
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

                    Log.d(TAG, "decode pcm file success,file path:$pcmFilePath")
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

    /**
     * @param videoVolume value is 0 ~ 100
     * @param bgMusicVolume value is 0 ~ 100
     */
    fun mixPCM(
        pcm1: String,
        pcm2: String,
        mixPcm: String,
        videoVolume: Int,
        bgMusicVolume: Int
    ) {
        val volume1: Float = videoVolume * 1.0f / 100
        val volume2: Float = bgMusicVolume * 1.0f / 100
        val buffSize = 2048
        val buffer1 = ByteArray(buffSize)
        val buffer2 = ByteArray(buffSize)
        val buffer3 = ByteArray(buffSize)
        val fis1 = FileInputStream(pcm1)
        val fis2 = FileInputStream(pcm2)
        val fosMix = FileOutputStream(mixPcm)
        var isEnd1 = false
        var isEnd2 = false
        var temp1: Short
        var temp2: Short
        var tempMixed: Int
        while (!isEnd1 || !isEnd2) {
            if (!isEnd1) {
                isEnd1 = fis1.read(buffer1) == -1
            }
            if (!isEnd2) {
                isEnd2 = fis2.read(buffer2) == -1
                for (i in buffer2.indices step 2) {
                    // java version
                    // temp1 = (short) ((buffer1[i] & 0xff) | (buffer1[i + 1] & 0xff) << 8);
                    // temp2 = (short) ((buffer2[i] & 0xff) | (buffer2[i + 1] & 0xff) << 8);

                    temp1 = ((buffer1[i].toInt() and 0xff) or ((buffer1[i + 1].toInt() and 0xff) shl 8)).toShort()
                    temp2 = ((buffer2[i].toInt() and 0xff) or ((buffer2[i + 1].toInt() and 0xff) shl 8)).toShort()

                    // The sum of two short values may be greater than short
                    tempMixed = (temp1 * volume1 + temp2 * volume2).toInt()
                    // The range of short value is [-32768 ~ 32767]
                    if (tempMixed > Short.MAX_VALUE) {
                        tempMixed = Short.MAX_VALUE.toInt()
                    } else if (tempMixed < Short.MIN_VALUE) {
                        tempMixed = Short.MIN_VALUE.toInt()
                    }

                    // java version
                    // buffer3[i] = (byte) (temp & 0x00ff);
                    // buffer3[i + 1] = (byte) ((temp & 0xFF00) >> 8 );
                    buffer3[i] = (tempMixed and 0x00ff).toByte()
                    buffer3[i + 1] = (tempMixed and 0xff00).shr(8).toByte()
                }
                fosMix.write(buffer3)
            }
        }
        fis1.close()
        fis2.close()
        fosMix.flush()
        fosMix.close()
        Log.d(TAG, "mix pcm success:$mixPcm")
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
        return TRACK_INDEX_UNFOUND
    }

    fun pcmToWav(pcmFilePath: String, wavFilePath: String) {
        PcmToWavUtil(
            SAMPLE_RATE, channelConfig = AudioFormat.CHANNEL_IN_STEREO, audioFormat = AudioFormat
                .ENCODING_PCM_16BIT
        ).pcmToWav(pcmFilePath, wavFilePath)
        Log.d(TAG, "pcm to wav success")
    }
}