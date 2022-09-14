package com.ss.ugc.android.editor.track.fuctiontrack

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.OverScroller
import com.ss.ugc.android.editor.track.widget.ScrollState
import kotlin.math.abs

internal class TrackVerticallyScrollHelper(private val trackGroup: TrackGroup) {
    private var downY: Float = 0F
    private var lastY: Float = 0F

    private var state = ScrollState.IDLE

    private val viewConfig = ViewConfiguration.get(trackGroup.context)
    private val touchSlop = viewConfig.scaledTouchSlop
    private val minFlingVelocity = viewConfig.scaledMinimumFlingVelocity.toFloat()
    private val maxFlingVelocity = viewConfig.scaledMaximumFlingVelocity.toFloat()

    private var velocityTracker: VelocityTracker? = null

    private val scroller = OverScroller(trackGroup.context)

    private var prepareClick = true

    private var disableScroll: Boolean = false

    fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            ACTION_DOWN -> {
                downY = event.y
                lastY = downY
                scroller.abortAnimation()
            }

            ACTION_MOVE -> {
                val deltaY = event.y - downY
                if (touchSlop <= abs(deltaY)) {
                    state = ScrollState.DRAGGING
                    trackGroup.requestDisallowInterceptTouchEvent(true)
                }
            }
        }

        return state == ScrollState.DRAGGING
    }

    @SuppressLint("Recycle")
    fun onTouchEvent(event: MotionEvent): Boolean {
        val tracker = velocityTracker ?: VelocityTracker.obtain()
        if (velocityTracker == null) {
            velocityTracker = tracker
        }
        tracker.addMovement(event)

        when (event.action) {
            ACTION_DOWN -> {
                downY = event.y
                lastY = downY
                prepareClick = true
            }

            ACTION_MOVE -> {
                if (state == ScrollState.DRAGGING) {
                    trackGroup.scrollBy(
                        0,
                        if (disableScroll) 0 else (lastY - event.y).toInt(),
                        invokeChangeListener = false
                    )
                } else {
                    val deltaY = event.y - downY
                    if (touchSlop <= abs(deltaY)) {
                        state = ScrollState.DRAGGING
                        trackGroup.requestDisallowInterceptTouchEvent(true)
                    }
                }
                lastY = event.y
                prepareClick = state != ScrollState.DRAGGING
            }

            ACTION_UP -> {
                if (state == ScrollState.DRAGGING) {
                    tracker.computeCurrentVelocity(1000, maxFlingVelocity)
                    if (!disableScroll) flingVertically(-pruneVelocity(tracker.yVelocity))
                } else if (prepareClick) {
                    trackGroup.blankClickListener?.onClick(trackGroup)
                }
                resetState()
            }

            else -> {
                resetState()
            }
        }

        return true
    }

    private fun resetState() {
        downY = 0F
        lastY = 0F
        velocityTracker?.recycle()
        velocityTracker = null
        trackGroup.requestDisallowInterceptTouchEvent(false)
        state = ScrollState.IDLE
    }

    private fun pruneVelocity(velocity: Float): Float {
        return if (abs(velocity) < minFlingVelocity) {
            0F
        } else if (abs(velocity) > maxFlingVelocity) {
            if (velocity > 0) maxFlingVelocity else -maxFlingVelocity
        } else {
            velocity
        }
    }

    private fun flingVertically(velocityY: Float) {
        if (velocityY == 0F) return

        scroller.fling(
            trackGroup.scrollX,
            trackGroup.scrollY,
            0,
            velocityY.toInt(),
            0,
            trackGroup.maxScrollX,
            0,
            trackGroup.maxScrollY
        )
        trackGroup.postInvalidateOnAnimation()
    }

    fun disableScroll(disable: Boolean) {
        disableScroll = disable
    }

    fun smoothScrollVerticallyBy(y: Int) {
        if (y == 0) return

        scroller.startScroll(trackGroup.scrollX, trackGroup.scrollY, 0, y)
        trackGroup.postInvalidateOnAnimation()
    }

    fun computeScroll() {
        val notFinished = scroller.computeScrollOffset()
        if (notFinished) {
            trackGroup.scrollTo(
                trackGroup.scrollX,
                scroller.currY,
                invokeChangeListener = false,
                disablePruneX = false,
                disablePruneY = false
            )
            trackGroup.postInvalidateOnAnimation()
        }
    }
}
