package com.ss.ugc.android.editor.track.utils

import com.bytedance.ies.nle.editor_jni.*


fun NLETrack.calculatePlayDuration(): Long {
    if (sortedSlots.isEmpty()) {
        return 0L
    }
    var duration = 0L
    sortedSlots.forEach {
        kotlin.run {
            val convertToSegmentVideo = NLESegmentVideo.dynamicCast(it.mainSegment)
            duration += convertToSegmentVideo.duration / 1000
        }
    }

    return duration
}

fun NLEModel.getTrackByVideoEffect(slot:NLETrackSlot) :NLETrack? {
    this.tracks.forEach {
        if (it.trackType == NLETrackType.VIDEO) {
            it.videoEffects.forEach { effect->
                if (effect.name == slot.name) {
                    return it
                }

            }
        } else if (it.trackType == NLETrackType.EFFECT) {
            it.slots.forEach { effect->
                if (effect.name == slot.name) {
                    return it
                }

            }
        }
    }
    return null
}