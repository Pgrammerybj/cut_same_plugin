package com.cutsame.ui.gallery.customview.utils

import android.content.Context
import android.graphics.Path
import android.util.AttributeSet

abstract class PathMotion {
    open fun PathMotion() {}

    open fun PathMotion(context: Context?, attrs: AttributeSet?) {}

    abstract fun getPath(var1: Float, var2: Float, var3: Float, var4: Float): Path?
}