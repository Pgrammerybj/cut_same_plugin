package com.ss.ugc.android.editor.track.data

import android.graphics.PointF

data class SpeedInfo(
    var normalSpeed: Float = 1.0F,
    var resourceId: String = "",
    var mode: Int = 0,
    var name: String = "",
    var aveSpeed: Float = 1.0F,
    var curveSpeed: List<PointF>? = null
) {
    companion object {
        const val NORMAL_SPEED = 0
        const val CURVE_SPEED = 1
    }
}