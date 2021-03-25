package com.shaowei.streaming.audio.clip

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

class AudioClipActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_clip)
        findViewById<Button>(R.id.audio_clip).setOnClickListener {
            startClip()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun startClip(){
        Thread {
            val sourceResourceFd = resources.openRawResourceFd(R.raw.beautifulday)
            AudioClipper().clip(sourceResourceFd, filesDir.absolutePath, 20 * 1000000, 30 * 1000000)
        }.start()
    }
}