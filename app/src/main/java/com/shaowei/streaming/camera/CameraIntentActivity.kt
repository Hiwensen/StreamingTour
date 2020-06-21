package com.shaowei.streaming.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.shaowei.streaming.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min


const val REQUEST_CODE_IMAGE_CAPTURE = 1
const val REQUEST_CODE_VIDEO_CAPTURE = 2
const val FILE_PROVIDER_AUTHORITY = "com.shaowei.streaming.fileprovider"

class CameraIntentActivity : AppCompatActivity() {
    private val TAG = CameraIntentActivity::class.java.simpleName
    private lateinit var mPictureView: ImageView
    private var mCurrentPhotoPath = ""
    private lateinit var mVideoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_intent)

        if (!hasCameraPermission(this)) {
            requestCameraPermission(this, true)
        }

        if (!hasWriteStoragePermission(this)) {
            requestWriteStoragePermission(this)
        }

        findViewById<Button>(R.id.image_capture).setOnClickListener {
            dispatchImageCaptureIntent()
        }
        mPictureView = findViewById(R.id.image_over_view)

        findViewById<Button>(R.id.video_capture).setOnClickListener {
            dispatchVideoCaptureIntent()
        }
        mVideoView = findViewById(R.id.video_view)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            // get the thumbnail, if the picture is stored, the data won't be returned
            val bitmap = data?.extras?.get("data")?.let { it as Bitmap }
            bitmap?.let {
                mPictureView.setImageBitmap(bitmap)
            }
        }

        if (requestCode == REQUEST_CODE_VIDEO_CAPTURE && resultCode == Activity.RESULT_OK) {
            val videoUri = data?.data
            Log.e(TAG, videoUri.toString())
            mVideoView.setVideoURI(videoUri)
            mVideoView.start()
        }
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

        if (!hasWriteStoragePermission(this)) {
            Toast.makeText(
                this,
                "Writing to external storage permission is needed to run this application",
                Toast.LENGTH_LONG
            ).show()
            launchPermissionSettings(this)
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        mVideoView.stopPlayback()
    }

    private fun dispatchImageCaptureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile = try {
                    createImageFile()
                } catch (ioException: IOException) {
                    Log.e(TAG, ioException.toString())
                    null
                }

                photoFile?.also {
                    // storage/emulated/0/Android/data/com.shaowei.streaming/files/Pictures/JPEG_...
                    val photoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, it)
//                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, REQUEST_CODE_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = absolutePath
            Log.e(TAG, mCurrentPhotoPath)
        }
    }

    private fun dispatchVideoCaptureIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_CODE_VIDEO_CAPTURE)
            }
        }
    }

    /**
     * scale the image based on the display width/height
     */
    private fun setPic() {
        // Get the dimensions of the View
        val targetW: Int = mPictureView.width
        val targetH: Int = mPictureView.height

        val bmOptions = BitmapFactory.Options().apply {
            // Get the dimensions of the bitmap
            inJustDecodeBounds = true

            val photoW: Int = outWidth
            val photoH: Int = outHeight

            // Determine how much to scale down the image
            val scaleFactor: Int = min(photoW / targetW, photoH / targetH)

            // Decode the image file into a Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions)?.also { bitmap ->
            mPictureView.setImageBitmap(bitmap)
        }
    }

}