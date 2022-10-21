package com.ss.ugc.android.editor.track.widget

import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.getLocationOnScreen
import com.ss.ugc.android.editor.track.utils.safelyPerformHapticFeedback
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Looper
import android.text.TextUtils
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.MainThread
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLEVideoAnimation
import com.bytedance.ies.nleeditor.getMainSegment
import com.bytedance.ies.nlemediajava.Log
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.track.BuildConfig
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.base.utils.runOnUiThread
import com.ss.ugc.android.editor.core.api.video.CURVE_SPEED_NAME
import com.ss.ugc.android.editor.core.avgSpeed
import com.ss.ugc.android.editor.track.data.SpeedInfo
import com.ss.ugc.android.editor.track.keyframe.FrameView
import com.ss.ugc.android.editor.track.keyframe.KeyframeSelectChangeListener
import kotlinx.android.synthetic.main.layout_video_track.view.ivTransition
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 *  author : wenlongkai
 *  date : 2019/3/26 下午6:01
 *  description :
 */

class MultiTrackLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private var adjustDuration: Long = 0
    private var itemTrackList = mutableListOf<ItemTrackLayout>()
    private var relativeLayout = RelativeLayout(context)
    private var slotList = mutableListOf<NLETrackSlot>()
    private var multiTrackListener: MultiTrackListener? = null
    private var resourceId = 1001
    private var screenWidth = SizeUtil.getScreenWidth(context)
    private var adjustImageView = ImageView(context)
    private var isInit = false

    private var lastTransitionIndex = -1
    private var selectedSegmentId: NLETrackSlot? = null

    private var trackStyle: TrackStyle = TrackStyle.NONE
    private var labelType: LabelType = LabelType.NONE

    var isLongClickEnable = true
    private var isCoverMode = false

    var scale = 1.0
        private set

    private var isDragging = false
    var myScrollX = 0
        private set
    var isDockerTopLevel: (() -> Boolean)? = null

    var isPreviewFullScreen: (() -> Boolean) = { false }

    var selectedTrackStyle = TrackStyle.CLIP

    var isSelectAgainToRoot = true

    private val pressScaleAnim: ValueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
        duration = 200
    }

    private var itemClipCallback: ItemClipCallback? = null
    private var reactCallback: ReactCallback? = null

