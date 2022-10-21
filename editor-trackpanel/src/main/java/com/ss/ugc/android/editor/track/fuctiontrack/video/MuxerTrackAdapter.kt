package com.ss.ugc.android.editor.track.fuctiontrack.video

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.track.TrackPanel
import com.ss.ugc.android.editor.track.fuctiontrack.BaseTrackAdapter
import com.ss.ugc.android.editor.track.fuctiontrack.PlayController
import com.ss.ugc.android.editor.track.fuctiontrack.TrackGroup
import com.ss.ugc.android.editor.track.fuctiontrack.TrackInfo
import com.ss.ugc.android.editor.track.fuctiontrack.TrackItemHolder
import com.ss.ugc.android.editor.track.fuctiontrack.TrackParams
import com.ss.ugc.android.editor.track.fuctiontrack.keyframe.KeyframeStateDelegate
import com.ss.ugc.android.editor.track.widget.HorizontalScrollContainer
import com.ss.ugc.android.editor.track.widget.ItemTrackLayout.Companion.MAX_SUB_VIDEO_TRACK_NUM
import com.ss.ugc.android.editor.track.widget.ItemTrackLayout.Companion.MIN_VIDEO_DURATION_IN_MS
import kotlin.math.abs

class MuxerTrackAdapter(
    private val activity: AppCompatActivity,
    private val trackPanel: TrackPanel,
    trackGroup: TrackGroup,
    container: HorizontalScrollContainer,
    playController: PlayController,
    frameDelegate: KeyframeStateDelegate
) : BaseTrackAdapter(trackGroup, container, playController, frameDelegate) {

    private val TAG = "MuxerTrackAdapter"
    private val cacheRequest =
        MuxerFrameRequest(
            trackGroup,
            VideoItemHolder.ITEM_HEIGHT, ITEM_MARGIN
        ).also {
            trackPanel.addFrameRequest(it)

        }

    private var selectedHolder: TrackItemHolder? = null
        set(value) {
            if (field != value) {
                (field as? VideoItemHolder)?.getView()
                    ?.updateLabelType(VideoItemView.LabelType.NONE)
                (value as? VideoItemHolder)?.getView()?.updateLabelType(labelType)
            }
            field = value
        }

    private var labelType: VideoItemView.LabelType =
        VideoItemView.LabelType.NONE

    private var lastRefreshScrollX: Int = 0
    private var lastRefreshScrollY: Int = 0


    override fun performStop() {
        super.performStop()
        selectedHolder = null
        trackPanel.refreshFrameCache()
    }


    override fun updateTracks(
        tracks: List<TrackInfo>,
        requestOnScreenTrack: Int,
        refresh: Boolean,
        selectSegment: NLETrackSlot?
    ) {
        super.updateTracks(tracks, requestOnScreenTrack, refresh, selectSegment)
        if (!isStopped) {
            cacheRequest.updateData(segmentParams)
            trackPanel.refreshFrameCache()
        }
    }


    override fun updateSelected(data: Pair<NLETrackSlot, TrackParams>?, dataUpdate: Boolean) {
        super.updateSelected(data, dataUpdate)
        if (!dataUpdate) {
            requestSelectedItemOnScreen(data)
//            viewModel.setSelected(data?.first?.id)
        }
        selectedHolder = data?.second?.holder
    }

    override fun createHolder(parent: ViewGroup, index: Int): TrackItemHolder =
        VideoItemHolder(activity, object : VideoItemHolder.FrameCallback {
            override fun getFrameBitmap(path: String, timestamp: Int): Bitmap? {
                return trackPanel.getFrameBitmap(path, timestamp)
            }

            override fun refreshFrameCache() {
                trackPanel.refreshFrameCache()
            }
        })

    override fun onScrollChanged() {
        if (isStopped) return

        val refreshThresholdHorizontally = VideoItemHolder.ITEM_WIDTH / 2F
        val refreshThresholdVertically = VideoItemHolder.ITEM_HEIGHT / 2F
        if (abs(lastRefreshScrollX - trackGroup.scrollX) >= refreshThresholdHorizontally) {
            lastRefreshScrollX = trackGroup.scrollX
            trackPanel.refreshFrameCache()
        } else if (abs(lastRefreshScrollY - trackGroup.scrollY) >= refreshThresholdVertically) {
            lastRefreshScrollY = trackGroup.scrollY
            trackPanel.refreshFrameCache()
        }
    }

    override fun onMove(
        fromTrackIndex: Int,
        toTrackIndex: Int,
        slot: NLETrackSlot,
        offsetInTimeline: Long,
        currPosition: Long
    ) {
        ILog.d(TAG, "onMove ")
    }

    override fun onClip(
        slot: NLETrackSlot,
        start: Long,
        timelineOffset: Long,
        duration: Long
    ) {
        ILog.d(TAG, "onClip  $slot $start $timelineOffset $duration")
        trackGroup.trackGroupActionListener?.onClip(slot, start, timelineOffset, duration)
    }

    override fun getItemHeight() = VideoItemHolder.ITEM_HEIGHT

    override fun getMaxTrackNum(): Int = MAX_SUB_VIDEO_TRACK_NUM

    override fun getClipMinDuration() = MIN_VIDEO_DURATION_IN_MS.toLong()

    fun startRequestSubVideoThumb(subVideoParams: MutableMap<NLETrackSlot, TrackParams>) {
        cacheRequest.updateData(subVideoParams)
        trackPanel.refreshFrameCache()
    }
}
