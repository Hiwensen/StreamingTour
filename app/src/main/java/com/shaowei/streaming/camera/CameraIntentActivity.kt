package com.shaowei.streaming.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

const val TAKE_PICTURE_REQUEST_CODE = 1

class CameraIntentActivity : AppCompatActivity() {
    private lateinit var mPictureView: ImageView
    private var mCurrentPhotoPath = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_intent)
        findViewById<Button>(R.id.dispatch_camera_intent).setOnClickListener {
            dispatchTakePictureIntent()
        }

        mPictureView = findViewById(R.id.picture_over_view)
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                //Notice that the startActivityForResult() method is protected by a condition
                // that calls resolveActivity(),
                // which returns the first activity component that can handle the intent.
                // Performing this check is important because if you call startActivityForResult()
                // using an intent that no app can handle, your app will crash.
                startActivityForResult(takePictureIntent, TAKE_PICTURE_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAKE_PICTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as Bitmap
            bitmap?.let {
                mPictureView.setImageBitmap(bitmap)
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
        }
    }

}