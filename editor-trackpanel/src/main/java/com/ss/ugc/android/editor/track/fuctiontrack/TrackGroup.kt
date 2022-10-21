package com.ss.ugc.android.editor.track.fuctiontrack

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import com.bytedance.ies.nle.editor_jni.NLETrack
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.BaseTrackKeyframeItemView
import com.ss.ugc.android.editor.track.utils.OrientationManager
import com.ss.ugc.android.editor.track.utils.PadUtil
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.utils.safelyPerformHapticFeedback
import com.ss.ugc.android.editor.track.widget.EditScroller
import com.ss.ugc.android.editor.track.widget.HorizontallyState
import com.ss.ugc.android.editor.track.widget.ScrollHandler
import com.ss.ugc.android.editor.track.widget.TrackConfig.PX_MS
import kotlin.math.max
import kotlin.math.round

class TrackGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : EditScroller(context, attrs, defStyleAttr) {

    private val edgeWaringPaint = Paint()
    private val TAG = "TrackGroup"

    var trackGroupActionListener: TrackGroupActionListener? = null

    init {
        setPadding(0, paddingTop, 0, paddingBottom)
        isClickable = true

        edgeWaringPaint.style = Paint.Style.FILL
        edgeWaringPaint.strokeWidth = EDGE_WARNING_WIDTH.toFloat()
        edgeWaringPaint.color = EDGE_WARNING_COLOR
        edgeWaringPaint.isAntiAlias = true
    }

    var trackCount: Int = KEEP_TRACK_COUNT
        private set

    internal var clipMinDuration = 100L
    internal var itemHeight: Int = 0
        private set
    internal var itemMargin: Int = 0
        private set
    internal var maxTrackNum: Int = 0
        private set
    internal var canMoveOutOfVideos: Boolean = false
        get() = adapter?.canMoveOutOfVideos() ?: false
        private set
    private val desireHeight: Int
        get() {
            val count = trackCount
            return (adapter?.getDesireHeight(count)
                ?: count * (itemHeight + itemMargin))
        }
    override val maxScrollY: Int
        get() {
            val result = desireHeight - paddingTop - paddingBottom - measuredHeight
            return if (result < 0) 0 else result
        }

    private val segmentParams: MutableMap<NLETrackSlot, TrackParams> = mutableMapOf()
    private var mainVideoDuration: Long = 0L
        get() = (callback?.getMainVideoDuration()?:0L)/1000
    internal val mainVideoLength: Float
        get() = mainVideoDuration * timelineScale
    private var videosDuration: Long = 0L
    internal val videosLength: Float
        get() = videosDuration * timelineScale

    private var moveTouchEdge = false

    private val clipHelper: TrackClipHelper by lazy {
        TrackClipHelper(
            context,
            this
        ) { callback }
    }
    private val scrollHelper: TrackVerticallyScrollHelper by lazy {
        TrackVerticallyScrollHelper(this)
    }
    private var isClipping = false
    private var isDragging = false

    private var animator: Animator? = null
    private var adapter: Adapter? = null
    var callback: Callback? = null
        private set

    internal var blankClickListener: OnClickListener? = null
        private set

    private fun getChildMeasureSpec(size: Int): Int {
        return MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getDefaultSize(suggestedMinimumWidth, widthMeasureSpec),
            getDefaultSize(suggestedMinimumHeight, heightMeasureSpec)
        )

