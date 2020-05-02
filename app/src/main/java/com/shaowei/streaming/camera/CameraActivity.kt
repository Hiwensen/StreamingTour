package com.shaowei.streaming.camera

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.shaowei.streaming.R

class CameraActivity :AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_camera)
        initView()
    }

    private fun initView() {

    }
}