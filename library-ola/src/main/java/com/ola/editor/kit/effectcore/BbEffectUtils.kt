package com.ola.editor.kit.effectcore

import android.util.Log
import com.ola.editor.kit.BuildConfig

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