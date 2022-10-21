package com.cutsame.ui.gallery.customview

import android.content.Context
import androidx.customview.widget.ViewDragHelper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout


class GalleryDragLayout : FrameLayout {

    private val dragHelper: ViewDragHelper
    private var dragOriginLeft = 0
    private var dragOriginTop = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        dragHelper = ViewDragHelper.create(this, Callback())
    }

    inner class Callback : ViewDragHelper.Callback() {
        override fun tryCaptureView(p0: View, p1: Int): Boolean {
            return true
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return Math.max(top, dragOriginTop)
        }

        override fun onViewCaptured(capturedChild: View, activePointerId: Int) {
            super.onViewCaptured(capturedChild, activePointerId)
            dragOriginLeft = capturedChild.left
            dragOriginTop = capturedChild.top
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            dragHelper.settleCapturedViewAt(dragOriginLeft, dragOriginTop)
            invalidate()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return dragHelper.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        dragHelper.processTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        if (dragHelper.continueSettling(true)) {
            invalidate()
        }
    }
}