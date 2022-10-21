package com.ola.chat.picker.customview.gesture

import android.graphics.Canvas
import android.view.MotionEvent

interface OnGestureListener {

    fun onScroll(e1: MotionEvent?, e2: MotionEvent?, diffX: Float, diffY: Float): Boolean

    fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean

    fun onSingleTapConfirmed(e: MotionEvent?): Boolean

    fun onDoubleClick(e: MotionEvent?): Boolean

    fun onScaleBegin(detector: ScaleGestureDetector?): Boolean

    /**
     * @param scaleFactor
     * @return
     */
    fun onScale(scaleFactor: ScaleGestureDetector?): Boolean

    fun onScaleEnd(scaleFactor: Float): Boolean

    fun onRotationBegin(detector: RotateGestureDetector?): Boolean

    fun onRotation(angle: Float): Boolean

    fun onRotationEnd(angle: Float): Boolean

    /**
     * 第一个手指按下
     */
    fun onDown(event: MotionEvent): Boolean

    /**
     * 最后一个手指离开
     */
    fun onUp(event: MotionEvent): Boolean

    /**
     * 非第一个手指按下
     */
    fun onPointerDown(): Boolean

    /**
     * 非第一个手指离开
     */
    fun onPointerUp(): Boolean

    fun onMove(detector: MoveGestureDetector): Boolean

    fun onMoveBegin(detector: MoveGestureDetector?, downX: Float, downY: Float): Boolean

    fun onMoveEnd(detector: MoveGestureDetector?)

    fun onLongPress(e: MotionEvent)

    fun dispatchDraw(canvas: Canvas?)
}
