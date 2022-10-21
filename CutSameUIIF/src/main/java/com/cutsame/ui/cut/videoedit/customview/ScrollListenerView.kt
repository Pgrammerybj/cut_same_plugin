package com.cutsame.ui.cut.videoedit.customview

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView

class ScrollListenerView : HorizontalScrollView {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    companion object {
        const val TAG = "ScrollListenerView"
        /**
         * 滑动完成
         */
        const val SCROLL_STATE_IDLE = 0
        /**
         * 用户正在滑动，手指处在屏幕上
         */
        const val SCROLL_STATE_TOUCH_SCROLL = 1

        /**
         * 用户滑动完成，正在惯性滑动
         */
        var SCROLL_STATE_FLING = 2
        private const val CHECK_SCROLL_STOP_DELAY_MILLIS = 80
        private const val MSG_SCROLL = 1
    }

    var onScrollListener: OnScrollListener? = null

    private var mIsTouched = false
    private var mScrollState =
        SCROLL_STATE_IDLE

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (mIsTouched) {
            setScrollState(SCROLL_STATE_TOUCH_SCROLL)
        } else {
            setScrollState(SCROLL_STATE_FLING)
            restartCheckStopTiming()
        }
        onScrollListener?.onScroll(this, mIsTouched, l, t, oldl, oldt)
    }

    private val mHandler = Handler(Looper.getMainLooper(), object : Handler.Callback {

        private var mLastY = Integer.MIN_VALUE

        override fun handleMessage(msg: Message): Boolean {
            if (msg.what == MSG_SCROLL) {
                val scrollY = scrollY
                if (!mIsTouched && mLastY == scrollY) {
                    mLastY = Integer.MIN_VALUE
                    setScrollState(SCROLL_STATE_IDLE)
                } else {
                    mLastY = scrollY
                    restartCheckStopTiming()
                }
                return true
            }
            return false
        }
    })

    private fun restartCheckStopTiming() {
        mHandler.removeMessages(MSG_SCROLL)
        mHandler.sendEmptyMessageDelayed(MSG_SCROLL, CHECK_SCROLL_STOP_DELAY_MILLIS.toLong())
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        handleDownEvent(ev)
        handleUpEvent(ev)
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        handleUpEvent(ev)
        return super.onTouchEvent(ev)
    }

    private fun handleDownEvent(ev: MotionEvent) {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mIsTouched = true
                onScrollListener?.onTouched(mIsTouched)
            }
        }
    }

    private fun handleUpEvent(ev: MotionEvent) {
        when (ev.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mIsTouched = false
                restartCheckStopTiming()
                onScrollListener?.onTouched(mIsTouched)
            }
        }
    }

    private fun setScrollState(state: Int) {
        if (mScrollState != state) {
            mScrollState = state
            onScrollListener?.onScrollStateChanged(this, state)
        }
    }

    interface OnScrollListener {

        fun onScrollStateChanged(view: ScrollListenerView, scrollState: Int) {
        }

        fun onTouched(touched: Boolean) {

        }

        fun onScroll(
            view: ScrollListenerView,
            isTouchScroll: Boolean,
            l: Int,
            t: Int,
            oldl: Int,
            oldt: Int
        ) {
        }
    }
}
