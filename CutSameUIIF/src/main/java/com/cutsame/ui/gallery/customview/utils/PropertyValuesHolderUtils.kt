package com.cutsame.ui.gallery.customview.utils

import android.animation.PropertyValuesHolder
import android.graphics.Path
import android.graphics.PointF
import android.util.Property

object PropertyValuesHolderUtils {
    private val IMPL: PropertyValuesHolderUtilsImpl = PropertyValuesHolderUtilsApi()

    fun ofPointF(property: Property<*, PointF?>?, path: Path?): PropertyValuesHolder? {
        return IMPL.ofPointF(property, path)
    }
}