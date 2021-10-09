package com.shaowei.streaming.audio.mix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R
import java.io.File
import java.io.FileDescriptor

class AudioMixActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_mix)

//        val originalVideoPath = "android.resource://$packageName/" + R.raw.audio_mix_original_video
        val musicPath = "android.resource://$packageName/" + R.raw.audio_mix_music
//        val videoFile = File("android.resource://$packageName/", "audio_mix_original_video.mp4")
        val musicFile = File("android.resource://$packageName/", "audio_mix_music.mp3")

//        AudioMixer().mixAudioTrack(originalVideoFilePath = "",originalMusicFilePath = musicFile.absolutePath,
//        mixedAudioPath = "",cacheDir = cacheDir,startTimeUs = 10*1000*1000,endTimeUs = 20*1000*1000,
//            originalVideoVolume = 2, musicVolume = 8)
    }
}