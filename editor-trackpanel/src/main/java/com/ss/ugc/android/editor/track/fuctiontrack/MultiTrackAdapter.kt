package com.ss.ugc.android.editor.track.fuctiontrack

import android.graphics.Canvas
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.bytedance.ies.nle.editor_jni.NLETrackType
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.track.TrackPanel
import com.ss.ugc.android.editor.track.fuctiontrack.adjust.AdjustTrackAdapter
import com.ss.ugc.android.editor.track.fuctiontrack.audio.AudioTrackAdapter
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.KeyframeStateDelegate
import com.ss.ugc.android.editor.track.fuctiontrack.sticker.StickerTrackAdapter
import com.ss.ugc.android.editor.track.fuctiontrack.video.MuxerTrackAdapter
import com.ss.ugc.android.editor.track.fuctiontrack.videoEffect.EffectTrackAdapter
import com.ss.ugc.android.editor.track.widget.HorizontalScrollContainer

class MultiTrackAdapter(
    private val activity: AppCompatActivity,
    private val trackPanel: TrackPanel,
    trackGroup: TrackGroup,
    container: HorizontalScrollContainer,
    playController: PlayController,
    frameDelegate: KeyframeStateDelegate
) : BaseTrackAdapter(trackGroup, container, playController, frameDelegate) {
    private val trackList: ArrayList<TrackInfo> = arrayListOf()

    private val TAG = "MultiTrackAdapter"


    private var muxerTrackAdapter: MuxerTrackAdapter = MuxerTrackAdapter(
        activity,
        trackPanel,
        trackGroup,
        container, playController, frameDelegate
    )
    private var stickerTrackAdapter: StickerTrackAdapter = StickerTrackAdapter(
        activity,
        trackGroup,
        container, playController, frameDelegate
    )
    private var effectTrackAdapter: EffectTrackAdapter = EffectTrackAdapter(
        activity,
        trackGroup,
        container, playController, frameDelegate
    )
    private var adjustTrackAdapter: AdjustTrackAdapter = AdjustTrackAdapter(
        activity,
        trackGroup,
        container, playController, frameDelegate
    )
    var audioTrackAdapter: AudioTrackAdapter = AudioTrackAdapter(
        activity,
        trackGroup,
        container, playController, frameDelegate
    )


    fun setTrackList(list: ArrayList<TrackInfo>) {
        trackList.clear()
        trackList.addAll(list)
        //是否需要添加
        performStart()
        muxerTrackAdapter.isStopped = false
        stickerTrackAdapter.isStopped = false
        audioTrackAdapter.isStopped = false
        updateTracks(trackList, -1, true, null)
        startRequestSubVideoThumb()
    }

    private fun startRequestSubVideoThumb() {
        val subVideoParams: MutableMap<NLETrackSlot, TrackParams> = mutableMapOf()
        segmentParams.forEach { (nleTrackSlot, trackParams) ->
            trackList.forEach {
                if (it.layer == trackParams.trackIndex && it.trackType == NLETrackType.VIDEO) {
                    subVideoParams[nleTrackSlot] = trackParams
                }
            }

        }
        muxerTrackAdapter.startRequestSubVideoThumb(subVideoParams)
    }

    override fun onScrollChanged() {
        super.onScrollChanged()
        muxerTrackAdapter.onScrollChanged()
        audioTrackAdapter.onScrollChanged()
        stickerTrackAdapter.onScrollChanged()
    }


    override fun createHolder(parent: ViewGroup, index: Int): TrackItemHolder {
        return obtainAdapterByIndex(index).createHolder(parent, index)
    }

    override fun bindHolder(holder: TrackItemHolder, slot: NLETrackSlot, index: Int) {
        return obtainAdapterByIndex(index).bindHolder(holder, slot, index)

    }

    private fun obtainAdapterByIndex(index: Int): BaseTrackAdapter {
        if (index >= 0) {
            trackList.forEach {
                if (it.layer == index) {
                   return obtainAdapterByType(it.trackType)?:stickerTrackAdapter
                }
            }
        }
        return stickerTrackAdapter
    }

    private fun obtainAdapterByType(trackType: NLETrackType?): BaseTrackAdapter? {
        when (trackType) {
            NLETrackType.VIDEO -> {
                return muxerTrackAdapter
            }

            NLETrackType.AUDIO -> {
                return audioTrackAdapter

            }

            NLETrackType.STICKER -> {
                return stickerTrackAdapter
            }

            NLETrackType.EFFECT -> {
                return effectTrackAdapter
            }

            NLETrackType.FILTER -> {
                return adjustTrackAdapter
            }
        }
        return null
    }

    override fun onMove(
        fromTrackIndex: Int,
        toTrackIndex: Int,
        slot: NLETrackSlot,
        offsetInTimeline: Long,
        currPosition: Long
    ) {
        ILog.d(
            TAG,
            "onMove from $fromTrackIndex to index $toTrackIndex  offsetTime $offsetInTimeline currentTime $currPosition \n $slot"
        )
        trackPanel.trackPanelActionListener?.onMove(
            fromTrackIndex,
            toTrackIndex,
            slot,
            offsetInTimeline * 1000,
            currPosition * 1000
        )

    }

    override fun onClip(slot: NLETrackSlot, start: Long, timelineOffset: Long, duration: Long) {
        trackGroup.trackGroupActionListener?.onClip(slot, start, timelineOffset, duration)
    }


    override fun drawDecorate(canvas: Canvas) {
        muxerTrackAdapter.drawDecorate(canvas)
        audioTrackAdapter.drawDecorate(canvas)
        stickerTrackAdapter.drawDecorate(canvas)
    }

    fun startRecord(recording: Boolean, recordPosition: Long, recordLayer: Int, recordCount : Int) {
        audioTrackAdapter.onRecordStart(recording, recordPosition, recordLayer,recordCount)
    }

    fun updateRecordWavePoints(it: MutableList<Float>) {
        audioTrackAdapter.recordWavePoints = it
    }

    override fun getDesireHeight(trackCount: Int): Int {
        return audioTrackAdapter.getDesireHeight(trackCount)
    }

    override fun canMoveOutOfMainVideo(): Boolean {
        return obtainAdapterByType(trackPanel.currentSelectTrack()?.trackType)?.canMoveOutOfMainVideo()
            ?: false
    }
}