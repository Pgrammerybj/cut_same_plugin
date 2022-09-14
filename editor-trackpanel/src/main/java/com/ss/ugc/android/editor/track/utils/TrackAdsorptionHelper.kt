package com.ss.ugc.android.editor.track.utils

import com.bytedance.ies.nle.editor_jni.NLEModel
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.bytedance.ies.nle.editor_jni.NLETrackType
import com.ss.ugc.android.editor.track.widget.HorizontallyState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by biyun
 * on 2019-07-09.
 */
object TrackAdsorptionHelper {

    var scale = 1.0
    private val MIN_ADSORPTION_INTERVAL
        get() = 100 / scale

    private var currentTimeList = mutableListOf<Long>()

    fun updateTimePoint(
        playHeadTime: Long,
        slot: NLETrackSlot,
        nleModel: NLEModel
    ) {
        currentTimeList = calcTimePoint(playHeadTime, slot, nleModel)
    }

    fun clearTimePoint() {
        currentTimeList.clear()
    }

    fun doAdsorptionOnClip(touchPosition: Long, handlePosition: Long): Long? {
        currentTimeList.forEach { time ->
            (time - touchPosition).apply {
                if (abs(this) < MIN_ADSORPTION_INTERVAL) {
                    return time - handlePosition
                }
            }
        }
        return null
    }

    fun doAdsorptionOnDrag(
        dragState: HorizontallyState,
        startTime: Long,
        endTime: Long,
        currentStartTime: Long,
        currentEndTime: Long
    ): Long {
        var deltaStart = Long.MIN_VALUE
        var deltaEnd = Long.MIN_VALUE

        currentTimeList.filter {
            if (dragState == HorizontallyState.LEFT) {
                it <= currentStartTime
            } else {
                it >= currentStartTime
            }
        }.forEach foreach@{ time ->
            if (abs(time - startTime) < MIN_ADSORPTION_INTERVAL) {
                deltaStart = time - currentStartTime
                return@foreach
            }
        }

        currentTimeList.filter {
            if (dragState == HorizontallyState.LEFT) {
                it <= currentEndTime
            } else {
                it >= currentEndTime
            }
        }.forEach foreach@{ time ->
            if (abs(time - endTime) < MIN_ADSORPTION_INTERVAL) {
                deltaEnd = time - currentEndTime
                return@foreach
            }
        }

        return if (deltaStart != Long.MIN_VALUE && deltaEnd != Long.MIN_VALUE) {
            min(deltaStart, deltaEnd)
        } else {
            max(deltaStart, deltaEnd)
        }
    }

    private fun calcTimePoint(
        playHeadTime: Long,
        slot: NLETrackSlot,
        nleModel: NLEModel
    ): MutableList<Long> {
        // 找到该segment的track类型

        val trackBySlot = nleModel.getTrackBySlot(slot)
        val timeList = mutableListOf<Long>()
        trackBySlot?.let {
            val trackType = trackBySlot.trackType
            nleModel.tracks?.forEach findSegment@{ track ->
                if (track.trackType == NLETrackType.VIDEO) {
                    track.slots.filterNot { it == slot }.forEach { segment ->
                        timeList.add(segment.startTime / 1000)
                        timeList.add(segment.measuredEndTime / 1000)
                    }
                } else if (track.trackType == trackType) {
                    track.slots.filterNot { it == slot }
                        .forEach { segment ->
                            timeList.add(segment.startTime / 1000)
                            timeList.add(segment.measuredEndTime / 1000)
                        }
                }
            }
            timeList.add(playHeadTime)

            var lastItem = -1L
            for (i in (timeList.size - 1) downTo 0) {
                val item = timeList[i]
                if (item == lastItem) {
                    timeList.removeAt(i)
                }
                lastItem = item
            }
        }

        timeList.sort()
        return timeList
    }
}
