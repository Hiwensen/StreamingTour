package com.shaowei.streaming.opengl

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.shaowei.streaming.R

class TriangleFragment : Fragment(R.layout.fragment_opengl_triangle) {
    private lateinit var mGLSurfaceView: GLSurfaceView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mGLSurfaceView = view.findViewById(R.id.gl_surface_view)
        initView()
    }

    override fun onResume() {
        super.onResume()
        mGLSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mGLSurfaceView.onPause()
    }

    private fun initView() {
        //Use OpenGLES2.0
        mGLSurfaceView.setEGLContextClientVersion(2)
        mGLSurfaceView.setRenderer(TriangleRender())
        // The renderer only renders when the surface is created, or when requestRender is called.
        mGLSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

}