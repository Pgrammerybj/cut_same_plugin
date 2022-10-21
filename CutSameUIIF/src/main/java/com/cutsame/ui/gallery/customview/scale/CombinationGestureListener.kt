package com.cutsame.ui.gallery.customview.scale

import android.view.MotionEvent
import com.cutsame.ui.cut.videoedit.customview.gesture.MoveGestureDetector
import com.cutsame.ui.cut.videoedit.customview.gesture.ScaleGestureDetector

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

    fun onScaleBegin(detector: ScaleGestureDetector): Boolean
    fun onScale(detector: ScaleGestureDetector): Boolean
    fun onScaleEnd(detector: ScaleGestureDetector)

    fun onDoubleTap(e: MotionEvent): Boolean
}