//    private val addEpilogueBuilder by lazy { AddEpilogueBuilder(context) }

    interface MultiTrackListener : ScrollHandler {
        fun onStartAndDuration(slot: NLETrackSlot, start: Int, duration: Int, side: Int)

        fun onTransitionClick(slot: NLETrackSlot, nextSlot: NLETrackSlot)

        fun onDragStart()

        fun onDragComplete(
            slot: NLETrackSlot,
            fromIndex: Int,
            targetIndex: Int
        )

        fun shouldDrawIcon(): Boolean

        fun onDeselectSegment()

        fun getParentScrollX(): Int

        fun onSegmentClick(slot: NLETrackSlot?)


        fun onKeyFrameSelect(frame: NLETrackSlot?)

        fun onKeyFrameClick(playHead: Long)

        fun onKeyFrameDeselect()

        fun getCurrentKeyframe(): NLETrackSlot?
    }

    interface ReactCallback {
        fun addTailLeader()

        fun getMusicBeats(): List<Long>

        fun getPlayPosition(): Long

        fun getFrameBitmap(path: String, timestamp: Int): Bitmap?
    }

    private val trackScrollHandle = object : ScrollHandler {
        override fun scrollBy(
            x: Int,
            y: Int,
            invokeChangeListener: Boolean,
            disablePruneX: Boolean,
            disablePruneY: Boolean
        ) {
            multiTrackListener?.scrollBy(
                x,
                y,
                invokeChangeListener,
                disablePruneX,
                disablePruneY
            )
        }

        override fun smoothScrollHorizontallyBy(x: Int, invokeChangeListener: Boolean) {
            multiTrackListener?.smoothScrollHorizontallyBy(
                x,
                invokeChangeListener
            )
        }

        override fun assignMaxScrollX(maxScrollX: Int) {
        }
    }


    fun init(segments: List<NLETrackSlot>) {
        if (!isInit) {
            addView(
                relativeLayout,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            )
            clipChildren = false
            isInit = true
        }
        reload(segments)
    }

    fun onPlayPositionChanged() {
        itemTrackList.filter { it.style != TrackStyle.NONE }.forEach {
            it.onPlayPositionChanged()
        }
    }

    fun refreshPadding() {
        val screenWidthByUtil = SizeUtil.getScreenWidth(context)
        val paddingHorizontal = screenWidthByUtil / 2 - SizeUtil.dp2px(20f)
        setPadding(paddingHorizontal, paddingTop, 0, paddingBottom)
    }


    private val keyFrameListener = object : KeyframeSelectChangeListener {
        override fun onKeyframeSelect(frame: NLETrackSlot?) {
            multiTrackListener?.onKeyFrameSelect(frame)
        }

        override fun onKeyframeDeselect() {
            multiTrackListener?.onKeyFrameDeselect()
        }

        override fun onKeyframeClick(playHead: Long) {
            multiTrackListener?.onKeyFrameClick(playHead)
        }
    }


    private fun reload(segments: List<NLETrackSlot>) {
        refreshPadding()
        ILog.i(TAG, "reload! size = ${segments.size}")
        relativeLayout.removeAllViews()
        slotList.clear()
        itemTrackList.clear()
        slotList.addAll(segments)
        slotList.forEachIndexed { index, segment ->
            resourceId++
            ILog.i(TAG, "reload! index = ${index}, segment = ${segment.toJsonString()}")

            val itemTrackLayout = ItemTrackLayout(context)
            itemTrackLayout.id = resourceId
            itemTrackLayout.setItemTrackCallback(itemTrackCallback)
            itemTrackLayout.setScrollHandler(trackScrollHandle)
            itemTrackLayout.setOnDragListener(DragTrackCallback(itemTrackLayout))
            itemTrackLayout.index = index
            itemTrackLayout.isDockerTopLevel = this.isDockerTopLevel
            if (isCoverMode) {
                itemTrackLayout.goneActionIcon()
            }


            if (index >= itemTrackList.size)
                itemTrackLayout.isCoverMode = this.isCoverMode
            itemTrackList.add(itemTrackLayout)

            // keyframe
            itemTrackLayout.setFrameViewCallback(object : FrameView.FrameViewCallback {
                override fun getParentScrollX(): Int = multiTrackListener?.getParentScrollX() ?: 0

                override fun shouldCallback(): Boolean =
                    itemTrackLayout.style == TrackStyle.CLIP || itemTrackLayout.style == TrackStyle.LINE

                override fun getActiveKeyframe(): NLETrackSlot? {
                    return multiTrackListener?.getCurrentKeyframe()
                }

                override fun shouldDrawIcon(): Boolean =
                    (multiTrackListener?.shouldDrawIcon() == true) && itemTrackLayout.style != TrackStyle.NONE
            })
            itemTrackLayout.setFrameSelectChangeListener(keyFrameListener)


            segment.let {
                var preSegmentTransitionDuration = 0L
                if (index != 0) {
                    preSegmentTransitionDuration =
                        getPreSegmentOverlapTransitionDuration(index)
                }
                itemTrackLayout.setData(
                    segment,
                    multiTrackListener?.getParentScrollX() ?: 0,
                    preSegmentTransitionDuration
                )

                if (itemTrackLayout.duration <= 1000)
                    itemTrackLayout.post {
                        val screenWidth = SizeUtil.getScreenWidth(context)
                        val partSize = screenWidth / 4 * 3
                        val left = screenWidth - partSize
                        val local = itemTrackLayout.getLocationOnScreen()

                        ILog.d(
                            "GuideManager", "local.width:${local.width}\n" +
                                    "left:$left\n" +
                                    "itemTrackLayout.width:${itemTrackLayout.width}\n" +
                                    "partSize:$partSize"
                        )

                    }
            }
        }
        layoutInitItem()
    }

    private fun layoutInitItem() {
        ILog.i(TAG, "reload! layoutInitItem")

        var deviation = 0F
        itemTrackList.forEachIndexed { index, itemTrackLayout ->
            val layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            if (index != 0) {
                layoutParams.addRule(RIGHT_OF, itemTrackList[index - 1].id)
                // 计算累计舍弃掉的小数，每次取整时，带上之前累计舍弃掉的小数，抹平误差
                val transMarginFloat = calTransitionMargin(index)
                Log.d(TAG, "transMarginFloat $transMarginFloat")
                val transitionMargin = (transMarginFloat + deviation).toInt()
                Log.d(TAG, "transitionMargin $transMarginFloat")
                deviation += (transMarginFloat - transitionMargin)
                if (isCoverMode) {
                    layoutParams.leftMargin =
                        -2 * TrackConfig.BORDER_WIDTH - transitionMargin - 8
                } else {
                    layoutParams.leftMargin =
                        -2 * TrackConfig.BORDER_WIDTH - transitionMargin
                }
                itemTrackLayout.setTransitionIcon(
                    getIconId(
                        slotList[index - 1].endTransition?.effectSDKTransition?.resourceFile ?: ""
                    )
                )
                itemTrackLayout.setNormalType()
            }

            relativeLayout.addView(itemTrackLayout, layoutParams)
        }
        setEpilogueEnable(true)
        val currentIndex = getSelectedIndex()
        if (currentIndex != -1) {
            refreshTrackStyle(trackStyle, currentIndex)
            if (selectIndex != -1) {
                if (!isValidIndex(selectIndex)) return
                if (selectedTrackStyle == TrackStyle.LINE) itemTrackList[selectIndex].setLineType()
                else itemTrackList[selectIndex].setClipType()
            }
        }
        refreshLabel(labelType)

    }

    private fun getSelectedIndex(): Int = selectedSegmentId?.let { segmentId ->
        slotList.indexOfFirst { it.id == segmentId.id }
    } ?: -1

    private fun initAddEpilogueLayoutParams(deviation: Float): LayoutParams {
        val lastIndex = itemTrackList.size - 1
        val leftTrackView = itemTrackList[lastIndex].id
        val layoutParams =
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                addRule(RIGHT_OF, leftTrackView)
                addRule(ALIGN_TOP, leftTrackView)
                addRule(ALIGN_BOTTOM, leftTrackView)
            }
        val transMarginFloat = calTransitionMargin(lastIndex + 1)
        val transitionMargin = (transMarginFloat + deviation).toInt()
        layoutParams.leftMargin =
            -TrackConfig.BORDER_WIDTH - transitionMargin
        layoutParams.width =
            (MOVIE_FOOT_DURATION / TrackConfig.DEFAULT_FRAME_DURATION).toInt() *
                    TrackConfig.THUMB_WIDTH
        return layoutParams
    }

    fun updateSegmentList(segments: List<NLETrackSlot>) {
        if (segments.size != slotList.size) { // 有一个IndexOutOfBoundsException,怀疑是这里更新了一个空的Segments
            return
        }
        slotList.clear()
        slotList.addAll(segments)
        ILog.i(TAG, "updateSegmentList size = ${segments.size}")
    }

    fun addAdjustLayout(duration: Long) {
        adjustDuration = duration
        relativeLayout.removeView(adjustImageView)
        val width = duration * TrackConfig.PX_MS
        // kotlin roundToInt throw NaN exception
        val layoutParams = LayoutParams(Math.round(width), TrackConfig.THUMB_HEIGHT)
        if (itemTrackList.isNotEmpty()) {
            layoutParams.addRule(RIGHT_OF, itemTrackList.last().id)
            layoutParams.leftMargin = -TrackConfig.BORDER_WIDTH
            layoutParams.rightMargin = TrackConfig.BORDER_WIDTH
        }
        adjustImageView.setBackgroundColor(Color.parseColor("#1B1B1B"))
        relativeLayout.addView(adjustImageView, 0, layoutParams)
    }

    fun resizeAdjustLayout() {
        val width = adjustDuration * TrackConfig.PX_MS
        val params = adjustImageView.layoutParams
        if (null != params) {
            // kotlin roundToInt throw NaN exception
            params.width = Math.round(width)
            adjustImageView.layoutParams = params
        }
    }

    inner class DragTrackCallback(val view: ItemTrackLayout) :
        OnTrackDragListener {
        private var moveX = 0F
        private var moveIndex = 0

//         * 开始拖动，将每个视频段缩放到一帧，按照按下位置排序
        override fun beginDrag(downX: Float, downY: Float) {
            if (!isLongClickEnable) {
                return
            }
            isDragging = true
            multiTrackListener?.onDragStart()

            relativeLayout.removeView(adjustImageView)
            var actionX = downX
            var centerX = 0f
            moveIndex = view.index
            view.bringToFront()
            view.safelyPerformHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )

            val itemWidthOffset = IntArray(itemTrackList.size)
            val itemTranslateOffset = IntArray(itemTrackList.size)
            val itemTranslateStart = IntArray(itemTrackList.size)

            itemTrackList.forEachIndexed { index, itemTrackLayout ->
                itemTrackLayout.goneActionIcon()
                when {
                    index < view.index -> actionX += itemTrackLayout.width - 2 * TrackConfig.BORDER_WIDTH - itemTrackLayout.getTransitionWidth()
                    index == view.index -> {
                        centerX = actionX
                        itemTranslateStart[index] =
                            itemTrackList[index].left + TrackConfig.BORDER_WIDTH
                        itemTranslateOffset[index] = actionX.toInt() - itemTranslateStart[index]
                    }
                    index > view.index -> {
                        actionX += TrackConfig.THUMB_WIDTH + TrackConfig.DIVIDER_WIDTH
                        itemTranslateStart[index] =
                            itemTrackList[index].left + TrackConfig.BORDER_WIDTH
                        itemTranslateOffset[index] = actionX.toInt() - itemTranslateStart[index]
                    }
                }
                itemWidthOffset[index] = itemTrackLayout.width - TrackConfig.THUMB_WIDTH
            }

            var positionX = centerX
            for (i in (view.index - 1) downTo 0) {
                positionX -= TrackConfig.THUMB_WIDTH + TrackConfig.DIVIDER_WIDTH
                // 设置goneActionIcon的时，由于handler bar为gone，会导致视频段往前移动，所以往后移动 BORDER_WIDTH 距离
                itemTranslateStart[i] = itemTrackList[i].left + TrackConfig.BORDER_WIDTH
                itemTranslateOffset[i] = positionX.toInt() - itemTranslateStart[i]
            }

            pressScaleAnim.cancel()
            pressScaleAnim.removeAllUpdateListeners()
            pressScaleAnim.addUpdateListener {
                itemTrackList.forEachIndexed { index, itemTrackLayout ->
                    val width =
                        TrackConfig.THUMB_WIDTH + (1F - it.animatedValue as Float) * itemWidthOffset[index]
                    itemTrackLayout.layoutParams =
                        LayoutParams(width.toInt(), TrackConfig.THUMB_HEIGHT)
                    val transX =
                        itemTranslateStart[index] + it.animatedValue as Float * itemTranslateOffset[index]
                    itemTrackLayout.translationX = transX
                    if (it.animatedValue == 1F) {
                        itemTrackLayout.beginDrag()
                    }
                }
            }
            pressScaleAnim.start()

            relativeLayout.layoutParams.width = SizeUtil.getScreenWidth(this@MultiTrackLayout.context) +
                    itemTrackList.size * TrackConfig.THUMB_WIDTH + itemTranslateOffset[0]

            itemTrackList.firstOrNull { it.isFooterType }?.let {
                it.visibility = View.GONE
            } ?: apply {
                //                addEpilogueBuilder.build().visibility = View.GONE
            }
        }

