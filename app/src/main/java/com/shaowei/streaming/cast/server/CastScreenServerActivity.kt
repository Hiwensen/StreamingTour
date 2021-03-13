package com.shaowei.streaming.cast.server

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import com.shaowei.streaming.hasWriteStoragePermission
import com.shaowei.streaming.requestWriteStoragePermission

class CastScreenServerActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val REQUEST_CODE_SCREEN_CAPTURE = 1
    private var mSocketLiveServer: SocketLiveServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cast_server)
        if (!hasWriteStoragePermission(this)) {
            requestWriteStoragePermission(this)
        }
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val createScreenCaptureIntent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(createScreenCaptureIntent, REQUEST_CODE_SCREEN_CAPTURE)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || requestCode != REQUEST_CODE_SCREEN_CAPTURE) {
            Toast.makeText(this, "start screen capture failed", Toast.LENGTH_LONG).show()
            return
        }

        val service = Intent(this, CaptureScreenService::class.java)
        service.putExtra("code", resultCode)
        service.putExtra("data", data)
        startForegroundService(service)
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocketLiveServer?.close()
        val service = Intent(this, CaptureScreenService::class.java)
        stopService(service)
    }
}