package com.ola.chat.picker.utils

import android.animation.PropertyValuesHolder
import android.animation.TypeConverter
import android.graphics.Path
import android.graphics.PointF
import android.util.Property

class PropertyValuesHolderUtilsApi : PropertyValuesHolderUtilsImpl {
    override fun ofPointF(property: Property<*, PointF?>?, path: Path?): PropertyValuesHolder? {
        return PropertyValuesHolder.ofObject(
            property,
            null as TypeConverter<PointF?, PointF?>?,
            path
        )
    }
}