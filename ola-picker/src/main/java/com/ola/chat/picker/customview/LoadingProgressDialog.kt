package com.ola.chat.picker.customview

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.ola.chat.picker.R

class LoadingProgressDialog(context: Context) : AlertDialog(context, R.style.DialogFullscreen) {
    private var mCreated: Boolean = false
    private var mMessage: CharSequence? = null
    private var mProgressIntValue: Int = 0
    private var mMaxProgress = 100

    private lateinit var progressLoading: LottieAnimationView
    private lateinit var functionTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var mRootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_stickpoint_cancelable_progressdialog)
        initView()
        mCreated = true
        setMessage(mMessage)
        setMax(mMaxProgress)
        setProgress(mProgressIntValue)
        setCanceledOnTouchOutside(false)
    }

    private fun initView() {
        mRootView = findViewById(R.id.root)
        progressTextView = findViewById(R.id.progressTextView)
        functionTextView = findViewById(R.id.functionTextView)
        progressLoading = findViewById(R.id.progressLoading)
    }

    fun setProgress(value: Int) {
        if (mCreated) {
            progressTextView.text = "$value%"
            //因为这个控件有在0%闪烁的问题，所以初始化置为gone了
        }
        mProgressIntValue = value
    }


    override fun setMessage(message: CharSequence?) {
        if (mCreated) {
            functionTextView.text = message
            functionTextView.visibility = if (TextUtils.isEmpty(message)) View.GONE else View.VISIBLE
        }
        mMessage = message
    }

    override fun dismiss() {
        if(isShowing) {
            progressLoading.cancelAnimation()
            super.dismiss()
        }
    }

    private fun setMax(maxProgress: Int) {
        mMaxProgress = maxProgress
    }

}