package com.shaowei.streaming.camera

import android.os.Bundle
import android.os.PersistableBundle
import android.view.SurfaceView
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

class CameraActivity : AppCompatActivity() {
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mTextureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_camera)
        initView()
    }

    private fun initView() {
        mSurfaceView = findViewById(R.id.camera_surface_view)
        mTextureView = findViewById(R.id.camera_texture_view)
    }
}