package com.shaowei.streaming.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * See https://developer.android.com/training/custom-views/custom-drawing for reference
 */
class CustomView : View {
    private val mPaint = Paint()
    private lateinit var mBitmap: Bitmap
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

    fun setBitmap(bitmap: Bitmap) {
        mBitmap = bitmap
    }

    private fun initView(context: Context) {
        mPaint.style = Paint.Style.STROKE
        mPaint.flags = Paint.ANTI_ALIAS_FLAG
        mPaint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        mMatrix.setScale(0.5f, 0.5f)
        canvas.drawBitmap(mBitmap, mMatrix, mPaint)
    }
}