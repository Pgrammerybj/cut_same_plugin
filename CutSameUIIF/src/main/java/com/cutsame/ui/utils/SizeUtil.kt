package com.cutsame.ui.utils

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
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

    fun getScreenWidth(context: Context): Int {
        return getDisplayMetrics(context).widthPixels
    }

    fun getScreenHeight(context: Context): Int {
        return getDisplayMetrics(context).heightPixels
    }

    private fun getDisplayMetrics(context: Context): DisplayMetrics {
        val metric = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metric)
        return metric
    }

}