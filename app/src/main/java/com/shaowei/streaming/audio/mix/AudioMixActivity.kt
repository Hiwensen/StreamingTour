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
    }

    fun mixAudio(view: View) {
        Thread {
            mAudioMixer.mixAudioTrack(
                resources.openRawResourceFd(R.raw.big_buck_bunny), resources.openRawResourceFd(R.raw.beautifulday),
                cacheDir, 20 * 1000000, 30 * 1000000, 80, 20)
        }.start()
    }
}