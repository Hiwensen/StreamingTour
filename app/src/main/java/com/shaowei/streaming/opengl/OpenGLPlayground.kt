package com.shaowei.streaming.opengl

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

class OpenGLPlayground : AppCompatActivity() {
    private lateinit var mFragmentPlaceHolder: FrameLayout
    private val mTriangleFragmentTag = TriangleFragment::class.java.simpleName
    private val mCameraFilterFragmentTag = CameraFilterFragment::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.opengl_playground)
        mFragmentPlaceHolder = findViewById(R.id.fragment_placeholder)
    }

    fun drawTriangle(view: View) {
        val triangleFragment = supportFragmentManager.findFragmentByTag(mTriangleFragmentTag)
        if (triangleFragment == null) {
            supportFragmentManager.beginTransaction().add(
                R.id.fragment_placeholder, TriangleFragment(),
                mTriangleFragmentTag
            ).commit()
        } else {
            supportFragmentManager.beginTransaction().show(triangleFragment).commit()
        }
    }

    fun showCameraPreview(view: View) {
        val cameraFragment = supportFragmentManager.findFragmentByTag(mCameraFilterFragmentTag)
        if (cameraFragment == null) {
            supportFragmentManager.beginTransaction().add(
                R.id.fragment_placeholder, CameraFilterFragment(),
                mCameraFilterFragmentTag
            ).commit()
        } else {
            supportFragmentManager.beginTransaction().show(cameraFragment).commit()
        }
    }

}