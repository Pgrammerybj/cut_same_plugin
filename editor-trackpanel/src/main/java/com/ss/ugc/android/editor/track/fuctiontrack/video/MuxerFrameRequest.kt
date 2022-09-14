package com.ss.ugc.android.editor.track.fuctiontrack.video

import com.bytedance.ies.nle.editor_jni.NLEResType
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.core.avgSpeed
import com.ss.ugc.android.editor.track.frame.CacheKey
import com.ss.ugc.android.editor.track.frame.FrameLoader
import com.ss.ugc.android.editor.track.frame.FrameRequest
import com.ss.ugc.android.editor.track.frame.PriorityFrame
import com.ss.ugc.android.editor.track.frame.RequestInfo
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.fuctiontrack.TrackParams
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

class MuxerFrameRequest(
    private val trackGroup: TrackGroup,
    private val itemHeight: Int,
    private val itemMargin: Int
) : FrameRequest {
    private val segmentParams = ConcurrentHashMap<NLETrackSlot, TrackParams>()
    private var lastRequests: MutableMap<NLETrackSlot, Set<PriorityFrame>> = mutableMapOf()

    fun updateData(segmentParams: Map<NLETrackSlot, TrackParams>) {
        this.segmentParams.clear()
        this.segmentParams.putAll(segmentParams)
    }

    @Synchronized
    override fun getRequestFrames(): RequestInfo {
        val lastRequests = lastRequests.toMutableMap()
        val removed = mutableSetOf<PriorityFrame>()
        val add = mutableSetOf<PriorityFrame>()
        val all = mutableSetOf<PriorityFrame>()
        val newRequests = mutableMapOf<NLETrackSlot, Set<PriorityFrame>>()

        val timelineScale = trackGroup.timelineScale
        val onScreenStart = (trackGroup.scrollX - TrackGroup.getPaddingHorizontal()) / timelineScale
        val onScreenDuration = trackGroup.width / timelineScale
        val frameTargetDuration = VideoItemHolder.ITEM_WIDTH / timelineScale

        segmentParams.forEach { (segment, params) ->
            val holder = params.holder as? VideoItemHolder
                ?: return@forEach
            if (holder.isDragging) {
                // 如果正在长按拖移过程中，就不刷新缓存，避免移动到5屏之外缓存被回收黑掉的问题
                lastRequests.remove(segment)?.let {
                    all.addAll(it)
                    newRequests[segment] = it
                }
                return@forEach
            }
            val itemView = holder.getView() as? VideoItemView
                ?: return@forEach
            val frames = getSegmentFrames(
                segment,
                params.trackIndex,
                itemView.isClipping,
                onScreenStart,
                onScreenDuration,
                frameTargetDuration
            )
            val oldFrames = lastRequests.remove(segment)
            if (oldFrames == null) {
                add.addAll(frames)
            } else {
                removed.addAll(oldFrames - frames)
                add.addAll(frames - oldFrames)
            }
            all.addAll(frames)
            newRequests[segment] = frames
        }
        lastRequests.forEach { (_, frames) ->
            removed.addAll(frames)
        }
        this.lastRequests = newRequests
        return RequestInfo(removed.toMutableList(), add.toMutableList(), all.toMutableList())
    }

    @Suppress("ComplexMethod")
    private fun getSegmentFrames(
        slot: NLETrackSlot,
        trackIndex: Int,
        isClipping: Boolean,
        onScreenStart: Float,
        onScreenDuration: Float,
        frameTargetDuration: Float
    ): Set<PriorityFrame> {
        // 如果正在裁剪，就把这个视频段所有帧都尽可能取出来，避免出现裁剪过程中闪烁都问题

        val convertToSegmentVideo = NLESegmentVideo.dynamicCast(slot.mainSegment)
        val clipLeft = if (isClipping) {
            -(convertToSegmentVideo.timeClipStart/1000 / convertToSegmentVideo.avgSpeed()).toLong()
        } else {
            0L
        }
        val segmentStart = slot.startTime/1000 + clipLeft
        val targetStart = max(segmentStart, (onScreenStart - 2 * onScreenDuration).toLong())

        // 如果正在裁剪，就把这个视频段所有帧都尽可能取出来，避免出现裁剪过程中闪烁都问题
        val clipLength = if (isClipping) {
            (convertToSegmentVideo.duration / 1000 )
        } else {
            slot.duration/1000
        }

        // 缓存5屏的帧，分别是屏上+离屏4屏
        val segmentEnd = slot.startTime/1000 + clipLength
        val targetEnd = min(segmentEnd, (onScreenStart + 3 * onScreenDuration).toLong())

        val targetDiff = targetEnd - targetStart
        // 不在5屏区域内，不需要cache
        if (targetDiff <= 0) return emptySet()

        val scrollY = trackGroup.scrollY
        val viewTop = trackIndex * (itemHeight + itemMargin)
        val viewBottom = viewTop + itemHeight
        val isOnScreenVertically =
            (viewTop - trackGroup.height) < scrollY && viewBottom > scrollY
        // 如果是图片类型，那么缓存一张就可以了
        if (convertToSegmentVideo.type == NLEResType.IMAGE) {
            // 优先级：屏上轨道屏上帧 > 屏上轨道离屏帧 > 屏外轨道屏上时间内的帧 > 屏外轨道屏上时间外的帧
            // 屏上轨道指的是轨道的y轴坐标在父View显示区域内的
            val priority = if (slot.measuredEndTime/1000> onScreenStart &&
                slot.startTime/1000 < onScreenDuration + onScreenStart
            ) {
                if (isOnScreenVertically) PriorityFrame.PRIORITY_SUPER
                else PriorityFrame.PRIORITY_MEDIUM
            } else {
                if (isOnScreenVertically) PriorityFrame.PRIORITY_HIGH
                else PriorityFrame.PRIORITY_LOW
            }
            return mutableSetOf(PriorityFrame(convertToSegmentVideo.resource.resourceFile, 0, priority, true))
        }

        val firstFrameTargetTime =
            (slot.startTime/1000 - convertToSegmentVideo.timeClipStart/1000 / convertToSegmentVideo.avgSpeed())
        var currFrameTargetTime = firstFrameTargetTime
        while (true) {
            val tmpTime = currFrameTargetTime + frameTargetDuration
            if (tmpTime < targetStart) {
                currFrameTargetTime = tmpTime
            } else {
                break
            }
        }

        val targetTimes = mutableListOf<Float>()
        targetTimes.add(currFrameTargetTime)

        var lastFrameTargetTime = currFrameTargetTime
        while (true) {
            val tmpTime = lastFrameTargetTime + frameTargetDuration
            if (tmpTime > targetEnd) {
                break
            } else {
                lastFrameTargetTime = tmpTime
                targetTimes.add(lastFrameTargetTime)
            }
        }

        val requests = mutableSetOf<PriorityFrame>()
        val path = FrameLoader.getFrameLoadPath(slot)
        // 优先级：屏上轨道屏上帧 > 屏上轨道离屏帧 > 屏外轨道屏上时间内的帧 > 屏外轨道屏上时间外的帧
        // 屏上轨道指的是轨道的y轴坐标在父View显示区域内的
        targetTimes.forEach { targetTime ->
            // 优先加载需要在屏上展示的帧
            val onScreenHorizontally = when {
                targetTime + frameTargetDuration < onScreenStart -> false
                targetTime - frameTargetDuration > onScreenStart + onScreenDuration -> false
                else -> true
            }

            val priority = if (isOnScreenVertically) {
                if (onScreenHorizontally) {
                    PriorityFrame.PRIORITY_SUPER
                } else {
                    PriorityFrame.PRIORITY_HIGH
                }
            } else {
                if (onScreenHorizontally) {
                    PriorityFrame.PRIORITY_MEDIUM
                } else {
                    PriorityFrame.PRIORITY_LOW
                }
            }

            val sourceTime = ((targetTime - firstFrameTargetTime) * convertToSegmentVideo.avgSpeed()).toInt()
            var pts = FrameLoader.accurateToSecond(sourceTime)
            if (pts > convertToSegmentVideo.resource.duration/1000)
                pts = (convertToSegmentVideo.resource.duration/1000 ).toInt()
            requests.add(PriorityFrame(path, pts, priority, false))
        }

        return requests
    }

    override fun onLoadFinished(key: CacheKey) {
        segmentParams.forEach { (segment, params) ->
            val itemView = params.holder.getView() as? VideoItemView
                ?: return@forEach
            lastRequests[segment]?.firstOrNull { it.key == key }?.let {
                if (itemView.isItemSelected) {
                    trackGroup.postInvalidate()
                } else {
                    itemView.postInvalidate()
                }
            }
        }
    }
}
