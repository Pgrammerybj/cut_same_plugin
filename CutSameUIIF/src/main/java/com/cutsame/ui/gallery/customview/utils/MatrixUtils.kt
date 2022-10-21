package com.cutsame.ui.gallery.customview.utils

import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.min

object MatrixUtils {

    const val EXIT_MIN_TRANSLATION_FACTOR_VALUE = 0.2f
    private const val ALPHA_0_TRANSLATION_FACTOR_VALUE = 1.0f
    private const val SCALE_MIN_TRANSLATION_FACTOR_VALUE = 1.0f

    fun scaleValue(matrix: Matrix): Float {
        val f = FloatArray(9)
        matrix.getValues(f)
        return f[Matrix.MSCALE_X]
    }

    fun translateValue(matrix: Matrix): FloatArray {
        val f = FloatArray(9)
        matrix.getValues(f)
        val translateX = f[2]
        val translateY = f[5]
        return floatArrayOf(translateX, translateY)
    }

    fun rotateValue(matrix: Matrix): Float {
        val v = FloatArray(9)
        matrix.getValues(v)
        return Math.round(Math.atan2(v[1].toDouble(), v[0].toDouble()) * 57.29577951308232)
            .toFloat()
    }

    fun isEqual(left: Matrix, right: Matrix): Boolean {
        val leftValues = FloatArray(9)
        left.getValues(leftValues)
        val rightValues = FloatArray(9)
        right.getValues(rightValues)
        for (i in leftValues.indices) {
            if (!isEqual(leftValues[i], rightValues[i])) {
                return false
            }
        }
        return true
    }

    private fun isEqual(firstValue: Float, secondValue: Float): Boolean {
        return abs(firstValue - secondValue) < 1.0f
    }

    fun getAlphaValue(viewRect: RectF, totalTranslationY: Float): Float {
        val alpha0TranslationValue = viewRect.height() * ALPHA_0_TRANSLATION_FACTOR_VALUE
        val progress = min(1.0f, abs(totalTranslationY) / alpha0TranslationValue)
        return 1.0f - progress
    }

    fun getScaleValue(minScaleValue: Float, viewRect: RectF, totalTranslationY: Float): Float {
        val alpha0TranslationValue = viewRect.height() * SCALE_MIN_TRANSLATION_FACTOR_VALUE
        val currentTranslationValue = abs(totalTranslationY)
        val progress = min(1.0f, currentTranslationValue / alpha0TranslationValue)
        return 1.0f + (minScaleValue - 1.0f) * progress
    }
}