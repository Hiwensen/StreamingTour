package com.shaowei.streaming.video

import android.content.res.AssetFileDescriptor
import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.shaowei.streaming.audio.AudioProcessor
import com.shaowei.streaming.mediaExtractor.MediaExtractorActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.N)
object VideoProcessor : CoroutineScope by CoroutineScope(Dispatchers.Default) {
    private val TAG = VideoProcessor::class.java.simpleName
    private val TRACK_UNKNOWN = -1
    private val SAMPLE_TIME_OFFSET_MICRO_SECONDS = 100000 // 0.1s
    private val TRACK_INDEX_UNFOUND = -1
    private val DEQUEUE_ENQUE_TIME_OUT_US = 100000L
    private val MAX_BUFFER_SIZE_DEFAULT = 100 * 1000
    private val SAMPLE_RATE_DEFAULT = 44100
    private val MIME_PREFIX_VIDEO = "video/"
    private val MIME_PREFIX_AUDIO = "audio/"

    suspend fun clipAndMixVideo(
        sourceAssetFileDescriptor: AssetFileDescriptor, startPositionUs: Long, endPositionUs: Long,
        backgroundMusicFileDescriptor: AssetFileDescriptor, cacheDir: File
    ) = withContext(Dispatchers.IO) {
        val originalAudioPCMFile = async {
            // Extract and decode original audio to PCM
            val originalAudioPCMFile = File(cacheDir, "originalAudio.pcm")
            AudioProcessor.decodeToPCMSync(
                sourceAssetFileDescriptor,
                originalAudioPCMFile.absolutePath,
                startPositionUs,
                endPositionUs
            )
            originalAudioPCMFile
        }

        val backgroundMusicPCMFile = async { // Decode background music to PCM
            val backgroundMusicPCMFile = File(cacheDir, "backgroundAudio.pcm")
            AudioProcessor.decodeToPCMSync(
                backgroundMusicFileDescriptor,
                backgroundMusicPCMFile.absolutePath,
                startPositionUs,
                endPositionUs
            )
            backgroundMusicPCMFile
        }

        // Mix original audio and background music PCM file
        val mixedPCMFile = File(cacheDir, "mixed.pcm")
        AudioProcessor.mixPCM(
            originalAudioPCMFile.await().absolutePath, backgroundMusicPCMFile.await().absolutePath
            , mixedPCMFile.absolutePath, 80, 20
        )

        // mixed pcm -> mp3
        val mixedWavFile = File(cacheDir, "mixed.mp3")
        AudioProcessor.pcmToWav(mixedPCMFile.absolutePath, mixedWavFile.absolutePath)


        // Merge mixed audio and original video
//        val mixedVideoFile = File(cacheDir, "mixed.mp4")
//        mixVideoAndMusic(
//            sourceAssetFileDescriptor,
//            mixedVideoFile.absolutePath,
//            startPositionUs,
//            endPositionUs,
//            mixedWavFile.absolutePath,
//            mixAudioVideoSuccess
//        )
    }

