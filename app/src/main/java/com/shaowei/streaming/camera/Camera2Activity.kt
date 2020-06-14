package com.shaowei.streaming.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.*
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.shaowei.streaming.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Display the camera preview with Camera2 API
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Activity : AppCompatActivity() {
    private lateinit var mSurfaceView: SurfaceView

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val mCameraManager: CameraManager by lazy {
        getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val mCharacteristics: CameraCharacteristics by lazy {
        //todo set cameraId
        mCameraManager.getCameraCharacteristics("")
    }

    /** Readers used as buffers for camera still shots */
    private lateinit var mImageReader: ImageReader

    /** [HandlerThread] where all camera operations run */
    private val mCameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [mCameraThread] */
    private val mCameraHandler = Handler(mCameraThread.looper)

    /** [HandlerThread] where all buffer reading operations run */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** [Handler] corresponding to [imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /** The [CameraDevice] that will be opened */
    private lateinit var mCamera: CameraDevice

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private lateinit var mSession: CameraCaptureSession

    /** Live data listener for changes in the device orientation relative to the camera */
//    private lateinit var relativeOrientation: OrientationLiveData

    private var mCameraId = ""
    private var mOutputFormat = PixelFormat.TRANSPARENT

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        setContentView(R.layout.camera2_activity)
        mSurfaceView = findViewById(R.id.camera2_surface_view)
        mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(
                holder: SurfaceHolder?,
                format: Int,
                width: Int,
                height: Int
            ) = Unit

            override fun surfaceDestroyed(holder: SurfaceHolder?) = Unit

            override fun surfaceCreated(holder: SurfaceHolder?) {
                initializeCamera()
            }

        })
    }

    private fun initializeCamera() = lifecycleScope.launch(Dispatchers.Main) {
        //open the selected camera
        mCamera = openCamera(mCameraManager, mCameraId, mCameraHandler)

        //
        mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?.getOutputSizes(mOutputFormat)?.maxBy { it.height * it.width }
    }

    /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        cameraManager: CameraManager,
        cameraId: String,
        cameraHandler: Handler
    ): CameraDevice {
        return suspendCoroutine { continuation ->
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    continuation.resume(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.e(TAG, "cameraId $cameraId has been disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    val msg = when (error) {
                        ERROR_CAMERA_DEVICE -> "Fatal (device)"
                        ERROR_CAMERA_DISABLED -> "Device policy"
                        ERROR_CAMERA_IN_USE -> "Camera in use"
                        ERROR_CAMERA_SERVICE -> "Fatal (service)"
                        ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                        else -> "Unknown"
                    }
                    val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                    Log.e(TAG, exc.message, exc)
                    continuation.resumeWithException(exc)
                }

            }, cameraHandler)
        }
    }

    companion object {
        private val TAG = Camera2Activity::class.java.simpleName
    }

}