/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.shaowei.streaming.camera.cameraapi

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.NonNull
import com.shaowei.streaming.hasCameraPermission
import com.shaowei.streaming.launchPermissionSettings
import com.shaowei.streaming.requestCameraPermission
import java.io.IOException

/**
 * More or less straight out of TextureView's doc.
 * Camera preview with TextureView
 *
 * TODO: add options for different display sizes, frame rates, camera selection, etc.
 */
class LiveCameraTextureViewActivity : Activity(), SurfaceTextureListener {
    private val TAG = "LiveCameraActivity"
    private var mCamera: Camera? = null
    private var mSurfaceTexture: SurfaceTexture? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textureView = TextureView(this)
        textureView.surfaceTextureListener = this
        setContentView(textureView)
    }

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        mSurfaceTexture = surface
        if (!hasCameraPermission(this)) {
            requestCameraPermission(this, false)
        } else {
            startPreview()
        }
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        // Ignored, Camera does all the work for us
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        mCamera?.stopPreview()
        mCamera?.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Invoked every time there's a new Camera preview frame
        //Log.d(TAG, "updated, ts=" + surface.getTimestamp());
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
            Log.e(TAG, "Exception starting preview", ioe)
        }
    }
}