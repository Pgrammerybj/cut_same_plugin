package com.ss.ugc.android.editor.track.fuctiontrack

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import androidx.core.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.utils.SizeUtil

@Suppress("ComplexInterface")
interface TrackItemHolder {
    fun setDrawDivider(draw: Boolean)
    fun setClipping(clipping: Boolean)
    fun setDragging(dragging: Boolean)
    fun setItemSelected(selected: Boolean)
    fun setTimelineScale(timelineScale: Float)
    fun getView(): View

    @ColorInt
    fun getViewBackground(): Int

    fun setViewBackground(@ColorInt color: Int)
    fun destroy()
    fun reset()
    fun drawOn(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        leftOffset: Float,
        clipLength: Float
    )

    fun setSegment(slot: NLETrackSlot)

    fun onParentScrollChanged(scrollX: Int)

    companion object {
        @Suppress("DEPRECATION")
        internal val PARENT_BG_COLOR =
            ContextCompat.getColor(TrackSdk.application,R.color.scroller_bg)

        val CORNER_WIDTH = SizeUtil.dp2px(2F).toFloat()
    }
}

abstract class BaseTrackItemHolder<ItemView>(
    protected val context: Context
) : TrackItemHolder where ItemView : View, ItemView : TrackItemView {
    protected abstract val itemView: ItemView

    @Volatile
    var isDragging = false
        private set

    /**
     * 选中时的绘制区域
     */
    private val drawOnRectF: RectF = RectF()

    @CallSuper
    override fun setDrawDivider(draw: Boolean) {
        itemView.drawDivider = draw
    }

    /**
     * 是否正在裁剪
     */
    @CallSuper
    override fun setClipping(clipping: Boolean) {
        itemView.isClipping = clipping
    }

    @CallSuper
    override fun setDragging(dragging: Boolean) {
        isDragging = dragging
    }

    @CallSuper
    override fun setItemSelected(selected: Boolean) {
        itemView.isItemSelected = selected
        if (!selected) {
            itemView.clipLeft = 0F
            itemView.clipLength = 0F
        }
    }

    @CallSuper
    override fun setTimelineScale(timelineScale: Float) {
        itemView.timelineScale = timelineScale
    }

    override fun getView() = itemView

    @CallSuper
    override fun destroy() {
        itemView.let {
            (it.parent as? ViewGroup)?.removeView(it)
        }
    }

    @CallSuper
    override fun getViewBackground(): Int {
        return itemView.bgColor
    }

    @CallSuper
    override fun setViewBackground(color: Int) {
        itemView.bgColor = color
    }

    @CallSuper
    override fun setSegment(slot: NLETrackSlot) {
        itemView.setSegment(slot)
    }

    @CallSuper
    override fun onParentScrollChanged(scrollX: Int) {
        itemView.onParentScrollChanged(scrollX)
    }

    @CallSuper
    override fun reset() {
        itemView.drawDivider = true
    }

    @CallSuper
    override fun drawOn(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        leftOffset: Float,
        clipLength: Float
    ) {
        itemView.clipLeft = leftOffset
        itemView.clipLength = clipLength
        canvas.save()
        drawOnRectF.set(left, top, right, bottom)
        canvas.clipRect(drawOnRectF)
        canvas.translate(left, top)
        itemView.drawOn(canvas)
        canvas.restore()
    }
}

@Suppress("ComplexInterface")
interface TrackItemView {
    var drawDivider: Boolean
    var isItemSelected: Boolean
    var isClipping: Boolean

    /**
     * 拖拽左侧裁剪按钮裁剪的距离，如果是想做裁剪则为负数，向右裁剪则为正数
     */
    var clipLeft: Float

    /**
     * 裁剪的长度，与`clipLeft`无关，当前右侧裁剪按钮与`Segment#targetTimeRange#start`的差值
     */
    var clipLength: Float
    var timelineScale: Float
    var bgColor: Int

    fun setSegment(slot: NLETrackSlot)
    fun onParentScrollChanged(scrollX: Int) {
    }

    fun drawOn(canvas: Canvas)
}
