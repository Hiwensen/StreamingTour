package com.shaowei.streaming.camera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import com.shaowei.streaming.camera.camera2api.Camera2Activity
import com.shaowei.streaming.camera.cameraapi.LiveCameraSurfaceViewActivity
import com.shaowei.streaming.camera.camerax.CameraXSimpleActivity
import com.shaowei.streaming.camera.camerax.CameraxEnhanceActivity

class CameraIndexActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_camera_index)

        findViewById<Button>(R.id.camera_intent).setOnClickListener {
            startActivity(Intent(this, CameraIntentActivity::class.java))
        }

        findViewById<Button>(R.id.camera_x_simple).setOnClickListener {
            startActivity(Intent(this, CameraXSimpleActivity::class.java))
        }

        findViewById<Button>(R.id.camera_x_enhance).setOnClickListener {
            startActivity(Intent(this, CameraxEnhanceActivity::class.java))
        }

        findViewById<Button>(R.id.camera_old).setOnClickListener {
            startActivity(Intent(this, LiveCameraSurfaceViewActivity::class.java))
        }

        findViewById<Button>(R.id.camera_2).setOnClickListener {
            startActivity(Intent(this, Camera2Activity::class.java))
        }

    }
}