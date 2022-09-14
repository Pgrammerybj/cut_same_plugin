package com.ss.ugc.android.editor.track.utils

import java.util.Locale

object FormatUtil {
    fun formatLabelTime(timeInMillis: Long): String {
        return if (timeInMillis >= 60 * 1000) {
            val minute = timeInMillis / 1000 / 60
            val second = timeInMillis / 1000 % 60
            String.format(Locale.ENGLISH, "%02d:%02d", minute.toInt(), second.toInt())
        } else {
            val second = timeInMillis / 1000.0F
            String.format(Locale.ENGLISH, "%.1fs", second)
        }
    }
}
