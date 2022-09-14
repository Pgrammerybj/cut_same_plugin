package com.cutsame.ui.exten

import android.content.res.Resources
import android.view.View

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

private val density: Float
    get() = Resources.getSystem().displayMetrics.density

fun Int.dp(): Int {
    return (this * density + 0.5f).toInt()
}

fun Int.px(): Int {
    return (this.toFloat() / density + 0.5f).toInt()
}

fun Float.dp(): Float {
    return this * density
}

fun Float.px(): Float {
    return this / density
}
