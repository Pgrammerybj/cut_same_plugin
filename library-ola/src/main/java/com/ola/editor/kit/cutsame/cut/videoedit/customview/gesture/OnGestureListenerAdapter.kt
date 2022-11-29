package com.ola.editor.kit.cutsame.cut.videoedit.customview.gesture

import android.graphics.Canvas
import android.view.MotionEvent

abstract class OnGestureListenerAdapter : OnGestureListener {

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent?,
        diffX: Float,
        diffY: Float
    ): Boolean {
        return false
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        return false
    }

    override fun onDoubleClick(e: MotionEvent?): Boolean {
        return false
    }

    override fun onScaleBegin(scaleFactor: ScaleGestureDetector?): Boolean {
        return true
    }

    override fun onScale(scaleFactor: ScaleGestureDetector?): Boolean {
        return false
    }

    override fun onScaleEnd(scaleFactor: Float): Boolean {
        return true
    }

    override fun onRotationBegin(detector: RotateGestureDetector?): Boolean {
        return false
    }

    override fun onRotation(angle: Float): Boolean {
        return false
    }

    override fun onRotationEnd(angle: Float): Boolean {
        return false
    }

    override fun onDown(event: MotionEvent): Boolean {
        return false
    }

    override fun onUp(event: MotionEvent): Boolean {
        return false
    }

    override fun onPointerDown(): Boolean {
        return false
    }

    override fun onPointerUp(): Boolean {
        return false
    }

    override fun onMove(detector: MoveGestureDetector): Boolean {
        return false
    }

    override fun onMoveBegin(detector: MoveGestureDetector?, downX: Float, downY: Float): Boolean {
        return true
    }

    override fun onMoveEnd(detector: MoveGestureDetector?) {
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun dispatchDraw(canvas: Canvas?) {
    }
}
