package com.cutsame.ui.customwidget

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

class DisableScrollViewPager @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : ViewPager(context, attributeSet) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }
}