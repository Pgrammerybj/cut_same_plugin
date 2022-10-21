package com.ss.ugc.android.editor.track.fuctiontrack.keyframe

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.fuctiontrack.TrackItemView
import com.ss.ugc.android.editor.track.keyframe.KeyframeDrawer
import com.ss.ugc.android.editor.track.keyframe.KeyframeSelectChangeListener
import com.ss.ugc.android.editor.track.keyframe.KeyframeView

abstract class BaseTrackKeyframeItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), TrackItemView ,KeyframeView{
    private val frameDrawer = KeyframeDrawer(this)
    var listener: KeyframeSelectChangeListener? = null
    var frameDelegate: KeyframeStateDelegate? = null

    override var isClipping: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (!value) {
                frameDrawer.onClipEnd()
            }
        }
    override var clipLeft: Float = 0F
        set(value) {
            field = value
            frameDrawer.onClipLeftDistance(value)
        }

    protected val parentView: ViewGroup?
        get() = parent as? ViewGroup
    protected var nleTrackSlot: NLETrackSlot? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        parentView?.let { frameDrawer.parentScrollX = { it.scrollX } }
    }

    override fun onParentScrollChanged(scrollX: Int) {
//        nleTrackSlot?.takeIf { isItemSelected }?.let { frameDrawer.update(it) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas ?: return
        val segment = nleTrackSlot ?: return
        frameDrawer.draw(canvas, segment)
    }

    fun clickKeyframeAt(x: Float): Boolean {
        if (!shouldDrawIcon()) return false
        val slot = nleTrackSlot ?: return false
        frameDrawer.checkKeyFrameClick(slot, x)?.also {
            onClickKeyframe(it)
            return true
        }
        return false
    }

    @CallSuper
    override fun drawOn(canvas: Canvas) {
        val segment = nleTrackSlot ?: return
        frameDrawer.draw(canvas, segment)
    }

    override fun activeFrame(): NLETrackSlot? = frameDelegate?.getActiveKeyframe()

    override fun shouldCallback() = isItemSelected
    override fun shouldDrawIcon(): Boolean {
        return frameDelegate?.shouldDrawIcon() ?: false && isItemSelected
    }

    override fun onSelectKeyframe(keyframe: NLETrackSlot?) {
        listener?.onKeyframeSelect(keyframe)
    }

    override fun onClickKeyframe(playHead: Long) {
        listener?.onKeyframeClick(playHead)
    }

    override fun onDeselectKeyframe() {
        listener?.onKeyframeDeselect()
    }

    override fun getItemWidth() = measuredWidth
    override fun getItemHeight() = measuredHeight
    override fun requestRefresh() = invalidate()
}

interface KeyframeStateDelegate {
    fun getActiveKeyframe(): NLETrackSlot?
    fun shouldDrawIcon(): Boolean
}
