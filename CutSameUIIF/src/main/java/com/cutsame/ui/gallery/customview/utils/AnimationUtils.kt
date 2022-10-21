package com.cutsame.ui.gallery.customview.utils

import android.animation.AnimatorSet
import android.animation.FloatArrayEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import com.cutsame.ui.gallery.customview.scale.ListItemScaleType
import com.cutsame.ui.gallery.customview.AppearOrDisappearAnimationConfiguration
import com.cutsame.ui.gallery.customview.scale.GestureLayoutUtils
import com.cutsame.ui.gallery.customview.scale.PathMatrix

fun getFromMatrix(
    fromRect: Rect,
    width: Int,
    height: Int,
    videoRect: Rect?,
    listItemScaleType: ListItemScaleType
): Matrix {
    val videoWidth = videoRect?.width() ?: 0
    val videoHeight = videoRect?.height() ?: 0
    val scale = if (isVideoResizeToFitHeight(width, height, videoWidth, videoHeight)) {
        height.toFloat() / videoHeight
    } else {
        width.toFloat() / videoWidth
    }
    val translationX = (width - videoWidth) / 2
    val translationY = (height - videoHeight) / 2

    val rectF = RectF(0F, 0F, videoWidth.toFloat(), videoHeight.toFloat())
    Matrix().apply {
        setScale(scale, scale, ((videoWidth / 2).toFloat()), (videoHeight / 2).toFloat())
        postTranslate(translationX.toFloat(), translationY.toFloat())
    }.mapRect(rectF, rectF)
    return listItemScaleType.getMatrix(rectF, fromRect)
}

fun getMatrixToMatrixAnimator(
    startMatrix: Matrix,
    endMatrix: Matrix,
    videoRect: Rect?,
    animationConfiguration: AppearOrDisappearAnimationConfiguration,
    callback: (matrix: Matrix) -> Unit
): AnimatorSet {
    val startMatrixValues = FloatArray(9).apply {
        startMatrix.getValues(this)
    }
    val endMatrixValues = FloatArray(9).apply {
        endMatrix.getValues(this)
    }
    val pathAnimatorMatrix = PathMatrix(videoRect, startMatrixValues, callback)
    val valuesProperty = PropertyValuesHolder.ofObject(
        GestureLayoutUtils.NON_TRANSLATIONS_PROPERTY,
        FloatArrayEvaluator(FloatArray(9)),
        startMatrixValues,
        endMatrixValues
    )
    val nonTranslationAnimator =
        ObjectAnimator.ofPropertyValuesHolder(pathAnimatorMatrix, valuesProperty).apply {
            duration = animationConfiguration.scaleDuration
            interpolator = animationConfiguration.scaleInterpolator
        }

    val pathMotion = animationConfiguration.pathMotion
    val rectF = RectF(if (videoRect != null) RectF(videoRect) else null)
    val startRectF = RectF()
    val endRectF = RectF()
    startMatrix.mapRect(startRectF, rectF)
    endMatrix.mapRect(endRectF, rectF)

    val path = pathMotion.getPath(
        startRectF.centerX(),
        startRectF.centerY(),
        endRectF.centerX(),
        endRectF.centerY()
    )
    val translationProperty =
        PropertyValuesHolderUtils.ofPointF(GestureLayoutUtils.TRANSLATIONS_PROPERTY, path)
    val translationAnimator =
        ObjectAnimator.ofPropertyValuesHolder(pathAnimatorMatrix, translationProperty).apply {
            duration = animationConfiguration.translationDuration
            interpolator = animationConfiguration.translationInterpolator
        }

    return AnimatorSet().apply {
        playTogether(nonTranslationAnimator, translationAnimator)
    }
}