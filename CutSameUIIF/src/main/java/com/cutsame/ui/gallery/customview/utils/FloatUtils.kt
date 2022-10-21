package com.cutsame.ui.gallery.customview.utils

import kotlin.math.abs

object FloatUtils {
    //float计算经常容易出现0.xxx，对于像素来说，相差1.0以内都算相同
    fun isEqual(firstValue: Float, secondValue: Float): Boolean {
        return abs(firstValue - secondValue) < 1.0f
    }

    fun isGreaterThanEqual(firstValue: Float, secondValue: Float): Boolean {
        return firstValue >= secondValue || isEqual(firstValue, secondValue)
    }

    fun isGreaterThan(firstValue: Float, secondValue: Float): Boolean {
        return firstValue > secondValue && !isEqual(firstValue, secondValue)
    }

    fun isLessThanEqual(firstValue: Float, secondValue: Float): Boolean {
        return firstValue <= secondValue || isEqual(firstValue, secondValue)
    }

    fun isLessThan(firstValue: Float, secondValue: Float): Boolean {
        return firstValue < secondValue && !isEqual(firstValue, secondValue)
    }
}