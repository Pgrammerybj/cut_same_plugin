package com.ola.chat.picker.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.annotation.IntDef
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import com.ola.chat.picker.R

@IntDef(Toast.LENGTH_LONG, Toast.LENGTH_SHORT)
@Retention(AnnotationRetention.SOURCE)
annotation class ToastDuration

private var lastToast: Toast? = null

@SuppressLint("InflateParams")
fun showToast(
    context: Context,
    textView: TextView,
    @ToastDuration duration: Int = Toast.LENGTH_SHORT
) {
    runOnUiThread {
        lastToast?.cancel()
        val newToast = Toast(context).apply {
            this.duration = duration
            view = textView
            setGravity(Gravity.CENTER, 0, 0)
        }
        newToast.show()
        lastToast = newToast
    }
}

fun showSuccessTipToast(context: Context, msg: String) {
    val tvToast: TextView = TextView(context).apply {
        gravity = Gravity.CENTER
        text = msg
        setBackgroundResource(R.drawable.bg_toast_tip)
        includeFontPadding = true
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12F)
        setTextColor(Color.WHITE)
        setPadding(SizeUtil.dp2px(11F), SizeUtil.dp2px(8F), SizeUtil.dp2px(11F), SizeUtil.dp2px(8F))
    }
    showToast(context, tvToast)
}

fun showErrorTipToast(context: Context, msg: String) {
    val tvToast: TextView = TextView(context).apply {
        gravity = Gravity.CENTER
        text = msg
        setBackgroundResource(R.drawable.bg_toast_tip)
        includeFontPadding = true
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12F)
        setTextColor(Color.WHITE)
        setPadding(SizeUtil.dp2px(11F), SizeUtil.dp2px(8F), SizeUtil.dp2px(11F), SizeUtil.dp2px(8F))
    }
    showToast(context, tvToast)
}



