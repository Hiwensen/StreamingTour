package com.shaowei.streaming.mediaExtractor

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.*
import com.shaowei.streaming.video.VideoProcessor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

@RequiresApi(Build.VERSION_CODES.N)
class MediaExtractorActivity : AppCompatActivity() {
    private val BUFFER_CAPACITY = 500 * 1024 //500kb
    private lateinit var mVideoOutputStream: FileOutputStream
    private lateinit var mAudioOutputStream: FileOutputStream

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_extractor)
        findViewById<Button>(R.id.extract_mp4_can_not_play).setOnClickListener { extractRawFileNPlay(R.raw.shariver) }

        findViewById<Button>(R.id.extract_video_can_play).setOnClickListener {
            extractVideoCanPlay(R.raw.shariver)
        }

        findViewById<Button>(R.id.extract_audio_can_play).setOnClickListener {
            extractAudioCanPlay(R.raw.shariver)
        }

        findViewById<Button>(R.id.compose).setOnClickListener {
            composeVideoAudio2()
        }

        if (!(hasWriteStoragePermission(this) && hasReadStoragePermission(this))) {
            requestReadWriteStoragePermission(this)
        }
    }

    private fun composeVideoAudio2() {
        val videoFile = File(this.filesDir, "videocanplay.mp4")
        val audioFile = File(this.filesDir, "audiocanplay.wav")
        val composedFile = File(cacheDir, "composed.mp4")
        VideoProcessor.mixVideoAudio(videoPath = videoFile.absolutePath,audioPath = audioFile.absolutePath
            , outputVideoPath = composedFile.absolutePath, mixAudioVideoSuccess = {})
    }

    private fun composeVideoAudio() {
        val videoExtractor = MediaExtractor()
        val videoFile = File(this.filesDir, "videocanplay.mp4")
        videoExtractor.setDataSource(videoFile.absolutePath)
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
        val audioFile = File(this.filesDir, "audiocanplay.wav")
        audioExtractor.setDataSource(audioFile.absolutePath)
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

        val composeFile = File(this.filesDir, "compose.mp4")
        val mediaMuxer = MediaMuxer(composeFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val writeVideoIndex = mediaMuxer.addTrack(videoTrackFormat)
        val writeAudioIndex = mediaMuxer.addTrack(audioTrackFormat)
        mediaMuxer.start()

        val byteBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
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

    /**
     * The extracted file lack of some head info so can't be played
     */
    private fun extractRawFileNPlay(rawFileId: Int) {
        // the mediaExtractor must be recreated,
        // Otherwise crash happen if extractor.setDataSource() be executed multiple times
        val extractor = MediaExtractor()
        prepareOutputStream()

        val rawResourceFd = resources.openRawResourceFd(rawFileId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            extractor.setDataSource(rawResourceFd)
        }
        val trackCount = extractor.trackCount
        var audioTrackIndex = -1
        var videoTrackIndex = -1
        for (i in 0 until trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val formatString = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "format string:$formatString")

            if (formatString?.startsWith("video/") == true) {
                videoTrackIndex = i
                Log.d(TAG, "video track:$videoTrackIndex")
            }

            if (formatString?.startsWith("audio/") == true) {
                audioTrackIndex = i
                Log.d(TAG, "audio track:$audioTrackIndex")
            }
        }

        val inputBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)
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

            // select audio track and write audio stream
            extractor.selectTrack(audioTrackIndex)
            while (true) {
                val readSampleData = extractor.readSampleData(inputBuffer, 0)
                if (readSampleData < 0) {
                    break
                }

                val byteArray = ByteArray(readSampleData)
                inputBuffer.get(byteArray)
                mAudioOutputStream.write(byteArray)
                inputBuffer.clear()
                extractor.advance()
            }
            mAudioOutputStream.close()

        } catch (ioException: IOException) {
            Log.e(TAG, "extractRawFileNPlay,${ioException}")
        } finally {
            extractor.release()
        }
    }

    private fun extractVideoCanPlay(rawFileId: Int) {
        val mediaExtractor = MediaExtractor()
        val videoFile = File(this.filesDir, "videocanplay.mp4")
        var mediaMuxer: MediaMuxer? = null

        val rawResourceFd = resources.openRawResourceFd(rawFileId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaExtractor.setDataSource(rawResourceFd)
        }

        val trackCount = mediaExtractor.trackCount
        var videoTrackIndex = -1
        var frameRate = -1
        for (i in 0 until trackCount) {
            val mediaFormat = mediaExtractor.getTrackFormat(i)
            val formatString = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "format string:$formatString")

            if (formatString?.startsWith("video/") == true) {
                frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE)

                mediaExtractor.selectTrack(i)

                mediaMuxer = MediaMuxer(videoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                videoTrackIndex = mediaMuxer.addTrack(mediaFormat)
                Log.d(TAG, "video track:$videoTrackIndex, videopath:${videoFile.absolutePath}")

                mediaMuxer.start()
                break
            }
        }

        mediaMuxer ?: return

        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.presentationTimeUs = 0

        val byteBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)

        while (true) {
            // Read data from mediaExtractor
            val readSampleData = mediaExtractor.readSampleData(byteBuffer, 0)
            if (readSampleData < 0) {
                break
            }

            bufferInfo.offset = 0
            bufferInfo.size = readSampleData
            bufferInfo.flags = mediaExtractor.sampleFlags
            bufferInfo.presentationTimeUs += 1000 * 1000 / frameRate

            //Write data to mediaMuxer
            mediaMuxer.writeSampleData(videoTrackIndex, byteBuffer, bufferInfo)
            mediaExtractor.advance()
        }

        mediaExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        Log.d(TAG, "extract video file can be played end")
    }

    /**
     *  todo the audio duration is larger than the original content duration
     */
    private fun extractAudioCanPlay(rawFileId: Int) {
        val mediaExtractor = MediaExtractor()
        val rawResourceFd = resources.openRawResourceFd(rawFileId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaExtractor.setDataSource(rawResourceFd)
        }

        var mediaMuxer: MediaMuxer? = null

        val trackCount = mediaExtractor.trackCount
        var audioTrackIndex = -1
        var frameRate = -1
        for (i in 0 until trackCount) {
            val mediaFormat = mediaExtractor.getTrackFormat(i)
            val formatString = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "format string:$formatString")

            if (formatString?.startsWith("audio/") == true) {
                mediaExtractor.selectTrack(i)

                val audioFile = File(this.filesDir, "audiocanplay.wav")
                mediaMuxer = MediaMuxer(audioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                audioTrackIndex = mediaMuxer.addTrack(mediaFormat)
                Log.d(TAG, "audio track:$audioTrackIndex, audioFilePath:${audioFile.absolutePath}")
                mediaMuxer.start()
            }

            if (formatString?.startsWith("video/") == true) {
                frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE)
                break
            }
        }

        mediaMuxer ?: return

        val bufferInfo = MediaCodec.BufferInfo()
        bufferInfo.presentationTimeUs = 0

        val byteBuffer = ByteBuffer.allocate(BUFFER_CAPACITY)

        while (true) {
            // Read data from mediaExtractor
            val readSampleData = mediaExtractor.readSampleData(byteBuffer, 0)
            if (readSampleData < 0) {
                break
            }

            bufferInfo.offset = 0
            bufferInfo.size = readSampleData
            bufferInfo.flags = mediaExtractor.sampleFlags
            bufferInfo.presentationTimeUs += 1000 * 1000 / frameRate

            //Write data to mediaMuxer
            mediaMuxer.writeSampleData(audioTrackIndex, byteBuffer, bufferInfo)
            mediaExtractor.advance()
        }

        mediaExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        Log.d(TAG, "extract audio file can be played end")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!(hasWriteStoragePermission(this) && hasReadStoragePermission(this))) {
            Toast.makeText(
                this,
                "Reading and Writing to external storage permission is needed to run this application",
                Toast.LENGTH_LONG
            ).show()
            launchPermissionSettings(this)
        } else {
            prepareOutputStream()
        }
    }

    private fun prepareOutputStream() {
        val videoFile = File(this.filesDir, "video.mp4")
        if (!videoFile.exists()) {
            val createNewFile = videoFile.createNewFile()
            Log.d(
                TAG,
                "create video file: $createNewFile, file path: ${videoFile.path}, abs path:${videoFile.absolutePath}"
            )
        } else {
            Log.d(TAG, "video file path: ${videoFile.path}, abs path:${videoFile.absolutePath}")
        }

        val audioFile = File(this.filesDir, "audio.pcm")
        if (!audioFile.exists()) {
            val createAudioFile = audioFile.createNewFile()
            Log.d(
                TAG,
                "create video file: $createAudioFile, file path: ${audioFile.path}, abs path:${audioFile.absolutePath}"
            )
        } else {
            Log.d(TAG, "audio file path: ${audioFile.path}, abs path:${audioFile.absolutePath}")
        }

        mVideoOutputStream = FileOutputStream(videoFile)
        mAudioOutputStream = FileOutputStream(audioFile)
    }

    companion object {
        private val TAG = MediaExtractorActivity::class.java.simpleName
    }

}