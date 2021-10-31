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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.experimental.and
import kotlin.experimental.or

@RequiresApi(Build.VERSION_CODES.N)
class AudioMixer {
    private val TRACK_INDEX_UNFOUND = -1
    private val TAG = AudioMixer::class.java.simpleName
    private val DEQUEUE_ENQUE_TIME_OUT_US = 100000L
    private val AUDIO_FORMAT_PREFIX = "audio/"

    fun mixAudioTrack(
        originalAudioFileDescriptor: AssetFileDescriptor, musicFileDescriptor: AssetFileDescriptor,
        cacheDir: File, startTimeUs: Long, endTimeUs: Long, originalVideoVolume: Int, musicVolume: Int
    ) {
        val originalAudioPCMFile = File(cacheDir, "audio.pcm")
        val musicPCMFile = File(cacheDir, "music.pcm")
        val mixedPCMFile = File(cacheDir, "mixed.pcm")
        val mixedMp3File = File(cacheDir, "mixed.mp3")

        decodeToPCM(originalAudioFileDescriptor, originalAudioPCMFile.absolutePath, startTimeUs, endTimeUs)
        decodeToPCM(musicFileDescriptor, musicPCMFile.absolutePath, startTimeUs, endTimeUs)
        mixPCM(
            originalAudioPCMFile.absolutePath, musicPCMFile.absolutePath, mixedPCMFile.absolutePath,
            originalVideoVolume,
            musicVolume
        )

        PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            .pcmToWav(mixedPCMFile.absolutePath, mixedMp3File.absolutePath)
    }

    private fun decodeToPCM(fileDescriptor: AssetFileDescriptor, pcmFilePath: String, startTimeUs: Long, endTimeUs:
    Long):Boolean {
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

    private fun mixPCM(
        movieAudioPCMFilePath: String, musicPCMFilePath: String, mixedPCMFilePath: String,
        movieAudioVolume: Int,
        musicVolume: Int
    ) {
        val volMovieAudio: Float = movieAudioVolume / 100f * 1
        val volMusic: Float = musicVolume / 100f * 1

        val bufferMovieAudio = ByteArray(2048)
        val bufferMusic = ByteArray(2048)
        val bufferMixed = ByteArray(2048)

        val inputStreamMovieAudio = FileInputStream(movieAudioPCMFilePath)
        val inputStreamMusic = FileInputStream(musicPCMFilePath)

        val outputStreamMixedPCM = FileOutputStream(mixedPCMFilePath)
        var tempMusic: Short
        var tempMovieAudio: Short
        var temp: Int

        var movieAudioSteamEnd = false
        var musicStreamEnd = false
        while (!movieAudioSteamEnd || !musicStreamEnd) {
            if (!movieAudioSteamEnd) {
                movieAudioSteamEnd = inputStreamMovieAudio.read(bufferMovieAudio) == -1
                // write the movie audio data to buffer first
                System.arraycopy(bufferMovieAudio, 0, bufferMixed, 0, bufferMovieAudio.size)
            }

            if (!musicStreamEnd) {
                musicStreamEnd = inputStreamMusic.read(bufferMusic) == -1
                // one voice value is two bytes
                var i = 0

                while (i < bufferMusic.size) {
                    tempMovieAudio =
                        (((bufferMovieAudio[i] and 0xff.toByte()) or (bufferMovieAudio[i + 1] and 0xff.toByte())).toLong() shl 8)
                            .toShort()
                    tempMusic =
                        (((bufferMusic[i] and 0xff.toByte() or (bufferMusic[i + 1] and 0xff.toByte())).toLong() shl 8))
                            .toShort()

                    temp = (tempMovieAudio * volMovieAudio + tempMusic * volMusic).toInt()

                    if (temp > Short.MAX_VALUE) {
                        temp = Short.MAX_VALUE.toInt()
                    } else if (temp < Short.MIN_VALUE) {
                        temp = Short.MIN_VALUE.toInt()
                    }

                    bufferMixed[i] = (temp and 0xFF).toByte()
                    bufferMixed[i + 1] = (temp ushr 8 and 0xFF).toByte()
                    i += 2
                }
                outputStreamMixedPCM.write(bufferMixed)
            }
        }
        Log.d(TAG,"mix pcm success")
        inputStreamMovieAudio.close()
        inputStreamMusic.close()
        outputStreamMixedPCM.close()
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