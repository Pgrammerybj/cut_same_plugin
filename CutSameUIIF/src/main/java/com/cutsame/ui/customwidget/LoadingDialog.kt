package com.cutsame.ui.customwidget

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.cutsame.ui.R

/**
 * 加载弹窗
 */
class LoadingDialog(context: Context) : AlertDialog(context, R.style.TransparentDialog) {
    private var mCreated: Boolean = false
    private lateinit var progressLoading: LottieAnimationView
    private lateinit var functionTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var root: View

    private var message: CharSequence = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_progressdialog)
        initView()
        mCreated = true
        setCanceledOnTouchOutside(false)
    }

    private fun initView() {
        root = findViewById(R.id.root)
        progressLoading = findViewById(R.id.progressLoading)
        progressTextView = findViewById(R.id.progressTextView)
        functionTextView = findViewById(R.id.functionTextView)
    }

    override fun setMessage(message: CharSequence) {
        this.message = message
    }

    override fun show() {
        super.show()
        functionTextView.text = message
    }

    override fun dismiss() {
        if (isShowing) {
            progressLoading.cancelAnimation()
            super.dismiss()
        }
    }


}