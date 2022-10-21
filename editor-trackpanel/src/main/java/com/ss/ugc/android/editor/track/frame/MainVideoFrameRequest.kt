package com.ss.ugc.android.editor.track.frame

import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.core.avgSpeed
import com.ss.ugc.android.editor.track.TrackSdk
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.widget.ItemTrackLayout
import com.ss.ugc.android.editor.track.widget.MultiTrackLayout
import com.ss.ugc.android.editor.track.widget.TrackConfig
import com.ss.ugc.android.editor.track.widget.TrackConfig.PX_MS
import kotlin.math.max
import kotlin.math.min

class MainVideoFrameRequest(
    private val multiTrack: MultiTrackLayout
) : FrameRequest {
    private var lastRequests: MutableMap<NLETrackSlot, Set<PriorityFrame>> = mutableMapOf()
    var slots: List<NLETrackSlot> = mutableListOf()

    var clipOffset: Float = 0F
    var clipSide: Int =
        INVALID_CLIP_SIDE
    var clipIndex: Int =
        INVALID_CLIP_INDEX

    override fun getRequestFrames(): RequestInfo {
        val lastRequests = lastRequests.toMutableMap()

        if (slots.isEmpty()) {
            return RequestInfo(lastRequests.values.flatten(), emptyList(), emptyList())
        }

        val removed = mutableSetOf<PriorityFrame>()
        val add = mutableSetOf<PriorityFrame>()
        val all = mutableSetOf<PriorityFrame>()
        val newRequests = mutableMapOf<NLETrackSlot, Set<PriorityFrame>>()

        val scrollX = multiTrack.myScrollX
        val halfScreenWidth = SizeUtil.getScreenWidth(TrackSdk.application) / 2
        val onScreenStart = (scrollX - halfScreenWidth) / PX_MS
        val onScreenDuration = halfScreenWidth * 2 / PX_MS
        val frameTargetDuration = TrackConfig.THUMB_WIDTH / PX_MS

        slots.forEachIndexed { index, slot ->
            if (slot.mainSegment.type != NLEResType.VIDEO && slot.mainSegment.type != NLEResType.IMAGE) return@forEachIndexed

            val frames = getSegmentFrames(
                slot, index, onScreenStart, onScreenDuration, frameTargetDuration
            )

            val oldFrames = lastRequests.remove(slot)
            if (oldFrames == null) {
                add.addAll(frames)
            } else {
                removed.addAll(oldFrames - frames)
                add.addAll(frames - oldFrames)
            }
            all.addAll(frames)
            newRequests[slot] = frames
        }
        lastRequests.forEach { (_, frames) ->
            removed.addAll(frames)
        }
        this.lastRequests = newRequests
        return RequestInfo(removed.toMutableList(), add.toMutableList(), all.toMutableList())
    }

    private fun getSegmentFrames(
        slot: NLETrackSlot,
        index: Int,
        onScreenStart: Float,
        onScreenDuration: Float,
        frameTargetDuration: Float
    ): Set<PriorityFrame> {
        val segmentTargetStart = if (clipIndex != INVALID_CLIP_INDEX &&
            (clipSide == ItemTrackLayout.LEFT && index < clipIndex ||
                    clipSide == ItemTrackLayout.RIGHT && index > clipIndex)
        ) {
            slot.startTime/1000 + clipOffset / PX_MS
        } else {
            (slot.startTime/1000).toFloat()
        }
        var segmentStart: Float = segmentTargetStart
        // todo check duration
        var segmentEnd: Float = segmentTargetStart + slot.duration/1000

        val videoSeg = NLESegmentVideo.dynamicCast(slot.mainSegment)

        // 如果正在裁剪当前片段，则预加载更多的帧
        if (clipIndex == index) {
            if (clipSide == ItemTrackLayout.LEFT) {
                segmentStart = segmentTargetStart - videoSeg.timeClipStart / 1000/ (videoSeg.avgSpeed())
            } else {
                segmentEnd = segmentTargetStart + videoSeg.duration /1000
            }
        }
        val targetStart = max(segmentStart, onScreenStart - onScreenDuration)
        val targetEnd = min(segmentEnd, onScreenStart + 2 * onScreenDuration)
        val path = FrameLoader.getFrameLoadPath(slot)
        val isImage = slot.mainSegment.type == NLEResType.IMAGE

        // 始终需要加载第一帧，用于长按拖拽换位置
        val firstOnTimelineFramePts = if (isImage) 0 else {
            val acc = FrameLoader.accurateToSecond(videoSeg.timeClipStart.toInt()/1000)
            
            val resourceDuration =videoSeg.resource.duration.toLong()/1000
            if (acc > resourceDuration) {
                resourceDuration.toInt()
            } else {
                acc
            }
        }
        if (targetEnd <= targetStart) return mutableSetOf(/**/
            PriorityFrame(path, firstOnTimelineFramePts, PriorityFrame.PRIORITY_HIGH, isImage)
        )

        val onScreenEnd = onScreenStart + onScreenDuration
        if (isImage) {
            val priority = if (onScreenStart in targetStart..targetEnd ||
                onScreenEnd in targetStart..targetEnd ||
                targetStart in onScreenStart..onScreenEnd
            ) {
                PriorityFrame.PRIORITY_SUPER
            } else {
                PriorityFrame.PRIORITY_HIGH
            }
            return mutableSetOf(
                PriorityFrame(videoSeg.resource.resourceFile, firstOnTimelineFramePts, priority, true)
            )
        }

        val firstFrameTargetTime = segmentTargetStart - videoSeg.timeClipStart / 1000/(videoSeg.avgSpeed())
        var currFrameTargetTime = firstFrameTargetTime
        while (true) {
            val tmpTargetTime = currFrameTargetTime + frameTargetDuration
            if (tmpTargetTime < targetStart) {
                currFrameTargetTime = tmpTargetTime
            } else {
                break
            }
        }

        val targetTimes = mutableListOf<Float>()
        targetTimes.add(currFrameTargetTime.toFloat())

        var lastFrameTargetTime: Float = currFrameTargetTime
        while (true) {
            val tmpTargetTime = lastFrameTargetTime + frameTargetDuration
            if (tmpTargetTime > targetEnd) {
                break
            } else {
                lastFrameTargetTime = tmpTargetTime
                targetTimes.add(lastFrameTargetTime)
            }
        }

        val requests = mutableSetOf<PriorityFrame>()
        var addFirstFrame = false
        targetTimes.forEach {
            val onScreen = when {
                it + frameTargetDuration < onScreenStart -> false
                it - frameTargetDuration > onScreenEnd -> false
                else -> true
            }

            val priority =
                if (onScreen) PriorityFrame.PRIORITY_SUPER else PriorityFrame.PRIORITY_HIGH
            val sourceTime = ((it - firstFrameTargetTime) * videoSeg.avgSpeed()).toInt()
            var pts = FrameLoader.accurateToSecond(sourceTime)
            val resourceDuration = videoSeg.resource.duration /1000
            if (pts > resourceDuration) {
                pts = resourceDuration.toInt()
            }
            if (pts == firstOnTimelineFramePts) addFirstFrame = true
            requests.add(PriorityFrame(path, pts, priority, false))
        }
        if (!addFirstFrame) {
            requests.add(
                PriorityFrame(path, firstOnTimelineFramePts, PriorityFrame.PRIORITY_HIGH, false)
            )
        }
        return requests
    }

    override fun onLoadFinished(key: CacheKey) {
        lastRequests.forEach { (slot, frames) ->
//            frames.firstOrNull { it.path == key.path && it.timestamp == key.timestamp }?.let {
//                val index = ProjectUtil.projectInfo
//                    ?.videoTrack?.segments?.indexOfFirst { it == slot } ?: -1
//
//                multiTrack.refreshFrames(0)
                multiTrack.refreshAllSlotFrames()
//            }
        }
    }

    companion object {
        const val INVALID_CLIP_INDEX = -1
        const val INVALID_CLIP_SIDE = -1
    }
}
