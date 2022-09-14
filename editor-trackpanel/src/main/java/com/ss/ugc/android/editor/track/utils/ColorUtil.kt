package com.ss.ugc.android.editor.track.utils


import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import com.ss.ugc.android.editor.base.logger.ILog
import java.util.Locale

object ColorUtil {
    private val rgbRegex = Regex("^#[0-9A-Fa-f]{6}$")
    private val rgbaRegex = Regex("^#[0-9A-Fa-f]{8}$")

    fun toStr(@ColorInt color: Int): String = String.format(
        "#%06X",
        0xFFFFFF and color
    ).toLowerCase(Locale.getDefault())

    @ColorInt
    fun adjustAlpha(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) alpha: Float): Int {
        val newAlpha = (Color.alpha(color) * alpha).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(newAlpha, red, green, blue)
    }

    fun argbIntToRgbaStr(colorInt: Int): String {
        return if (colorInt == Color.TRANSPARENT) "" else String.format(
            "#%08X", colorInt shl 8 or (colorInt ushr 24)
        )
    }

    fun rgbaStrToArgbInt(colorStr: String?, defaultColor: Int = Color.WHITE): Int {
        if (colorStr == null || colorStr.isNullOrBlank()) {
            return defaultColor
        }
        return if (!colorStr.startsWith("#")) {
            try {
                colorStr.toInt()
            } catch (e: Throwable) {
                ILog.e("ColorUtils", "rgbaStrToArgbInt error: colorStr = $colorStr")
                return defaultColor
            }
        } else {
            val rgbaStr = when {
                colorStr.matches(rgbRegex) -> "${colorStr}FF"
                colorStr.matches(rgbaRegex) -> colorStr
                else -> return defaultColor
            }
            val rgba = Color.parseColor(rgbaStr)
            val aa = rgba shl 24
            val rgb = rgba ushr 8
            rgb or aa
        }
    }
}
