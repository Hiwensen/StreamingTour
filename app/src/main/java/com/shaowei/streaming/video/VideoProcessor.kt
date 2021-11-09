package com.shaowei.streaming.video

import android.content.res.AssetFileDescriptor
import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.shaowei.streaming.audio.AudioProcessor
import java.io.File
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.N)
object VideoProcessor {
    private val TAG = VideoProcessor::class.java.simpleName
    private val TRACK_UNKNOWN = -1
    private val SAMPLE_TIME_OFFSET_MICRO_SECONDS = 100000 // 0.1s
    private val TRACK_INDEX_UNFOUND = -1
    private val DEQUEUE_ENQUE_TIME_OUT_US = 100000L
    private val MAX_BUFFER_SIZE_DEFAULT = 100 * 1000
    private val SAMPLE_RATE_DEFAULT = 44100
    private val MIME_PREFIX_VIDEO = "video/"
    private val MIME_PREFIX_AUDIO = "audio/"

    fun clipAndMixVideo(
        sourceAssetFileDescriptor: AssetFileDescriptor, startPositionUs: Long, endPositionUs: Long,
        backgroundMusicFileDescriptor: AssetFileDescriptor, cacheDir: File, mixAudioVideoSuccess: () -> Unit
    ) {
        // Extract and decode original audio to PCM
        val originalAudioPCMFile = File(cacheDir, "originalAudio.pcm")
        AudioProcessor.decodeToPCMSync(
            sourceAssetFileDescriptor,
            originalAudioPCMFile.absolutePath,
            startPositionUs,
            endPositionUs
        )

        // Decode background music to PCM
        val backgroundMusicPCMFile = File(cacheDir, "backgroundAudio.pcm")
        AudioProcessor.decodeToPCMSync(
            backgroundMusicFileDescriptor,
            backgroundMusicPCMFile.absolutePath,
            startPositionUs,
            endPositionUs
        )

        // Mix original audio and background music PCM file
        val mixedPCMFile = File(cacheDir, "mixed.pcm")
        AudioProcessor.mixPCM(
            originalAudioPCMFile.absolutePath, backgroundMusicPCMFile.absolutePath
            , mixedPCMFile.absolutePath, 80, 20
        )

        // mixed pcm -> mp3
        val mixedWavFile = File(cacheDir, "mixed.mp3")
        AudioProcessor.pcmToWav(mixedPCMFile.absolutePath, mixedWavFile.absolutePath)

        // Merge mixed audio and original video
        val mixedVideoFile = File(cacheDir, "mixed.mp4")
        mixVideoAndMusic(
            sourceAssetFileDescriptor,
            mixedVideoFile.absolutePath,
            startPositionUs,
            endPositionUs,
            mixedWavFile,
            mixAudioVideoSuccess
        )
    }

    fun mixVideoAndMusic(
        videoFileDescriptor: AssetFileDescriptor,
        outputVideoPath: String,
        startTimeUs: Long,
        endTimeUs: Long,
        wavFile: File, mixAudioVideoSuccess: () -> Unit
    ) {
        val mediaMuxer = MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Prepare video track
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoFileDescriptor)
        val videoIndex: Int = getTrack(videoExtractor, false)
        val audioIndex: Int = getTrack(videoExtractor, true)
        val videoFormat = videoExtractor.getTrackFormat(videoIndex)
        mediaMuxer.addTrack(videoFormat)

        // Prepare audio track, audioFormat of the mixed video should be the format from the original video
        val audioFormat = videoExtractor.getTrackFormat(audioIndex)
        val audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        val audioIndexOfMixedVideo = mediaMuxer.addTrack(audioFormat)
        mediaMuxer.start()

        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(wavFile.absolutePath)
        val audioTrack = getTrack(audioExtractor, true)
        audioExtractor.selectTrack(audioTrack)
        val pcmTrackFormat = audioExtractor.getTrackFormat(audioTrack)
        var maxBufferSize = if (pcmTrackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            pcmTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            MAX_BUFFER_SIZE_DEFAULT
        }

