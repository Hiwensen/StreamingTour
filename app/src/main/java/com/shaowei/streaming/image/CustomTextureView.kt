package com.shaowei.streaming.image

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.TextureView

class CustomTextureView :TextureView,TextureView.SurfaceTextureListener{
    private val mMatrix = Matrix()

    constructor(context: Context) : super(context) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context)
    }

    private fun initView(context: Context) {
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
        return true
    }

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.flags = Paint.ANTI_ALIAS_FLAG

        val canvas = lockCanvas()
        mMatrix.setScale(0.5f,0.5f)
        canvas.drawBitmap(getAssetsBitmap(), mMatrix, paint)

        unlockCanvasAndPost(canvas)
    }

    private fun getAssetsBitmap(): Bitmap {
        val assets = context.resources.assets
        val assetsInputStream = assets.open("hippo.jpg")
        return BitmapFactory.decodeStream(assetsInputStream)
    }
}