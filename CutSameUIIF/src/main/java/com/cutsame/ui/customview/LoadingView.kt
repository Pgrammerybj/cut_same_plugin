package com.cutsame.ui.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.cutsame.ui.R

class LoadingView(context: Context, attrs: AttributeSet? = null): FrameLayout(context, attrs) {
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var functionTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var mRootView: View
    private var isShowing = true

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        mRootView = LayoutInflater.from(context).inflate(R.layout.layout_stickpoint_cancelable_progressdialog, this, true)
        progressLoading = mRootView.findViewById(R.id.progressLoading)
        progressTextView = mRootView.findViewById(R.id.progressTextView)
        functionTextView = mRootView.findViewById(R.id.functionTextView)
    }

    fun setProgress(value: Int) {
        progressTextView.text = "$value%"
    }

    fun setMessage(message: String) {
        functionTextView.text = message
    }

    fun show(){
        isShowing = true
        this.visibility = View.VISIBLE
    }

    fun dismiss(){
        progressLoading.cancelAnimation()
        isShowing = false
        this.visibility= View.GONE
    }

    fun isShowing(): Boolean{
        return isShowing
    }


}