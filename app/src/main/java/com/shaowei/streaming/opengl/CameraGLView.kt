package com.shaowei.streaming.opengl

import android.content.Context
import android.opengl.GLSurfaceView

class CameraGLView(context: Context?) : GLSurfaceView(context) {

    init {
        setEGLContextClientVersion(2)
        setRenderer(CameraFilterRender(this))
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

}