package com.angelstar.ola.effectcore

import android.util.Log
import com.angelstar.ola.BuildConfig

object BbEffectUtils {

  private const val DEBUG = true

  fun logD(tag: String, msg: String) {
    if (BuildConfig.DEBUG || DEBUG) {
      Log.d(tag, msg)
    }
  }

  fun logE(tag: String, msg: String, e: Throwable? = null) {
    Log.e(tag, msg, e)
  }
}