package com.ss.ugc.android.editor.track.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class MoveViewGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        const val TAG = "MoveViewGroup"
    }

    // 需要记录的数值
    private var downX = 0F // 第一次按下的x值
    var onMoveListener: ((dis: Float, rawX: Float) -> Unit)? = null
    var onMoveUpListener: ((dis: Float) -> Unit)? = null
    var onMoveDownListener: ((dis: Float) -> Unit)? = null
    var isTouchAble = true

    private var rawDownX = 0F
    private var rawUpX = 0F
    private var lastRawX = 0F

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!isTouchAble) {
            return false
        }
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                rawDownX = event.rawX
                downX = event.x
                lastRawX = event.rawX
                onMoveDownListener?.invoke(downX)
            }
            MotionEvent.ACTION_MOVE -> {
                onMoveListener?.invoke(event.rawX - lastRawX, event.rawX)
                lastRawX = event.rawX
            }
            MotionEvent.ACTION_UP -> {
                rawUpX = event.rawX
                onMoveUpListener?.invoke(rawUpX - rawDownX)
            }
        }

        return true
    }
}
