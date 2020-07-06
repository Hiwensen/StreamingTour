package com.shaowei.streaming.camera.cameraapi

import android.app.Activity
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.NonNull
import com.shaowei.streaming.R
import com.shaowei.streaming.hasCameraPermission
import com.shaowei.streaming.launchPermissionSettings
import com.shaowei.streaming.requestCameraPermission
import java.io.IOException

class LiveCameraSurfaceViewActivity :Activity(){
    private lateinit var mSurfaceHolder: SurfaceHolder
    private var mCamera: Camera? = null
    private val TAG = LiveCameraSurfaceViewActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.live_camera_surface_view)
        initView()
    }

    private fun initView() {
        val mSurfaceView = findViewById<SurfaceView>(R.id.live_camera_surface_view)

        mSurfaceView.holder.addCallback(object :SurfaceHolder.Callback{
            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) {
                Log.d(TAG,"surfaceChanged")

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                Log.d(TAG,"surfaceDestroyed")
            }

            override fun surfaceCreated(holder: SurfaceHolder) {
                Log.d(TAG,"surfaceCreated")

                mSurfaceHolder = holder
                mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT)

                if (!hasCameraPermission(this@LiveCameraSurfaceViewActivity)) {
                    requestCameraPermission(this@LiveCameraSurfaceViewActivity, false)
                } else {
                    startPreview()
                }
            }

        })
    }

    private fun startPreview() {
        mCamera = Camera.open()
        if (mCamera == null) {
            // Seeing this on Nexus 7 2012 -- I guess it wants a rear-facing camera, but
            // there isn't one.  TODO: fix
            throw RuntimeException("Default camera not available")
        }
        try {
            mCamera?.setPreviewDisplay(mSurfaceHolder)
            val display =
                (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            if (display.rotation == Surface.ROTATION_0) {
                mCamera?.setDisplayOrientation(90)
            }
            if (display.rotation == Surface.ROTATION_270) {
                mCamera?.setDisplayOrientation(180)
            }
            mCamera?.startPreview()
        } catch (ioe: IOException) {
            // Something bad happened
            Log.e(TAG, "Exception starting preview", ioe)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        @NonNull permissions: Array<String>,
        @NonNull grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!hasCameraPermission(this)) {
            Toast.makeText(
                this,
                "Camera permission is needed to run this application", Toast.LENGTH_LONG
            ).show()
            launchPermissionSettings(this)
            finish()
        } else {
            startPreview()
        }
    }
}