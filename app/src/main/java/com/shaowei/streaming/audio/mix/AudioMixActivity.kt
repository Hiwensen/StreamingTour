package com.shaowei.streaming.audio.mix

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.io.File

@RequiresApi(Build.VERSION_CODES.N)
class AudioMixActivity : AppCompatActivity() {
    private val mAudioMixer = AudioMixer()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_mix)

//        val originalVideoPath = "android.resource://$packageName/" + R.raw.audio_mix_original_video
        val musicPath = "android.resource://$packageName/" + R.raw.audio_mix_music
//        val videoFile = File("android.resource://$packageName/", "audio_mix_original_video.mp4")
        val musicFile = File("android.resource://$packageName/", "audio_mix_music.mp3")
    }

    fun mixAudio(view: View) {
        mAudioMixer.mixAudioTrack(
            resources.openRawResourceFd(R.raw.beautifulday), resources.openRawResourceFd(
                R.raw.audio_mix_music), cacheDir, 20 * 1000000, 30 * 1000000, 5, 5
        )
    }
}