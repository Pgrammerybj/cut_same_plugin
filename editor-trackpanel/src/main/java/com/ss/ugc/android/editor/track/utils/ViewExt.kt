package com.ss.ugc.android.editor.track.utils

import android.app.Activity
import android.util.Size
import android.view.View
import android.view.ViewGroup

/**
 * @since 17/6/27
 * @author joffychim
 */

var View.visible: Boolean
    get() = this.visibility == View.VISIBLE
    set(value) {
        this.visibility = if (value) View.VISIBLE else View.GONE
    }

fun View.gone() {
    this.visibility = View.GONE
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.INVISIBLE
}

fun View.safelyPerformHapticFeedback(feedbackConstant: Int): Boolean {
    return kotlin.runCatching {
        performHapticFeedback(feedbackConstant)
    }.onFailure {
//        EnsureManager.ensureNotReachHere(it)
    }.getOrDefault(false)
}

fun View.safelyPerformHapticFeedback(feedbackConstant: Int, flags: Int): Boolean {
    return kotlin.runCatching {
        performHapticFeedback(feedbackConstant, flags)
    }.onFailure {
//        EnsureManager.ensureNotReachHere(it)
    }.getOrDefault(false)
}



fun View.getLocationOnScreen(): Size {

    val appScreenLocation = IntArray(2) { 0 }
    val rootLayout =
        (context as? Activity)?.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0)
            ?: rootView
    rootLayout.getLocationOnScreen(appScreenLocation)

    val screenLocation = IntArray(2) { 0 }
    getLocationOnScreen(screenLocation)

    val drawingLocation = IntArray(2) { 0 }
    drawingLocation[0] = screenLocation[0] - appScreenLocation[0]
    drawingLocation[1] = screenLocation[1] - appScreenLocation[1]

    return Size(drawingLocation[0], drawingLocation[1])
}