//*
//         * 拖动过程
//         * 1、拖动块在屏幕边界时，自动滚动
//         * 2、拖动到其他视频块位置时，自动切换位置


        override fun drag(
            deltaX: Float,
            deltaY: Float,
            isLeftScreenBorder: Boolean,
            isRightScreenBorder: Boolean
        ) {
            if (!isLongClickEnable) {
                return
            }
            moveX += deltaX
            ILog.d(TAG, "this is move is $moveX deltaX is $deltaX")
            view.translationX = view.translationX + deltaX
            val currentIndexArray = IntArray(2)
            view.getLocationOnScreen(currentIndexArray)
            val currentX = currentIndexArray[0]
            var absoluteDeltaX = deltaX
            var isNeedScroll = false
            if (moveX < 0) {
                val intArray = IntArray(2)
                if (view.index == 0) {
                    if (itemTrackList.size == 1) {
                        itemTrackList[0].getLocationOnScreen(intArray)
                    } else {
                        itemTrackList[1].getLocationOnScreen(intArray)
                    }
                } else {
                    itemTrackList[0].getLocationOnScreen(intArray)
                }
                if (intArray[0] - currentX < screenWidth / 2) {
                    isNeedScroll = true
                }
            } else {
                val intArray = IntArray(2)
                // 排除片尾影响
                if (view.index == itemTrackList.size - 2) {
                    if (itemTrackList.size - 3 < 0) {
                        itemTrackList[itemTrackList.size - 2].getLocationOnScreen(intArray)
                    } else {
                        itemTrackList[itemTrackList.size - 3].getLocationOnScreen(intArray)
                    }
                } else {
                    if (itemTrackList.size - 2 >= 0) {
                        itemTrackList[itemTrackList.size - 2].getLocationOnScreen(intArray)
                    }
                }
                if (currentX - intArray[0] < screenWidth / 2) {
                    isNeedScroll = true
                }
            }
            if ((isLeftScreenBorder || isRightScreenBorder)) {
                itemTrackList.forEachIndexed { index, itemTrackLayout ->
                    if (view.index != index) {
                        if (isLeftScreenBorder) {
                            if (isNeedScroll) {
                                itemTrackLayout.translationX += 15
                                absoluteDeltaX -= 15
                            }
                        } else if (isRightScreenBorder) {
                            if (isNeedScroll) {
                                itemTrackLayout.translationX -= 15
                                absoluteDeltaX += 15
                            }
                        }
                    }
                }
            }
            itemTrackList.forEachIndexed { index, itemTrackLayout ->
                if (index != view.index) {
                    val intArray = IntArray(2)
                    itemTrackLayout.getLocationOnScreen(intArray)
                    if (absoluteDeltaX > 0) {
                        if (abs(currentX - intArray[0]) < TrackConfig.THUMB_WIDTH / 2) {
                            itemTrackLayout.translationX -= TrackConfig.THUMB_WIDTH
                            moveIndex++
                        }
                    } else {
                        if (abs(currentX - intArray[0]) < TrackConfig.THUMB_WIDTH / 2) {
                            itemTrackLayout.translationX += TrackConfig.THUMB_WIDTH
                            moveIndex--
                        }
                    }
                }
            }
        }

        override fun endDrag() {
            if (!isLongClickEnable) {
                return
            }

            pressScaleAnim.cancel()
            pressScaleAnim.removeAllUpdateListeners()
            if (!isValidIndex(moveIndex)) {
                moveIndex = view.index
            }
            // 拖移到片尾位置，回退一个处理
            if (itemTrackList[moveIndex].isFooterType) {
                moveIndex--
            }
            itemTrackList.forEach {
                it.endDrag()
            }
            isDragging = false
            if (isValidIndex(view.index)) {
                multiTrackListener?.onDragComplete(slotList[view.index],view.index, moveIndex)
            }
            relativeLayout.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
    }

    var selectIndex = -1


    private val itemTrackCallback = object : ItemTrackLayout.ItemTrackCallback {
        override fun stopClip(index: Int) {
            itemClipCallback?.stopClip()
        }

        override fun startClip(index: Int) {
            if (isValidIndex(index)) {
                itemClipCallback?.startClip(slotList[index], index)
            }
        }

        override fun onClip(side: Int, start: Int, duration: Int, index: Int, moveX: Float) {
            if (isValidIndex(index) && slotList.isNotEmpty()) {
                val segment = slotList[index]
                // 计算当前点对视频时间线起点的偏差值
                if (side == ItemTrackLayout.LEFT) {
                    itemClipCallback?.clipStateChanged(
                        segment,
                        side,
                        start,
                        duration,
                        index,
                        -moveX
                    )
                } else if (side == ItemTrackLayout.RIGHT) {
                    itemClipCallback?.clipStateChanged(segment, side, start, duration, index, moveX)
                }
                itemTrackList.filter { it.index != index }.forEach {
                    if (side == ItemTrackLayout.LEFT && it.index < index) {
                        it.updateClipWidth(-moveX)
                    } else if (side == ItemTrackLayout.RIGHT && it.index > index) {
                        it.updateClipWidth(moveX)
                    }
                }
            }
        }

        override fun onTransitionClick(index: Int) {
            if (lastTransitionIndex == index) {
                return
            }
            if (isValidIndex(lastTransitionIndex)) {
                itemTrackList[lastTransitionIndex].ivTransition.isSelected = false
            }
            lastTransitionIndex = index
            multiTrackListener?.onTransitionClick(slotList[index - 1], slotList[index])
        }

        override fun onUpMoveChange(index: Int, side: Int, move: Float, location: Int) {
            //todo check 目前是全量刷新，所以不需要更新margin 后面看看要不要
//            when (side) {
//                ItemTrackLayout.LEFT -> {
//                    val layoutParams = itemTrackList[index].layoutParams as MarginLayoutParams
//                    layoutParams.leftMargin += move.toInt()
//                    itemTrackList[index].layoutParams = layoutParams
//                    for (i in 0 until index) {
//                        itemTrackList[i].translationX += move.toInt()
//                    }
//                }
//            }
        }


        override fun onItemClick(index: Int) {
            selectSlot(index)
        }

        override fun onEditChange(index: Int, dis: Int, side: Int, move: Float) {
            when (side) {
                ItemTrackLayout.LEFT -> {
                    for (i in 0 until index) {
                        itemTrackList[i].translationX += dis
                        ILog.i(TAG, "this is index is $i move is $move")
                    }
                }
                ItemTrackLayout.RIGHT -> {
                }
            }
        }

        override fun onStartAndDuration(index: Int, start: Int, duration: Int, side: Int) {
            val videoSeg = NLESegmentVideo.dynamicCast(slotList[index].mainSegment)
            val startMs = (abs(start) / TrackConfig.PX_MS * videoSeg.avgSpeed()).toInt()
            var durationMs = (duration / TrackConfig.PX_MS * videoSeg.avgSpeed()).roundToInt()
            if (startMs + durationMs > slotList[index].mainSegment.resource.duration / 1000) {
                durationMs = slotList[index].mainSegment.resource.duration.toInt() / 1000 - startMs
            }
            multiTrackListener?.onStartAndDuration(
                slotList[index],
                startMs,
                durationMs,
                side
            )
            ILog.d(TAG, "start is $startMs duration is $durationMs")
        }

        override fun getAdsorbOffset(
            index: Int,
            duration: Long,
            clipOrientation: HorizontallyState
        ): Float {
            val timestamp = slotList[index].startTime / 1000 + duration
            val beats = reactCallback?.getMusicBeats() ?: emptyList()
            var adsorbBeat: Long? = null
            val adsorbWidth = (SizeUtil.dp2px(15F) / TrackConfig.PX_MS)
            if (clipOrientation == HorizontallyState.LEFT) {
                for (i in beats.size - 1 downTo 0) {
                    if (timestamp - adsorbWidth <= beats[i] && timestamp >= beats[i]) {
                        adsorbBeat = beats[i]
                        break
                    }
                }
            } else {
                for (i in beats.indices) {
                    if (timestamp + adsorbWidth >= beats[i] && timestamp <= beats[i]) {
                        adsorbBeat = beats[i]
                        break
                    }
                }
            }
            return adsorbBeat?.let { (it - timestamp) * TrackConfig.PX_MS } ?: 0F
        }

        override fun isAbleToSetTransition(index: Int): Boolean {
            if (index == 0 || slotList.isEmpty()) {
                return false
            }
            // 当前段已经有转场，则都允许打开面板
            var hasTransition = false
            if (index - 1 >= 0) {
                val segment = slotList[index - 1]
                hasTransition =
                    segment.endTransition != null && !TextUtils.isEmpty(segment.endTransition!!.effectSDKTransition.resourceFile)
            }
            if (hasTransition) {
                return true
            }
            var hasPreTransition = false
            var preTransitionDuration = 0
            if (index - 2 >= 0) {
                val preSegment = slotList[index - 2]
                hasPreTransition =
                    preSegment.endTransition != null && !TextUtils.isEmpty(preSegment.endTransition!!.effectSDKTransition.resourceFile)
                if (hasPreTransition) {
                    preTransitionDuration =
                        (preSegment.endTransition?.transitionDuration?.toInt() ?: 0) / 1000
                }
            }
            val preMinDuration =
                if (hasPreTransition) MIN_VIDEO_DURATION_TO_SET_TRANSITION + preTransitionDuration else MIN_VIDEO_DURATION_TO_SET_TRANSITION
            return slotList[index].duration / 1000 >= MIN_VIDEO_DURATION_TO_SET_TRANSITION &&
                    slotList[index - 1].duration / 1000 >= preMinDuration
        }

        override fun onUnableToSetTransition(index: Int) {
        }

        override fun getFrameBitmap(path: String, timestamp: Int): Bitmap? =
            reactCallback?.getFrameBitmap(path, timestamp)
    }


    fun selectSlot(
        index: Int,
        forceSelect: Boolean = false,
        onlyRefreshUI: Boolean = false
    ) {
        if (!forceSelect && ((index == selectIndex && isSelectAgainToRoot) || index == -1)) {
            // 再次点击选择的状态的则当成未选择
            if (isValidIndex(selectIndex)) {
                itemTrackList[selectIndex].setNormalType()
            }
            selectIndex = -1
            setSelectedSegment(null)
            if (!onlyRefreshUI) {
                multiTrackListener?.onSegmentClick(null)
            }
            setTrackStyle(TrackStyle.NONE)
            return
        }

        if (!forceSelect && (index == selectIndex && !isSelectAgainToRoot)) {
            if (isDragging || !isValidIndex(index)) return
            val segment = slotList[index]
            setSelectedSegment(segment)
            // 在执行选中前把所有item全部置为normal
            itemTrackList.forEach {
                kotlin.run {
                    it.setNormalType()
                }
            }
            itemTrackList[index].setClipType()
            // 有可能在比例面板未选中 segment 时修改了Segment
            if (!onlyRefreshUI) {
                multiTrackListener?.onSegmentClick(segment)
            }
            selectIndex = index
            setTrackStyle(TrackStyle.CLIP)
            return
        }

        if (isDragging || !isValidIndex(index)) return
        val segment = slotList[index]
        setSelectedSegment(segment)
        // 在执行选中前把所有item全部置为normal
        itemTrackList.forEach {
            kotlin.run {
                it.setNormalType()
            }
        }
        if (selectedTrackStyle == TrackStyle.LINE) itemTrackList[index].setLineType()
        else itemTrackList[index].setClipType()
        // 有可能在比例面板未选中 segment 时修改了Segment
        if (!onlyRefreshUI) {
            multiTrackListener?.onSegmentClick(segment)
        }
        selectIndex = index
        setTrackStyle(selectedTrackStyle)
    }

    fun setMultiTrackListener(listener: MultiTrackListener) {
        multiTrackListener = listener
    }

    fun setTransitionIcon(segments: List<NLETrackSlot>) {
        itemTrackList.forEachIndexed { index, itemTrack ->
            if (index != 0) {
                val resId = getIconId(
                    segments[index - 1].endTransition?.effectSDKTransition?.resourceFile ?: ""
                )
                itemTrack.setTransitionIcon(resId)
            }
        }
    }

    private fun getIconId(name: String): Int {
        var resId = R.drawable.transition_ic_none
        if (name.isNotEmpty()) {
            resId = R.drawable.transition_ic_using
        }
        return resId
    }

    private fun setLineType(currentIndex: Int) {
        if (!isValidIndex(currentIndex)) {
            return
        }
        itemTrackList.forEach {
            it.isShowDivider(false)
        }
        itemTrackList[currentIndex].setLineType()

        if (currentIndex > 0) {
            itemTrackList[currentIndex - 1].setDrawMyTransitionOverlap(false)
        }
        if (currentIndex < itemTrackList.size - 1) {
            itemTrackList[currentIndex + 1].setDrawPreTransitionOverlap(false)
        }
    }

    private fun setClipType(currentIndex: Int) {
        if (!isValidIndex(currentIndex)) {
            return
        }
        itemTrackList.forEach {
            it.isShowDivider(true)
        }
        itemTrackList[currentIndex].setClipType()
        if (isValidIndex(currentIndex + 1)) {
            itemTrackList[currentIndex + 1].isShowDivider(false)
        }

        if (currentIndex > 0) {
            itemTrackList[currentIndex - 1].setDrawMyTransitionOverlap(false)
        }
        if (currentIndex < itemTrackList.size - 1) {
            itemTrackList[currentIndex + 1].setDrawPreTransitionOverlap(false)
        }

        itemTrackList[currentIndex].isShowDivider(false)
    }

    private fun setNormalType() {
        val index = getSelectedIndex()
        if (!isValidIndex(index)) {
            return
        }

        itemTrackList[index].setNormalType()
        if (index > 0) {
            itemTrackList[index - 1].setDrawMyTransitionOverlap(true)
        }
        if (index < itemTrackList.size - 1) {
            itemTrackList[index + 1].setDrawPreTransitionOverlap(true)
        }

        itemTrackList.forEach {
            it.isShowDivider(true)
        }
    }

    fun isNormalType(): Boolean {
        return !isValidIndex(getSelectedIndex())
    }

    private fun isLineType(): Boolean = trackStyle == TrackStyle.LINE

    fun isValidIndex(index: Int): Boolean =
        slotList.isNotEmpty() && itemTrackList.isNotEmpty() && index in itemTrackList.indices

    fun setTransitionUnselected() {
        if (isValidIndex(lastTransitionIndex)) {
            itemTrackList[lastTransitionIndex].ivTransition.isSelected = false
        }
        lastTransitionIndex = -1
    }

    fun setBeautyChange(currentIndex: Int, beauty: Float, shape: Float) {
        if (labelType == LabelType.BEAUTY && isValidIndex(currentIndex)) {
            itemTrackList[currentIndex].setBeautyIcon(beauty, shape)
        }
    }

    fun setFilterChange(currentIndex: Int, filterName: String?) {
        if (labelType == LabelType.FILTER && isValidIndex(currentIndex)) {
            if (filterName == context.getString(R.string.original_picture)) {
                itemTrackList[currentIndex].setFilterIcon("")
            } else {
                itemTrackList[currentIndex].setFilterIcon(filterName)
            }
            ILog.i(TAG, "filter change is filter $filterName")
        }
    }

    fun setIsMute(currentIndex: Int, isMute: Boolean) {
        if (isValidIndex(currentIndex)) {
            itemTrackList[currentIndex].setMuteIcon(isMute)
        }
    }

