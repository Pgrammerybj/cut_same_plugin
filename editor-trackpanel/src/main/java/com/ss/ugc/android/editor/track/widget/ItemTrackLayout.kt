package com.ss.ugc.android.editor.track.widget

import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.R
import com.ss.ugc.android.editor.track.tip.ItemTrackTipsManager
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.gone
import com.ss.ugc.android.editor.track.utils.hide
import com.ss.ugc.android.editor.track.utils.show


import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.bytedance.ies.nle.editor_jni.NLEFilter
import com.bytedance.ies.nle.editor_jni.NLEResType
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLEVideoAnimation
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.core.avgSpeed
import com.ss.ugc.android.editor.track.data.SpeedInfo
import com.ss.ugc.android.editor.track.keyframe.FrameView
import com.ss.ugc.android.editor.track.keyframe.KeyframeSelectChangeListener
import com.ss.ugc.android.editor.track.utils.FormatUtil
import com.ss.ugc.android.editor.track.utils.safelyPerformHapticFeedback
import kotlinx.android.synthetic.main.layout_epilogue.view.iconAddEpilogue
import kotlinx.android.synthetic.main.layout_epilogue.view.iconEpilogue
import kotlinx.android.synthetic.main.layout_epilogue.view.rlEpilogue
import kotlinx.android.synthetic.main.layout_video_track.view.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt


class ItemTrackLayout : RelativeLayout {

    constructor(context: Context?) : this(context, null)

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr)

    companion object {
        const val LEFT = 0
        const val RIGHT = 1
        const val TAG = "ItemTrackLayout"
        val MIN_ABSORPTION_INTERVAL = SizeUtil.dp2px(10F)
        const val MIN_VIDEO_DURATION_IN_MS = 33
        const val MOVIE_FOOT_DURATION = 2000L
        const val MAX_SUB_VIDEO_TRACK_NUM = 6


    }
    var enableDragMode = true
    var isCoverMode = false

    var isDockerTopLevel: (() -> Boolean)? = null
    var style = MultiTrackLayout.TrackStyle.NONE
        private set
    private var startPx = 0
    private var durationPx = 0f
    private var startTime = 0L
    var duration = 0L
    private var sourceDuration = 0L
    private var speed = 1.0F
    private var movePx = 0f
    private var segmentInfo: NLETrackSlot? = null
    var index = 0
    private var itemTrackCallback: ItemTrackCallback? = null

    private var onDragListener: OnTrackDragListener? = null

    private var minPx = 0f
    private var maxPx = 0f
    private var initLeft = 0f
    private var initRight = 0f
    private var dealDx = 0f
    private var startClipLeftPosition = 0
    private var startClipRightPosition = 0

    private var autoScrollSpeedRate = 1.0f

    private var maxLength = 0f

    // 避免误差
    private var currentLength = 0f
    private var leftPosition = 0
    private var rightPosition = 0

    var isFooterType: Boolean = false
    private var epilogueAttached = false


    private var minVideoDurationInMs =
        MIN_VIDEO_DURATION_IN_MS

    private val screenWidth = SizeUtil.getScreenWidth(context)

    private val autoScrollAnim = ValueAnimator.ofFloat(0F, 1F)

    private var lpFramesLayout: LayoutParams
    private var lpTopLine: LayoutParams
    private var lpBottomLine: LayoutParams
    private var lpTvEpilogue: MarginLayoutParams? = null
    private var tipsManager: ItemTrackTipsManager
    private var frameView:FrameView

    private var preSegmentTransitionDuration = 0L

//*
//     * 滚动方向，LEFT表示往左边滚动，RIGHT表示往右边滚动


    private var scrollState = HorizontallyState.NULL
        set(value) {
            if (field == value) return

            field = value
            when (value) {
                HorizontallyState.NULL -> {
                    // 停止滚动后，重新计算开始吸附位置
                    startClipLeftPosition = leftPosition
                    startClipRightPosition = rightPosition
                    dealDx = 0F
                    autoScrollAnim.cancel()
                }
                else -> autoScrollAnim.start()
            }
        }

