package com.ss.ugc.android.editor.track.keyframe

import android.animation.Animator
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.bytedance.ies.nlemediajava.nleSlotId
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlin.math.abs

private const val TAG = "KeyframeDrawer"

class KeyframeDrawer(private val frameView: KeyframeView) {
    private val targetRect = Rect()
    private val bmpSrcRect = Rect()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animating = false

    /**左裁剪时被裁剪掉的宽度*/
    private var clipDistanceX = 0
    private var clipLiftSum = 0F

    private var clickPlayHead = -1L
    var parentScrollX: (() -> Int) = { 0 }

    fun draw(canvas: Canvas, slot: NLETrackSlot) {
        var hasDrawSelectItem = false
        var selectItem: NLETrackSlot? = null
        val activeKeyframe = frameView.activeFrame()
        clipLiftSum += clipDistanceX

        slot.keyframes.forEach {
            if (isKeyframeSelected(slot, it) && !hasDrawSelectItem) {
                hasDrawSelectItem = true
                selectItem = it
            } else {
                if (frameView.shouldDrawIcon()) {
                    frameIcon.drawAsKeyframe(canvas, slot, it, 1F)
                }
            }
        }

        if (selectItem != null) {
            val item = selectItem!!
            val frameChanged = (activeKeyframe == null) || (item.nleSlotId != activeKeyframe.nleSlotId)
            if (frameChanged && frameView.shouldCallback()) {
                frameView.onSelectKeyframe(selectItem)
            }
            if (frameView.shouldDrawIcon()) {
                frameSelectedIcon.drawAsKeyframe(canvas, slot, item, 1.2F)
            }
        } else if (selectItem == null && activeKeyframe != null) {
            if (frameView.shouldCallback()) {
                frameView.onDeselectKeyframe()
            }
        }
    }

    fun onClipLeft(distance: Int) {
        clipDistanceX = distance
    }

    fun onClipLeftDistance(distance: Float) {
        clipLiftSum = distance
    }

    fun onClipEnd() {
        clipDistanceX = 0
        clipLiftSum = 0F
    }

    private fun isKeyframeSelected(slot: NLETrackSlot, keyframeSlot: NLETrackSlot): Boolean {
        if (frameView.getItemWidth() <= 0) return false
        val iconCenterX = TrackConfig.PX_MS * keyframeSlot.keyframePlayHead(slot)
        return abs(iconCenterX - parentScrollX()) < TrackConfig.KEYFRAME_ICON_WIDTH / 2F
    }

    private fun Bitmap.drawAsKeyframe(
        canvas: Canvas,
        slot: NLETrackSlot,
        keyframe: NLETrackSlot,
        scale: Float
    ) {
        bmpSrcRect.set(0, 0, this.width, this.height)

        val offset =
            ((keyframe.keyframePlayHead(slot) - slot.startTime / 1000) * TrackConfig.PX_MS - clipLiftSum).toInt()
        val y = (frameView.getItemHeight() / 2F).toInt()
        targetRect.set(
            offset - TrackConfig.KEYFRAME_ICON_WIDTH / 2,
            y - TrackConfig.KEYFRAME_ICON_WIDTH / 2,
            offset + TrackConfig.KEYFRAME_ICON_WIDTH / 2,
            y + TrackConfig.KEYFRAME_ICON_WIDTH / 2
        )
        canvas.drawBitmap(this, bmpSrcRect, targetRect * scale, paint)
    }

    fun onTouchEvent(ev: MotionEvent, nleTrackSlot: NLETrackSlot): Boolean? {
        return when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                clickPlayHead = checkKeyFrameClick(nleTrackSlot, ev.x) ?: -1L
                frameView.shouldDrawIcon() && frameView.shouldCallback() && clickPlayHead > 0
            }
            MotionEvent.ACTION_UP -> {
                if (clickPlayHead > 0) {
                    frameView.onClickKeyframe(clickPlayHead)
                }
                null
            }
            else -> {
                null
            }
        }
    }

    fun checkKeyFrameClick(nleTrackSlot: NLETrackSlot, x: Float): Long? {
        if (frameView.getItemWidth() <= 0) {
            return null
        }
        val clickX = x + TrackConfig.PX_MS * nleTrackSlot.startTime/1000
        nleTrackSlot.keyframes.forEach {
            val iconCenterX = TrackConfig.PX_MS * it.keyframePlayHead(nleTrackSlot)
            if (abs(iconCenterX - clickX) < TrackConfig.KEYFRAME_ICON_WIDTH / 2) {
                return it.keyframePlayHead(nleTrackSlot)
            }
        }
        return null
    }

    private fun animateScaleIcon(fromScale: Float, toScale: Float) {
        if (animating) {
            return
        }
        animating = true
        val animator = ValueAnimator.ofFloat(fromScale, toScale)
        animator.duration = 100
        animator.addUpdateListener { animation ->
            scaleRatio = animation.animatedValue as Float
            frameView.requestRefresh()
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                animating = false
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })
        animator.start()
    }

    operator fun Rect.times(value: Float): Rect {
        if (value <= 0) return this
        if (value == 1F) return this
        val diffW = this.width() * (value - 1)
        val diffH = this.height() * (value - 1)
        val left = this.left - diffW / 2
        val top = this.top - diffH / 2
        return Rect(
            left.toInt(),
            top.toInt(),
            (left + value * this.width()).toInt(),
            (top + value * this.height()).toInt()
        )
    }

    /**
     * 返回这个关键帧的绝对时间
     */
    private fun NLETrackSlot.keyframePlayHead(slot: NLETrackSlot): Long {
        return (this.startTime / 1000 + slot.startTime / 1000)
    }

    companion object {
        private var scaleRatio: Float = 1F

        private val frameIcon by lazy {
            BitmapFactory.decodeResource(
                TrackSdk.application.resources,
                R.drawable.ic_keyframe_no_select
            )
        }

        private val frameSelectedIcon by lazy {
            BitmapFactory.decodeResource(
                TrackSdk.application.resources,
                R.drawable.ic_keyframe_select
            )
        }
    }
}

/**
 * 关键帧选中变化
 */
interface KeyframeSelectChangeListener {
    fun onKeyframeSelect(frame: NLETrackSlot?)
    fun onKeyframeClick(playHead: Long)
    fun onKeyframeDeselect()
}

interface KeyframeView {
    fun activeFrame(): NLETrackSlot?
    fun shouldCallback(): Boolean
    fun shouldDrawIcon(): Boolean
    fun onSelectKeyframe(keyframe: NLETrackSlot?)
    fun onClickKeyframe(playHead: Long)
    fun onDeselectKeyframe()
    fun getItemWidth(): Int
    fun getItemHeight(): Int
    fun requestRefresh()
}