        segmentParams.forEach { (segment, params) ->
            val child = params.holder.getView()
            val duration = segment.duration / 1000
            child.measure(
                getChildMeasureSpec(round(duration * PX_MS).toInt()),
                getChildMeasureSpec(itemHeight)
            )
        }

//        TrackLog.d(
//            TAG,
//            "measure width $measuredWidth / ${MeasureSpec.getSize(widthMeasureSpec)} height  $measuredHeight   / ${MeasureSpec.getSize(
//                heightMeasureSpec
//            )} "
//        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val paddingHorizontal = getPaddingHorizontal()
        segmentParams.forEach { (segment, params) ->
            val top = params.trackIndex * (itemHeight + itemMargin)
            val bottom = top + itemHeight
            val child = params.holder.getView()
            val start = segment.startTime / 1000
            val left: Int = paddingHorizontal + round(start * PX_MS).toInt()
            child.layout(left, top, left + child.measuredWidth, bottom)
//            TrackLog.d(TAG, "on layout width $measuredWidth  height  $measuredHeight")
//            TrackLog.d(TAG, "on layout top $top  bottom  $bottom")

        }
    }

    override fun computeScroll() {
        scrollHelper.computeScroll()
    }

    override fun dispatchDraw(canvas: Canvas?) {
        canvas ?: return
        super.dispatchDraw(canvas)
        clipHelper.drawDecorate(canvas)
        adapter?.drawDecorate(canvas)

        if (moveTouchEdge) {
            val y = desireHeight - EDGE_WARNING_WIDTH / 2F
            val stopX = (desireMaxScrollX + getPaddingHorizontal() * 2).toFloat()
            canvas.drawLine(0F, y, stopX, y, edgeWaringPaint)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        isClipping = clipHelper.onInterceptTouchEvent(scrollX, scrollY, ev)
        return isClipping || (ev != null && !isDragging && scrollHelper.onInterceptTouchEvent(ev))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)

        return if (isClipping) {
            val clip: (NLETrackSlot, Long, Long, Long) -> Unit =
                { segment, start, timelineOffset, duration ->
                    adapter?.onClip(segment, start, timelineOffset, duration)
                }
            clipHelper.onTouchEvent(scrollX, scrollY, event, clip)
        } else {
            scrollHelper.onTouchEvent(event)
        }
    }

    override fun checkAddView(child: View) {
    }

    override fun setOnBlankClickListener(listener: OnClickListener?) {
        blankClickListener = listener
    }

    internal fun disableScroll(disable: Boolean) {
        scrollHelper.disableScroll(disable)
    }

    fun smoothScrollVerticallyBy(y: Int) {
        scrollHelper.smoothScrollVerticallyBy(y)
    }

    @MainThread
    fun updateTracks(
        segmentParams: Map<NLETrackSlot, TrackParams>,
        trackCount: Int,
        requestOnScreenTrack: Int,
        refresh: Boolean = true,
        nleTrackSlot: NLETrackSlot? = null
    ) {
        this.segmentParams.clear()
        this.segmentParams.putAll(segmentParams)
        this.trackCount = max(KEEP_TRACK_COUNT, trackCount)

        if (refresh) requestLayout()

        // 如果需要选中特定项，选中特定项会执行滚动
        if (nleTrackSlot != null) {
            selectSegment(nleTrackSlot, false)
        } else {
            // 裁剪之后，会重新刷新数据，selectSegment引用的对象被拷贝并修改
            // 所以这里需要把selectSegment指向新拷贝的对象
//            clipHelper.updateSelected(segmentParams)
        }
        // 把需要展示的行展示出来
        requestTrackOnScreen(requestOnScreenTrack)
        animator?.cancel()
    }

    private fun requestTrackOnScreen(requestOnScreenTrack: Int) {
        if (requestOnScreenTrack >= 0) {
            val y = getScrollByVerticalPxOfRequestOnScreen(requestOnScreenTrack)
            if (y != 0) {
                smoothScrollVerticallyBy(y)
                return
            }
        }

        if (scrollY > maxScrollY) {
            smoothScrollVerticallyBy(scrollY - maxScrollY)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupHolderTouchHandler(holder: TrackItemHolder) {
        val dragListener = object : TrackDragListener {
            private val dragHelper = TrackDragHelper(this@TrackGroup, holder) { callback }

            override fun beginDrag() {
                // 开始长按拖拽，则取消选中状态
                resetSelected()

                animator?.cancel()
                animator = dragHelper.beginDrag(segmentParams)
                adapter?.onDragBegin()
                isDragging = true
                holder.setDragging(true)
            }

            override fun drag(rawX: Float, rawY: Float, deltaX: Float, deltaY: Float) {
                dragHelper.drag(rawX, deltaX, deltaY , segmentParams)
            }

            override fun endDrag() {
                animator?.cancel()
                animator = dragHelper.endDrag { fromTrackIndex: Int,
                                                toTrackIndex: Int,
                                                segment: NLETrackSlot,
                                                offsetInTimeline: Long ->
                    val currPosition = (this@TrackGroup.scrollX / timelineScale).toLong()
                    adapter?.onMove(
                        fromTrackIndex,
                        toTrackIndex,
                        segment,
                        offsetInTimeline,
                        currPosition
                    )
                }
                isDragging = false
                holder.setDragging(false)
            }
        }
        holder.getView().run {
            setOnTouchListener(
                TrackTouchHelper(dragListener, clickListener = {
                    if (this is BaseTrackKeyframeItemView && clickKeyframeAt(it)) {
                        return@TrackTouchHelper
                    }
                    clipHelper.selectByTap(holder, segmentParams)
                },
                    doubleClickListener = {
                        segmentParams.forEach { (segment, v) ->
                            if (v.holder == holder) {
                                adapter?.onTrackDoubleClick(segment)
                                return@forEach
                            }
                        }
                    })
            )
        }
    }

    fun resetSelected() {
        clipHelper.resetSelected()
    }

    internal fun selectSegment(
        slot: NLETrackSlot,
        isFromUser: Boolean = false
    ) {
        val requestOnScreenTrackIndex = findSelectTrackIndex(slot)
        if (requestOnScreenTrackIndex != -1) {
            clipHelper.selectBySegmentId(slot, segmentParams)
            requestTrackOnScreen(requestOnScreenTrackIndex)

        }
    }

    fun scrollToSlotIfNeeded(nleTrackSlot: NLETrackSlot) {
        requestTrackOnScreen(findSelectTrackIndex(nleTrackSlot))
    }

    private fun findSelectTrackIndex(slot: NLETrackSlot): Int {
        var index = -1
        segmentParams.forEach { (nleTrackSlot, trackParams) ->
            if (slot.id == nleTrackSlot.id) {
                index = trackParams.trackIndex
                return@forEach
            }
        }
        return index
    }

    @MainThread
    fun setAdapter(adapter: Adapter?) {
        if (this.adapter == adapter) return

        this.adapter = adapter
        segmentParams.clear()

        removeAllViews()

        scrollY = 0

        itemHeight = adapter?.getItemHeight() ?: 0
        itemMargin = adapter?.getItemMargin() ?: 0
        maxTrackNum = adapter?.getMaxTrackNum() ?: 0
        clipMinDuration = adapter?.getClipMinDuration() ?: 100L
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    fun setMainVideoDuration(duration: Long) {
        this.mainVideoDuration = duration
    }

    fun setVideosDuration(duration: Long) {
        this.videosDuration = duration
    }

    fun setMoveTouchEdge(moveTouchEdge: Boolean) {
        if (this.moveTouchEdge != moveTouchEdge) {
            if (moveTouchEdge) {
                safelyPerformHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            this.moveTouchEdge = moveTouchEdge
            invalidate()
        }
    }

    fun getScrollByVerticalPxOfRequestOnScreen(requestOnScreenTrack: Int): Int {
        // 要将item及其topMargin和bottomMargin都显示出来
        val requestShowTop = requestOnScreenTrack * (itemHeight + itemMargin)
        var verticalPx = requestShowTop - scrollY
        return if (verticalPx < 0)
            verticalPx
        else {
            val requestShowBottom = requestShowTop + itemHeight + itemMargin
            verticalPx = requestShowBottom - measuredHeight - scrollY
            if (verticalPx > 0) verticalPx else 0
        }
    }


    override fun setTimelineScale(scale: Float) {
        if (timelineScale != scale) {
            super.setTimelineScale(scale)
            segmentParams.values.forEach {
                it.holder.setTimelineScale(scale)
            }
            requestLayout()
            clipHelper.onTimelineScaleChanged(segmentParams)
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        adapter?.onScrollChanged()
        segmentParams.values.forEach {
            it.holder.onParentScrollChanged(scrollX)
        }
    }

    /**
     * 有一些情况不是用户选中、反选，或改变选中的segment，而仅仅是因为数据刷新了，要更新一下外部保存的数据
     * 这种情况下`dataUpdate`为`true`
     */
    fun onSelectChanged(data: Pair<NLETrackSlot, TrackParams>?, dataUpdate: Boolean = false) {
        adapter?.updateSelected(data, dataUpdate)
    }

    fun canMoveOutOfMainVideo(): Boolean {
        return adapter?.canMoveOutOfMainVideo() ?: false
    }


    companion object {
        private val EDGE_WARNING_WIDTH = SizeUtil.dp2px(2F)
        private val EDGE_WARNING_COLOR = Color.parseColor("#00E5F6")

        const val KEEP_TRACK_COUNT = 3

        private val halfWidth: Int
        private val halfHeight: Int

        init {
            val size = SizeUtil.getScreenSize(TrackSdk.application)
            if (OrientationManager.isLand()) {
                halfWidth = size.y / 2
                halfHeight = size.x / 2
            } else {
                halfWidth = size.x / 2
                halfHeight = size.y / 2
            }
        }

        fun getPaddingHorizontal(): Int {
            return if (PadUtil.isPad && OrientationManager.isLand()) halfHeight else halfWidth
        }
    }

    @Suppress("ComplexInterface")
    interface Adapter {
        fun getDesireHeight(trackCount: Int): Int

        fun drawDecorate(canvas: Canvas)

        fun onDragBegin()

        fun createHolder(parent: ViewGroup, index: Int): TrackItemHolder

        fun bindHolder(
            holder: TrackItemHolder,
            slot: NLETrackSlot,
            index: Int
        )

        fun onScrollChanged()

        /**
         * @param dataUpdate 是否只是数据更新，用以区分新增数据
         */
        fun updateSelected(data: Pair<NLETrackSlot, TrackParams>?, dataUpdate: Boolean)

        fun onMove(
            fromTrackIndex: Int,
            toTrackIndex: Int,
            slot: NLETrackSlot,
            offsetInTimeline: Long,
            currPosition: Long
        )

        fun canMoveOutOfMainVideo(): Boolean

        fun canMoveOutOfVideos(): Boolean

        fun onClip(
            slot: NLETrackSlot,
            start: Long,
            timelineOffset: Long,
            duration: Long
        )

        fun onTrackDoubleClick(slot: NLETrackSlot)

        fun getItemHeight(): Int

        fun getItemMargin(): Int

        fun getMaxTrackNum(): Int

        fun getClipMinDuration(): Long
    }

    abstract class Callback(scrollHandler: ScrollHandler) : ScrollHandler by scrollHandler {
        fun scrollBy(trackGroup: TrackGroup, x: Int, y: Int, invokeChangeListener: Boolean) {
            // 裁剪的过程中，scrollX有可能超过实际的maxScrollX，在这种情况下，是需要以用户
            // 裁剪产生的scrollX为准的，所以要设一个假的maxScrollX
            if (x > 0) {
                val assignMaxScrollX = trackGroup.scrollX + x
                assignMaxScrollX(assignMaxScrollX)
            } else if (x < 0) {
                val assignMaxScrollX = trackGroup.scrollX +
                        SizeUtil.getScreenWidth(TrackSdk.application)
                assignMaxScrollX(assignMaxScrollX)
            }
            scrollBy(x, y, invokeChangeListener)
        }

        abstract fun clipTo(px: Int)

        abstract fun onStartAdsorption(nleTrackSlot: NLETrackSlot)

        abstract fun onCancelAdsorption()

        abstract fun doAdsorptionOnClip(
            touchPositionInTime: Long,
            handlePositionInTime: Long
        ): Long?

        abstract fun doAdsorptionOnDrag(
            nleTrackSlot: NLETrackSlot,
            dragState: HorizontallyState,
            startTime: Long,
            endTime: Long,
            currentStartTime: Long,
            currentEndTime: Long
        ): Long

        abstract fun onSelectKeyframe(keyframe: NLETrackSlot?)
        abstract fun onDeselectKeyframe()
        abstract fun onClickKeyframe(playHead: Long)
        abstract fun getMainVideoDuration(): Long
    }
}

data class UpdateTrackParams(
    val requestOnScreenTrack: Int = -1,
    val tracks: List<NLETrack> = emptyList(),
    val refresh: Boolean = true,
    val selectSegment: String? = null
)

data class TrackParams(val trackIndex: Int, val holder: TrackItemHolder)


interface TrackGroupActionListener {

    /**
     * 轨道上选中了某类slot
     */
    fun onSlotSelect(nleTrackSlot: NLETrackSlot?)


    fun onClip(
        slot: NLETrackSlot,
        start: Long,
        timelineOffset: Long,
        duration: Long
    )


}
