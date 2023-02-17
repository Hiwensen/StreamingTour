package com.shaowei.streaming.camera.cameraapi

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import com.shaowei.streaming.hasCameraPermission
import com.shaowei.streaming.launchPermissionSettings
import com.shaowei.streaming.requestCameraPermission
import java.io.IOException

//todo can't work, almost same code with LiveCameraActivityTextureView
/**
 * Camera preview with the deprecated Camera API
 */
class CameraActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {
    private val TAG = CameraActivity::class.java.simpleName
    private var mCamera: Camera? = null
    private var mSurfaceTexture: SurfaceTexture? = null

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_camera)
        val textureView = findViewById<TextureView>(R.id.camera_texture_view)
        textureView.surfaceTextureListener = this
//        val textureView = TextureView(this)
//        textureView.surfaceTextureListener = this
//        setContentView(textureView)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.e(TAG, "onSurfaceTextureAvailable")
        mSurfaceTexture = surface
        if (!hasCameraPermission(this)) {
            requestCameraPermission(this, false)
        } else {
            startPreview()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // Ignored, Camera does all the work for us
        Log.e(TAG, "onSurfaceTextureSizeChanged")

    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Invoked every time there's a new Camera preview frame
        //Log.d(TAG, "updated, ts=" + surface.getTimestamp());
        Log.e(TAG, "onSurfaceTextureUpdated")

    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.e(TAG, "onSurfaceTextureDestroyed")
        mCamera?.stopPreview()
        mCamera?.release()
        return true
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
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

    private fun startPreview() {
        mCamera = Camera.open()
        if (mCamera == null) {
            // Seeing this on Nexus 7 2012 -- I guess it wants a rear-facing camera, but
            // there isn't one.  TODO: fix
            throw RuntimeException("Default camera not available")
        }

        try {
            mCamera?.setPreviewTexture(mSurfaceTexture)
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
            Log.e(TAG, "camera preview fail", ioe)
        }
    }
}