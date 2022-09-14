package com.ss.ugc.android.editor.track.fuctiontrack

import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.bytedance.ies.nle.editor_jni.NLETrackType

data class TrackInfo(val layer : Int, val trackType : NLETrackType,  val slotList : List<NLETrackSlot>)