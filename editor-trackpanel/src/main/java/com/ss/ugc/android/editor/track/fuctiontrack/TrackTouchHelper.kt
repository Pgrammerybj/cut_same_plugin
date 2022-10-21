package com.ss.ugc.android.editor.track.fuctiontrack

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.ss.ugc.android.editor.track.TrackSdk

internal interface TrackDragListener {
    fun beginDrag()

    fun drag(rawX: Float, rawY: Float, deltaX: Float, deltaY: Float)

    fun endDrag()
}

internal class TrackTouchHelper(
    private val dragListener: TrackDragListener,
    private val clickListener: (Float) -> Unit,
    private val doubleClickListener: () -> Unit
) : View.OnTouchListener {

    private val touchSlop = ViewConfiguration.get(TrackSdk.application).scaledTouchSlop

    private val handler = Handler(Looper.getMainLooper())

    private val longPressTimeout: Long = ViewConfiguration.getLongPressTimeout().toLong()
    private val longPressRunnable: Runnable = Runnable {
        beginDragging = true
        dragListener.beginDrag()
    }

    private val clickRunnable = Runnable { clickListener(lastClickX) }

    private var lastX = 0F
    private var lastY = 0F
    private var downTime = 0L

    private var brokeClick = false

    private var beginDragging = false

    private var lastUpEventTime = 0L
    private var lastClickX = 0F

    @SuppressLint("ClickableViewAccessibility")
    @Suppress("ComplexMethod")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                downTime = System.currentTimeMillis()

                handler.postDelayed(longPressRunnable, longPressTimeout)
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - lastX
                val deltaY = event.rawY - lastY
                if (beginDragging) {
                    dragListener.drag(event.rawX, event.rawY, deltaX, deltaY)
                }

                if (!brokeClick && touchSlop * touchSlop <= deltaX * deltaX + deltaY * deltaY) {
                    handler.removeCallbacks(longPressRunnable)
                    brokeClick = true
                }

                lastX = event.rawX
                lastY = event.rawY
            }

            MotionEvent.ACTION_UP -> {
                if (beginDragging) {
                    dragListener.endDrag()
                    beginDragging = false
                } else if (!brokeClick) {
                    handler.removeCallbacks(longPressRunnable)
                    if (System.currentTimeMillis() - lastUpEventTime < 200) {
                        handler.removeCallbacks(clickRunnable)
                        doubleClickListener()
                    } else {
                        lastClickX = event.x
                        handler.postDelayed(clickRunnable, 200)
                    }
                }

                lastUpEventTime = System.currentTimeMillis()

                brokeClick = false
            }

            MotionEvent.ACTION_CANCEL -> {
                if (beginDragging) {
                    dragListener.endDrag()
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
}
