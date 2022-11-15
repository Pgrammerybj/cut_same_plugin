package com.volcengine.base

import android.annotation.SuppressLint
import android.content.Context

/**
 *Author: gaojin
 *Time: 2020/4/23 2:19 PM
 */

@SuppressLint("StaticFieldLeak")
object AppSingleton {
    @Volatile
    lateinit var instance: Context

    fun bindInstance(context: Context) {
        instance = context
    }
}