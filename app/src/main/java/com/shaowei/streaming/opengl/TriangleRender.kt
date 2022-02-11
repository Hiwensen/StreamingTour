package com.shaowei.streaming.opengl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TriangleRender : GLSurfaceView.Renderer {
    //red, green, blue, alpha
    private val mColor = floatArrayOf(1.0f, 0f, 0f, 1.0f)
    private lateinit var mVertexBuffer: FloatBuffer
    private var mProgram = -1
    private var mPositionHandler = -1
    private var mColorHandler = -1

    private val mTriangleCoordinates = floatArrayOf(
        0.5f, 0.5f, 0.0f,  // top
        -0.5f, -0.5f, 0.0f,  // bottom left
        0.5f, -0.5f, 0.0f // bottom right
    )
    private val mVertexShaderCode =
        "attribute vec4 vPosition; " +
                "void main() {" +
                "gl_Position = vPosition;" +
                "}"

    private val mFragmentShaderCode =
        "precision mediump float;" +
                "uniform vec4 vColor;" +
                "void main() {" +
                "  gl_FragColor = vColor;" +
                "}"

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // clear previous color
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f)
        val byteBuffer = ByteBuffer.allocateDirect(mTriangleCoordinates.size * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        mVertexBuffer = byteBuffer.asFloatBuffer()
        mVertexBuffer.put(mTriangleCoordinates)
        // Set position to the triangle as 0
        mVertexBuffer.position(0)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, mVertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, mFragmentShaderCode)
        // Create an empty OpenGL ES program
        mProgram = GLES20.glCreateProgram()
        // Attach shaders to program
        GLES20.glAttachShader(mProgram, vertexShader)
        GLES20.glAttachShader(mProgram, fragmentShader)
        // Link to program
        GLES20.glLinkProgram(mProgram)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) = Unit

    override fun onDrawFrame(gl: GL10?) {
        // Link program
        GLES20.glUseProgram(mProgram)
        // feed data to vPosition
        mPositionHandler = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(mPositionHandler)
        GLES20.glVertexAttribPointer(mPositionHandler, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer)

        // feed data to vColor
        mColorHandler = GLES20.glGetUniformLocation(mProgram, "vColor")
        // set color to the triangle
        GLES20.glUniform4fv(mColorHandler, 1, mColor, 0)
        // draw triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        // forbid others use the positionHandler
        GLES20.glDisableVertexAttribArray(mPositionHandler)
    }

    private fun loadShader(shaderType: Int, shaderCode: String): Int {
        // Create shader
        val shader = GLES20.glCreateShader(shaderType)
        // Load source to shader and compile
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

}