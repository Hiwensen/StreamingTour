package com.shaowei.streaming.audio.mix

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class AudioMixActivity : AppCompatActivity() {
    private val mAudioMixer = AudioMixer()
    private val mMainScope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_mix)
    }

    override fun onDestroy() {
        super.onDestroy()
        mMainScope.cancel()
    }

    fun mixAudio(view: View) {
        mMainScope.launch {
            mAudioMixer.mixAudioTrack(
                resources.openRawResourceFd(R.raw.big_buck_bunny), resources.openRawResourceFd(R.raw.beautifulday),
                cacheDir, 20 * 1000000, 30 * 1000000, 80, 20
            )
        }
    }
}