    suspend fun mixVideoAndMusic(
        originalVideoFileDescriptor: AssetFileDescriptor,
        outputVideoPath: String,
        startTimeUs: Long,
        endTimeUs: Long,
        musicFilePath: String, mixAudioVideoSuccess: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val mediaMuxer = MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        // Prepare video track
        val originalVideoExtractor = MediaExtractor()
        originalVideoExtractor.setDataSource(originalVideoFileDescriptor)
        val originalVideoIndex: Int = getTrack(originalVideoExtractor, false)
        val originalAudioIndex: Int = getTrack(originalVideoExtractor, true)
        val originalVideoFormat = originalVideoExtractor.getTrackFormat(originalVideoIndex)
        val videoTrackOfMediaMuxer = mediaMuxer.addTrack(originalVideoFormat)
        Log.d(TAG, "mediaMuxer video track:$videoTrackOfMediaMuxer")

        // Prepare audio track, audioFormat of the mixed video should be the format from the original video
        val originalAudioFormat = originalVideoExtractor.getTrackFormat(originalAudioIndex)
        val audioBitrate = originalAudioFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        originalAudioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        val audioTrackOfMediaMuxer = mediaMuxer.addTrack(originalAudioFormat)
        Log.d(TAG, "mediaMuxer audio track:$audioTrackOfMediaMuxer")

        mediaMuxer.start()

        val mixedAudioExtractor = MediaExtractor()
        mixedAudioExtractor.setDataSource(musicFilePath)
        val audioTrack = getTrack(mixedAudioExtractor, true)
        mixedAudioExtractor.selectTrack(audioTrack)
        val audioMediaFormat = mixedAudioExtractor.getTrackFormat(audioTrack)
        var maxBufferSize = if (audioMediaFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            audioMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            MAX_BUFFER_SIZE_DEFAULT
        }

        // Encode audio from PCM to AAC
        val encodeFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE_DEFAULT, 2
        )
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate)
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
                val sampleTime = mixedAudioExtractor.sampleTime
                if (sampleTime < 0) {
                    // End of the stream
                    aacEncoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    // read pcm date to buffer
                    val flags = mixedAudioExtractor.sampleFlags
                    val size = mixedAudioExtractor.readSampleData(buffer, 0)
                    val inputBuffer = aacEncoder.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    inputBuffer?.put(buffer)
                    inputBuffer?.position(0)
                    aacEncoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags)
                    mixedAudioExtractor.advance()
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
                    Log.d(TAG, "write audio to mediaMuxer")
                    mediaMuxer.writeSampleData(audioTrackOfMediaMuxer, this, bufferInfo)
                    encodeOutputBuffer.clear()
                }
                aacEncoder.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = aacEncoder.dequeueOutputBuffer(bufferInfo, DEQUEUE_ENQUE_TIME_OUT_US)
            }
        }

        //start to write video data to mediaMuxer
        originalVideoExtractor.selectTrack(originalVideoIndex)
        originalVideoExtractor.seekTo(startTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        maxBufferSize = if (originalVideoFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            originalVideoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            MAX_BUFFER_SIZE_DEFAULT
        }

        buffer = ByteBuffer.allocateDirect(maxBufferSize)

        while (true) {
            val sampleTimeUs = originalVideoExtractor.sampleTime
            if (sampleTimeUs == -1L) {
                break
            }
            if (sampleTimeUs < startTimeUs) {
                originalVideoExtractor.advance()
                continue
            }
            if (sampleTimeUs > endTimeUs) {
                break
            }

            //Set the new time
            bufferInfo.presentationTimeUs = sampleTimeUs - startTimeUs + 600
            bufferInfo.flags = originalVideoExtractor.sampleFlags
            bufferInfo.size = originalVideoExtractor.readSampleData(buffer, 0)
            if (bufferInfo.size < 0) {
                break
            }
            Log.d(TAG, "write video to mediaMuxer")
            mediaMuxer.writeSampleData(videoTrackOfMediaMuxer, buffer, bufferInfo)
            originalVideoExtractor.advance()
        }

        try {
            mixedAudioExtractor.release()
            originalVideoExtractor.release()
            aacEncoder.stop()
            aacEncoder.release()
            mediaMuxer.release()
            Log.d(TAG, "mix audio video success")
            mixAudioVideoSuccess.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "mix audio video fail")
        }
    }

    // todo mixed video is not the same with the original video
    suspend fun mixVideoAudio(
        videoPath: String,
        audioPath: String,
        outputVideoPath: String,
        mixAudioVideoSuccess: () -> Unit
    ) = withContext(Dispatchers.IO) {
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

    // todo fix can't stop MediaMuxer crash
    private fun composeVideoAudio(videoPath: String,
                                  audioPath: String,
                                  outputVideoPath: String) {
        val videoExtractor = MediaExtractor()
        videoExtractor.setDataSource(videoPath)
        val videoTrackCount = videoExtractor.trackCount
        var frameRate = 0

        var videoTrackFormat: MediaFormat? = null
        for (i in 0 until videoTrackCount) {
            videoTrackFormat = videoExtractor.getTrackFormat(i)
            val formatString = videoTrackFormat.getString(MediaFormat.KEY_MIME)
            if (formatString?.startsWith("video/") == true) {
                videoExtractor.selectTrack(i)
                Log.d(TAG,"compose, video track:$i")
                frameRate = videoTrackFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
            }
        }

        val audioExtractor = MediaExtractor()
        audioExtractor.setDataSource(audioPath)
        val audioTrackCount = audioExtractor.trackCount
        var audioTrackFormat: MediaFormat? = null
        for (i in 0 until audioTrackCount) {
            audioTrackFormat = audioExtractor.getTrackFormat(i)
            val formatString = audioTrackFormat.getString(MediaFormat.KEY_MIME)
            if (formatString?.startsWith("audio/") == true) {
                Log.d(TAG,"compose, audio track:$i")
                audioExtractor.selectTrack(i)
            }
        }

        videoTrackFormat ?: return
        audioTrackFormat ?: return

        val videoBufferInfo = MediaCodec.BufferInfo()
        val audioBufferInfo = MediaCodec.BufferInfo()

        val mediaMuxer = MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val writeVideoIndex = mediaMuxer.addTrack(videoTrackFormat)
        val writeAudioIndex = mediaMuxer.addTrack(audioTrackFormat)
        mediaMuxer.start()

        val byteBuffer = ByteBuffer.allocate(500 * 1024)
        while (true) {
            // Read data from mediaExtractor
            val readSampleData = videoExtractor.readSampleData(byteBuffer, 0)
            if (readSampleData <= 0) {
                break
            }

            videoBufferInfo.offset = 0
            videoBufferInfo.size = readSampleData
            videoBufferInfo.flags = videoExtractor.sampleFlags
            videoBufferInfo.presentationTimeUs += 1000 * 1000 / frameRate

            //Write data to mediaMuxer
            mediaMuxer.writeSampleData(writeVideoIndex, byteBuffer, videoBufferInfo)
            videoExtractor.advance()
        }

        while (true) {
            // Read data from mediaExtractor
            val readSampleData = audioExtractor.readSampleData(byteBuffer, 0)
            if (readSampleData <= 0) {
                break
            }

            audioBufferInfo.offset = 0
            audioBufferInfo.size = readSampleData
            audioBufferInfo.flags = videoExtractor.sampleFlags
            audioBufferInfo.presentationTimeUs += 1000 * 1000 / frameRate

            //Write data to mediaMuxer
            mediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, audioBufferInfo)
            audioExtractor.advance()
        }

        videoExtractor.release()
        audioExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()

        Log.d(TAG, "compose video audio end")
    }

    private fun getTrack(mediaExtractor: MediaExtractor, audio: Boolean): Int {
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