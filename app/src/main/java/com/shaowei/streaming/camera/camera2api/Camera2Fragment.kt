package com.shaowei.streaming.camera.camera2api

import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.shaowei.streaming.R
import com.shaowei.streaming.camera.utils.AutoFitSurfaceView
import com.shaowei.streaming.camera.utils.OrientationLiveData
import kotlinx.android.synthetic.main.fragment_camera2.*

class Camera2Fragment : Fragment() {

    /** AndroidX navigation arguments */
    private val args: Camera2FragmentArgs by navArgs()

    /** Host's navigation controller*/
    private val navController: NavController by lazy {
        Navigation.findNavController(requireActivity(), R.id.fragment_container_camera2)
    }

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val applicationContext = requireActivity().applicationContext
        applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /**
     * [CameraCharacteristics] corresponding to the provided Camera ID
     */
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(args.cameraId)
    }

    /**
     * Readers used as buffers for camera still shots
     */
    private lateinit var imageReader:ImageReader

    /**
     * [HandlerThread] corresponding to [CameraThread]
     */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    private val cameraHandler = Handler(cameraThread.looper)

    private val animationTask:Runnable by lazy {
        Runnable {
            // Flash white animation
            overlay.background = Color.argb(150,255,255,255).toDrawable()
            overlay.postDelayed({
                // Remove white falsh animation
                overlay.background = null
            }, Camera2Activity.ANIMATION_FAST_MILLIS)
        }
    }

    /** [HandlerThread] where all buffer reading operations run */
    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }

    /** [Handler] corresponding to [imageReaderThread] */
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    /** Where the camera preview is displayed */
    private lateinit var viewFinder: AutoFitSurfaceView

    /** Overlay on top of the camera preview */
    private lateinit var overlay: View

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    private lateinit var session: CameraCaptureSession

    /** Live data listener for changes in the device orientation relative to the camera */
    private lateinit var relativeOrientation: OrientationLiveData

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_camera2, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

}