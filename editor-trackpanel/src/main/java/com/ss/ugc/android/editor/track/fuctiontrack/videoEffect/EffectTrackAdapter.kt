package com.ss.ugc.android.editor.track.fuctiontrack.videoEffect

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.ies.nle.editor_jni.NLEResType
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.track.fuctiontrack.BaseTrackAdapter
import com.ss.ugc.android.editor.track.fuctiontrack.PlayController
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.fuctiontrack.TrackItemHolder
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.KeyframeStateDelegate
import com.ss.ugc.android.editor.track.widget.HorizontalScrollContainer

class EffectTrackAdapter(
    private val activity: AppCompatActivity,
    trackGroup: TrackGroup,
    container: HorizontalScrollContainer,
    playController: PlayController,
    frameDelegate: KeyframeStateDelegate
) : BaseTrackAdapter(trackGroup, container, playController, frameDelegate) {


    override fun createHolder(parent: ViewGroup, index: Int): TrackItemHolder =
        EffectItemViewHolder(parent.context)


    override fun onTrackDoubleClick(slot: NLETrackSlot) {
        if (slot.mainSegment.type == NLEResType.EFFECT) {
            selectSegment(slot)
        }
    }

    override fun onMove(
        fromTrackIndex: Int,
        toTrackIndex: Int,
        slot: NLETrackSlot,
        offsetInTimeline: Long,
        currPosition: Long
    ) {
//        trackPanel.trackPanelActionListener?.onMove(fromTrackIndex,toTrackIndex,slot,offsetInTimeline * 1000,currPosition*1000)
//        viewModel.move(slot, fromTrackIndex, toTrackIndex, offsetInTimeline)
    }

    override fun onClip(
        slot: NLETrackSlot,
        start: Long,
        timelineOffset: Long,
        duration: Long
    ) {
        trackGroup.trackGroupActionListener?.onClip(slot, start, timelineOffset, duration)
    }

    override fun canMoveOutOfVideos(): Boolean = false

    override fun canMoveOutOfMainVideo(): Boolean {
        return false
    }

}
