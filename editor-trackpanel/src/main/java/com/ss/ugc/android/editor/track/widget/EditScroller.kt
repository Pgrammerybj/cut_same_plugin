package com.ss.ugc.android.editor.track.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import com.ss.ugc.android.editor.track.utils.PadUtil
import com.ss.ugc.android.editor.track.utils.SizeUtil
import kotlin.math.max

abstract class EditScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var screenWidth = SizeUtil.getScreenWidth(context)

    private var invokeChangeListener = true
    protected var scrollChangeListener: ((Int, Int) -> Unit)? = null
        private set
    private var mustUpdateScrollListener: ((Int) -> Unit)? = null

    private var totalDuration: Long = 0L

    var desireMaxScrollX: Int = 0
        private set
    private var assignedMaxScrollX: Int = 0

    var timelineScale = TrackConfig.PX_MS
        private set

    val maxScrollX: Int
        get() = max(desireMaxScrollX, assignedMaxScrollX)

    open val maxScrollY: Int = 0

    @CallSuper
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (invokeChangeListener) {
            scrollChangeListener?.invoke(scrollX, (l - oldl))
        }
        mustUpdateScrollListener?.invoke(scrollX)
    }

    override fun addView(child: View) {
        checkAddView(child)
        super.addView(child)
    }

    override fun addView(child: View, index: Int) {
        checkAddView(child)
        super.addView(child, index)
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams) {
        checkAddView(child)
        super.addView(child, params)
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        checkAddView(child)
        super.addView(child, index, params)
    }

    protected abstract fun checkAddView(child: View)

    protected fun getScreenWidth(): Int {
        // Pad有可能会横竖切换，所以最好实时获取
        // 手机目前不可横屏，获取之后一直用那个值就好
        if (PadUtil.isPad) {
            return SizeUtil.getScreenWidth(context)
        }
        return screenWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = {
            MeasureSpec.getSize(widthMeasureSpec).let {
                if (it == 0) getScreenWidth() else it
            }
        }
        if (childCount == 0) {
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            if (heightMode == MeasureSpec.EXACTLY) {
                val height = MeasureSpec.getSize(heightMeasureSpec)
                setMeasuredDimension(width.invoke(), height)
            } else {
                setMeasuredDimension(0, 0)
            }
            return
        }

        var desireHeight = paddingTop + paddingBottom
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                continue
            }

            measureChild(
                child,
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                heightMeasureSpec
            )
            val params = child.layoutParams as MarginLayoutParams
            desireHeight += (params.topMargin + params.bottomMargin + child.measuredHeight)
        }
        setMeasuredDimension(width.invoke(), desireHeight)
    }

    fun scrollBy(
        x: Int,
        y: Int,
        invokeChangeListener: Boolean,
        disablePruneX: Boolean = false,
        disablePruneY: Boolean = false
    ) {
        this.invokeChangeListener = invokeChangeListener
        scrollTo(scrollX + x, scrollY + y, invokeChangeListener, disablePruneX, disablePruneY)
    }

    /**
     * @param disablePruneX X轴是否能在小于0时朝负方向滚动
     */
    fun scrollTo(
        x: Int,
        y: Int,
        invokeChangeListener: Boolean,
        disablePruneX: Boolean = false,
        disablePruneY: Boolean = false
    ) {
        this.invokeChangeListener = invokeChangeListener

        val targetX = if (disablePruneX) x else pruneScroll(x, maxScrollX)
        val targetY =
            if (disablePruneY) y
            else pruneScroll(y, maxScrollY)
        scrollTo(targetX, targetY)
    }

    private fun pruneScroll(desireScroll: Int, maxScroll: Int): Int {
        return when {
            desireScroll < 0 -> 0
            desireScroll > maxScroll -> maxScroll
            else -> desireScroll
        }
    }

    fun setMustUpdateScrollXListener(listener: ((Int) -> Unit)?) {
        mustUpdateScrollListener = listener
    }

    fun assignMaxScrollX(maxScrollX: Int) {
        if (assignedMaxScrollX != maxScrollX) {
            assignedMaxScrollX = maxScrollX
        }
    }

    fun setScrollChangeListener(listener: ((Int, Int) -> Unit)?) {
        scrollChangeListener = listener
    }

    @CallSuper
    open fun updateTotalDuration(duration: Long) {
        totalDuration = duration
        updateMaxScrollX()
    }

    private fun updateMaxScrollX() {
        desireMaxScrollX = (totalDuration * timelineScale).toInt()
        assignedMaxScrollX = 0
    }

    @CallSuper
    open fun setTimelineScale(scale: Float) {
        timelineScale = scale
        updateMaxScrollX()
    }

    internal open fun setOnBlankClickListener(listener: OnClickListener?) {
    }
}

class VisualTrackScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EditScroller(context, attrs, defStyleAttr) {

    init {
        setHorizontalPadding()
        clipToPadding = false
        clipChildren = false
    }

    override fun checkAddView(child: View) {
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        setHorizontalPadding()
    }

    private fun setHorizontalPadding() {
        val paddingHorizontal = getScreenWidth() / 2
        setPadding(paddingHorizontal, paddingTop, paddingHorizontal, paddingBottom)
    }
}

class TimeRulerScroller @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EditScroller(context, attrs, defStyleAttr) {
    override fun checkAddView(child: View) {
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        scrollChangeListener?.invoke(scrollX, (l - oldl))
    }
}
