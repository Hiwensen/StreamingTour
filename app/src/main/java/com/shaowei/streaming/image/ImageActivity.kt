package com.shaowei.streaming.image

import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.TextureView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

class ImageActivity :AppCompatActivity(){
    private lateinit var mImageView: ImageView
    private lateinit var mCustomView: CustomView
    private lateinit var mSurfaceView: SurfaceView
    private lateinit var mCustomSurfaceView: CustomSurfaceView
    private lateinit var mTextureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)
        mImageView = findViewById(R.id.image_view)
        mCustomView = findViewById(R.id.custom_view)
        mSurfaceView = findViewById(R.id.surface_view)
        mCustomSurfaceView = findViewById(R.id.custom_surface_view)
        mTextureView = findViewById(R.id.texture_view)
        drawImage()
    }

    private fun drawImage() {
        val imageDrawer = ImageDrawer(this)
        imageDrawer.withImageView(mImageView)
        imageDrawer.withCustomView(mCustomView)
        imageDrawer.withSurfaceView(mSurfaceView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imageDrawer.withTextureView(mTextureView)
        }
    }
}