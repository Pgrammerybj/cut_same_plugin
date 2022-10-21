package com.cutsame.ui.gallery.customview.scale

interface ScaleGestureLayoutListener {

    fun onAlphaPercent(percent: Float)

    fun onClick()

    fun onExit()

    fun onScaleBegin()

    fun onScaleEnd(scaleFactor: Float)
}