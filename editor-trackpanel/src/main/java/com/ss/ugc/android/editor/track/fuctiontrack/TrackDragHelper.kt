package com.ss.ugc.android.editor.track.fuctiontrack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View.ALPHA
import android.view.View.TRANSLATION_X
import android.view.View.TRANSLATION_Y
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.core.Constants
import com.ss.ugc.android.editor.core.isVideoSlot
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.openUntil
import com.ss.ugc.android.editor.track.utils.safelyPerformHapticFeedback
import com.ss.ugc.android.editor.track.widget.HorizontallyState
import com.ss.ugc.android.editor.track.widget.HorizontallyState.NULL
import com.ss.ugc.android.editor.track.widget.HorizontallyState.RIGHT
import com.ss.ugc.android.editor.track.widget.HorizontallyState.LEFT
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlin.math.ceil

private const val TAG: String = "TrackDragHelper"
private val CONFLICT_COLOR = Color.parseColor("#666666")

private const val AUTO_SCROLL_INTERVAL = 500
private val HORIZONTAL_SCROLL_SLOP: Int = SizeUtil.dp2px(20F)

internal typealias TrackMoveListener = (
    fromTrackIndex: Int,
    toTrackIndex: Int,
    slot: NLETrackSlot,
    offsetInTimeline: Long
) -> Unit