//*
//     * 代表hold住的是哪边的裁剪按钮，LEFT表示左边，RIGHT表示右边


    private var clipState = HorizontallyState.NULL
        set(value) {
            if (field != value) field = value
        }
    private var outsideScrollHandler: ScrollHandler? = null

    private var adsorbOffset = 0
    private var adsorbClipOrientation = HorizontallyState.NULL

    // 避免多次显示Tips
    private var shownTips = false

    init {
        val main = LayoutInflater.from(context).inflate(R.layout.layout_video_track, this)
        val container: ViewGroup = main.findViewById(R.id.container)
        tipsManager = ItemTrackTipsManager(container, framesLayout)
        lpFramesLayout = framesLayout.layoutParams as LayoutParams
        lpTopLine = ivTopLine.layoutParams as LayoutParams
        lpBottomLine = ivBottomLine.layoutParams as LayoutParams
        frameView = main.findViewById(R.id.frameView)


        if(ThemeStore.getCustomMainTrackHeight()!=null){
            ThemeStore.setCustomMainTrackHeight(container)
            ThemeStore.setCustomMainTrackHeight(framesLayout)
            ThemeStore.setCustomMainTrackHeight(leftMove)
            ThemeStore.setCustomMainTrackHeight(rightMove)
            ThemeStore.setCustomMainTrackHeight(itemFrameView)
        }
        ThemeStore.setMainTrackLeftMoveIconRes(leftMove)
        ThemeStore.setMainTrackRightMoveIconRes(rightMove)

        autoScrollAnim.repeatCount = ValueAnimator.INFINITE
        autoScrollAnim.addUpdateListener {
            if (clipState == HorizontallyState.NULL) return@addUpdateListener

            val scrollDistance: Int
            val touchX: Float
            when (scrollState) {
                HorizontallyState.NULL -> return@addUpdateListener
                HorizontallyState.LEFT -> {
                    scrollDistance = (-TrackConfig.AUTO_SCROLL_SIZE * autoScrollSpeedRate).toInt()
                    touchX = 0F
                }
                HorizontallyState.RIGHT -> {
                    scrollDistance = (TrackConfig.AUTO_SCROLL_SIZE * autoScrollSpeedRate).toInt()
                    touchX = screenWidth.toFloat()
                }
            }

            clip(clipState, scrollDistance.toFloat(), touchX)
            outsideScrollHandler?.scrollBy(scrollDistance, 0, false, disablePruneX = true)
        }
    }

    private fun initBase() {
        style = MultiTrackLayout.TrackStyle.NONE
        tipsManager.hideFeatureTips()
        tipsManager.hideMuteTips()
        leftMove.hide()
        rightMove.hide()
        ivTopLine.hide()
        ivBottomLine.hide()
        ivLeftLine.hide()
        ivRightLine.hide()
        if (index == 0) {
            rlTransition.gone()
        }
        initListener()
    }

    private fun initListener() {
        if (isFooterType) {
            leftMove.isTouchAble = false
            rightMove.isTouchAble = false
        }
        leftMove.onMoveListener = { dis, rawX ->
            autoScrollSpeedRate = TrackConfig.calAutoScrollSpeedRate(rawX, screenWidth)
            setLeftAndRightPosition()
            onMove(HorizontallyState.LEFT, dis, rawX)
//            ILog.d(TAG, "left onMove $autoScrollSpeedRate   $dis    $rawX")

        }
        rightMove.onMoveListener = { dis, rawX ->
            autoScrollSpeedRate = TrackConfig.calAutoScrollSpeedRate(rawX, screenWidth)
            setLeftAndRightPosition()
            onMove(HorizontallyState.RIGHT, dis, rawX)
            ILog.d(TAG, "right onMove $autoScrollSpeedRate   $dis    $rawX")
        }
        leftMove.onMoveDownListener = {
            requestDisallowInterceptTouchEvent(true)
            setLeftAndRightPosition()
            startClipLeftPosition = leftPosition
            dealDx = 0f
            itemTrackCallback?.startClip(index)
            ILog.d(TAG, "left onMove down  $startClipLeftPosition   $dealDx    $index")
        }

        rightMove.onMoveDownListener = {
            requestDisallowInterceptTouchEvent(true)
            setLeftAndRightPosition()
            startClipRightPosition = rightPosition
            dealDx = 0f
            itemTrackCallback?.startClip(index)
            ILog.d(TAG, "right onMove down  $startClipLeftPosition   $dealDx    $index")

        }
        leftMove.onMoveUpListener = {
            requestDisallowInterceptTouchEvent(false)
            itemTrackCallback?.stopClip(index)
            itemTrackCallback?.onStartAndDuration(
                index, startPx, Math.round(durationPx),
                LEFT
            )
            itemTrackCallback?.onUpMoveChange(
                index,
                LEFT, movePx, leftPosition
            )
            scrollState = HorizontallyState.NULL
            clipState = HorizontallyState.NULL
            movePx = 0f
        }
        rightMove.onMoveUpListener = {
            requestDisallowInterceptTouchEvent(false)
            itemTrackCallback?.stopClip(index)
            setLeftAndRightPosition()
            itemTrackCallback?.onStartAndDuration(
                index, startPx, Math.round(durationPx),
                RIGHT
            )
            itemTrackCallback?.onUpMoveChange(
                index,
                RIGHT, movePx, rightPosition
            )
            scrollState = HorizontallyState.NULL
            clipState = HorizontallyState.NULL
            movePx = 0f
        }
        rlTransition.setOnClickListener {
//            if (ivTransition.isSelected){
//                ivTransition.isSelected = false
//                return@setOnClickListener
//            }
            val isUnableToSetTransition =
                itemTrackCallback?.isAbleToSetTransition(index)?.not() ?: false
            if (isUnableToSetTransition) {
//                showToast(R.string.segment_too_short_to_add_transition, Toast.LENGTH_SHORT)
                itemTrackCallback?.onUnableToSetTransition(index)
                return@setOnClickListener
            }
            ivTransition.isSelected = true
            itemTrackCallback?.onTransitionClick(index)
        }
        framesLayout.setOnTouchListener(
            OnTrackTouchListener(
                clickListener = {
                    if (!isCoverMode) {
                        itemTrackCallback?.onItemClick(index)
                    }
                },
                dragListener = object :
                    OnTrackDragListener {
                    override fun beginDrag(downX: Float, downY: Float) {
                        if (!enableDragMode || isCoverMode) {
                            return
                        }
                        if (isFooterType) return
                        safelyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        requestDisallowInterceptTouchEvent(true)
                        onDragListener?.beginDrag(downX, downY)
                    }

                    override fun drag(
                        deltaX: Float,
                        deltaY: Float,
                        isLeftScreenBorder: Boolean,
                        isRightScreenBorder: Boolean
                    ) {
                        if (!enableDragMode || isCoverMode) {
                            return
                        }
                        if (isFooterType) return
                        onDragListener?.drag(
                            deltaX,
                            deltaY,
                            isLeftScreenBorder,
                            isRightScreenBorder
                        )
                    }

                    override fun endDrag() {
                        if (!enableDragMode || isCoverMode) {
                            return
                        }
                        if (isFooterType) return
                        requestDisallowInterceptTouchEvent(false)
                        onDragListener?.endDrag()
                    }
                })
        )
    }

    fun refreshTransitionState(select : Boolean){
        if (ivTransition.isSelected && !select){
            ivTransition.isSelected = false
        }

        if (select && !ivTransition.isSelected){
            ivTransition.isSelected = true
            itemTrackCallback?.onTransitionClick(index)
        }

    }


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // 点击交叠前半段，事件交给前半段处理，否则交给后半段处理
        if (preSegmentTransitionDuration != 0L && style != MultiTrackLayout.TrackStyle.CLIP) {
            if (ev?.x ?: -1F >= 0 && (ev?.x
                    ?: 0F) <= preSegmentTransitionDuration * TrackConfig.PX_MS / 2
            ) {
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun onMove(clipState: HorizontallyState, dis: Float, rawX: Float) {
        this.clipState = clipState
        clip(clipState, dis, rawX)
    }

    private fun clip(clipState: HorizontallyState, dis: Float, rawX: Float) {
        if (clipState == HorizontallyState.LEFT) {
            clipLeft(dis, rawX)
        } else if (clipState == HorizontallyState.RIGHT) {
            clipRight(dis, rawX)
        }
    }

    private fun clipLeft(dis: Float, rawX: Float) {
        if (rawX < leftPosition - TrackConfig.BORDER_WIDTH && dis >= 0) { // 手指在左裁剪键的左边，但是是向右拖动，则认为此拖无效
            return
        }

        if (rawX > leftPosition && dis <= 0) { // 手指在左裁剪键的右边，但是是向左拖动，则认为此拖无效
            return
        }
        val realDis = dis.toInt()
        if (realDis == 0) { // 当dis为 0.X时，realDis将为0，为无效滚动
            return
        }

        var desireDis = realDis
        if (scrollState == HorizontallyState.NULL) {
            // 计算吸附
            dealDx += realDis
            (TrackConfig.PLAY_HEAD_POSITION / 2 - (startClipLeftPosition + dealDx)).apply {
                if (abs(this) < MIN_ABSORPTION_INTERVAL) {
                    desireDis = (TrackConfig.PLAY_HEAD_POSITION / 2 - leftPosition)
                    if (desireDis != 0) {
                        safelyPerformHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                    }
                }
            }
        }
        if (minPx + desireDis < 0 || maxLength - minPx - maxPx - desireDis < minVideoDurationInMs * TrackConfig.PX_MS) {
            scrollState = HorizontallyState.NULL
            return
        }
        minPx += desireDis
        leftEdit(desireDis.toFloat())
        itemTrackCallback?.onEditChange(index, desireDis, LEFT, movePx)
        setScrollState(rawX)
        frameView.onClipLeft(desireDis)
    }

    private fun clipRight(dis: Float, rawX: Float) {
        if (rawX < rightPosition && dis >= 0) {
            return
        }

        if (rawX > rightPosition + TrackConfig.BORDER_WIDTH && dis <= 0) {
            return
        }

        if (dis.toInt() == 0) { // 滑动太短 视为无效滑动
            return
        }

        var adsorbDis = trimAdsorbRightDis(dis)
        if (adsorbDis == 0) { // 到右极限 停止自滑
            scrollState = HorizontallyState.NULL
            return
        }

        val clipOrientation =
            if (adsorbDis > 0) HorizontallyState.RIGHT else HorizontallyState.LEFT
        if (clipOrientation != adsorbClipOrientation) {
            if (abs(adsorbOffset) > abs(adsorbDis)) {
                adsorbOffset += adsorbDis
                return
            } else {
                adsorbClipOrientation = clipOrientation
                adsorbDis += adsorbOffset
                adsorbOffset = 0
            }
        }
        if (adsorbOffset == 0) {
            val offset = if (scrollState == HorizontallyState.NULL) {
                val duration =
                    ((lpFramesLayout.width + adsorbDis) / TrackConfig.PX_MS * speed).toLong()
                itemTrackCallback?.getAdsorbOffset(index, duration, adsorbClipOrientation)?.toInt()
                    ?: 0
            } else 0
            if (offset != 0) {
                safelyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            adsorbDis += offset
            clipRightWithAdsorb(adsorbDis)
            adsorbOffset = offset
        } else {
            if (abs(adsorbOffset) > abs(adsorbDis)) {
                adsorbOffset -= adsorbDis
            } else {
                adsorbDis -= adsorbOffset
                clipRightWithAdsorb(adsorbDis)
                adsorbOffset = 0
            }
        }
        setScrollState(rawX)
        frameView.onClipEnd()
    }

    private fun clipRightWithAdsorb(adsorbDis: Int) {
        var desireDis = adsorbDis

        if (scrollState == HorizontallyState.NULL) {
            // 计算吸附
            dealDx += adsorbDis
            (TrackConfig.PLAY_HEAD_POSITION / 2 - (startClipRightPosition + dealDx)).apply {
                if (abs(this) < MIN_ABSORPTION_INTERVAL) {
                    desireDis = (TrackConfig.PLAY_HEAD_POSITION / 2 - rightPosition)
                    if (desireDis != 0) {
                        safelyPerformHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                        )
                    }
                }
            }
        }

        maxPx -= desireDis
        rightEdit(desireDis.toFloat())
        itemTrackCallback?.onEditChange(index, desireDis, RIGHT, movePx)
    }

    private fun trimAdsorbRightDis(dis: Float): Int {
        val minLen = minVideoDurationInMs * TrackConfig.PX_MS
        var adsorbDis = dis
        if (maxPx - adsorbDis < 0) {
            adsorbDis = maxPx
        }
        if (maxLength - minPx - maxPx + adsorbDis < minLen) {
            adsorbDis = minLen - maxLength + minPx + maxPx
        }
        return adsorbDis.toInt()
    }

    private fun setScrollState(rawX: Float) {
        scrollState = when {
            rawX >= screenWidth - TrackConfig.AUTO_SCROLL_START_POSITION -> HorizontallyState.RIGHT
            rawX <= TrackConfig.AUTO_SCROLL_START_POSITION -> HorizontallyState.LEFT
            else -> HorizontallyState.NULL
        }
    }

    fun setNormalType() {
        style = MultiTrackLayout.TrackStyle.NONE
        leftMove.animate().alpha(0f).setDuration(100).start()
        rightMove.animate().alpha(0f).setDuration(100).start()
        ivTopLine.animate().alpha(0f).setDuration(100).start()
        ivBottomLine.animate().alpha(0f).setDuration(100).start()
        ivLeftLine.animate().alpha(0f).setDuration(100).start()
        ivRightLine.animate().alpha(0f).setDuration(100).start()
        leftMove.isTouchAble = false
        rightMove.isTouchAble = false
        setDurationIcon(duration)
        itemFrameView.setTrackStyle(style)
        frameView.update()
    }

    fun setClipType() {
        style = MultiTrackLayout.TrackStyle.CLIP
        if (!isFooterType) {
            leftMove.isTouchAble = true
            rightMove.isTouchAble = true
        }
        leftMove.show()
        rightMove.show()
        ivTopLine.show()
        ivBottomLine.show()
        leftMove.animate().alpha(1f).setDuration(100).start()
        rightMove.animate().alpha(1f).setDuration(100).start()
        ivTopLine.animate().alpha(1f).setDuration(100).start()
        ivBottomLine.animate().alpha(1f).setDuration(100).start()
        ivLeftLine.animate().alpha(0f).setDuration(100).start()
        ivRightLine.animate().alpha(0f).setDuration(100).start()
        parent.bringChildToFront(this)
        setDurationIcon(duration)
        itemFrameView.setTrackStyle(style)
        frameView.update()
    }

    fun setLineType() {
        style = MultiTrackLayout.TrackStyle.LINE
        ivTopLine.show()
        ivBottomLine.show()
        ivLeftLine.show()
        ivRightLine.show()
        ivLeftLine.animate().alpha(1f).setDuration(100).start()
        ivRightLine.animate().alpha(1f).setDuration(100).start()
        ivTopLine.animate().alpha(1f).setDuration(100).start()
        ivBottomLine.animate().alpha(1f).setDuration(100).start()
        leftMove.animate().alpha(0f).setDuration(100).start()
        rightMove.animate().alpha(0f).setDuration(100).start()
        if (segmentInfo?.endTransition?.overlap == true) {
            parent.bringChildToFront(this)
        }
        leftMove.isTouchAble = false
        rightMove.isTouchAble = false
        setDurationIcon(duration)
        itemFrameView.setTrackStyle(style)
        frameView.update()
    }

    fun goneActionIcon() {
        removeView(leftMove)
        removeView(rightMove)
        (framesLayout.layoutParams as MarginLayoutParams).leftMargin = 0
        ivTopLine.gone()
        ivBottomLine.gone()
        ivLeftLine.gone()
        ivRightLine.gone()
        rlTransition.gone()
        leftMove.gone()
        rightMove.gone()
        tipsManager.hideMuteTips()
        tipsManager.hideFeatureTips()
        tipsManager.hideDurationTips()
    }

    fun setTransitionIcon(resId: Int) {
        // todo 以下代码有逻辑问题，需要调整再打开
//        if(ThemeStore.trackUIConfig.trackTransitionCustomIcon!=0){
//            ThemeStore.setTransitionIconRes(ivTransition)
//            return
//        }
        ivTransition.setImageResource(resId)
    }

    fun hideIcon() {
        tipsManager.hideMuteTips()
        tipsManager.hideFeatureTips()
        itemFrameView.enableAnimationLabel(false)
    }

    fun setSpeed(speedInfo: SpeedInfo?) {
        var normalChangeSpeedIcon :Int = R.drawable.ic_speed_n
        var curveChangeSpeedIcon: Int = R.drawable.ic_speed_n
        ThemeStore.getCustomMainTrackNormalChangeSpeedIcon()?.apply { normalChangeSpeedIcon = this }
        ThemeStore.getCustomMainTrackCurveChangeSpeedIcon()?.apply { curveChangeSpeedIcon = this }
        speedInfo?.let {
            if (it.mode == SpeedInfo.NORMAL_SPEED) {
                if (abs(it.normalSpeed - 1.0F) <= 0.01) {
                    tipsManager.hideFeatureTips()
                } else {
                    val text = String.format(Locale.ENGLISH, "%.1fx", speed)
                    tipsManager.showFeatureTipsWithText(text, normalChangeSpeedIcon)
                }
            } else {
                tipsManager.showFeatureTipsWithText(it.name, curveChangeSpeedIcon)
            }
        }
    }

    fun setBeautyIcon(beauty: Float, shape: Float) {
        if (isFooterType) return
        if (beauty == 0F && shape == 0F) {
            tipsManager.hideFeatureTips()
        } else {
            tipsManager.showFeatureTipsWithText(
                context.getString(R.string.beautify),
                R.drawable.ic_beauty_n
            )
            if (lpTopLine.width > tipsManager.getTotalWidth()) {
                tipsManager.lpLayout.width = ViewGroup.LayoutParams.WRAP_CONTENT
                tipsManager.llTipsLayout.requestLayout()
            }
        }
    }

    fun setFilterIcon(filterName: String?) {
        if (isFooterType) return
        if (filterName.isNullOrBlank()) {
            tipsManager.hideFeatureTips()
        } else {
            if (lpTopLine.width > tipsManager.getTotalWidth()) {
                tipsManager.lpLayout.width = ViewGroup.LayoutParams.WRAP_CONTENT
                tipsManager.llTipsLayout.requestLayout()
            }

            if (isDockerTopLevel?.invoke() != true) {
                tipsManager.showFeatureTipsWithText(filterName, R.drawable.ic_fliter_n)
            }
        }
    }

    fun setMuteIcon(isMute: Boolean) {
        if (isFooterType || segmentInfo?.mainSegment?.type == NLEResType.IMAGE) return
        if (isMute && !isCoverMode) {
            tipsManager.showMuteTips()
        } else {
            tipsManager.hideMuteTips()
        }
    }

   /* fun setPictureAdjustIcon(adjustInfo: PictureAdjustInfo) {
        if (isFooterType) return
        if (!adjustInfo.hasAdjustStrength()) {
            tipsManager.hideFeatureTips()
        } else {
            tipsManager.showFeatureTipsWithText(
                getString(R.string.adjust),
                R.drawable.ic_adjust_n
            )
            if (lpTopLine.width > tipsManager.getTotalWidth()) {
                tipsManager.lpLayout.width = ViewGroup.LayoutParams.WRAP_CONTENT
                tipsManager.llTipsLayout.requestLayout()
            }
        }
    }*/

    fun setVideoAnimMask(videoAnimInfo: NLEVideoAnimation?) {
            itemFrameView.updateVideoAnimInfo(videoAnimInfo)
    }

    fun updateAnimMaskWidth(animDuration: Long) {
        itemFrameView?.updateVideoAnimInfoDuration(animDuration)
    }

    private fun setLeftAndRightPosition() {
        val leftIntArray = IntArray(2)
        val rightIntArray = IntArray(2)
        leftMove.getLocationOnScreen(leftIntArray)
        rightMove.getLocationOnScreen(rightIntArray)
        leftPosition = leftIntArray[0]
        leftPosition += TrackConfig.BORDER_WIDTH
        rightPosition = rightIntArray[0]
        ILog.d(TAG, "leftPosition is $leftPosition right position is $rightPosition")
    }

    fun setDrawPreTransitionOverlap(draw: Boolean) {
        itemFrameView.setDrawPreTransitionOverlap(draw)
    }

    fun setDrawMyTransitionOverlap(draw: Boolean) {
        itemFrameView.setDrawMyTransitionOverlap(draw)
    }

    fun isShowDivider(isShow: Boolean) {
        if (isShow && (!isFooterType || isFooterType && epilogueAttached)) {
            if (index != 0) {
                rlTransition.show()
                rlTransition.bringToFront()
                parent?.bringChildToFront(this)
            }
        } else {
            rlTransition.hide()
        }
    }


    fun setOnDragListener(listener: OnTrackDragListener?) {
        onDragListener = listener
    }

    private fun leftEdit(dis: Float, isInit: Boolean = false) {
        currentLength -= dis
        lpTopLine.width = Math.round(currentLength)
        lpBottomLine.width = Math.round(currentLength)
        lpFramesLayout.width = Math.round(currentLength)
        framesLayout.layoutParams = lpFramesLayout
        ivTopLine.layoutParams = lpTopLine
        ivBottomLine.layoutParams = lpBottomLine

        durationPx = currentLength
        framesLayout.paddingLeft.let { framesLayout.setPadding(Math.round(it - dis), 0, 0, 0) }
        startPx = framesLayout.paddingLeft
        startTime = (abs(startPx) / TrackConfig.PX_MS * speed).toLong()
        duration = Math.round(durationPx / TrackConfig.PX_MS * speed).toLong()
        setDurationIcon(duration)

        val retainWidth = lpBottomLine.width -
                Math.round(preSegmentTransitionDuration * TrackConfig.PX_MS)
        tipsManager.setMaxWidth(retainWidth)
        if (retainWidth <= 0) {
            tipsManager.llTipsLayout.visibility = View.GONE
        } else {
            tipsManager.llTipsLayout.visibility = View.VISIBLE
        }
        tipsManager.lpLayout.width = retainWidth
        tipsManager.llTipsLayout.layoutParams = tipsManager.lpLayout

        ILog.d(TAG, "recycle layoutParams.width is " + lpFramesLayout.width)
        if (!isInit) {
            (this.layoutParams as MarginLayoutParams).leftMargin += dis.toInt()
            movePx -= dis
            itemTrackCallback?.onClip(LEFT, startTime.toInt(), duration.toInt(), index, movePx)
            itemFrameView.clipLeft(-movePx)
        } else {
            movePx = 0f
        }
    }

    private fun rightEdit(dis: Float, isInit: Boolean = false) {
        currentLength += dis
        lpTopLine.width = Math.round(currentLength)
        lpBottomLine.width = Math.round(currentLength)
        lpFramesLayout.width = Math.round(currentLength)
        framesLayout.layoutParams = lpFramesLayout
        ivTopLine.layoutParams = lpTopLine
        ivBottomLine.layoutParams = lpBottomLine

        startPx = framesLayout.paddingLeft
        startTime = (abs(startPx) / TrackConfig.PX_MS * speed).toLong()
        durationPx = currentLength
        duration = Math.round(durationPx / TrackConfig.PX_MS * speed).toLong()
        setDurationIcon(duration)

        val retainWidth = lpBottomLine.width -
                Math.round(preSegmentTransitionDuration * TrackConfig.PX_MS)
        tipsManager.setMaxWidth(retainWidth)
        if (retainWidth <= 0) {
            tipsManager.llTipsLayout.visibility = View.GONE
        } else {
            tipsManager.llTipsLayout.visibility = View.VISIBLE
        }
        tipsManager.lpLayout.width = retainWidth
        tipsManager.llTipsLayout.layoutParams = tipsManager.lpLayout

        if (!isInit) {
            movePx += dis
            itemTrackCallback?.onClip(RIGHT, startTime.toInt(), duration.toInt(), index, movePx)
            itemFrameView.clipRight(movePx)
        }
        ILog.d(TAG, "recycle layoutParams.width is " + lpFramesLayout.width)
    }

    fun setData(
        nleTrackSlot: NLETrackSlot,
        scrollX: Int,
        preOverlapTransitionDuration: Long
    ) {
        NLESegmentVideo.dynamicCast(nleTrackSlot.mainSegment)?.apply {

            this@ItemTrackLayout.startTime = this.timeClipStart/1000
            // 这里不要考虑速度折算
            this@ItemTrackLayout.duration = (nleTrackSlot.duration/1000 * this.avgSpeed()).toLong()
            this@ItemTrackLayout.sourceDuration = this.resource?.duration?.div(1000) ?:  0
            this@ItemTrackLayout.speed = this.avgSpeed()
            this@ItemTrackLayout.segmentInfo = nleTrackSlot
            this@ItemTrackLayout.preSegmentTransitionDuration = preOverlapTransitionDuration
            frameView.setSlot(nleTrackSlot)
            itemFrameView.updateData(
                    nleTrackSlot,
                    scrollX,
                    preOverlapTransitionDuration
            )
            initBase()
            initData()
            setDurationIcon(duration)

            setAudioFilter(nleTrackSlot.audioFilter)

            val mlp = rlTransition.layoutParams as MarginLayoutParams
            if (preOverlapTransitionDuration != 0L) {
                val transitionIconMargin =
                        preSegmentTransitionDuration * TrackConfig.PX_MS
                mlp.marginStart =
                        (transitionIconMargin / 2 -
                                SizeUtil.dp2px(14F)).roundToInt()
                tipsManager.lpLayout.leftMargin = Math.round(transitionIconMargin)
                lpTvEpilogue?.leftMargin = Math.round(transitionIconMargin)
            } else {
                mlp.marginStart = -SizeUtil.dp2px(14F)
                tipsManager.lpLayout.leftMargin = 0
                lpTvEpilogue?.leftMargin = 0
            }
            rlTransition.layoutParams = mlp

        }

//        val videoSeg = NLESegmentVideo.dynamicCast(segmentInfo.mainSegment)
//        this.startTime = videoSeg.timeClipStart/1000
//        // 这里不要考虑速度折算
//        this.duration = (segmentInfo.duration/1000 * videoSeg.absSpeed).toLong()
//        this.sourceDuration = videoSeg?.avFile?.duration/1000
//        this.speed = videoSeg.absSpeed
//        this.segmentInfo = segmentInfo
//        this.preSegmentTransitionDuration = preOverlapTransitionDuration
//
//        itemFrameView.updateData(
//            segmentInfo,
//            scrollX,
//            preOverlapTransitionDuration
//        )
//        initBase()
//        initData()
//        setDurationIcon(duration)
//        val mlp = rlTransition.layoutParams as MarginLayoutParams
//        if (preOverlapTransitionDuration != 0L) {
//            val transitionIconMargin =
//                preSegmentTransitionDuration * TrackConfig.PX_MS
//            mlp.marginStart =
//                (transitionIconMargin / 2 -
//                        SizeUtil.dp2px(14F)).roundToInt()
//            tipsManager.lpLayout.leftMargin = Math.round(transitionIconMargin)
//            lpTvEpilogue?.leftMargin = Math.round(transitionIconMargin)
//        } else {
//            mlp.marginStart = -SizeUtil.dp2px(14F)
//            tipsManager.lpLayout.leftMargin = 0
//            lpTvEpilogue?.leftMargin = 0
//        }
//        rlTransition.layoutParams = mlp
    }

    private fun initData() {
        maxLength = when {
            isFooterAndNotAttached() -> (MOVIE_FOOT_DURATION / TrackConfig.DEFAULT_FRAME_DURATION).toInt() * TrackConfig.THUMB_WIDTH.toFloat()
            isFooterType -> MOVIE_FOOT_DURATION / speed * TrackConfig.PX_MS
            else -> sourceDuration / speed * TrackConfig.PX_MS
        }
        initLeft = (startTime / speed * TrackConfig.PX_MS)
        initRight = when {
            isFooterAndNotAttached() -> 0F
            isFooterType -> 0F
            else -> (sourceDuration - duration - startTime) / speed * TrackConfig.PX_MS
        }
        tipsManager.setMaxWidth(maxLength.toInt())
        minPx = initLeft
        maxPx = initRight
        lpFramesLayout.width = maxLength.toInt()
        lpTopLine.width = maxLength.toInt()
        lpBottomLine.width = maxLength.toInt()
        currentLength = maxLength
        leftEdit(initLeft, true)
        rightEdit(-initRight, true)
        ILog.d(TAG, "init left is $initLeft init right is $initRight max length is $maxLength")
    }

    // kotlin roundToInt 会抛出 check NaN 异常
    fun setScaleSize() {
        maxLength = when {
            isFooterAndNotAttached() -> (MOVIE_FOOT_DURATION / TrackConfig.DEFAULT_FRAME_DURATION).toInt() * TrackConfig.THUMB_WIDTH.toFloat()
            isFooterType -> MOVIE_FOOT_DURATION / speed * TrackConfig.PX_MS
            else -> sourceDuration / speed * TrackConfig.PX_MS
        }
        initLeft = (startTime / speed * TrackConfig.PX_MS)
        initRight = when {
            isFooterAndNotAttached() -> 0F
            isFooterType -> 0F
            else -> (sourceDuration - duration - startTime) / speed * TrackConfig.PX_MS
        }
        minPx = initLeft
        maxPx = initRight
        currentLength = maxLength - initLeft - initRight
        lpFramesLayout.width = Math.round(currentLength)
        lpTopLine.width = Math.round(currentLength)
        lpBottomLine.width = Math.round(currentLength)
        framesLayout.layoutParams = lpFramesLayout

        val retainWidth = lpBottomLine.width -
                Math.round(preSegmentTransitionDuration * TrackConfig.PX_MS)
        if (retainWidth <= 0) {
            tipsManager.llTipsLayout.visibility = View.GONE
        } else {
            tipsManager.llTipsLayout.visibility = View.VISIBLE
        }
        tipsManager.setMaxWidth(retainWidth)
        tipsManager.lpLayout.width = retainWidth
        tipsManager.llTipsLayout.layoutParams = tipsManager.lpLayout

        framesLayout.paddingLeft.let { framesLayout.setPadding(-Math.round(initLeft), 0, 0, 0) }

        ILog.d(TAG, "init left is $initLeft init right is $initRight max length is $maxLength")

        val mlp = rlTransition.layoutParams as MarginLayoutParams
        if (preSegmentTransitionDuration != 0L) {
            val transitionIconMargin =
                preSegmentTransitionDuration * TrackConfig.PX_MS
            mlp.marginStart =
                Math.round(transitionIconMargin / 2) -
                        SizeUtil.dp2px(14F)
            tipsManager.lpLayout.leftMargin = Math.round(transitionIconMargin)
            lpTvEpilogue?.leftMargin = Math.round(transitionIconMargin)
        } else {
            mlp.marginStart = -SizeUtil.dp2px(14F)
            tipsManager.lpLayout.leftMargin = 0
            lpTvEpilogue?.leftMargin = 0
        }
    }

    private fun isFooterAndNotAttached() = isFooterType && epilogueAttached.not()

    fun startScale() {
    }

    fun endScale() {
    }

    fun setItemTrackCallback(callback: ItemTrackCallback) {
        itemTrackCallback = callback
        itemFrameView.frameFetcher = { path, timestamp ->
            callback.getFrameBitmap(path, timestamp)
        }
    }

    fun setScrollHandler(scrollHandler: ScrollHandler?) {
        outsideScrollHandler = scrollHandler
    }

    private fun setDurationIcon(duration: Long) {
        if (isFooterType || style != MultiTrackLayout.TrackStyle.CLIP) {
            tipsManager.hideDurationTips()
        } else {
            val absoluteDuration = duration / speed
            val formatDuration = FormatUtil.formatLabelTime(absoluteDuration.toLong())
            tipsManager.showDurationTipsWithText(formatDuration)
        }
    }

    private fun setAudioFilter(audioFilter: NLEFilter?) {
        if (audioFilter == null) {
            tipsManager.hideAudioFilter()
        } else {
            tipsManager.showAudioFilterWithText(audioFilter.segment.filterName)
        }
    }

    fun setFooterType() {
        isFooterType = true
        vsEpilogue.show()
        rlEpilogue.show()
        lpTvEpilogue = iconEpilogue.layoutParams as MarginLayoutParams
        setScaleSize()
        itemFrameView.setIsFooter(true)
    }

    fun setEpilogueEnable(enable: Boolean) {
        epilogueAttached = enable
        if (enable) {
            iconEpilogue.visibility = View.VISIBLE
            iconAddEpilogue.visibility = View.GONE
        } else {
            iconAddEpilogue.visibility = View.VISIBLE
            iconEpilogue.visibility = View.GONE
        }
        // 调用缩放函数，让界面缩放正常，1.0这个参数其实里面没用，随便传一个
        setScaleSize()
    }

    fun setOnEpilogueEnableListener(listener: () -> Unit) {
        iconAddEpilogue.setOnClickListener { listener() }
    }

    fun updateScrollX(scrollX: Int) {
        val tvTopTransitionX: Float
        if (clipState == HorizontallyState.NULL &&
            (style == MultiTrackLayout.TrackStyle.CLIP || style == MultiTrackLayout.TrackStyle.LINE)
        ) {
            // recyclerView的X滚动量减去左边的padding，则是滚动的偏移
            val offSet = scrollX - screenWidth / 2F

            val endDuration =
                (segmentInfo?.measuredEndTime ?: 0)/1000
            // 这个片段在屏幕上右方的像素位置
            val itemRightPx = endDuration * TrackConfig.PX_MS
            // 这个片段在屏幕上左边的像素位置
            val itemLeftPx = (segmentInfo?.startTime
                ?: 0) * TrackConfig.PX_MS/1000
            // 变速、静音按钮始终跟随屏幕左方滑动, tips左边触碰到了屏幕边缘
            if (offSet > itemLeftPx + tipsManager.lpLayout.leftMargin) {
                // 偏移量+上这个漂浮TextView的位置没有超出片段的右边
                if (offSet + tipsManager.getTotalWidth() < itemRightPx) {
                    // margin就是偏移量的位置减去片段左边的像素位
                    val leftMargin = offSet - itemLeftPx - tipsManager.lpLayout.leftMargin
                    tvTopTransitionX = leftMargin
                } else {
                    tvTopTransitionX = itemRightPx - itemLeftPx - tipsManager.getTotalWidth()
                }
            } else {
                tvTopTransitionX = 0F
            }
        } else {
            tvTopTransitionX = 0F
        }
        tipsManager.setTipsTranslateX(tvTopTransitionX)
        if (!isFooterType) {
            itemFrameView.updateScrollX(scrollX)
        }
    }

    fun beginDrag() {
        itemFrameView.beginDrag()
    }

    fun endDrag() {
        itemFrameView.endDrag()
    }

    fun updateClipWidth(clipWidth: Float) {
        itemFrameView.updateClipWidth(clipWidth)
    }

    fun refreshFrames() {
        itemFrameView?.postInvalidate()
    }

    fun getTransitionWidth(): Float {
        val transitionPath = segmentInfo?.endTransition?.effectSDKTransition?.resourceFile ?: ""
        val transitionDuration = if (transitionPath.isNotBlank()) {
            if (segmentInfo?.endTransition?.overlap == true) {
                (segmentInfo?.endTransition?.transitionDuration ?: 0)/1000
            } else {
                0
            }
        } else {
            0
        }
        return transitionDuration * TrackConfig.PX_MS
    }

    fun onPlayPositionChanged() {
        if (style != MultiTrackLayout.TrackStyle.NONE) {
            frameView.update()
        }
    }


    fun setFrameViewCallback(callback: FrameView.FrameViewCallback) {
        frameView.setFrameViewCallback(callback)
    }

    fun setFrameSelectChangeListener(listener: KeyframeSelectChangeListener) {
        frameView.frameSelectChangeChangeListener = listener
    }




    interface ItemTrackCallback {
        fun onItemClick(index: Int)

        fun onEditChange(index: Int, dis: Int, side: Int, move: Float)

        fun onStartAndDuration(index: Int, start: Int, duration: Int, side: Int)

        fun onUpMoveChange(index: Int, side: Int, move: Float, location: Int)

        fun onTransitionClick(index: Int)

        fun getAdsorbOffset(index: Int, duration: Long, clipOrientation: HorizontallyState): Float

        fun isAbleToSetTransition(index: Int): Boolean

        fun onUnableToSetTransition(index: Int)

        fun startClip(index: Int)

        fun onClip(side: Int, start: Int, duration: Int, index: Int, moveX: Float)

        fun stopClip(index: Int)

        fun getFrameBitmap(path: String, timestamp: Int): Bitmap?
    }
}
