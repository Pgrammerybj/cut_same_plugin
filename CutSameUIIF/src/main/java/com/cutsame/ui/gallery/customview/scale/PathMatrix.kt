package com.cutsame.ui.gallery.customview.scale

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF

class PathMatrix(
    videoRect: Rect?,
    values: FloatArray,
    private val callback: (matrix: Matrix) -> Unit
) {
    private val matrix = Matrix()
    private val mRectF = RectF(videoRect)
    private val mTmpRectF = RectF()
    private val mValues = values.clone()
    private var mCenterX = 0f
    private var mCenterY = 0f

    init {
        setAnimationMatrix()
    }

    private fun setAnimationMatrix() {
        matrix.run {
            setValues(mValues)
            mapRect(mTmpRectF, mRectF)
            val translationX = mCenterX - mTmpRectF.centerX()
            val translationY = mCenterY - mTmpRectF.centerY()
            postTranslate(translationX, translationY)
            callback(this)
        }
    }

    fun setValues(values: FloatArray) {
        System.arraycopy(values, 0, mValues, 0, values.size)
        setAnimationMatrix()
    }

    fun setTranslation(translation: PointF) {
        mCenterX = translation.x
        mCenterY = translation.y
        setAnimationMatrix()
    }
}