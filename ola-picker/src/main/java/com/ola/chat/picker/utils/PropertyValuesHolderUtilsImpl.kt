package com.ola.chat.picker.utils

import android.animation.PropertyValuesHolder
import android.graphics.Path
import android.graphics.PointF
import android.util.Property

interface PropertyValuesHolderUtilsImpl {
    fun ofPointF(var1: Property<*, PointF?>?, var2: Path?): PropertyValuesHolder?

}