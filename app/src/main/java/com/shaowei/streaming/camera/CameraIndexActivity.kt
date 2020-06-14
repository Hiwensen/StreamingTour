package com.shaowei.streaming.camera

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.TextureView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

class CameraIndexActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_camera_index)
        findViewById<Button>(R.id.camera_intent).setOnClickListener{
            startActivity(Intent(this, CameraIntentActivity::class.java))
        }

        findViewById<Button>(R.id.camera_x).setOnClickListener{
            startActivity(Intent(this, CameraXActivity::class.java))
        }

        findViewById<Button>(R.id.camera_old).setOnClickListener{
            startActivity(Intent(this, LiveCameraSurfaceViewActivity::class.java))
        }

        findViewById<Button>(R.id.camera_2).setOnClickListener{
            startActivity(Intent(this, Camera2Activity::class.java))
        }

    }
}