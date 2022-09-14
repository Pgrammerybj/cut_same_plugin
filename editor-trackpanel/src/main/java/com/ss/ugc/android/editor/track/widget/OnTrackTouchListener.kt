package com.ss.ugc.android.editor.track.widget

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.utils.SizeUtil

interface OnTrackDragListener {
    fun beginDrag(downX: Float, downY: Float)

    fun drag(deltaX: Float, deltaY: Float, isLeftScreenBorder: Boolean = false, isRightScreenBorder: Boolean = false)

    fun endDrag()
}

class OnTrackTouchListener(
    private val clickListener: (() -> Unit),
    private val dragListener: OnTrackDragListener
) : View.OnTouchListener {

    private val touchSlop = ViewConfiguration.get(TrackSdk.application).scaledTouchSlop

    private val handler = Handler(Looper.getMainLooper())

    private val longPressTimeout: Long = ViewConfiguration.getLongPressTimeout().toLong()
    private val longPressRunnable: Runnable = Runnable {
        beginDragging = true
        dragListener.beginDrag(downX, downY)
    }

    private var downX = -1F
    private var downY = -1F
    private var downTime = 0L

    private var brokeClick = false

    private var beginDragging = false

    private var downRawX: Float = -1F
    private var downRawY: Float = -1F

    private var moveX: Float = -1F
    private var moveY: Float = -1F

    private var screenWidth = SizeUtil.getScreenWidth(TrackSdk.application)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x + (v?.left ?: 0)
                downY = event.y
                downRawX = event.rawX
                downRawY = event.rawY
                moveX = event.rawX
                downTime = System.currentTimeMillis()

                handler.postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE -> {
                val disX = event.rawX - moveX
                val disY = event.rawY - moveY
                moveX = event.rawX
                moveY = event.rawY
                val deltaX = event.rawX - downRawX
                val deltaY = event.rawY - downRawY
                if (beginDragging) {
                    dragListener.drag(disX, disY, isOnLeftScreenBorder(event.rawX), isOnRightScreenBorder(event.rawX))
                    return true
                }

                if (!brokeClick && touchSlop * touchSlop <= deltaX * deltaX + deltaY * deltaY) {
                    handler.removeCallbacks(longPressRunnable)
                    brokeClick = true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (beginDragging) {
                    dragListener.endDrag()
                    beginDragging = false
                } else if (!brokeClick) {
                    handler.removeCallbacks(longPressRunnable)
                    clickListener()
                }

                brokeClick = false
            }

            MotionEvent.ACTION_CANCEL -> {
                if (beginDragging) {
                    //todo double check annotation ？
//                    dragListener.endDrag()
                    beginDragging = false
                }

                if (brokeClick) {
                    brokeClick = false
                } else {
                    handler.removeCallbacks(longPressRunnable)
                }
            }
        }

        return true
    }

    private fun isOnLeftScreenBorder(rawX: Float): Boolean {
        if (rawX - TrackConfig.THUMB_WIDTH < 0) {
            return true
        }
        return false
    }

    private fun isOnRightScreenBorder(rawX: Float): Boolean {
        if (rawX + TrackConfig.THUMB_WIDTH > screenWidth) {
            return true
        }
        return false
    }
}
