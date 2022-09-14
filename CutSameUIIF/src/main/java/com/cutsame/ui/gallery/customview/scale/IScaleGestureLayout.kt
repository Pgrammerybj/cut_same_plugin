package com.cutsame.ui.gallery.customview.scale

import android.graphics.Rect
import com.cutsame.ui.gallery.customview.AppearOrDisappearAnimationConfiguration


interface IScaleGestureLayout {

    fun setVideoSize(videoWidth: Int, videoHeight: Int)

    fun setZoomListener(listener: ScaleGestureLayoutListener)

    @Suppress("LongParameterList")
    fun animateAppear(
        fromRect: Rect?,
        visibleRect: Rect?,
        radius: Float,
        maskInsetPixel: IntArray?,
        listItemScaleType: ListItemScaleType,
        animationConfiguration: AppearOrDisappearAnimationConfiguration
    )

    @Suppress("LongParameterList")
    fun animateDisappear(
        fromRect: Rect?,
        visibleRect: Rect?,
        radius: Float,
        maskInsetPixel: IntArray?,
        listItemScaleType: ListItemScaleType,
        animationConfiguration: AppearOrDisappearAnimationConfiguration,
        endAction: () -> Unit
    )

    fun reset()
}