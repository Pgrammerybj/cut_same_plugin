package com.cutsame.ui.gallery.customview.utils

import android.animation.TypeEvaluator
import android.graphics.Matrix

class MatrixEvaluator : TypeEvaluator<Matrix> {
    private val mTempStartValues = FloatArray(9)
    private val mTempEndValues = FloatArray(9)
    private val mTempMatrix = Matrix()

    override fun evaluate(fraction: Float, startValue: Matrix, endValue: Matrix): Matrix? {
        startValue.getValues(mTempStartValues)
        endValue.getValues(mTempEndValues)
        for (i in 0..8) {
            val diff = mTempEndValues[i] - mTempStartValues[i]
            mTempEndValues[i] = mTempStartValues[i] + fraction * diff
        }
        mTempMatrix.setValues(mTempEndValues)
        return mTempMatrix
    }
}