package com.shaowei.streaming.camera

import android.os.Bundle
import android.os.PersistableBundle
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

/**
 * Display the camera preview with Camera2 API
 */
class Camera2Activity: AppCompatActivity() {
    private lateinit var mSurfaceView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        setContentView(R.layout.camera2_activity)
        mSurfaceView = findViewById(R.id.camera2_surface_view)
    }
}