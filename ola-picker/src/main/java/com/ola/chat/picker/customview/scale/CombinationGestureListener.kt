package com.ola.chat.picker.customview.scale

import android.view.MotionEvent
import com.ola.chat.picker.customview.gesture.MoveGestureDetector

interface CombinationGestureListener {

    /**
     * 第一个手指按下
     */
    fun onDown(e: MotionEvent): Boolean
    fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean
    fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean

    fun onMoveEnd(detector: MoveGestureDetector)

    fun onSingleTapConfirmed(event: MotionEvent): Boolean
    fun onLongPress(event: MotionEvent)

    fun onScaleBegin(detector: com.ola.chat.picker.customview.gesture.ScaleGestureDetector): Boolean
    fun onScale(detector: com.ola.chat.picker.customview.gesture.ScaleGestureDetector): Boolean
    fun onScaleEnd(detector: com.ola.chat.picker.customview.gesture.ScaleGestureDetector)

    fun onDoubleTap(e: MotionEvent): Boolean
}