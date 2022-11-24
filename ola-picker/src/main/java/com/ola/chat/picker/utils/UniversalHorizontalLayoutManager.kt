package com.ola.chat.picker.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/20 21:12
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 通用的RecycleView的线性横向布局管理器
 */
class UniversalHorizontalLayoutManager(context: Context) :
    LinearLayoutManager(context, HORIZONTAL, false) {
    override fun smoothScrollToPosition(
        recyclerView: RecyclerView?,
        state: RecyclerView.State?,
        position: Int
    ) {
        val linearSmoothScroller = object :
            androidx.recyclerview.widget.LinearSmoothScroller(recyclerView!!.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return super.calculateSpeedPerPixel(displayMetrics) * 2
            }

            override fun calculateDxToMakeVisible(
                view: View?,
                snapPreference: Int
            ): Int {
                val layoutManager = this.layoutManager
                return if (layoutManager != null && layoutManager.canScrollHorizontally()) {
                    val params =
                        view!!.layoutParams as RecyclerView.LayoutParams
                    val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
                    val right =
                        layoutManager.getDecoratedRight(view) + params.rightMargin
                    val start = layoutManager.paddingLeft
                    val end = layoutManager.width - layoutManager.paddingRight
                    return start + (end - start) / 2 - (right - left) / 2 - left
                } else {
                    0
                }
            }
        }
        linearSmoothScroller.targetPosition = position
        startSmoothScroll(linearSmoothScroller)
    }
}