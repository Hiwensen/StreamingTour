package com.shaowei.streaming

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.audio.AudioActivity
import com.shaowei.streaming.image.ImageActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.image_playground).setOnClickListener{
            startActivity(Intent(this, ImageActivity::class.java))
        }

        findViewById<Button>(R.id.audio_playground).setOnClickListener{
            startActivity(Intent(this, AudioActivity::class.java))
        }
    }

}
