package com.ss.ugc.android.editor.track.fuctiontrack

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.ViewConfiguration
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.core.avgSpeed
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.safelyPerformHapticFeedback
import com.ss.ugc.android.editor.track.widget.HorizontallyState
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min


val CLIP_BORDER_WIDTH = SizeUtil.dp2px(1.5F).toFloat()
@Px
private val CLIP_HANDLE_WIDTH = SizeUtil.dp2px(20F)
@Px
const val CLIP_ELIMINATE_OF_ERROR = 2

internal typealias TrackClipListener = (
    slot: NLETrackSlot,
    start: Long,
    timelineOffset: Long,
    duration: Long
) -> Unit

internal class TrackClipHelper(
    context: Context,
    private val trackGroup: TrackGroup,
    private val callbackFetcher: () -> TrackGroup.Callback?
) {

    private val TAG = "TrackClipHelper"

    private val leftHandleSrc: Bitmap = BitmapFactory
        .decodeResource(
            context.resources,
            if (ThemeStore.getViceTrackLeftMoveIconRes() != null) ThemeStore.getViceTrackLeftMoveIconRes()!!
            else R.drawable.clip_btn_left
        )

    private val rightHandleSrc: Bitmap = BitmapFactory
        .decodeResource(
            context.resources,
            if (ThemeStore.getViceTrackRightMoveIconRes() != null) ThemeStore.getViceTrackRightMoveIconRes()!!
            else R.drawable.clip_btn_right
        )
    private val handleSrcRect = Rect(0, 0, leftHandleSrc.width, leftHandleSrc.height)

    private val scaledSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0F
    private var downY = 0F

    private var downRawX = 0F
    private var touchRawX = 0F
    private var lastRawX = 0F
    private var initOffset = 0F

    private var isAutoScrollProtect = false // 此标志位为true时，某些自动滑动状态将关闭
    private var performClick = true

    private val screenWidth = SizeUtil.getScreenWidth(context)

    private val minWidth: Float
        get() = trackGroup.clipMinDuration * trackGroup.timelineScale

    private val paint = Paint()
    @ColorInt
    private val borderColor = Color.WHITE

    private val callback: TrackGroup.Callback?
        get() = callbackFetcher.invoke()

    /**
     * 滚动方向，LEFT表示往左边滚动，RIGHT表示往右边滚动
     */
    private var scrollState = HorizontallyState.NULL
    private val autoScrollAnim = ValueAnimator.ofFloat(0F, 1F)

    init {
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.strokeWidth = CLIP_BORDER_WIDTH

        autoScrollAnim.repeatCount = ValueAnimator.INFINITE
        autoScrollAnim.addUpdateListener {
            if (clipState == HorizontallyState.NULL || scrollState == HorizontallyState.NULL) return@addUpdateListener

            val scrollHorizontallyPx = if (scrollState == HorizontallyState.LEFT) {
                -TrackConfig.AUTO_SCROLL_SIZE * autoScrollSpeedRate
            } else {
                TrackConfig.AUTO_SCROLL_SIZE * autoScrollSpeedRate
            }

            if (clipState == HorizontallyState.LEFT) {
                operateLeftCutBtn(leftRect.right + scrollHorizontallyPx, true)
            } else {
                operateRightCutBtn(rightRect.left + scrollHorizontallyPx, true)
            }
            trackGroup.invalidate()
        }
    }

    private val leftRect = RectF() // 坐裁减键的位置
    private val rightRect = RectF()

    /**
     * 代表hold住的是哪边的裁剪按钮，LEFT表示左边，RIGHT表示右边
     */
    private var clipState = HorizontallyState.NULL
    /**
     * 拖动左侧裁剪按钮时，因为音频起始点不能通过裁剪改变，所以最大只能裁剪到对该音频设置的起始点。
     * 又因为音频播放的结束点只能通过拖动右侧裁剪按钮改变，所以左侧裁剪最大距离是：
     * 当前播放起始点 + 当前播放时长 - 音频起始点（当前版本音频起始点无法更改，都是0）
     */
    private var maxLeftClip = 0F
    /**
     * 拖动右侧裁剪按钮时，最大只能裁剪到音频自身的结束点：音频总时长 - 当前播放起始点
     */
    private var maxRightClip = 0F

    private var leftEdge = TrackGroup.getPaddingHorizontal().toFloat()
    private var rightEdge = Int.MAX_VALUE.toFloat()

    private var clipHappened = false

    private var selectedData: Pair<NLETrackSlot, TrackParams>? = null
        set(value) {
            trackGroup.disableScroll(value != null)

            if (field == value) {
                field?.second?.holder?.setItemSelected(true)
                return
            }

            // id相同，说明是刷新数据
            if (field?.first == value?.first) {
                trackGroup.onSelectChanged(value, true)
            } else {
                trackGroup.onSelectChanged(value)
            }
            if (field?.second?.holder != value?.second?.holder) {
                field?.second?.holder?.setItemSelected(false)
                value?.second?.holder?.setItemSelected(true)
            } else {
                value?.second?.holder?.setItemSelected(true)
            }
            field = value
        }

    private var autoScrollSpeedRate = 1.0f

    fun resetSelected() {
        selectedData?.let { (_, params) ->
            resetChildDrawDivider(null, params.holder)
            selectedData = null
            trackGroup.invalidate()
        }
    }

    private fun resetChildDrawDivider(currSelect: TrackItemHolder?, preSelect: TrackItemHolder?) {
        if (currSelect == preSelect) {
            currSelect?.setDrawDivider(false)
        } else {
            preSelect?.setDrawDivider(true)
            currSelect?.setDrawDivider(false)
        }
    }

    fun updateSelected(trackParamsMap: Map<NLETrackSlot, TrackParams>) {
        val (preSegment, preParams) = selectedData ?: return

        var found = false
        trackParamsMap.forEach { (segment, params) ->
            if (segment == preSegment) {
                found = true
                resetChildDrawDivider(params.holder, preParams.holder)
                selectedData = Pair(segment, params)
                return@forEach
            }
        }

        if (!found) {
            selectedData = null
        }

        // 数据刷新了，且之前选中的segment还在，需要重新计算边界值
        selectedData?.let { updateEdges(trackParamsMap, it) }
        trackGroup.trackGroupActionListener?.onSlotSelect(selectedData?.first)
        trackGroup.invalidate()
    }

    fun selectByTap(
        tappedItem: TrackItemHolder,
        trackParamsMap: Map<NLETrackSlot, TrackParams>
    ) {
        trackParamsMap.forEach { (segment, params) ->
            if (params.holder === tappedItem) {
                val selected = selectedData
                selectedData = if (selected == null || selected.first != segment) {
                    resetChildDrawDivider(params.holder, selected?.second?.holder)
                    Pair(segment, params)
                } else {
                    resetChildDrawDivider(null, selected.second.holder)
                    null
                }
                return@forEach
            }
        }

        // 选中情况下才需要重新计算边界值
        selectedData?.let { updateEdges(trackParamsMap, it) }
        trackGroup.trackGroupActionListener?.onSlotSelect(selectedData?.first)
        selectedData?.first?.let { trackGroup.scrollToSlotIfNeeded(it) }
        trackGroup.invalidate()
    }

    fun selectBySegmentId(
        nleTrackSlot: NLETrackSlot,
        trackParamsMap: Map<NLETrackSlot, TrackParams>
    ) {
        trackParamsMap.forEach { (segment, params) ->
            if (segment == nleTrackSlot || segment.id == nleTrackSlot.id) {
                // todo need remove after nle fix
                val preSelect = selectedData?.second?.holder
                resetChildDrawDivider(params.holder, preSelect)
                selectedData = Pair(segment, params)
                return@forEach
            }
        }

        // 选中情况下才需要重新计算边界值
        selectedData?.let { updateEdges(trackParamsMap, it) }
        trackGroup.invalidate()
    }

    fun onTimelineScaleChanged(trackParamsMap: Map<NLETrackSlot, TrackParams>) {
        selectedData?.let { updateEdges(trackParamsMap, it) }
    }

    fun onOrientationChange(trackParamsMap: Map<NLETrackSlot, TrackParams>) {
        selectedData?.let { updateEdges(trackParamsMap, it) }
    }

    private fun updateEdges(
        trackParamsMap: Map<NLETrackSlot, TrackParams>,
        selected: Pair<NLETrackSlot, TrackParams>
    ) {
        val paddingHorizontal = TrackGroup.getPaddingHorizontal()
        val (selSlot, selParams) = selected
        val selTop: Float =
            (selParams.trackIndex * (trackGroup.itemHeight + trackGroup.itemMargin)).toFloat()
        val selBottom: Float = selTop + trackGroup.itemHeight
        val selTimeDuration = selSlot.duration / 1000
        val selLeft: Float =
            selSlot.startTime / 1000 * trackGroup.timelineScale + paddingHorizontal
        val selRight: Float = selLeft + (selTimeDuration) * trackGroup.timelineScale
        leftRect.set(selLeft - CLIP_HANDLE_WIDTH, selTop, selLeft, selBottom)
        rightRect.set(selRight, selTop, selRight + CLIP_HANDLE_WIDTH, selBottom)
        val nleSegmentVideo = NLESegmentVideo.dynamicCast(selSlot.mainSegment)
        val nleSegmentAudio = NLESegmentAudio.dynamicCast(selSlot.mainSegment)
        val stickerImage: NLESegmentImageSticker? =
            NLESegmentImageSticker.dynamicCast(selSlot.mainSegment)
        val stickerInfo: NLESegmentInfoSticker? =
            NLESegmentInfoSticker.dynamicCast(selSlot.mainSegment)
        val stickerText: NLESegmentTextSticker? =
            NLESegmentTextSticker.dynamicCast(selSlot.mainSegment)
        val textTemplate: NLESegmentTextTemplate?=
            NLESegmentTextTemplate.dynamicCast(selSlot.mainSegment)
        val videoEffect: NLESegmentEffect? =
            NLESegmentEffect.dynamicCast(selSlot.mainSegment)
        val filterSegment: NLESegmentFilter? =
            NLESegmentFilter.dynamicCast(selSlot.mainSegment)

        nleSegmentVideo ?: nleSegmentAudio ?: stickerImage ?: stickerInfo ?: stickerText ?: videoEffect ?: textTemplate?: filterSegment ?: return
        // todo 这段代码写得不行，得改改   +1 
//        val sourceStart = convertToSegmentAudio.timeClipStart/1000
        if (selSlot.mainSegment.duration == 0L) {
            maxLeftClip = Float.MAX_VALUE
            maxRightClip = Float.MAX_VALUE
        } else {
            nleSegmentVideo?.let {
                val sourceStart = it.timeClipStart / 1000
                val playDuration = it.duration / 1000
                maxRightClip =
                    (it.resource.duration / 1000 - sourceStart) * trackGroup.timelineScale / it.avgSpeed()
                val sourceEnd = sourceStart + playDuration
                maxLeftClip = sourceEnd * trackGroup.timelineScale / it.avgSpeed()
            } ?: nleSegmentAudio?.let {
                val sourceStart = it.timeClipStart / 1000
                val playDuration = it.duration / 1000
                maxRightClip =
                    (it.resource.duration / 1000 - sourceStart) * trackGroup.timelineScale / it.absSpeed
                val sourceEnd = sourceStart + playDuration
                maxLeftClip = sourceEnd * trackGroup.timelineScale / it.absSpeed
            } ?: stickerImage?.let {
                //todo check is it zero ?
                val sourceStart = 0
                val playDuration = it.duration / 1000
//                maxRightClip =
//                    (it.resource.duration / 1000 - sourceStart) * trackGroup.timelineScale
                maxRightClip = Float.MAX_VALUE
                val sourceEnd = sourceStart + playDuration
                maxLeftClip = sourceEnd * trackGroup.timelineScale
            } ?: stickerInfo?.let {
                val sourceStart = 0
                val playDuration = it.duration / 1000
                val sourceEnd = sourceStart + playDuration
                maxRightClip = Float.MAX_VALUE
                maxLeftClip = sourceEnd * trackGroup.timelineScale
            }?: stickerText ?.let {
                val sourceStart = 0
                val playDuration = it.duration / 1000
                val sourceEnd = sourceStart + playDuration
                maxRightClip = Float.MAX_VALUE
                maxLeftClip = sourceEnd * trackGroup.timelineScale
            } ?: videoEffect ?.let {
                val sourceStart = 0
                val playDuration = it.duration / 1000
                val sourceEnd = sourceStart + playDuration
                maxRightClip = Float.MAX_VALUE
                maxLeftClip = sourceEnd * trackGroup.timelineScale
            } ?: textTemplate?.let {
                val sourceStart = 0
                val playDuration = it.duration / 1000
                val sourceEnd = sourceStart + playDuration
                maxRightClip = Float.MAX_VALUE
                maxLeftClip = sourceEnd * trackGroup.timelineScale
            } ?: filterSegment?.let {
                val sourceStart = 0
                val playDuration = it.duration / 1000
                val sourceEnd = sourceStart + playDuration
                maxRightClip = Float.MAX_VALUE
                maxLeftClip = sourceEnd * trackGroup.timelineScale
            }

        }
        leftEdge = paddingHorizontal.toFloat()
        rightEdge = Float.MAX_VALUE

        val segments = trackParamsMap.filter { (_, t) ->
            t.trackIndex == selParams.trackIndex
        }.map { it.key }.sortedBy { it.startTime / 1000 }
        val index = segments.indexOfFirst { it == selSlot }
        if (index >= 0) {
            if (index > 0) {
                val rightInPx = segments[index - 1].measuredEndTime / 1000 *
                        trackGroup.timelineScale + paddingHorizontal
                leftEdge = max(rightInPx, leftEdge)
            }

            if (index < segments.size - 1) {
                val leftInPx = segments[index + 1].startTime / 1000 *
                        trackGroup.timelineScale + paddingHorizontal
                rightEdge = min(leftInPx, rightEdge)
            }
        }

        // 如果不能超过视频长度，那么加个限制
        if (rightEdge == Float.MAX_VALUE) {
            if (!trackGroup.canMoveOutOfMainVideo()) {
                rightEdge = trackGroup.mainVideoLength + paddingHorizontal
            } else if (!trackGroup.canMoveOutOfVideos) {
                rightEdge = trackGroup.videosLength + paddingHorizontal
            }
        }
    }

    fun onInterceptTouchEvent(scrollX: Int, scrollY: Int, ev: MotionEvent?): Boolean {
        ev ?: return false

        if (ev.action != ACTION_DOWN) return false

        // 要加上滚动的距离才能与子View的坐标做对比
        val x = ev.x + scrollX
        val y = ev.y + scrollY
        return (leftRect.contains(x, y) || rightRect.contains(x, y))
    }

    fun onTouchEvent(
        scrollX: Int,
        scrollY: Int,
        event: MotionEvent,
        clip: TrackClipListener
    ): Boolean {

        val (segment, params) = this.selectedData ?: return false
        // 要加上滚动的距离才能与子View的坐标做对比
        val x = event.x + scrollX
        val y = event.y + scrollY

        when (event.action) {
            ACTION_DOWN -> {
                downX = x
                downY = y

                downRawX = event.rawX
                touchRawX = event.rawX
                lastRawX = touchRawX
                // 如果在自动滚动区按下，置true，将关闭一些自动滑动状态
                isAutoScrollProtect = isAutoScrollGestureArea(downRawX)

                performClick = true
                clipState = when {
                    leftRect.contains(x, y) -> { // 按下了左裁减键
                        initOffset = leftRect.right - x
                        trackGroup.requestDisallowInterceptTouchEvent(true)
                        selectedData?.first?.let {
                            callback?.onStartAdsorption(it)
                        }
                        params.holder.setClipping(true)
                        HorizontallyState.LEFT
                    }
                    rightRect.contains(x, y) -> {
                        initOffset = x - rightRect.left
                        trackGroup.requestDisallowInterceptTouchEvent(true)
                        selectedData?.first?.let {
                            callback?.onStartAdsorption(it)
                        }
                        params.holder.setClipping(true)
                        HorizontallyState.RIGHT
                    }
                    else -> HorizontallyState.NULL
                }
            }

            ACTION_MOVE -> {
                touchRawX = event.rawX

                if (clipState == HorizontallyState.LEFT) {
                    val targetRight = x + initOffset
                    operateLeftCutBtn(targetRight, false)
                    trackGroup.invalidate()
                } else {
                    val targetLeft = x - initOffset
                    operateRightCutBtn(targetLeft, false)
                    trackGroup.invalidate()
                }
                lastRawX = touchRawX

                val scaledX = x - downX
                val scaledY = y - downY
                if (scaledSlop * scaledSlop < scaledX * scaledX + scaledY * scaledY) {
                    performClick = false
                }
            }

            ACTION_UP, ACTION_CANCEL -> {
                params.holder.setClipping(false)
                trackGroup.requestDisallowInterceptTouchEvent(false)
                if (performClick) {
                    trackGroup.resetSelected()
                } else if (clipState != HorizontallyState.NULL) {
                    onClipEnd(segment, clip, scrollX)
                }
                setAutoScrollState(HorizontallyState.NULL)
                clipState = HorizontallyState.NULL
            }
        }

        return clipState != HorizontallyState.NULL
    }

    private fun onClipEnd(
        slot: NLETrackSlot,
        clip: TrackClipListener,
        scrollX: Int
    ) {
        // 降低误差带来的影响
        val targetPosition: Int = if (clipState == HorizontallyState.LEFT)
            ceil(leftRect.right).toInt()
        else
            floor(rightRect.left - CLIP_ELIMINATE_OF_ERROR).toInt()
        val nleSegmentVideo = NLESegmentVideo.dynamicCast(slot.mainSegment)
        val nleSegmentAudio = NLESegmentAudio.dynamicCast(slot.mainSegment)
        val stickerImage: NLESegmentImageSticker? =
            NLESegmentImageSticker.dynamicCast(slot.mainSegment)
        val stickerInfo: NLESegmentInfoSticker? =
            NLESegmentInfoSticker.dynamicCast(slot.mainSegment)
        val stickerText: NLESegmentTextSticker? =
            NLESegmentTextSticker.dynamicCast(slot.mainSegment)
        val textTemplate: NLESegmentTextTemplate? =
            NLESegmentTextTemplate.dynamicCast(slot.mainSegment)
        val videoEffect: NLESegmentEffect? =
            NLESegmentEffect.dynamicCast(slot.mainSegment)
        val filterSegment: NLESegmentFilter? =
            NLESegmentFilter.dynamicCast(slot.mainSegment)
        val segment =
            nleSegmentVideo ?: nleSegmentAudio ?: stickerImage ?: stickerInfo ?: stickerText
            ?: videoEffect ?: textTemplate ?: filterSegment?:return


        if (clipHappened) {
            var trimStart =
                (nleSegmentVideo?.timeClipStart ?: nleSegmentAudio?.timeClipStart ?: 0) / 1000
            var offsetStart = slot.startTime / 1000
            var duration = slot.duration / 1000
            val newDuration =
                ((rightRect.left - leftRect.right) / trackGroup.timelineScale).toLong()
            // 前后长度一致，需要调用接口
            if (newDuration != duration) {
                val isInfoStickerOrTextSticker = segment is NLESegmentInfoSticker || segment is NLESegmentTextSticker || segment is NLESegmentImageSticker
                if (segment is NLESegmentSticker && isInfoStickerOrTextSticker) {
                    //这里要对动画进行动态按比例调整时间
                    slot.apply {
                        if (segment.animation != null) {
                            if (segment.animation.inAnim != null) {
                                val time = segment.animation.inDuration
                                segment.animation.inDuration = (newDuration.toDouble() / duration * time).toInt()
                            }

                            if (segment.animation.outAnim != null) {
                                val time = segment.animation.outDuration
                                segment.animation.outDuration = (newDuration.toDouble() / duration * time).toInt()
                            }
                        }
                    }
                }
                if (clipState == HorizontallyState.LEFT) {
                    val offset = newDuration - duration
                    offsetStart -= offset
                    trimStart -= (offset * (nleSegmentVideo?.avgSpeed()
                        ?: nleSegmentAudio?.absSpeed ?: 1f)).toLong()
                    duration = newDuration
                    clip(slot, -offset, offsetStart, duration)
                } else {
                    duration = newDuration
                    clip(slot, 0, offsetStart, duration)
                }

            }
        } else {
            // 没有发生过有效的裁剪也过要seek到视频上
            callback?.clipTo(targetPosition - TrackGroup.getPaddingHorizontal())
        }
        callback?.smoothScrollHorizontallyBy(getScrollDiff(scrollX, targetPosition), false)
        clipHappened = false
    }

    /**
     * 操作左裁减键
     * @param uncheckedBound 手指的位置
     * @param isAutoCut 是否是自动裁减
     */
    private fun operateLeftCutBtn(uncheckedBound: Float, isAutoCut: Boolean) {
        // 检查移动是否超出裁剪边界，去掉超出的部分
        var checkedBound = getLeftCutBtnValidBound(uncheckedBound)

        // 计算吸附点跟把手位置的偏移
        // 如果刚好到达了边界，就不处理吸附，不然时间线刚好在边界附近的情况下可能会永远无法裁剪到边界
        val adsorptionDistance = if (checkedBound == rightRect.left - maxLeftClip) {
            null
        } else {
            calcLeftAdsorptionDistance(checkedBound)
        }
        if (adsorptionDistance != null) {
            // 把手的位置加上吸附产生的偏移，就是新的裁剪位置
            val adsorptionBound = leftRect.right + adsorptionDistance

            // 新的位置计算出来要重新检查一下是否超出裁剪边界，并去掉超出的部分
            checkedBound = getLeftCutBtnValidBound(adsorptionBound)

            // 如果吸附偏移不为0，并且加上吸附偏移后的裁剪位置跟边界检查后的位置相同，就说明产生了吸附，需要震动
            if (adsorptionDistance != 0F && adsorptionBound == checkedBound) {
                trackGroup.safelyPerformHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }
        val lastBound = leftRect.right
        leftRect.left = checkedBound - CLIP_HANDLE_WIDTH
        leftRect.right = checkedBound
        cut(checkedBound, lastBound, isAutoCut)
    }

    private fun operateRightCutBtn(uncheckedBound: Float, isAutoCut: Boolean) {
        // 检查移动是否超出裁剪边界，去掉超出的部分
        var checkedBound = getRightCutBtnValidBound(uncheckedBound)

        // 计算吸附点跟把手位置的偏移
        // 如果刚好到达了边界，就不处理吸附，不然时间线刚好在边界附近的情况下可能会永远无法裁剪到边界
        val adsorptionDistance = if (checkedBound == maxRightClip + leftRect.right) {
            null
        } else {
            calcRightAdsorptionDistance(checkedBound)
        }
        if (adsorptionDistance != null) {
            // 把手的位置加上吸附产生的偏移，就是新的裁剪位置
            val adsorptionBound = rightRect.left + adsorptionDistance

            // 新的位置计算出来要重新检查一下是否超出裁剪边界，并去掉超出的部分
            checkedBound = getRightCutBtnValidBound(adsorptionBound)

            // 如果吸附偏移不为0，并且加上吸附偏移后的裁剪位置跟边界检查后的位置相同，就说明产生了吸附，需要震动
            if (adsorptionDistance != 0F && adsorptionBound == checkedBound) {
                trackGroup.safelyPerformHapticFeedback(
                    HapticFeedbackConstants.LONG_PRESS,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                )
            }
        }

        val lastBound = rightRect.left
        rightRect.left = checkedBound
        rightRect.right = checkedBound + CLIP_HANDLE_WIDTH
        cut(checkedBound, lastBound, isAutoCut)
    }

    /**
     * 进行裁减
     * @param targetBound 期望裁剪到的目标边界
     * @param lastBound 上次已经裁减到的边界
     */
    private fun cut(targetBound: Float, lastBound: Float, isAutoCut: Boolean) {
        if (isAutoCut && targetBound == lastBound) { // 自动滚动到达边界，不能再自动滚动了
            setAutoScrollState(HorizontallyState.NULL)
            return
        }

        val lastTimeLineBound = getTimeLineBound(lastBound)
        val targetTimeLineBound = getTimeLineBound(targetBound)
        callback?.apply {
            // 如果实际上没有发生变化，就不用做接下去的操作了
            if (targetTimeLineBound == lastTimeLineBound) return@apply

            clipHappened = true
            clipTo(targetTimeLineBound) // 先裁剪到指定位置
            // 自动裁减，裁完之后需要滚动一下裁减的距离，这样裁减键才能一直跟随手指的位置不变
            if (isAutoCut) scrollBy(trackGroup, targetTimeLineBound - lastTimeLineBound, 0, false)
        }
        checkAutoScrollCut()
    }

    /**
     * 计算 左裁减键 能裁减的合法边界范围
     */
    private fun getLeftCutBtnValidBound(targetBound: Float): Float {
        var resultBound = targetBound
        val maxRight = rightRect.left - minWidth
        // 裁剪后长度不能小于最小音频长度
        if (resultBound > maxRight) {
            resultBound = maxRight
        }

        // 向左拖动最多只能拖动到音频播放起始点（sourceTimeRange.start）为0
        if (rightRect.left - resultBound > maxLeftClip) {
            resultBound = rightRect.left - maxLeftClip
        }

        // 向左拖动不能超过左边界（左边界是往左边第一个segment的尾部，没有的话就是整个轨道到头部）
        if (resultBound < leftEdge) {
            resultBound = leftEdge
        }

        return resultBound
    }

    private fun getRightCutBtnValidBound(targetBound: Float): Float {
        var resultBound = targetBound
        val minLeft = leftRect.right + minWidth
        // 裁剪后长度不能小于最小音频长度
        if (resultBound < minLeft) {
            resultBound = minLeft
        }

        // 向右拖动最多只能拖动到音频资源的结束位置
        if (resultBound - leftRect.right > maxRightClip) {
            resultBound = maxRightClip + leftRect.right
        }

        // 向右拖动不能超过右边第一个segment（如果有）的头部
        if (resultBound > rightEdge) {
            resultBound = rightEdge
        }

        return resultBound
    }

    /**
     * 当手指处于屏幕边缘一定距离时，将启动自动滚动裁减
     */
    private fun checkAutoScrollCut() {
        if (isAutoScrollProtect) { // 当手指touch区域超过自动滑动区，则置false，自滑状态全部开启
            isAutoScrollProtect = isAutoScrollGestureArea(touchRawX)
        }

        autoScrollSpeedRate = TrackConfig.calAutoScrollSpeedRate(touchRawX, screenWidth)

        var horizontallyState = HorizontallyState.NULL
        when {
            screenWidth - touchRawX <= TrackConfig.AUTO_SCROLL_START_POSITION // 手指当前在右边的自动滚动区
                    && (!isAutoScrollProtect || touchRawX - downRawX > 0) -> { // 如果自滑保护开启，则手势朝右时，才允许自滑
                horizontallyState = HorizontallyState.RIGHT
            }
            touchRawX <= TrackConfig.AUTO_SCROLL_START_POSITION && // 手指当前在左边的自动滚动区
                    (!isAutoScrollProtect || touchRawX - downRawX < 0) -> { // 如果自滑保护开启，则手势朝左时，才允许自滑
                horizontallyState = HorizontallyState.LEFT
            }
        }

        setAutoScrollState(horizontallyState)
    }

    private fun setAutoScrollState(newScrollState: HorizontallyState) {
        if (scrollState == newScrollState) return

        scrollState = newScrollState
        when (newScrollState) {
            HorizontallyState.NULL -> {
                autoScrollAnim.cancel()
                // 停止滚动时启用吸附
                selectedData?.first?.let {
                    callback?.onStartAdsorption(it)
                }
            }
            else -> {
                // 滚动时取消吸附
                callback?.onCancelAdsorption()
                autoScrollAnim.start() // 状态不变不重新start
            }
        }
    }

    private fun isAutoScrollGestureArea(rawX: Float): Boolean {
        return screenWidth - rawX <= TrackConfig.AUTO_SCROLL_START_POSITION || rawX <= TrackConfig.AUTO_SCROLL_START_POSITION
    }

    private fun getScrollDiff(scrollX: Int, targetPosition: Int): Int {
        // PlayHead的初始位置（scrollX为0时）刚好与host到paddingLeft对齐，
        // 所以拖动的子View到边缘要对齐到PlayHead，需要减去父View到paddingLeft
        return targetPosition - TrackGroup.getPaddingHorizontal() - scrollX
    }

    /**
     * 视图边界 转换为 时间轴 边界
     */
    private fun getTimeLineBound(viewBounds: Float): Int {
        return viewBounds.toInt() - TrackGroup.getPaddingHorizontal()
    }

    fun drawDecorate(canvas: Canvas) {
        val item = selectedData?.second?.holder ?: return
        val slot = selectedData?.first ?: return
//        TrackLog.d(TAG, "draw decorate ${selectedData?.first}")

        // 先绘制一下背景色，盖住view本来绘制的，然后重新绘制拖动裁剪后的部分
        paint.color = TrackItemHolder.PARENT_BG_COLOR
        val view = item.getView()
        canvas.drawRect(
            view.left.toFloat(), view.top.toFloat(),
            view.right.toFloat(), view.bottom.toFloat(), paint
        )

        val exactViewLeft =
            slot.startTime / 1000 * trackGroup.timelineScale + TrackGroup.getPaddingHorizontal()
        item.drawOn(
            canvas,
            leftRect.right,
            leftRect.top,
            rightRect.left,
            leftRect.bottom,
            leftRect.right - exactViewLeft,
            rightRect.left - exactViewLeft
        )

        paint.color = borderColor
        val borderTop = leftRect.top + CLIP_BORDER_WIDTH / 2F
        canvas.drawLine(leftRect.right, borderTop, rightRect.left, borderTop, paint)
        val borderBottom = leftRect.bottom - CLIP_BORDER_WIDTH / 2F
        canvas.drawLine(leftRect.right, borderBottom, rightRect.left, borderBottom, paint)

        canvas.drawBitmap(leftHandleSrc, handleSrcRect, leftRect, null)
        canvas.drawBitmap(rightHandleSrc, handleSrcRect, rightRect, null)
    }

    /**
     * @return offset 如果没有找到吸附点就为null，如果找到了吸附点就返回当前把手位置跟吸附点的距离
     */
    private fun calcLeftAdsorptionDistance(
        targetRight: Float
    ): Float? = callback?.doAdsorptionOnClip(
        ((targetRight - TrackGroup.getPaddingHorizontal()) / trackGroup.timelineScale).toLong(),
        ((leftRect.right - TrackGroup.getPaddingHorizontal()) / trackGroup.timelineScale).toLong()
    )?.let { it * trackGroup.timelineScale }

    private fun calcRightAdsorptionDistance(
        targetLeft: Float
    ): Float? = callback?.doAdsorptionOnClip(
        ((targetLeft - TrackGroup.getPaddingHorizontal()) / trackGroup.timelineScale).toLong(),
        ((rightRect.left - TrackGroup.getPaddingHorizontal()) / trackGroup.timelineScale).toLong()
    )?.let { it * trackGroup.timelineScale }
}
