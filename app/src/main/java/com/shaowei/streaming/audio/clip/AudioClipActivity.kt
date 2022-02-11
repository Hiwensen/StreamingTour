package com.shaowei.streaming.audio.clip

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.util.concurrent.Executors

class AudioClipActivity : AppCompatActivity() {
    private val mFixedThreadPool = Executors.newFixedThreadPool(3)
    private val mAudioClipper = AudioClipper()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_clip)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun clipAudioSync(view: View) {
        mFixedThreadPool.execute {
            val sourceResourceFd = resources.openRawResourceFd(R.raw.beautifulday)
            // 20s ~ 30s
//            val clipSuccess = mAudioClipper.clipSync(sourceResourceFd, filesDir.absolutePath, 20 * 1000000, 30 * 1000000)
//            if (clipSuccess) {
//                runOnUiThread {
//                    Toast.makeText(applicationContext,"clip success", Toast.LENGTH_SHORT).show()
//                }
//            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun clipAudioAsync(view: View) {
        mAudioClipper.clipAsync(applicationContext, resources.openRawResourceFd(R.raw.beautifulday), filesDir
        .absolutePath,20 * 1000000,30 * 1000000)
    }

    override fun onStop() {
        super.onStop()
        mFixedThreadPool.shutdown()
    }

}