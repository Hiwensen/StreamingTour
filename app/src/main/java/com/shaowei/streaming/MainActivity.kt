package com.shaowei.streaming

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.audio.clip.AudioClipActivity
import com.shaowei.streaming.audio.mix.AudioMixActivity
import com.shaowei.streaming.audio.record.AudioRecordActivity
import com.shaowei.streaming.camera.CameraIndexActivity
import com.shaowei.streaming.cast.server.CastScreenServerActivity
import com.shaowei.streaming.image.ImageActivity
import com.shaowei.streaming.mediaExtractor.MediaExtractorActivity
import com.shaowei.streaming.mediacodec.MediaCodecIndexActivity
import com.shaowei.streaming.opengl.OpenGLPlayground
import com.shaowei.streaming.video.VideoClipActivity

class MainActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.image_playground).setOnClickListener {
            startActivity(Intent(this, ImageActivity::class.java))
        }

        findViewById<Button>(R.id.audio_record_playground).setOnClickListener {
            startActivity(Intent(this, AudioRecordActivity::class.java))
        }

        findViewById<Button>(R.id.audio_clip).setOnClickListener {
            startActivity(Intent(this, AudioClipActivity::class.java))
        }

        findViewById<Button>(R.id.camera_playground).setOnClickListener {
            startActivity(Intent(this, CameraIndexActivity::class.java))
        }

        findViewById<Button>(R.id.media_extractor).setOnClickListener {
            startActivity(Intent(this, MediaExtractorActivity::class.java))
        }

        findViewById<Button>(R.id.media_codec).setOnClickListener {
            startActivity(Intent(this, MediaCodecIndexActivity::class.java))
        }

        findViewById<Button>(R.id.cast_screen).setOnClickListener {
            startActivity(Intent(this, CastScreenServerActivity::class.java))
        }

        findViewById<Button>(R.id.audio_mix).setOnClickListener {
            startActivity(Intent(this, AudioMixActivity::class.java))
        }

        findViewById<Button>(R.id.video_clip).setOnClickListener {
            startActivity(Intent(this, VideoClipActivity::class.java))
        }

        findViewById<TextView>(R.id.ffmpeg_jni).text = stringFromJNI()

    }

    fun startOpenGL(view: View) {
        startActivity(Intent(this, OpenGLPlayground::class.java))
    }

    /**
     * A native method that is implemented by the 'streaming' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'ffmpegplayground' library on application startup.
        init {
            System.loadLibrary("streaming")
        }
    }

}
