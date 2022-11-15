package com.volcengine.base

import android.content.Context

/**
 *Author: gaojin
 *Time: 2022/6/10 00:55
 */

object UserData {

    private const val NAME = "thrall_user"
    private const val VERSION = "versionCode"

    fun getVersion(context: Context): Int {
        return context.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(VERSION, 0)
    }

    fun setVersion(context: Context, version: Int) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().putInt(VERSION, version).apply()
    }
}