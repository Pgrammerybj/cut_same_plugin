package com.ss.ugc.android.editor.core.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.widget.Toast


interface IToast {
    fun onToast(context: Context, message: String, duration: Int? = null)
}

object DefaultToast : IToast {

    private var toast: Toast? = null

    @SuppressLint("ShowToast")
    override fun onToast(context: Context, message: String, duration: Int?) {
        val dur = duration ?: Toast.LENGTH_SHORT
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (toast == null) {
                toast = Toast.makeText(context, "", dur)
            }
            toast?.setText(message)
        }else{
            if (null != toast) {
                toast?.cancel()
                toast = null
            }
            toast = Toast.makeText(context, "", Toast.LENGTH_SHORT)
            toast?.setText(message)
        }
        toast?.show()
    }
}