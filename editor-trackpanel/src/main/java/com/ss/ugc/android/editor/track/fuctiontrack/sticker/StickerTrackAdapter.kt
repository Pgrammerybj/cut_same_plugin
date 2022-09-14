package com.ss.ugc.android.editor.track.fuctiontrack.sticker

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

class StickerTrackAdapter(
    private val activity: AppCompatActivity,
    trackGroup: TrackGroup,
    container: HorizontalScrollContainer,
    playController: PlayController,
    frameDelegate: KeyframeStateDelegate
) : BaseTrackAdapter(trackGroup, container, playController, frameDelegate) {


    override fun createHolder(parent: ViewGroup, index: Int): TrackItemHolder =
        StickerItemHolder(parent.context)


    override fun onTrackDoubleClick(slot: NLETrackSlot) {
        if (slot.mainSegment.type == NLEResType.STICKER) {
            selectSegment(slot)
//            stickerUIViewModel.showTextPanelEvent.value = EmptyEvent()
            val map = mapOf(
                "type" to "time_line_text",
                "edit_type" to "edit",
                "click_from" to "edit"
            )
//            ReportManager.onEvent("click_text", map)
        }
    }

    override fun onMove(
        fromTrackIndex: Int,
        toTrackIndex: Int,
        slot: NLETrackSlot,
        offsetInTimeline: Long,
        currPosition: Long
    ) {
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
}
