package com.shaowei.streaming.camera

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.*
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

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
    private lateinit var mSession :CameraCaptureSession

    /** Live data listener for changes in the device orientation relative to the camera */
//    private lateinit var relativeOrientation: OrientationLiveData


    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        setContentView(R.layout.camera2_activity)
        mSurfaceView = findViewById(R.id.camera2_surface_view)
    }
}