        // Encode audio from PCM to AAC
        val encodeFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE_DEFAULT, 2
        )
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate) //比特率
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize)
        val aacEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        aacEncoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        aacEncoder.start()

        var buffer = ByteBuffer.allocateDirect(maxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()
        var encodeDone = false
        while (!encodeDone) {
            // Read pcm data and put it to encoder
            val inputBufferIndex = aacEncoder.dequeueInputBuffer(DEQUEUE_ENQUE_TIME_OUT_US)
            if (inputBufferIndex >= 0) {
                val sampleTime = audioExtractor.sampleTime
                if (sampleTime < 0) {
                    // End of the stream
                    aacEncoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    // read pcm date to buffer
                    val flags = audioExtractor.sampleFlags
                    val size = audioExtractor.readSampleData(buffer, 0)
                    val inputBuffer = aacEncoder.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    inputBuffer?.put(buffer)
                    inputBuffer?.position(0)
                    aacEncoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags)
                    audioExtractor.advance()
                }
            }

            // get output data
            var outputBufferIndex = aacEncoder.dequeueOutputBuffer(bufferInfo, DEQUEUE_ENQUE_TIME_OUT_US)
            while (outputBufferIndex >= 0) {
                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    encodeDone = true
                    break
                }

                val encodeOutputBuffer = aacEncoder.getOutputBuffer(outputBufferIndex)
                encodeOutputBuffer?.apply {
                    // Write the encoded aac audio date to mediaMuxer
                    mediaMuxer.writeSampleData(audioIndexOfMixedVideo, this, bufferInfo)
                    encodeOutputBuffer.clear()
                }
                aacEncoder.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = aacEncoder.dequeueOutputBuffer(bufferInfo, DEQUEUE_ENQUE_TIME_OUT_US)
            }
        }

        if (audioTrack >= 0) {
            videoExtractor.unselectTrack(audioTrack)
        }

        //start to write video date to mediaMuxer
        videoExtractor.selectTrack(videoIndex)
        videoExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        buffer = ByteBuffer.allocateDirect(maxBufferSize)

        while (true) {
            val sampleTimeUs = videoExtractor.sampleTime
            if (sampleTimeUs == -1L) {
                break
            }
            if (sampleTimeUs < startTimeUs) {
                videoExtractor.advance()
                continue
            }
            if (sampleTimeUs > endTimeUs) {
                break
            }

            //Set the new time
            bufferInfo.presentationTimeUs = sampleTimeUs - startTimeUs + 600
            bufferInfo.flags = videoExtractor.sampleFlags
            //                读取视频文件的数据  画面 数据   压缩1  未压缩2
            bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) {
                break
            }
            mediaMuxer.writeSampleData(videoIndex, buffer, bufferInfo)
            videoExtractor.advance()
        }

        try {
            audioExtractor.release()
            videoExtractor.release()
            aacEncoder.stop()
            aacEncoder.release()
            mediaMuxer.release()
            Log.d(TAG, "mix audio video success")
            mixAudioVideoSuccess.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "mix audio video fail")
        }
    }

    fun mixVideoAudio(
        videoPath: String,
        audioPath: String,
        outputVideoPath: String,
        mixAudioVideoSuccess: () -> Unit
    ) {
        val mediaMuxer = MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Prepare video track
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoPath)
        val videoIndex: Int = getTrack(videoExtractor, false)
        videoExtractor.selectTrack(videoIndex)
        val videoFormat = videoExtractor.getTrackFormat(videoIndex)
        val videoMaxBufferSize = if (videoFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            MAX_BUFFER_SIZE_DEFAULT
        }
        val videoTrackIndex = mediaMuxer.addTrack(videoFormat)

        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(audioPath)
        val audioTrack = getTrack(audioExtractor, true)
        audioExtractor.selectTrack(audioTrack)
        val audioFormat = audioExtractor.getTrackFormat(audioTrack)
        val audioMaxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            MAX_BUFFER_SIZE_DEFAULT
        }
        val audioTrackIndex = mediaMuxer.addTrack(audioFormat)
        mediaMuxer.start()

        val videoBuffer = ByteBuffer.allocate(videoMaxBufferSize)
        val bufferInfo = MediaCodec.BufferInfo()

        //start to write video date to mediaMuxer
        while (true) {
            val sampleTimeUs = videoExtractor.sampleTime
            if (sampleTimeUs == -1L) {
                break
            }

            //Set the new time
            bufferInfo.presentationTimeUs = sampleTimeUs
            bufferInfo.flags = videoExtractor.sampleFlags
            bufferInfo.size = videoExtractor.readSampleData(videoBuffer, 0)
            if (bufferInfo.size < 0) {
                break
            }
            mediaMuxer.writeSampleData(videoTrackIndex, videoBuffer, bufferInfo)
            videoExtractor.advance()
        }

        val audioBuffer = ByteBuffer.allocate(audioMaxBufferSize)
        while (true) {
            val sampleTimeUs = audioExtractor.sampleTime
            if (sampleTimeUs == -1L) {
                break
            }

            //Set the new time
            bufferInfo.presentationTimeUs = sampleTimeUs
            bufferInfo.flags = audioExtractor.sampleFlags
            bufferInfo.size = audioExtractor.readSampleData(audioBuffer, 0)
            if (bufferInfo.size < 0) {
                break
            }
            mediaMuxer.writeSampleData(audioTrackIndex, audioBuffer, bufferInfo)
            audioExtractor.advance()
        }

        try {
            audioExtractor.release()
            videoExtractor.release()
            mediaMuxer.release()
            Log.d(TAG, "mix audio video success")
            mixAudioVideoSuccess.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "mix audio video fail")
        }
    }

    fun getTrack(mediaExtractor: MediaExtractor, audio: Boolean): Int {
        val trackCount = mediaExtractor.trackCount
        for (i in 0 until trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(MIME_PREFIX_VIDEO) == true && !audio) {
                return i
            }

            if (mime?.startsWith(MIME_PREFIX_AUDIO) == true && audio) {
                return i
            }
        }

        return TRACK_UNKNOWN
    }

}