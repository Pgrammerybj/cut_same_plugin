package com.ss.ugc.android.editor.track.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs
import kotlin.math.max

class HorizontalScrollContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ScrollHandler {
    val realScrollX: Int
        get() {
            var childScrollX = 0
            forEachScroller { childScrollX = max(childScrollX, it.scrollX) }
            return childScrollX
        }

    init {
        isFocusable = true
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        isVerticalScrollBarEnabled = false
        isVerticalFadingEdgeEnabled = false
        isHorizontalScrollBarEnabled = false
        isHorizontalFadingEdgeEnabled = false
    }

    private val scrollerX: OverScroller by lazy { OverScroller(context) }

    private val viewConfig = ViewConfiguration.get(context)
    private val touchSlop = viewConfig.scaledTouchSlop
    private val minFlingVelocity = viewConfig.scaledMinimumFlingVelocity.toFloat()
    private val maxFlingVelocity = viewConfig.scaledMaximumFlingVelocity.toFloat()

    private var isCanScale = false
    var scaleGestureDetector: ScaleGestureDetector? = null

    private var lastX: Float = 0F
    private var downX: Float = 0F
    private var downY: Float = 0F

    private var scrollState = ScrollState.IDLE
        set(value) {
            if (field == value) return
            field = value
            val childScrollX = getChildScrollX()
            onScrollLStateChangeListener?.onScrollStateChanged(value, childScrollX, 0)
        }
    private var onScrollLStateChangeListener: OnScrollStateChangeListener? = null

    private var velocityTracker: VelocityTracker? = null

    private var performClick = false
    private var blankClickListener: OnClickListener? = null

    private var invokeChangeListener = true

    private var fingerStopListener: ((Int) -> Unit)? = null
    private val fingerStopRunner: Runnable by lazy {
        Runnable {
            fingerStopListener?.let {
                val childScrollX = getChildScrollX()
                it.invoke(childScrollX)
            }
        }
    }
    private var fingerStopX: Float = -1F

    private fun getChildScrollX(): Int {
        var childScrollX = 0
        forEachScroller {
            childScrollX = max(childScrollX, it.scrollX)
        }
        return childScrollX
    }

    private inline fun forEachScroller(block: (scroller: EditScroller) -> Unit) {
        for (index in 0 until childCount) {
            val child = getChildAt(index) as? EditScroller ?: continue
            block(child)
        }
    }

    override fun computeScroll() {
        if (!scrollerX.isFinished && scrollerX.computeScrollOffset()) {
            scrollToHorizontally(scrollerX.currX, invokeChangeListener)
            postInvalidateOnAnimation()
        } else {
            if (scrollState == ScrollState.SETTLING) {
                scrollState = ScrollState.IDLE
            }
        }
    }

    fun scrollToHorizontally(x: Int) {
        forEachScroller { it.scrollTo(x, it.scrollY, false) }
    }

    override fun scrollTo(x: Int, y: Int) {
        scrollToHorizontally(x, false)
    }

    fun scrollToHorizontally(x: Int, invokeChangeListener: Boolean) {
        this.invokeChangeListener = invokeChangeListener
        forEachScroller {
            it.scrollTo(
                x,
                it.scrollY,
                invokeChangeListener
            )
        }
    }

    override fun scrollBy(x: Int, y: Int) {
        scrollBy(x, y, false)
    }

    override fun scrollBy(
        x: Int,
        y: Int,
        invokeChangeListener: Boolean,
        disablePruneX: Boolean,
        disablePruneY: Boolean
    ) {
        this.invokeChangeListener = invokeChangeListener
        forEachScroller {
            it.scrollBy(
                x,
                y,
                invokeChangeListener,
                disablePruneX,
                disablePruneY
            )
        }
    }

    fun forceScrollTo( y :Int) {
//        this.invokeChangeListener = invokeChangeListener
        forEachScroller {
            it.scrollTo(
                scrollX,
                y,
                true,
                disablePruneX = false,
                disablePruneY = false
            )
        }

    }

    override fun smoothScrollHorizontallyBy(x: Int, invokeChangeListener: Boolean) {
        if (x == 0) return

        this.invokeChangeListener = invokeChangeListener
        var childScrollX = 0
        forEachScroller { childScrollX = max(childScrollX, it.scrollX) }
        if (x != 0) {
            scrollState = ScrollState.SETTLING
            scrollerX.startScroll(childScrollX, 0, x, 0)
            postInvalidateOnAnimation()
        }
    }

    private fun flingHorizontally(velocityX: Int) {
        var maxScrollX = 0
        var childScrollX = 0
        forEachScroller {
            maxScrollX = max(maxScrollX, it.maxScrollX)
            childScrollX = max(childScrollX, it.scrollX)
        }

        scrollerX.fling(childScrollX, 0, velocityX, 0, 0, maxScrollX, 0, 0)
        postInvalidateOnAnimation()
    }

