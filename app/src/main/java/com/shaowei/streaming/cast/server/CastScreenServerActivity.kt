package com.shaowei.streaming.cast.server

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import com.shaowei.streaming.R
import com.shaowei.streaming.hasWriteStoragePermission
import com.shaowei.streaming.requestWriteStoragePermission

class CastScreenServerActivity : AppCompatActivity() {
    private val CHANNEL_DEFAULT_IMPORTANCE = PRIORITY_LOW
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val REQUEST_CODE_SCREEN_CAPTURE = 1
    private var port = 11000
    private var mSocketLiveServer: SocketLiveServer? = null
    private var mMediaProjection: MediaProjection? = null

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

        val pendingIntent: PendingIntent =
            Intent(this, CastScreenServerActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = Notification.Builder(this,"channel")
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.ticker_text))
            .build()

        // Notification ID cannot be 0.
        this.startForegroundService(intent)
        startForeground(ONGOING_NOTIFICATION_ID, notification)

        data?.let {
            // MediaProjections require a foreground service of type ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            val mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            val socketLive = SocketLiveServer(port)
            socketLive.start(mediaProjection)
            mMediaProjection = mediaProjection
            mSocketLiveServer = socketLive
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSocketLiveServer?.close()
    }
}