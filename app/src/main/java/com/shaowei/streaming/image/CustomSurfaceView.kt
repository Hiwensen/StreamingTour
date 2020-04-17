package com.shaowei.streaming.image

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class CustomSurfaceView : SurfaceView, SurfaceHolder.Callback {
    private var mCanvas: Canvas? = null
    private lateinit var mSurfaceHolder: SurfaceHolder
    private lateinit var mPaint: Paint
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
        setZOrderOnTop(true)
        mSurfaceHolder = holder
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT)
        mSurfaceHolder.addCallback(this)

        mPaint = Paint()
        mPaint.style = Paint.Style.STROKE
        mPaint.flags = Paint.ANTI_ALIAS_FLAG
        mPaint.isAntiAlias = true
    }

    override fun surfaceCreated(surfaceHolder: SurfaceHolder?) {
        drawImage()
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
    }

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
    }

    private fun drawImage() {
        mCanvas = mSurfaceHolder.lockCanvas()
        mMatrix.setScale(0.5f, 0.5f)
        mCanvas?.drawBitmap(getBitmap(), mMatrix, mPaint)
        mSurfaceHolder.unlockCanvasAndPost(mCanvas)
    }

    private fun getBitmap(): Bitmap {
        val assets = context.resources.assets
        val assetsInputStream = assets.open("hippo.jpg")
        return BitmapFactory.decodeStream(assetsInputStream)
    }
}