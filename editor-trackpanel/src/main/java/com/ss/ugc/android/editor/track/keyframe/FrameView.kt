package com.ss.ugc.android.editor.track.keyframe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.KeyframeStateDelegate


class FrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), KeyframeView {

    private val frameDrawer = KeyframeDrawer(this)
    private var slot: NLETrackSlot? = null
    var frameSelectChangeChangeListener: KeyframeSelectChangeListener? = null

    // ItemTrackLayout 里的 clipType 或者 lineType 时需要关键帧 选中和取消选中 回调
    var callback: FrameViewCallback? = null


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
//        DLog.d("=== before draw  ")
        if (slot == null || canvas == null || visibility != VISIBLE) return
//        DLog.d("=== start draw  ")
        frameDrawer.draw(canvas, slot!!)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return super.onTouchEvent(ev)
        slot ?: return super.onTouchEvent(ev)
        return frameDrawer.onTouchEvent(ev, slot!!) ?: super.onTouchEvent(ev)
    }

    fun setSlot(slot: NLETrackSlot) {
        this.slot = slot
    }

    fun update() {
        if (visibility == VISIBLE && slot != null) {
            invalidate()
        }
    }

    fun setFrameViewCallback(frameViewCallback: FrameViewCallback) {
        callback = frameViewCallback
        frameDrawer.parentScrollX = {
            callback?.getParentScrollX() ?: 0
        }
    }

    fun onClipLeft(distance: Int) {
        frameDrawer.onClipLeft(distance)
        invalidate()
    }

    fun onClipEnd() {
        frameDrawer.onClipEnd()
    }

    override fun getItemWidth(): Int = measuredWidth
    override fun getItemHeight(): Int = measuredHeight
    override fun requestRefresh() = invalidate()
    override fun activeFrame(): NLETrackSlot? = callback?.getActiveKeyframe()
    override fun shouldCallback() = callback?.shouldCallback() ?: false
    override fun shouldDrawIcon() = callback?.shouldDrawIcon() ?: false

    override fun onSelectKeyframe(keyframe: NLETrackSlot?) {
        frameSelectChangeChangeListener?.onKeyframeSelect(keyframe)
    }

    override fun onClickKeyframe(playHead: Long) {
        frameSelectChangeChangeListener?.onKeyframeClick(playHead)
    }

    override fun onDeselectKeyframe() {
        frameSelectChangeChangeListener?.onKeyframeDeselect()
    }

    interface FrameViewCallback : KeyframeStateDelegate {
        fun getParentScrollX(): Int
        fun shouldCallback(): Boolean
    }
}
