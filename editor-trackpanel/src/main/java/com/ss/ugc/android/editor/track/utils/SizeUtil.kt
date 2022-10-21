package com.ss.ugc.android.editor.track.utils


import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.view.WindowManager

object SizeUtil {
    private const val ERROR_SHRINK = 0.5F

    private val density: Float
        get() = Resources.getSystem().displayMetrics.density

    private val scaleDensity: Float
        get() = Resources.getSystem().displayMetrics.scaledDensity

    fun dp2px(dp: Float): Int {
        return (dp * density + ERROR_SHRINK).toInt()
    }

    fun px2dp(px: Int): Float {
        return px.toFloat() / density + ERROR_SHRINK
    }

    fun sp2px(sp: Float): Int {
        return (sp * scaleDensity + ERROR_SHRINK).toInt()
    }

    @SuppressLint("NewApi")
    fun getScreenSize(context: Context): Point {
        val point = Point()
        val wm = (context.getSystemService(Context.WINDOW_SERVICE)) as WindowManager
        wm.defaultDisplay.getRealSize(point)
        return point
    }

    fun getScreenWidth(context: Context): Int {
        return getScreenSize(context).x
    }

    fun getScreenHeight(context: Context): Int {
        return getScreenSize(context).y
    }
}
