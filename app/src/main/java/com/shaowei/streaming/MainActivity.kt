package com.shaowei.streaming

import android.os.Bundle
import android.view.SurfaceView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var mImageView: ImageView
    private lateinit var mCustomView: CustomView
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mCustomSurfaceView: CustomSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mImageView = findViewById(R.id.image_view)
        mCustomView = findViewById(R.id.custom_view)
        mSurfaceView = findViewById(R.id.surface_view)
        mCustomSurfaceView = findViewById(R.id.custom_surface_view)
        drawImage()
    }

    private fun drawImage() {
        val imageDrawer = ImageDrawer(this)
        imageDrawer.withImageView(mImageView)
        imageDrawer.withCustomView(mCustomView)
        imageDrawer.withSurfaceView(mSurfaceView)
    }

}