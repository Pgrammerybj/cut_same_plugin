package com.cutsame.ui.gallery.customview.scale

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.view.GestureDetectorCompat
import com.cutsame.ui.cut.videoedit.customview.gesture.MoveGestureDetector
import com.cutsame.ui.cut.videoedit.customview.gesture.ScaleGestureDetector

class CombinationGestureDetector(context: Context, var listener: CombinationGestureListener?) {
    private val doubleTapListener = object : GestureDetector.OnDoubleTapListener {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return listener?.onSingleTapConfirmed(e) ?: false
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            return listener?.onDoubleTap(e) ?: false
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            return false
        }
    }

    private val gestureListener = object : GestureDetector.OnGestureListener {
        override fun onDown(e: MotionEvent): Boolean {
            return listener?.onDown(e) ?: false
        }

        override fun onShowPress(e: MotionEvent?) {
        }

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            return false
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            return listener?.onScroll(e1, e2, distanceX, distanceY) ?: false
        }

        override fun onLongPress(e: MotionEvent) {
            listener?.onLongPress(e)
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            return listener?.onFling(e1, e2, velocityX, velocityY) ?: false
        }
    }

    private val scaleGestureListener = object : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
            return listener?.onScale(detector) ?: false
        }

        override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
            return listener?.onScaleBegin(detector) ?: false
        }

        override fun onScaleEnd(view: View, detector: ScaleGestureDetector) {
            listener?.onScaleEnd(detector)
        }
    }

    private val moveGestureListener = object : MoveGestureDetector.OnMoveGestureListener {
        override fun onMoveBegin(
            detector: MoveGestureDetector,
            downX: Float,
            downY: Float
        ): Boolean {
            return true
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return true
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {
            listener?.onMoveEnd(detector)
        }
    }

    private val gestureDetector = GestureDetectorCompat(context, gestureListener)
    private var scaleDetector = ScaleGestureDetector(scaleGestureListener)
    private val moveGestureDetector = MoveGestureDetector(context, moveGestureListener)

    init {
        gestureDetector.setOnDoubleTapListener(doubleTapListener)
    }

    fun onTouchEvent(view: View, motionEvent: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(view, motionEvent)
        moveGestureDetector.onTouchEvent(motionEvent)
        gestureDetector.onTouchEvent(motionEvent)
        return true
    }
}