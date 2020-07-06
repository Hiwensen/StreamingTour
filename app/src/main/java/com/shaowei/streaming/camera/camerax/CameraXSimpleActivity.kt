package com.shaowei.streaming.camera.camerax

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.shaowei.streaming.R
import com.shaowei.streaming.hasCameraPermission
import com.shaowei.streaming.launchPermissionSettings
import com.shaowei.streaming.requestCameraPermission
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXSimpleActivity : AppCompatActivity() {
    private lateinit var mPreviewUseCase: Preview
    private lateinit var mCaptureButton: Button
    private lateinit var mPreviewView: PreviewView
    private lateinit var mImageCapture: ImageCapture
    private lateinit var mImageAnalyzer: ImageAnalysis.Analyzer
    private lateinit var mOutputDirectory: File
    private lateinit var mCameraExecutor: ExecutorService
    private var mCamera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camerax)
        mCaptureButton = findViewById<Button>(R.id.camera_capture_button)
        mCaptureButton.setOnClickListener { takePhoto() }

        mPreviewView = findViewById(R.id.viewFinder)
        mOutputDirectory = getOutputDir()
        mCameraExecutor = Executors.newSingleThreadExecutor()

        requestPermission()
    }

    private fun getOutputDir(): File {
        val mediaDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            externalMediaDirs.firstOrNull()?.let {
                File(it, resources.getString(R.string.app_name)).apply {
                    mkdirs()
                }
            }
        } else {
            TODO("VERSION.SDK_INT < LOLLIPOP")
        }

        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else filesDir
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
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = mImageCapture ?: return

        // Create timestamped output file to hold the image
        val photoFile = File(
            mOutputDirectory,
            SimpleDateFormat(
                FILE_NAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Setup image capture listener which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.d(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            })
    }

    private fun requestPermission() {
        if (!hasCameraPermission(this)) {
            requestCameraPermission(this, true)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            mPreviewUseCase = Preview.Builder().build()

            //ImageCapture
            mImageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

            //ImageAnalysis

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(mCameraExecutor,
                        LuminosityAnalyzer(
                            object :
                                LumaListener {
                                override fun analyzeLuma(luma: Double) {
                                    Log.d(
                                        TAG,
                                        luma.toString()
                                    )
                                }
                            })
                    )
                }

            // Select back camera
            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                mCamera =
                    cameraProvider.bindToLifecycle(this, cameraSelector, mPreviewUseCase, mImageCapture, imageAnalyzer)
                mPreviewUseCase.setSurfaceProvider(mPreviewView.createSurfaceProvider())
            } catch (exc: Exception) {
                Log.d(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            Log.d(TAG, "luma: $luma")
            listener.analyzeLuma(luma)

            image.close()
        }

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }
    }

    interface LumaListener {
        fun analyzeLuma(luma: Double)
    }

    companion object {
        private val TAG = CameraXSimpleActivity::class.java.simpleName
        private const val FILE_NAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

}