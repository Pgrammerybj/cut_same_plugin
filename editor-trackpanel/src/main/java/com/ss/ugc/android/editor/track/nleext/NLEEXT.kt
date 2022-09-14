package com.ss.ugc.android.editor.track.nleext

import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.core.avgSpeed

fun NLETrackSlot.avgSpeed() : Float {
    val nLESegmentVideo = NLESegmentVideo.dynamicCast(mainSegment)
    return nLESegmentVideo?.avgSpeed() ?: 1.0f
}


