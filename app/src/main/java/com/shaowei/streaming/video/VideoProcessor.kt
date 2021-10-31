package com.shaowei.streaming.video

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.*
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs

class VideoProcessor {
    private val TAG = VideoProcessor::class.java.simpleName
    private val TRACK_UNKNOWN = -1
    private val SAMPLE_TIME_OFFSET_MICRO_SECONDS = 100000 // 0.1s
    private val AUDIO_TRACK_INDEX_UNFOUND = -1

    private lateinit var mMediaCodec: MediaCodec

    @RequiresApi(Build.VERSION_CODES.N)
    fun clipAndMixVideo(
        context: Context, sourceAssetFileDescriptor: AssetFileDescriptor, startPositionUs: Long, endPositionUs: Long,
        backgroundMusicFileDescriptor: AssetFileDescriptor, cacheDir: File) {
        // Extract and decode original audio to PCM
        val originalAudioPCMFile = File(cacheDir, "originalAudio.pcm")
        decodeAudioFileToPCMAsync(
            context,
            sourceAssetFileDescriptor,
            originalAudioPCMFile.absolutePath,
            startPositionUs,
            endPositionUs)


        // Decode background music to PCM
        val backgroundMusicPCMFile = File(cacheDir, "backgroundAudio.pcm")
        decodeAudioFileToPCMAsync(
            context,
            backgroundMusicFileDescriptor,
            backgroundMusicPCMFile.absolutePath,
            startPositionUs,
            endPositionUs)

        // Mix original audio and background music PCM file


        // Merge mixed audio and original video

    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun decodeAudioFileToPCMAsync(context: Context, sourceFileDescriptor: AssetFileDescriptor, desParentFilePath:String
                                  , startTimeUs: Long, endTimeUs: Long): Boolean {
        if (endTimeUs < startTimeUs) {
            Log.e(TAG, "endTime should greater than startTime")
            return false
        }

        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(sourceFileDescriptor)
        val audioTrackIndex = getTrack(mediaExtractor, true)
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

        val pcmFile = File(desParentFilePath)
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

                    Toast.makeText(context,"clip video success", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "clip audio success,file path:$desParentFilePath")
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

                            Log.d(TAG, "onInputBufferAvailable, index:$index, size:$size, " +
                                    "sampleTime:${sampleTimeUs}, flags:$flags")
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

    private fun decodeAudioFileToPCMSync(
        context: Context,
        originalFilePath: String, startTimeUS: Int, endTimeUs: Int,
        outputPCMFilePath: String) {

    }

    private fun getTrack(mediaExtractor: MediaExtractor, audio: Boolean): Int {
        val trackCount = mediaExtractor.trackCount
        for (i in 0 until trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("video/") == true && !audio) {
                return i
            }

            if (mime?.startsWith("audio/") == true && audio) {
                return i
            }
        }

        return TRACK_UNKNOWN
    }

    private fun mixPcmFiles(originalAudioFilePath:String,originalVolumeWeight:Int,
                            backgroundFilePath:String, backgroundVolumeWeight:Int,
                            mixedPcmFilePath:String) {

    }

    private fun mixVideoAudio() {

    }
}