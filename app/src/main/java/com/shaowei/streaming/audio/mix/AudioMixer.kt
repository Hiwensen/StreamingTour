package com.shaowei.streaming.audio.mix

import android.content.res.AssetFileDescriptor
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.shaowei.streaming.audio.PcmToWavUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.N)
class AudioMixer {
    private val TRACK_INDEX_UNFOUND = -1
    private val TAG = AudioMixer::class.java.simpleName
    private val DEQUEUE_ENQUE_TIME_OUT_US = 100000L
    private val AUDIO_FORMAT_PREFIX = "audio/"

    suspend fun mixAudioTrack(
        originalAudioFileDescriptor: AssetFileDescriptor, musicFileDescriptor: AssetFileDescriptor,
        cacheDir: File, startTimeUs: Long, endTimeUs: Long, originalVideoVolume: Int, musicVolume: Int
    ) = withContext(Dispatchers.IO) {


        val mixedPCMFile = File(cacheDir, "mixed.pcm")
        val mixedMp3File = File(cacheDir, "mixed.mp3")

        val originalAudioPcmFile = async {
            val originalAudioPCMFile = File(cacheDir, "audio.pcm")
            decodeToPCM(originalAudioFileDescriptor, originalAudioPCMFile.absolutePath, startTimeUs, endTimeUs)
            originalAudioPCMFile
        }

        val musicPcmFile = async {
            val musicPCMFile = File(cacheDir, "music.pcm")
            decodeToPCM(musicFileDescriptor, musicPCMFile.absolutePath, startTimeUs, endTimeUs)
            musicPCMFile
        }

        mixPCM(
            originalAudioPcmFile.await().absolutePath, musicPcmFile.await().absolutePath, mixedPCMFile.absolutePath,
            originalVideoVolume,
            musicVolume
        )

        PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            .pcmToWav(mixedPCMFile.absolutePath, mixedMp3File.absolutePath)
    }

    private suspend fun decodeToPCM(
        fileDescriptor: AssetFileDescriptor, pcmFilePath: String, startTimeUs: Long, endTimeUs:
        Long
    ): Boolean = withContext(Dispatchers.IO) {
        if (endTimeUs < startTimeUs) {
            Log.e(TAG, "endTime should greater than startTime")
            return@withContext false
        }

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(fileDescriptor)
        val audioTrackIndex = getAudioTrackIndex(mediaExtractor)
        if (audioTrackIndex == TRACK_INDEX_UNFOUND) {
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
        return@withContext true
    }

    /**
     * @param videoVolume value is 0 ~ 100
     * @param bgMusicVolume value is 0 ~ 100
     */
    private suspend fun mixPCM(
        pcm1: String,
        pcm2: String,
        mixPcm: String,
        videoVolume: Int,
        bgMusicVolume: Int
    ) = withContext(Dispatchers.IO) {
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
        Log.d(TAG, "mixPcm:$mixPcm")
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
}