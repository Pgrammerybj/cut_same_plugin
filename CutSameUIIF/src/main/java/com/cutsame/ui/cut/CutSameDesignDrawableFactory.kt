package com.cutsame.ui.cut

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable

object CutSameDesignDrawableFactory {
    
    fun createRectNormalDrawable(strokeResColor: Int, fillResColor: Int, strokeWidth: Int, radius: Int): Drawable{
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.setColor(fillResColor)
        drawable.setStroke(strokeWidth, strokeResColor)
        drawable.cornerRadius = radius.toFloat()
        return drawable
    }
    fun createGradientColorDrawable(colors: IntArray, orientation: GradientDrawable.Orientation): Drawable {
        val drawable = GradientDrawable(orientation, colors)
        drawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        return drawable
    }
}