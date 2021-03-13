package com.shaowei.streaming.cast.server

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.annotation.RequiresApi
import com.shaowei.streaming.R


@RequiresApi(Build.VERSION_CODES.O)
class CaptureScreenService : Service() {
    private val CHANNEL_ID = "capture screen"
    private var mResultCode: Int = 0
    private var port = 11000
    private lateinit var mResultData: Intent
    private lateinit var mMediaProjection: MediaProjection
    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private lateinit var mSocketLiveServer: SocketLiveServer

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()
        mResultCode = intent.getIntExtra("code", -1)
        mResultData = intent.getParcelableExtra("data")
        mMediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        Handler().postDelayed({
            // The code must be executed with a delay
            // Refer to https://stackoverflow.com/questions/61276730/media-projections-require-a-foreground-service-of-type-serviceinfo-foreground-se
            mMediaProjection = mMediaProjectionManager.getMediaProjection(
                mResultCode,
                mResultData
            )
            mSocketLiveServer = SocketLiveServer(port)
            mSocketLiveServer.start(mMediaProjection)
        }, 1000)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        val channelId = createNotificationChannelId("notification_id", "notification_name")
        val intent = Intent(this, CastScreenServerActivity::class.java)
        val builder = Notification.Builder(this.applicationContext, channelId)

        builder.setContentIntent(PendingIntent.getActivity(this, 0, intent, 0)) // 设置PendingIntent
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher))
            .setContentTitle("capture screen")
            .setSmallIcon(R.drawable.ic_share)
            .setContentText("is running......")
            .setWhen(System.currentTimeMillis())

        val notification = builder.build()
        startForeground(110, notification)
    }

    private fun createNotificationChannelId(channelId: String, channelName: String): String{
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        mSocketLiveServer.close()
    }

}