package com.cutsame.ui.cut.videoedit.customview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.cutsame.ui.R
import com.ss.android.ugc.cut_ui.MediaItem
import kotlinx.android.synthetic.main.layout_single_frame.view.*


class SingleSelectFrameView : FrameLayout, ViewTreeObserver.OnGlobalLayoutListener {

    private var inited = false

    companion object {
        const val TAG = "SingleSelectFrameView"
    }

    private var lpProgressLine: RelativeLayout.LayoutParams? = null
    private var duration = 0L

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_single_frame, this)
        ivLeft.setOnTouchListener { _, _ -> true }
        ivRight.setOnTouchListener { _, _ -> true }
        lpProgressLine = ivProgressLine.layoutParams as? RelativeLayout.LayoutParams

        viewTreeObserver.addOnGlobalLayoutListener(this)
    }

    fun initFrames(
        data: MediaItem,
        videoLength: Long,
        scrollListener: ScrollListener
    ) {
        this.duration = if (data.type == MediaItem.TYPE_PHOTO) {
            data.duration
        } else {
            videoLength
        }
        itemFrameView.updateData(data.source, data.duration, duration, 0)
        scrollView.onScrollListener = object :
            ScrollListenerView.OnScrollListener {
            var stats: Int =
                ScrollListenerView.SCROLL_STATE_IDLE
            var lastL: Int = 0
            override fun onScrollStateChanged(view: ScrollListenerView, scrollState: Int) {
                super.onScrollStateChanged(view, scrollState)
                stats = scrollState
                if (stats == ScrollListenerView.SCROLL_STATE_IDLE) {
                    val startTime =
                        videoLength * (view.scrollX.toFloat() / itemFrameView.width.toFloat())
                    scrollListener.onScrollIdle(startTime.toInt())
                }
            }

            override fun onTouched(touched: Boolean) {
                super.onTouched(touched)
                scrollListener.onTouched()
            }

            override fun onScroll(
                view: ScrollListenerView,
                isTouchScroll: Boolean,
                l: Int,
                t: Int,
                oldl: Int,
                oldt: Int
            ) {
                super.onScroll(view, isTouchScroll, l, t, oldl, oldt)
                itemFrameView.updateScrollX(l)
                if (isTouchScroll) {
                    val startTime =
                        videoLength * (l.toFloat() / itemFrameView.width.toFloat())
                    scrollListener.onScroll(startTime.toInt())
                    data.sourceStartTime = startTime.toLong()
                }
            }
        }
    }

    /**
     * 进行进度的跳转
     */
    fun moveTo(progress: Float) {
        scrollView.scrollTo((itemFrameView.width * progress).toInt(), 0)
        itemFrameView.updateScrollX((itemFrameView.width * progress).toInt())
    }

    /**
     * 传入0到100的值
     * 计算进度条位置的公式
     * (cover-(lPadding+rPadding+line_w))/progress+ivLeft_w+lPadding
     */
    fun changeProgress(progress: Float) {
        if (inited && progress <= 1 && progress >= 0) {
            lpProgressLine?.marginStart = (vFrameCover.width * progress + ivLeft.width).toInt()
            ivProgressLine.layoutParams = lpProgressLine
            requestLayout()
        }
    }

    override fun onGlobalLayout() {
        viewTreeObserver.removeOnGlobalLayoutListener(this)
        inited = true
    }

    interface ScrollListener {
        fun onTouched()
        fun onScroll(timePos: Int)
        fun onScrollIdle(timePos: Int)
    }
}
