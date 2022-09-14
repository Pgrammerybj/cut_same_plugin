package com.ss.ugc.android.editor.track.fuctiontrack.video

import android.content.Context
import android.graphics.Bitmap
import com.ss.ugc.android.editor.base.theme.ThemeStore
import com.ss.ugc.android.editor.track.fuctiontrack.BaseTrackItemHolder
import com.ss.ugc.android.editor.track.utils.SizeUtil
import com.ss.ugc.android.editor.track.widget.TrackConfig

internal class VideoItemHolder(
    context: Context,
    private val frameCallback: FrameCallback
) : BaseTrackItemHolder<VideoItemView>(context) {

    override val itemView: VideoItemView = VideoItemView(context).also {
        it.setFrameFetcher { path, timestamp ->
            frameCallback.getFrameBitmap(path, timestamp)
        }
    }

    override fun setClipping(clipping: Boolean) {
        super.setClipping(clipping)
        if (clipping) frameCallback.refreshFrameCache()
    }

    override fun setTimelineScale(timelineScale: Float) {
        super.setTimelineScale(timelineScale)
        frameCallback.refreshFrameCache()
    }

    override fun reset() {
        super.reset()
        itemView.updateLabelType(VideoItemView.LabelType.NONE)
    }

    companion object {
        val ITEM_HEIGHT = if (ThemeStore.getCustomViceTrackItemFrameHeight() != null)
            ThemeStore.getCustomViceTrackItemFrameHeight()!!
        else
            SizeUtil.dp2px(40F)
        val ITEM_WIDTH = if (ThemeStore.getCustomViceTrackItemFrameWidth() != null)
            ThemeStore.getCustomViceTrackItemFrameWidth()!!
        else
            TrackConfig.THUMB_WIDTH

        val BITMAP_TOP =
            if (ThemeStore.getCustomViceTrackItemFrameHeight() != null && (TrackConfig.THUMB_HEIGHT - ITEM_HEIGHT) / 2 < 0)
                0
            else
                (TrackConfig.THUMB_HEIGHT - ITEM_HEIGHT) / 2

        val BITMAP_BOTTOM =
            if (ThemeStore.getCustomViceTrackItemFrameHeight() != null && (TrackConfig.THUMB_HEIGHT - ITEM_HEIGHT) / 2 < 0)
                ThemeStore.trackUIConfig.viceTrackItemFrameHeight!!
            else
                TrackConfig.THUMB_HEIGHT - BITMAP_TOP

    }

    interface FrameCallback {
        fun getFrameBitmap(path: String, timestamp: Int): Bitmap?
        fun refreshFrameCache()
    }
}
