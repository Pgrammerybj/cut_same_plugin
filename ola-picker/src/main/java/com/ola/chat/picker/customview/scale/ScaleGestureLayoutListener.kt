package com.ola.chat.picker.customview.scale

interface ScaleGestureLayoutListener {

    fun onAlphaPercent(percent: Float)

    fun onClick()

    fun onExit()

    fun onScaleBegin()

    fun onScaleEnd(scaleFactor: Float)
}