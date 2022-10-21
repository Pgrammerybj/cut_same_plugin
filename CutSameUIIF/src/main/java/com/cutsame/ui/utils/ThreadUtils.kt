package com.cutsame.ui.utils

import android.os.Handler
import android.os.Looper

private val uiHandler: Handler by lazyOf(Handler(Looper.getMainLooper()))

val isUiThread: Boolean
    get() = Looper.getMainLooper() == Looper.myLooper()

fun postOnUiThread(delayMs: Long = 0, block: () -> Unit) {
    if (delayMs > 0) {
        uiHandler.postDelayed(Runnable {
            block()
        }, delayMs)
    } else {
        uiHandler.post {
            block()
        }
    }
}

fun runOnUiThread(delayMs: Long = 0, block: () -> Unit) {
    if (delayMs > 0) {
        uiHandler.postDelayed(Runnable {
            block()
        }, delayMs)
    } else {
        if (isUiThread) {
            block()
        } else {
            uiHandler.post {
                block()
            }
        }
    }
}

fun postOnUiThread(delayMs: Long = 0, run: Runnable) {
    if (delayMs > 0) {
        uiHandler.postDelayed(run, delayMs)
    } else {
        uiHandler.post(run)
    }
}

fun removeOnUiThread(run: Runnable) {
    uiHandler.removeCallbacks(run)
}