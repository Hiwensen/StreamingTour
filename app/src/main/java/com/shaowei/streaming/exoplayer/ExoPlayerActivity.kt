package com.shaowei.streaming.exoplayer

import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.shaowei.streaming.R
import kotlinx.android.synthetic.main.activity_exo_player.view.*

class ExoPlayerActivity : AppCompatActivity() {
    private lateinit var playerView: StyledPlayerView
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)
        playerView = findViewById(R.id.player_view)
        findViewById<Button>(R.id.play_drm_content).setOnClickListener {
            playDRM()
        }

        findViewById<Button>(R.id.play_clear_content).setOnClickListener {
            playClearContent()
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer()
            playerView.onResume()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23) {
            initializePlayer()
            playerView.onResume()
        }
    }

    override fun onStop() {
        super.onStop()
        player.stop()
        player.release()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        player.playWhenReady = true
        playerView.player = player
    }

    private fun playDRM() {
        val drmConfiguration = MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
            .setLicenseUri(URI_DRM_LICENSE).build()
        val mediaItem = MediaItem.Builder().setUri(URI_WIDEVINE)
            .setDrmConfiguration(drmConfiguration).build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun playClearContent() {
        val mediaItem = MediaItem.Builder().setUri(URI_CLEAR_CONTENT).build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

}