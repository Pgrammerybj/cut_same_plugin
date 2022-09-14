package com.cutsame.ui.utils

import android.content.Context
import android.os.PowerManager

object ScreenUtil {

    fun isScreenOn(context: Context): Boolean{
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isScreenOn
    }
}