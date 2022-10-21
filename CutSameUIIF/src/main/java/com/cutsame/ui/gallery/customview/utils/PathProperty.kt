package com.cutsame.ui.gallery.customview.utils

import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.util.Property

class PathProperty<T>(private val mProperty: Property<T, PointF?>?, path: Path?) :
    Property<T, Float>(Float::class.java, mProperty?.name) {
    private val mPathMeasure: PathMeasure
    private val mPathLength: Float
    private val mPosition = FloatArray(2)
    private val mPointF = PointF()
    private var mCurrentFraction = 0f
    override fun get(`object`: T): Float {
        return mCurrentFraction
    }

    override fun set(target: T, fraction: Float) {
        mCurrentFraction = fraction
        mPathMeasure.getPosTan(mPathLength * fraction, mPosition, null as FloatArray?)
        mPointF.x = mPosition[0]
        mPointF.y = mPosition[1]
        mProperty?.set(target, mPointF)
    }

    init {
        mPathMeasure = PathMeasure(path, false)
        mPathLength = mPathMeasure.length
    }
}