//    fun setPictureAdjustChange(currentIndex: Int, adjustInfo: PictureAdjustInfo) {
//        if (labelType == LabelType.ADJUST && isValidIndex(currentIndex)) {
//            itemTrackList[currentIndex].setPictureAdjustIcon(adjustInfo)
//        }
//    }

    fun setVideoAnimChange(currentIndex: Int, videoAnimInfo: NLEVideoAnimation?) {
        if (/*labelType == LabelType.ANIM &&*/ isValidIndex(currentIndex)) {
            itemTrackList[currentIndex].setVideoAnimMask(videoAnimInfo)
        }
    }


    @MainThread
    fun updateVideoAnimDuration(duration: Long) {
        if (BuildConfig.DEBUG && Looper.myLooper() != Looper.getMainLooper()) {
            error("Only work in main thread")
        }

        if (labelType != LabelType.ANIM) return

        val index = getSelectedIndex()
        if (isValidIndex(index)) {
            itemTrackList[index].updateAnimMaskWidth(duration)
        }
    }

    private fun setEpilogueEnable(enable: Boolean) {
        // 片尾前一段视频在片尾禁用时不画交叠转场斜切
        if (isValidIndex(itemTrackList.size - 2)) {
            itemTrackList[itemTrackList.size - 2].setDrawMyTransitionOverlap(enable)
            if (enable) {
                itemTrackList.firstOrNull { it.isFooterType }?.let {
                    it.setEpilogueEnable(enable)
                    if (!isLineType()) {
                        it.isShowDivider(enable)
                    }
                }
            }
        }
    }

    fun setScaleSize(scale: Double) {
        this.scale = scale
        var deviation = 0F
        itemTrackList.forEachIndexed { index, itemTrackLayout ->
            itemTrackLayout.setScaleSize()
            if (index != 0) {
                val mlp = itemTrackLayout.layoutParams as MarginLayoutParams
                var transitionMargin = 0
                if (slotList[index - 1].endTransition?.overlap == true) {
                    // 计算累计舍弃掉的小数，每次取整时，带上之前累计舍弃掉的小数，抹平误差
                    val transMarginFloat = calTransitionMargin(index)
                    transitionMargin = (transMarginFloat + deviation).toInt()
                    deviation += (transMarginFloat - transitionMargin)
                }
                mlp.leftMargin =
                    -2 * TrackConfig.BORDER_WIDTH - transitionMargin-SizeUtil.dp2px(1f)
            }
        }
    }

    private fun calTransitionMargin(index: Int): Float {
        if (index < 1) {
            return 0F
        }
        val transitionDuration =
            getPreSegmentOverlapTransitionDuration(index)
        return transitionDuration * TrackConfig.PX_MS
    }

    private fun getPreSegmentOverlapTransitionDuration(index: Int): Long {
        val preSegment = slotList[index - 1]
        val transitionPath = preSegment.endTransition?.effectSDKTransition?.resourceFile ?: ""
        return run {
            if (transitionPath.isNotBlank()) {
                if (preSegment.endTransition?.overlap == true) {
                    (preSegment.endTransition?.transitionDuration ?: 0) / 1000
                } else {
                    0
                }
            } else {
                0
            }
        }
    }

    fun setIsCoverMode(isCoverMode: Boolean) {
        this.isCoverMode = isCoverMode
    }

    fun startScale() {
        itemTrackList.forEach {
            it.startScale()
        }
    }

    fun endScale() {
        itemTrackList.forEach {
            it.endScale()
        }
    }

    fun setTrackStyle(style: TrackStyle) {
        trackStyle = style
        val index = getSelectedIndex()
        refreshTrackStyle(style, index)
    }

    private fun refreshTrackStyle(style: TrackStyle, selectedIndex: Int) {
        setNormalType()
        if (isValidIndex(selectedIndex)) {
            if (style == TrackStyle.CLIP) {
                setClipType(selectedIndex)
            } else if (style == TrackStyle.LINE) {
                setLineType(selectedIndex)
            }
        }
    }

    fun setSelectedSegment(slot: NLETrackSlot?) {
        val index = slot?.let { id ->
            slotList.indexOfFirst { it == id }
        } ?: -1
        refreshTrackStyle(trackStyle, index)
        refreshLabel(labelType)

        selectedSegmentId = slot
    }

    fun setLabelType(type: LabelType) {
        labelType = type
        refreshLabel(type)
    }

    private fun refreshLabel(type: LabelType) {
        itemTrackList.forEachIndexed { index, itemTrackLayout ->
            itemTrackLayout.hideIcon()
            // 更新轨道上的小标签，后面按需再加
            itemTrackLayout.setSpeed(getSpeedInfo(slotList[index]))
            // 显示轨道上的静音图标
            itemTrackLayout.setMuteIcon(isMute(slotList[index]))
//            if (type == LabelType.BEAUTY) {
//                itemTrackList[index].setBeautyIcon(
//                    segmentList[index].beautyInfo?.strength ?: 0F,
//                    segmentList[index].reshapeInfo?.cheekStrength ?: 0F
//                )
//            } else if (type == LabelType.FILTER) {
//                if (segmentList[index].filterInfo?.filterName == getString(R.string.original_picture)) {
//                    itemTrackList[index].setFilterIcon("")
//                } else {
//                    itemTrackList[index].setFilterIcon(
//                        segmentList[index].filterInfo?.filterName ?: ""
//                    )
//                }
//            } else if (type == LabelType.ADJUST) {
//                segmentList[index].pictureAdjustInfo?.let {
//                    itemTrackList[index].setPictureAdjustIcon(it)
//                }
//            } else if (type == LabelType.ANIM) {
//                segmentList[index].videoAnimInfo.let {
//                    itemTrackList[index].setVideoAnimMask(it)
//                }
//            }
        }
    }

    fun updateScrollX(scrollX: Int) {
        myScrollX = scrollX
        itemTrackList.forEach {
            it.updateScrollX(scrollX)
        }
        onPlayPositionChanged()
    }

    fun setCallback(callback: ItemClipCallback?, reactCallback: ReactCallback?) {
        itemClipCallback = callback
        this.reactCallback = reactCallback
    }

    fun refreshFrames(index: Int) = runOnUiThread {
        if (index >= 0 && itemTrackList.size > index) {
            itemTrackList[index].refreshFrames()
        }
    }

    fun refreshAllSlotFrames() = runOnUiThread {
        for (i in 0..itemTrackList.size - 1) {
            itemTrackList[i].refreshFrames()
        }
    }

    private fun isMute(segment: NLETrackSlot): Boolean {
        val enableAudio = segment.getMainSegment<NLESegmentVideo>()?.enableAudio ?: true
        return !enableAudio
    }

    private fun getSpeedInfo(segment: NLETrackSlot): SpeedInfo {
        val nLESegmentVideo = NLESegmentVideo.dynamicCast(segment.mainSegment)
        return SpeedInfo().apply {
            mode = if (nLESegmentVideo.segCurveSpeedPoints.isEmpty()) SpeedInfo.NORMAL_SPEED else SpeedInfo.CURVE_SPEED
            normalSpeed = nLESegmentVideo.absSpeed
            aveSpeed = nLESegmentVideo.curveAveSpeed.toFloat()
            name = nLESegmentVideo.getExtra(CURVE_SPEED_NAME)
        }
    }

    companion object {
        const val TAG = "MultiTrackLayout"
        const val MOVIE_FOOT_DURATION = 2000L
        const val MIN_VIDEO_DURATION_TO_SET_TRANSITION = 200L

    }

    // clip 选择，line 线框，none 无
    enum class TrackStyle {
        NONE, CLIP, LINE
    }

    enum class LabelType {
        NONE, BEAUTY, FILTER, ADJUST, ANIM
    }
}

interface ItemClipCallback {
    fun startClip(segment: NLETrackSlot, index: Int)
    fun stopClip()
    fun clipStateChanged(
        segment: NLETrackSlot,
        side: Int,
        start: Int,
        duration: Int,
        index: Int,
        offset: Float
    )
}
