package com.shaowei.streaming.cast

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import com.shaowei.streaming.cast.server.SocketLive
import com.shaowei.streaming.hasWriteStoragePermission
import com.shaowei.streaming.requestWriteStoragePermission

class CastScreenActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val REQUEST_CODE_SCREEN_CAPTURE = 1
    private var port = 11000
    private var mSocketLive: SocketLive? = null
    private var mMediaProjection: MediaProjection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast_screen)
        if (!hasWriteStoragePermission(this)) {
            requestWriteStoragePermission(this)
        }
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val createScreenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(createScreenCaptureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || requestCode != REQUEST_CODE_SCREEN_CAPTURE) {
            Toast.makeText(this, "start screen capture failed", Toast.LENGTH_LONG).show()
            return
        }

        data?.let {
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            val socketLive = SocketLive(port)
            socketLive.start(mediaProjection)
            mMediaProjection = mediaProjection
            mSocketLive = socketLive
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocketLive?.close()
    }
}