internal class TrackDragHelper(
    private val trackGroup: TrackGroup,
    private val itemHolder: TrackItemHolder,
    private val callbackFetcher: () -> TrackGroup.Callback?
) {
    private val itemView = itemHolder.getView()

    private var transX = 0F
    private var transY = 0F

    private var dragX = 0F
    private var dragY = 0F
    private var dragDeltaX = 0F
    private var dragDirection = NULL

    private val parentPosition = IntArray(2) { 0 }

    private var fromTrackIndex = 0
    private var initColor = Color.parseColor("#03B1BE")

    private var toTrackIndex = 0
    private var conflict = false

    private var nleTrackSlot: NLETrackSlot? = null

    private var lastAutoScrollTime: Long = 0

    private val callback: TrackGroup.Callback?
        get() = callbackFetcher.invoke()

    private val scrollAnim: ValueAnimator = ValueAnimator.ofFloat(0F, 1F)
    private var autoScrollSize = 0
    private var scrollSizeGrowingStep = HORIZONTAL_SCROLL_SLOP / 10

    /**
     * 滚动方向，LEFT表示往左边滚动，RIGHT表示往右边滚动
     */
    private var scrollState = NULL
        set(value) {
            if (field == value) return

            field = value
            if (value == NULL) {
                scrollAnim.cancel()
                // 停止滚动时启用吸附
                nleTrackSlot?.let {
                    callback?.onStartAdsorption(it)
                }
            } else {
                scrollAnim.start()
                // 滚动时取消吸附
                callback?.onCancelAdsorption()
            }
        }

    init {
        scrollAnim.repeatCount = ValueAnimator.INFINITE
        scrollAnim.addUpdateListener {
            val scrollX = trackGroup.scrollX
            growAutoScrollSize()
            val x = when (scrollState) {
                NULL -> return@addUpdateListener
                LEFT -> if (scrollX - autoScrollSize < 0) -scrollX else -autoScrollSize
                RIGHT -> {
                    if (!trackGroup.canMoveOutOfMainVideo()) {
                        with(trackGroup.mainVideoLength.toInt() - scrollX) {
                            if (this < autoScrollSize) this else autoScrollSize
                        }
                    } else if (!trackGroup.canMoveOutOfVideos) {
                        with(trackGroup.videosLength.toInt() - scrollX) {
                            if (this < autoScrollSize) this else autoScrollSize
                        }
                    } else {
                        autoScrollSize
                    }
                }
            }
            setX(x)
        }
        scrollAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator?) {
                autoScrollSize = 0
            }
        })
    }

    /**
     * 逐渐增大自动滚动的距离，避免在开始自动滚动的时候出现抖动的感觉
     */
    private fun growAutoScrollSize() {
        autoScrollSize += scrollSizeGrowingStep
        if (autoScrollSize > HORIZONTAL_SCROLL_SLOP) {
            autoScrollSize = HORIZONTAL_SCROLL_SLOP
        }
    }

    private fun setX(x: Int) {
        dragX += x
        dragDeltaX += x
        callback?.scrollBy(trackGroup, x, 0, true)
        // scrollBy最后会调用postInvalidateOnAnimation()，
        // 为了让滚动和translation尽可能同步，这里post一下
        trackGroup.postOnAnimation {
            if (scrollState == NULL) {
                return@postOnAnimation
            }
            setTranslationX()
        }
    }

    private fun setTranslationX() {
        val paddingHorizontal = TrackGroup.getPaddingHorizontal()
        transX = if (itemView.left + dragDeltaX < paddingHorizontal) {
            (paddingHorizontal - itemView.left).toFloat()
        } else {
            if (!trackGroup.canMoveOutOfMainVideo()) {
                val rightEdge = paddingHorizontal + trackGroup.mainVideoLength
                if (itemView.right + dragDeltaX > rightEdge) rightEdge - itemView.right else dragX
            } else if (!trackGroup.canMoveOutOfVideos) {
                val rightEdge = paddingHorizontal + trackGroup.videosLength
                if (itemView.right + dragDeltaX > rightEdge) rightEdge - itemView.right else dragX
            } else {
                dragX
            }
        }
        itemView.translationX = transX
    }

    fun beginDrag(paramsMap: Map<NLETrackSlot, TrackParams>): Animator {
        trackGroup.requestDisallowInterceptTouchEvent(true)
        trackGroup.getLocationOnScreen(parentPosition)
        itemView.safelyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        itemView.bringToFront()
        itemHolder.setDrawDivider(false)

        paramsMap.forEach { (segment, params) ->
            if (params.holder === itemHolder) {
                nleTrackSlot = segment
                return@forEach
            }
        }

        var top = 0
        while (top < itemView.top) {
            fromTrackIndex++
            top += (trackGroup.itemHeight + trackGroup.itemMargin)
        }
        toTrackIndex = fromTrackIndex
        initColor = itemHolder.getViewBackground()
        dragDirection = NULL

        nleTrackSlot?.let {
            callback?.onStartAdsorption(it)
        }

        val alphaAnim = ObjectAnimator.ofFloat(itemView, ALPHA, 1F, 0.5F)
        alphaAnim.duration = 50
        alphaAnim.start()
        return alphaAnim
    }

    fun drag(
        rawX: Float,
        deltaX: Float,
        deltaY: Float,
        paramsMap: Map<NLETrackSlot, TrackParams>
    ) {
        dragY += deltaY
        dragDeltaX += deltaX
        dragDirection = if (dragDeltaX - dragX > 0) RIGHT else LEFT

        val scrollY = trackGroup.scrollY
        // 水平方向上到达屏幕边缘需要自动滚动
        val width = trackGroup.measuredWidth
        scrollState = when {
            rawX > parentPosition[0] + width - HORIZONTAL_SCROLL_SLOP -> RIGHT
            rawX < parentPosition[0] + HORIZONTAL_SCROLL_SLOP -> HorizontallyState.LEFT
            else -> {
                // 计算吸附
                dragX = calcMoveAdsorption()
                setTranslationX()
                HorizontallyState.NULL
            }
        }

        var y = 0
        var targetTrack = calcNearestTrackIndex()
        // 如果在最后一行之下，则展示添加一行的提示，并贴到TrackGroup的最下方
        val trackCount = trackGroup.trackCount
        val now = SystemClock.uptimeMillis()
        if (trackCount <= targetTrack) {
            if (toTrackIndex != trackCount) {
                if (now - lastAutoScrollTime >= AUTO_SCROLL_INTERVAL) {
                    trackGroup.setMoveTouchEdge(true)
                    lastAutoScrollTime = now
                    toTrackIndex = trackCount
                    transY = (trackCount * (trackGroup.itemMargin +
                            trackGroup.itemHeight) - itemView.bottom).toFloat()
                } else {
                    transY = ((toTrackIndex - fromTrackIndex) *
                            (trackGroup.itemHeight + trackGroup.itemMargin)).toFloat()
                }
            }
        } else {
            trackGroup.setMoveTouchEdge(false)

            // 如果在第一行之上，则只能摆在第一行
            if (targetTrack < 0) {
                targetTrack = 0
            }

            y = calcScrollY(targetTrack, scrollY)
            var updateToTrackIndex = true
            if (y != 0) {
                if (now - lastAutoScrollTime < AUTO_SCROLL_INTERVAL) {
                    y = 0
                    updateToTrackIndex = false
                } else {
                    lastAutoScrollTime = now
                }
            }

            if (updateToTrackIndex) {
                toTrackIndex = targetTrack
            }
            transY = ((toTrackIndex - fromTrackIndex) *
                    (trackGroup.itemHeight + trackGroup.itemMargin)).toFloat()
        }
        dragY += y
        callback?.scrollBy(trackGroup, 0, y, true)

        conflict = checkConflict(toTrackIndex, fromTrackIndex, paramsMap)
        itemHolder.setViewBackground(if (conflict) CONFLICT_COLOR else initColor)

        itemView.translationY = transY
    }

    private fun calcNearestTrackIndex(): Int {
        var nearestTrackIndex = fromTrackIndex
        var tmpY = dragY
        if (tmpY < 0) {
            while (tmpY < 0) {
                if (tmpY <= -(trackGroup.itemHeight / 2 + trackGroup.itemMargin)) {
                    nearestTrackIndex--
                }

                tmpY += (trackGroup.itemHeight + trackGroup.itemMargin)
            }
        } else if (tmpY > 0) {
            while (tmpY > 0) {
                if (tmpY >= trackGroup.itemHeight / 2 + trackGroup.itemMargin) {
                    nearestTrackIndex++
                }

                tmpY -= (trackGroup.itemHeight + trackGroup.itemMargin)
            }
        }
        return if (nearestTrackIndex < trackGroup.maxTrackNum)
            nearestTrackIndex
        else
            trackGroup.maxTrackNum - 1
    }

    private fun checkConflict(
        toTrackIndex: Int,
        fromTrackIndex: Int,
        paramsMap: Map<NLETrackSlot, TrackParams>
    ): Boolean {
        val left: Int = itemView.left + ceil(transX).toInt()
        val right: Int = itemView.right + transX.toInt()

        val segments = paramsMap.entries.filter { (_, params) ->
            params.trackIndex == toTrackIndex
        }.map { it.key }
        // 检查一下跟这一行的Segment是否有冲突，如果该行没有任何Segment，那么不可能发生冲突
        for (segment in segments) {
            val comparison = paramsMap[segment]?.holder
            if (comparison == null || comparison === itemHolder) {
                continue
            }

            // 计算一下该行该位置是否有互斥的其他View，如果有就不能附着
            val comparisonView = comparison.getView()
            if ((left in comparisonView.left openUntil comparisonView.right) ||
                (right in comparisonView.left openUntil comparisonView.right) ||
                (left <= comparisonView.left && right >= comparisonView.right)
            ) {
                return true
            }
        }
        if (isVideoLimit(fromTrackIndex, toTrackIndex)) {
            return true
        }

        return false
    }

    private fun isVideoLimit(
        fromTrackIndex: Int,
        toTrackIndex: Int
    ): Boolean {
        if (nleTrackSlot?.isVideoSlot() != true) {
            return false
        }
        if (toTrackIndex >= Constants.MAX_SUB_VIDEO_LIMIT) {
            return true
        }
        return false
    }

    /**
     * 要把目标行加上其topMargin和bottomMargin都显示出来
     */
    private fun calcScrollY(targetTrackIndex: Int, scrollY: Int): Int {
        var y = 0
        val targetTop = targetTrackIndex * (trackGroup.itemHeight + trackGroup.itemMargin)
        val scrollToTop = targetTop - scrollY
        if (scrollToTop < 0) {
            y = scrollToTop
        } else {
            val targetBottom = (targetTrackIndex + 1) *
                    (trackGroup.itemHeight + trackGroup.itemMargin)
            val height = trackGroup.measuredHeight
            val scrollToBottom = targetBottom - scrollY - height
            if (scrollToBottom > 0) {
                y = scrollToBottom
            }
        }
        return y
    }

    fun endDrag(move: TrackMoveListener): Animator {
        scrollState = HorizontallyState.NULL
        itemHolder.setDrawDivider(true)
        trackGroup.setMoveTouchEdge(false)

        val newAnimator: Animator
        val segment = nleTrackSlot
        if (conflict || segment == null) {
            itemHolder.setViewBackground(initColor)

            newAnimator = AnimatorSet()
            newAnimator.playTogether(
                ObjectAnimator.ofFloat(itemView, ALPHA, 0.5F, 1F),
                ObjectAnimator.ofFloat(itemView, TRANSLATION_X, transX, 0F),
                ObjectAnimator.ofFloat(itemView, TRANSLATION_Y, transY, 0F)
            )
        } else {
            val left = (itemView.left + transX).toInt()
            var start = ((left - TrackGroup.getPaddingHorizontal()) / TrackConfig.PX_MS).toLong()
            if (start < 0) start = 0

            ILog.i(
                TAG, "fromTrackIndex: $fromTrackIndex, " +
                        "toTrackIndex: $toTrackIndex, start: $start"
            )
            move(fromTrackIndex, toTrackIndex, segment, start)

            newAnimator = ObjectAnimator.ofFloat(itemView, ALPHA, 0.5F, 1F)
        }

        newAnimator.duration = 100
        newAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationCancel(animation: Animator?) {
                itemView.alpha = 1F
                itemView.translationX = 0F
                itemView.translationY = 0F
            }
        })
        newAnimator.start()

        transX = 0F
        transY = 0F

        dragX = 0F
        dragY = 0F
        dragDeltaX = 0F

        fromTrackIndex = 0

        trackGroup.requestDisallowInterceptTouchEvent(false)
        return newAnimator
    }

    private fun calcMoveAdsorption(): Float {
        var desireDis = dragDeltaX - dragX

        val paddingHorizontal = TrackGroup.getPaddingHorizontal()
        val startTime =
            ((itemView.left + dragDeltaX - paddingHorizontal) / TrackConfig.PX_MS).toLong()
        val endTime = startTime + (nleTrackSlot?.duration ?: 0L) / 1000
        val currentStartTime =
            ((itemView.left + dragX - paddingHorizontal) / TrackConfig.PX_MS).toLong()
        val currentEndTime = currentStartTime + (nleTrackSlot?.duration ?: 0L) / 1000
        nleTrackSlot?.let {
            callback?.doAdsorptionOnDrag(
                it,
                dragDirection,
                startTime,
                endTime,
                currentStartTime,
                currentEndTime
            )?.apply {
                if (this != Long.MIN_VALUE) {
                    desireDis = this * TrackConfig.PX_MS
                    if (desireDis != 0f) {
                        trackGroup.safelyPerformHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                    }
                }
            }
        }

        return dragX + desireDis
    }
}
