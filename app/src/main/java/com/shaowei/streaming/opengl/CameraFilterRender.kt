package com.shaowei.streaming.opengl

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Not work because can't get the mCameraSurfaceTexture
 */
class CameraFilterRender(private val cameraView: CameraGLView) : GLSurfaceView.Renderer {
    private lateinit var textures: IntArray
    // todo how to get this surfaceTexture
    private lateinit var mCameraSurfaceTexture: SurfaceTexture
    private lateinit var mScreenFilter: ScreenFilter
    private var mMatrix = FloatArray(16)

    init {
        val lifecycleOwner = cameraView.context as? LifecycleOwner
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(cameraView.context)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            (cameraView.context as? LifecycleOwner)?.let {
                cameraProvider.bindToLifecycle(it, cameraSelector, preview)
            }
        }, ContextCompat.getMainExecutor(cameraView.context))
    }


    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        textures = IntArray(1)
        mCameraSurfaceTexture.attachToGLContext(textures[0])
        mCameraSurfaceTexture.setOnFrameAvailableListener { cameraView.requestRender() }
        mScreenFilter = ScreenFilter(cameraView.context)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        mScreenFilter.setSize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        mCameraSurfaceTexture.updateTexImage()
        mCameraSurfaceTexture.getTransformMatrix(mMatrix)
        mScreenFilter.setTransformMatrix(mMatrix)
        mScreenFilter.onDraw(textures[0])
    }

}