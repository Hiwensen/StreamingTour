package com.shaowei.streaming.audio.clip

import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.util.concurrent.Executors

class AudioClipActivity : AppCompatActivity() {
    private val mFixedThreadPool = Executors.newFixedThreadPool(3)

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_clip)
        findViewById<Button>(R.id.audio_clip).setOnClickListener {
            startClip()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun startClip() {
        mFixedThreadPool.execute {
            val sourceResourceFd = resources.openRawResourceFd(R.raw.beautifulday)
            // 20s ~ 30s
            AudioClipper().clip(sourceResourceFd, filesDir.absolutePath, 20 * 1000000, 30 * 1000000)
        }
    }

    override fun onStop() {
        super.onStop()
        mFixedThreadPool.shutdown()
    }

}