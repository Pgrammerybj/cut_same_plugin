package com.cutsame.ui.gallery.customview.scale

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF

interface ListItemScaleType {
    fun getMatrix(var1: RectF, var2: Rect): Matrix
}