    fun setOnScrollStateChangeListener(listener: OnScrollStateChangeListener?) {
        onScrollLStateChangeListener = listener
    }

    fun setFingerStopListener(listener: ((Int) -> Unit)) {
        fingerStopListener = listener
    }

    fun stopFling() {
        if (!scrollerX.isFinished) {
            scrollerX.abortAnimation()
        }
    }

    fun updateTotalDuration(duration: Long) {
        forEachScroller { it.updateTotalDuration(duration) }
    }

    fun setTimelineScale(scale: Float) {
        forEachScroller {
            it.setTimelineScale(scale)
        }
    }

    override fun assignMaxScrollX(maxScrollX: Int) {
        forEachScroller { it.assignMaxScrollX(maxScrollX) }
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return true
    }

    override fun requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) {
            velocityTracker?.recycle()
            velocityTracker = null
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onInterceptTouchEvent(event)
        isCanScale = event.pointerCount >= 2
        if (event.actionMasked == ACTION_POINTER_DOWN) {
            scaleGestureDetector?.onTouchEvent(this, event)
            return true
        }
        if (isCanScale) {
            return true
        }

        if (event.action == ACTION_MOVE && scrollState == ScrollState.DRAGGING) {
            return true
        }

        if (super.onInterceptTouchEvent(event)) {
            return true
        }

        when (event.action) {
            ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                lastX = event.x

                stopFling()
            }

            ACTION_MOVE -> {
                val deltaX = event.x - downX
                val deltaY = event.y - downY
                if (abs(deltaX) >= abs(deltaY) && touchSlop <= abs(deltaX)) {
                    scrollState = ScrollState.DRAGGING
                }
            }
        }

        return scrollState == ScrollState.DRAGGING
    }

    @SuppressLint("ClickableViewAccessibility", "Recycle")
    @Suppress("ComplexMethod")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)

        if (event.pointerCount >= 2) {
            scaleGestureDetector?.onTouchEvent(this, event)
            return true
        }
        val velocityTrackerIns = velocityTracker ?: VelocityTracker.obtain().also {
            velocityTracker = it
        }
        velocityTrackerIns.addMovement(event)

        when (event.action) {
            ACTION_DOWN -> {
                performClick = true
            }

            ACTION_MOVE -> {
                if (scrollState == ScrollState.DRAGGING) {
                    val x = lastX - event.x
                    scrollBy(
                        x.toInt(),
                        0,
                        invokeChangeListener = true
                    )
                } else {
                    val deltaX = event.x - downX
                    val deltaY = event.y - downY
                    if (abs(deltaX) >= abs(deltaY) && touchSlop <= abs(deltaX)) {
                        scrollState = ScrollState.DRAGGING
                    }
                }

                lastX = event.x
                // 让移动超出1px才认为是移动了
                if (abs(fingerStopX - lastX) >= 1F) {
                    fingerStopX = lastX
                    handler?.let {
                        removeCallbacks(fingerStopRunner)
                        postDelayed(fingerStopRunner, 96)
                    }
                }
                performClick = scrollState != ScrollState.DRAGGING
            }

            ACTION_UP -> {
                if (scrollState == ScrollState.DRAGGING) {
                    velocityTrackerIns.computeCurrentVelocity(1000, maxFlingVelocity)
                    val velocityX = -pruneVelocity(velocityTrackerIns.xVelocity)
                    scrollState = if (velocityX != 0F) {
                        flingHorizontally(velocityX.toInt())
                        ScrollState.SETTLING
                    } else {
                        ScrollState.IDLE
                    }
                } else if (performClick) {
                    blankClickListener?.onClick(this)
                    scrollState = ScrollState.IDLE
                }
                velocityTracker?.recycle()
                velocityTracker = null
            }

            ACTION_CANCEL -> {
                velocityTracker?.recycle()
                velocityTracker = null
                scrollState = ScrollState.IDLE
            }
        }

        return true
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

    fun setOnBlankClickListener(listener: OnClickListener?) {
        blankClickListener = listener
        forEachScroller { it.setOnBlankClickListener(listener) }
    }
}

interface ScrollHandler {

    fun scrollBy(
        x: Int,
        y: Int,
        invokeChangeListener: Boolean,
        disablePruneX: Boolean = false,
        disablePruneY: Boolean = false
    )

    fun smoothScrollHorizontallyBy(x: Int, invokeChangeListener: Boolean)

    fun assignMaxScrollX(maxScrollX: Int)
}

interface OnScrollStateChangeListener {
    fun onScrollStateChanged(
        state: ScrollState,
        scrollX: Int,
        scrollY: Int
    )
}

enum class ScrollState {
    IDLE,
    DRAGGING,
    SETTLING
}
