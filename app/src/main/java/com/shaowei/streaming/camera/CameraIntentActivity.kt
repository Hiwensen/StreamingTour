package com.shaowei.streaming.camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.shaowei.streaming.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

const val TAKE_PICTURE_REQUEST_CODE = 1
const val FILE_PROVIDER_AUTHORITY = "com.shaowei.streaming.fileprovider"

class CameraIntentActivity : AppCompatActivity() {
    private val TAG = CameraIntentActivity::class.java.simpleName
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
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile = try {
                    createImageFile()
                } catch (ioException: IOException) {
                    Log.e(TAG, ioException.toString())
                    null
                }

                photoFile?.also {
                    //storage/emulated/0/Android/data/com.shaowei.streaming/files/Pictures/JPEG_...
                    val photoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, it)
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(takePictureIntent, TAKE_PICTURE_REQUEST_CODE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == TAKE_PICTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // get the thumbnail, if the picture is stored, the data won't be returned
            val bitmap = data?.extras?.get("data")?.let { it as Bitmap }

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