package com.ss.ugc.android.editor.track.fuctiontrack

import android.graphics.Canvas
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.BaseTrackKeyframeItemView
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.KeyframeStateDelegate
import com.ss.ugc.android.editor.track.keyframe.KeyframeSelectChangeListener
import com.ss.ugc.android.editor.track.widget.HorizontalScrollContainer
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

abstract class BaseTrackAdapter(
    protected val trackGroup: TrackGroup,
    protected val container: HorizontalScrollContainer,
    private val controller: PlayController,
    private val frameDelegate: KeyframeStateDelegate
) : TrackGroup.Adapter {
    protected val scrollX: Int
        get() = trackGroup.scrollX
    protected val timelineScale: Float
        get() = trackGroup.timelineScale

    var isStopped = true
    protected val segmentParams: MutableMap<NLETrackSlot, TrackParams> = mutableMapOf()

    @MainThread
    @CallSuper
    open fun performStart() {
        if (isStopped) {
            isStopped = false
            trackGroup.setAdapter(this)
        }
    }

    @MainThread
    @CallSuper
    open fun performStop() {
        if (!isStopped) {
            trackGroup.setAdapter(null)
            segmentParams.clear()
            isStopped = true
        }
    }

    override fun bindHolder(
        holder: TrackItemHolder,
        slot: NLETrackSlot,
        index: Int
    ) {
        holder.setSegment(slot)
        if (holder.getView() is BaseTrackKeyframeItemView) {
            (holder.getView() as BaseTrackKeyframeItemView).also {
                it.listener = object : KeyframeSelectChangeListener {
                    override fun onKeyframeSelect(frame: NLETrackSlot?) {
                        trackGroup.callback?.onSelectKeyframe(frame)
                    }

                    override fun onKeyframeClick(playHead: Long) {
                        trackGroup.callback?.onClickKeyframe(playHead)
                    }

                    override fun onKeyframeDeselect() {
                        trackGroup.callback?.onDeselectKeyframe()
                    }

                }
                it.frameDelegate = frameDelegate
            }
        }
    }

    override fun onScrollChanged() {
    }

    @CallSuper
    override fun updateSelected(data: Pair<NLETrackSlot, TrackParams>?, dataUpdate: Boolean) {
        if (!dataUpdate && data != null) controller.pause()
    }

    protected fun requestSelectedItemOnScreen(data: Pair<NLETrackSlot, TrackParams>?) {
        val (slot, params) = data ?: return
        val tapViewTop = params.trackIndex * (getItemHeight() + getItemMargin())
        val scrollY = trackGroup.scrollY
        val scrollToTop = tapViewTop - scrollY
        val y = if (scrollToTop < 0) {
            scrollToTop
        } else {
            val height = trackGroup.measuredHeight
            val scrollToBottom = tapViewTop + getItemHeight() - scrollY - height
            if (scrollToBottom > 0) scrollToBottom else 0
        }

        trackGroup.smoothScrollVerticallyBy(y)

        val range = slot.duration / 1000
        val timestamp = trackGroup.scrollX / timelineScale
        val x = when {
            timestamp < slot.startTime / 1000 ->
                ceil(slot.startTime / 1000 * timelineScale).toInt()
            timestamp >= slot.startTime / 1000 + range ->
                // 往前挪2个像素
                floor(slot.measuredEndTime / 1000 * timelineScale - CLIP_ELIMINATE_OF_ERROR).toInt()
            else -> -1
        }
        if (x >= 0) {
            trackGroup.callback?.clipTo(x)
            container.smoothScrollHorizontallyBy(x - trackGroup.scrollX, false)
        }
    }

    @CallSuper
    open fun updateTracks(
        tracks: List<TrackInfo>,
        requestOnScreenTrack: Int,
        refresh: Boolean = true,
        selectSegment: NLETrackSlot? = null
    ) {
        if (!isStopped) {
            doUpdateTracks(tracks, requestOnScreenTrack, refresh, selectSegment)
        }
    }


    private fun doUpdateTracks(
        tracks: List<TrackInfo>,
        requestOnScreenTrack: Int,
        refresh: Boolean = true,
        selectSegment: NLETrackSlot? = null
    ) {
        val oldParams = mutableMapOf<NLETrackSlot, Map.Entry<NLETrackSlot, TrackParams>>()
        segmentParams.forEach { oldParams[it.key] = it }
        segmentParams.clear()

        val noMatchSegment = mutableMapOf<NLETrackSlot, Int>()
        tracks.forEachIndexed { index, trackInfo ->
            trackInfo.slotList.forEach { slot ->
                val recycled = findCachedSlotById(oldParams, slot)

                if (recycled != null) {
                    val (oldSegment, params) = recycled
                    // 如果segmentInfo改变了，就需要重新bind一下，否则不需要
//                    if (slot.id != oldSegment.id) {
                    // 都bind 一次，防止数据有更新,考虑是不是
                        bindHolder(params.holder, slot, trackInfo.layer)
//                    }
                    params.holder.getView().apply {
                        translationX = 0F
                        translationY = 0F
                    }
                    segmentParams[slot] = TrackParams(trackInfo.layer, params.holder)
                } else {
                    noMatchSegment[slot] = trackInfo.layer
                }
            }
        }

        // 不可能同时存在新增和删除，所以没匹配上的新数据直接新创建View
        noMatchSegment.forEach { (segment, trackIndex) ->
            val holder = createHolder(trackGroup, trackIndex)
            segmentParams[segment] = TrackParams(trackIndex, holder)
            trackGroup.setupHolderTouchHandler(holder)
            trackGroup.addView(holder.getView())
            bindHolder(holder, segment, trackIndex)
        }

        // 不可能同时存在新增和删除，所以没匹配上的旧View直接删除
        oldParams.values.forEach { (_, params) ->
            params.holder.destroy()
        }
        var trackCount = -1
        segmentParams.forEach {
            trackCount = max(trackCount, it.value.trackIndex)
        }
        trackGroup.updateTracks(
            segmentParams,
            trackCount + 1,
            requestOnScreenTrack,
            true,
            selectSegment
        )
    }

    private fun findCachedSlotById(
        oldParams: MutableMap<NLETrackSlot, Map.Entry<NLETrackSlot, TrackParams>>,
        slot: NLETrackSlot
    ): Map.Entry<NLETrackSlot, TrackParams>? {
        oldParams.keys.forEach {
            if (slot.id == it.id) {
                val entry = oldParams[it]
                oldParams.remove(it)
                return entry
            }
        }
        return null
    }


    fun selectSegment(segmentId: NLETrackSlot?) {
        if (isStopped) return
        if (segmentId == null) {
            trackGroup.resetSelected()
        } else {
            trackGroup.selectSegment(segmentId, false)
        }
    }

    protected fun scrollBy(x: Int, y: Int, invokeScrollListener: Boolean = false) {
        container.scrollBy(x, y, invokeScrollListener)
    }

    override fun getDesireHeight(trackCount: Int): Int {
        return trackCount * (getItemHeight() + getItemMargin())
    }

    @CallSuper
    override fun onDragBegin() {
        controller.pause()
    }

    override fun canMoveOutOfMainVideo(): Boolean = true

    override fun canMoveOutOfVideos(): Boolean = true

    override fun getItemHeight() = ITEM_HEIGHT

    override fun getItemMargin() = ITEM_MARGIN

    override fun getMaxTrackNum(): Int = Int.MAX_VALUE

    override fun getClipMinDuration() = TrackConfig.MIN_AUDIO_DURATION

    override fun drawDecorate(canvas: Canvas) {
    }

    override fun onTrackDoubleClick(slot: NLETrackSlot) {
    }

    companion object {
        val ITEM_HEIGHT = TrackConfig.SUB_TRACK_HEIGHT
        val ITEM_MARGIN = TrackConfig.TRACK_MARGIN
    }
}

interface PlayController {
    fun pause()
}
