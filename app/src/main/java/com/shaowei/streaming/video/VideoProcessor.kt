package com.shaowei.streaming.video

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.shaowei.streaming.audio.AudioProcessor
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.N)
class VideoProcessor {
    private val TAG = VideoProcessor::class.java.simpleName
    private val TRACK_UNKNOWN = -1
    private val SAMPLE_TIME_OFFSET_MICRO_SECONDS = 100000 // 0.1s
    private val AUDIO_TRACK_INDEX_UNFOUND = -1
    private val DEQUEUE_ENQUE_TIME_OUT_US = 100000L

    private lateinit var mMediaCodec: MediaCodec

    fun clipAndMixVideo(
        context: Context, sourceAssetFileDescriptor: AssetFileDescriptor, startPositionUs: Long, endPositionUs: Long,
        backgroundMusicFileDescriptor: AssetFileDescriptor, cacheDir: File
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
            mixedWavFile
        )
    }

    private fun mixVideoAndMusic(
        videoFileDescriptor: AssetFileDescriptor, outputVideoPath: String, startTimeUs: Long, endTimeUs: Long,
        wavFile: File
    ) {

        //        初始化一个视频封装容器
        val mediaMuxer = MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        //            一个轨道    既可以装音频 又视频   是 1 不是2
        //            取音频轨道  wav文件取配置信息
        //            先取视频
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(videoFileDescriptor)
        //            拿到视频轨道的索引
        val videoIndex: Int = getTrack(mediaExtractor, false)
        val audioIndex: Int = getTrack(mediaExtractor, true)

        //            视频配置 文件
        val videoFormat = mediaExtractor.getTrackFormat(videoIndex)
        //开辟了一个 轨道   空的轨道   写数据     真实
        mediaMuxer.addTrack(videoFormat)

        //        ------------音频的数据已准备好----------------------------
        //            视频中音频轨道   应该取自于原视频的音频参数
        val audioFormat = mediaExtractor.getTrackFormat(audioIndex)
        val audioBitrate = audioFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        audioFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC)
        //        添加一个空的轨道  轨道格式取自 视频文件，跟视频所有信息一样
        val muxerAudioIndex = mediaMuxer.addTrack(audioFormat)

        //            音频轨道开辟好了  输出开始工作
        mediaMuxer.start()

        //音频的wav
        val pcmExtrator = MediaExtractor()
        pcmExtrator.setDataSource(wavFile.absolutePath)
        val audioTrack: Int = getTrack(pcmExtrator, true)
        pcmExtrator.selectTrack(audioTrack)
        val pcmTrackFormat = pcmExtrator.getTrackFormat(audioTrack)

        //最大一帧的 大小
        var maxBufferSize = 0
        maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            pcmTrackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        } else {
            100 * 1000
        }

        //    最终输出   后面   混音   -----》     重采样   混音     这个下节课讲
        val encodeFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            44100, 2
        ) //参数对应-> mime type、采样率、声道数
        encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, audioBitrate) //比特率
        //            音质等级
        encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        //            解码  那段
        encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxBufferSize)
        //解码 那
        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        //            配置AAC 参数  编码 pcm   重新编码     视频文件变得更小
        encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
        //            容器
        var buffer = ByteBuffer.allocateDirect(maxBufferSize)
        val info = MediaCodec.BufferInfo()
        var encodeDone = false
        while (!encodeDone) {
            val inputBufferIndex = encoder.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                val sampleTime = pcmExtrator.sampleTime
                if (sampleTime < 0) {
                    //                        pts小于0  来到了文件末尾 通知编码器  不用编码了
                    encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    val flags = pcmExtrator.sampleFlags
                    //
                    val size = pcmExtrator.readSampleData(buffer, 0)
                    //                    编辑     行 1 还是不行 2   不要去用  空的
                    val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                    inputBuffer!!.clear()
                    inputBuffer.put(buffer)
                    inputBuffer.position(0)
                    encoder.queueInputBuffer(inputBufferIndex, 0, size, sampleTime, flags)
                    //                        读完这一帧
                    pcmExtrator.advance()
                }
            }
            //                获取编码完的数据
            var outputBufferIndex =
                encoder.dequeueOutputBuffer(info, DEQUEUE_ENQUE_TIME_OUT_US)
            while (outputBufferIndex >= 0) {
                if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    encodeDone = true
                    break
                }
                val encodeOutputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                //                    将编码好的数据  压缩 1     aac
                mediaMuxer.writeSampleData(muxerAudioIndex, encodeOutputBuffer!!, info)
                encodeOutputBuffer.clear()
                encoder.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex =
                    encoder.dequeueOutputBuffer(info, DEQUEUE_ENQUE_TIME_OUT_US)
            }
        }
        //    把音频添加好了
        if (audioTrack >= 0) {
            mediaExtractor.unselectTrack(audioTrack)
        }
        //视频
        mediaExtractor.selectTrack(videoIndex)
        mediaExtractor.seekTo(startTimeUs.toLong(), MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
        maxBufferSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
        buffer = ByteBuffer.allocateDirect(maxBufferSize)
        //封装容器添加视频轨道信息
        while (true) {
            val sampleTimeUs = mediaExtractor.sampleTime
            if (sampleTimeUs == -1L) {
                break
            }
            if (sampleTimeUs < startTimeUs) {
                mediaExtractor.advance()
                continue
            }
            if (endTimeUs != null && sampleTimeUs > endTimeUs) {
                break
            }
            //                pts      0
            info.presentationTimeUs = sampleTimeUs - startTimeUs + 600
            info.flags = mediaExtractor.sampleFlags
            //                读取视频文件的数据  画面 数据   压缩1  未压缩2
            info.size = mediaExtractor.readSampleData(buffer, 0)
            if (info.size < 0) {
                break
            }
            //                视频轨道  画面写完了
            mediaMuxer.writeSampleData(videoIndex, buffer, info)
            mediaExtractor.advance()
        }
        try {
            pcmExtrator.release()
            mediaExtractor.release()
            encoder.stop()
            encoder.release()
            mediaMuxer.release()
            Log.d(TAG, "mix audio video success")
        } catch (e: Exception) {
            Log.e(TAG, "mix audio video fail")
        }
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

}