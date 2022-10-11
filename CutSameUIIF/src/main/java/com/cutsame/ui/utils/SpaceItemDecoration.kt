package com.cutsame.ui.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/8 20:04
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 该类用于给 recyclerView 的每个 item 设置间隔距离
 */
class SpaceItemDecoration (var top: Int,var bottom: Int,var left:Int,var right: Int): RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = top
        outRect.bottom = bottom
        outRect.left = left
        outRect.right = right
    }
}