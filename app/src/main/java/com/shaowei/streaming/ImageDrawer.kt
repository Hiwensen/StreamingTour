package com.shaowei.streaming

import android.content.Context
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import java.io.File

class ImageDrawer(private val context: Context) {

    fun withImageView(imageView: ImageView) {

        val bitmap = getAssetsBitmap()
        imageView.setImageBitmap(bitmap)
    }

    fun withCustomView(customView: CustomView) {
        customView.setBitmap(getAssetsBitmap())
    }

    fun withSurfaceView(surfaceView: SurfaceView) {
        surfaceView.setZOrderOnTop(true)
        val matrix = Matrix()
        val surfaceHolder = surfaceView.holder
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT)

        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder?) {

            }

            override fun surfaceCreated(surfaceHolder: SurfaceHolder?) {
                if (surfaceHolder == null) {
                    return
                }

                val paint = Paint()
                paint.isAntiAlias = true
                paint.style = Paint.Style.STROKE
                paint.flags = Paint.ANTI_ALIAS_FLAG

                val canvas = surfaceHolder.lockCanvas()
                matrix.setScale(0.5f,0.5f)
                canvas.drawBitmap(getAssetsBitmap(), matrix, paint)
                surfaceHolder.unlockCanvasAndPost(canvas)
            }

        })

    }

    fun withCustomSurfaceView(customSurfaceView: CustomSurfaceView) {

    }

    private fun getAssetsBitmap(): Bitmap {
        val assets = context.resources.assets
        val assetsInputStream = assets.open("hippo.jpg")
        return BitmapFactory.decodeStream(assetsInputStream)
    }

    private fun getFileBitmap(): Bitmap {
        val file = File("")
        return BitmapFactory.decodeFile(file.absolutePath)
    }

}