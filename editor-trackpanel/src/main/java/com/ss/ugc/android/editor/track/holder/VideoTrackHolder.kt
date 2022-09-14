package com.ss.ugc.android.editor.track.holder

import android.graphics.Bitmap
import androidx.lifecycle.Observer
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLETrack
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.base.logger.ILog
import com.ss.ugc.android.editor.track.TrackPanel
import com.ss.ugc.android.editor.track.frame.MainVideoFrameRequest
import com.ss.ugc.android.editor.track.widget.ItemClipCallback
import com.ss.ugc.android.editor.track.widget.ItemTrackLayout
import com.ss.ugc.android.editor.track.widget.MultiTrackLayout
import com.ss.ugc.android.editor.track.widget.TrackConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

/**
 *  author : wenlongkai
 *  date : 2019/4/24 下午8:11
 *  description :
 */
class VideoTrackHolder(
//    activity: AppCompatActivity,
    private val trackPanel: TrackPanel,
    private val multiTrack: MultiTrackLayout
) : CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()


    private var cacheRequest = MainVideoFrameRequest(multiTrack)


    private val playPositionObserver: Observer<Long> by lazy {
        Observer<Long> {
            multiTrack.onPlayPositionChanged()
        }
    }

    init {
        trackPanel.addFrameRequest(cacheRequest)


        val clipCallback = object : ItemClipCallback {
            override fun startClip(segment: NLETrackSlot, index: Int) {
                cacheRequest.clipIndex = index
                trackPanel.refreshFrameCache()
//                mainVideoViewModel.lockIndex(segment.id)
            }

            override fun clipStateChanged(
                slot: NLETrackSlot,
                side: Int,
                start: Int,
                duration: Int,
                index: Int,
                offset: Float
            ) {
                cacheRequest.clipIndex = index
                cacheRequest.clipOffset = offset
                cacheRequest.clipSide = side
                val videoSegment = NLESegmentVideo.dynamicCast(slot.mainSegment)
                val position = if (side == ItemTrackLayout.LEFT) {
                    (slot.startTime / 1000 + (start - videoSegment.timeClipStart / 1000)).toInt()
                } else {
                    (slot.startTime / 1000 + (start + duration - videoSegment.timeClipStart / 1000) - 1).toInt()
                }
//                mainVideoViewModel.indexSeekTo(index, position)
            }

            override fun stopClip() {
                cacheRequest.clipIndex = MainVideoFrameRequest.INVALID_CLIP_INDEX
                cacheRequest.clipOffset = 0F
                cacheRequest.clipSide = MainVideoFrameRequest.INVALID_CLIP_SIDE
                trackPanel.refreshFrameCache()
//                mainVideoViewModel.unlockIndex()
            }
        }
        val reactCallback = object : MultiTrackLayout.ReactCallback {
            override fun addTailLeader() {

            }

            override fun getMusicBeats(): List<Long> {
                return emptyList<Long>()
            }

            override fun getPlayPosition(): Long {
                return 0L
            }

            override fun getFrameBitmap(path: String, timestamp: Int): Bitmap? =
                trackPanel.getFrameBitmap(path, timestamp)
        }
        multiTrack.setCallback(clipCallback, reactCallback)
    }

    private var lastCacheRefreshScrollX = 0

    var trackStage: Long? = -1L

    fun reloadVideoTrack(track: NLETrack, forceRefresh: Boolean = false) {
        ILog.d(TAG, "track.stage = ${track.stage}, trackStage = $trackStage" )
        if (track.stage?.id == trackStage && !forceRefresh) {
            ILog.d(TAG, "same stage no need to refresh $trackStage ")
            return
        }
        trackStage = track.stage?.id
        reloadVideoTrack(track.sortedSlots)
    }


    private fun reloadVideoTrack(segments: List<NLETrackSlot>) {
        ILog.i(TAG, "start reload video track")
        multiTrack.init(segments)
        cacheRequest.slots = segments
        trackPanel.refreshFrameCache()
        ILog.i(TAG, "end reload video track")
    }

    fun updateScrollX(
        scrollX: Int,
        onlyRefreshFile: Boolean = false,
        mustRefresh: Boolean = false
    ) {
        multiTrack.updateScrollX(scrollX)
        if (abs(scrollX - lastCacheRefreshScrollX) >= CACHE_REFRESH_THRESHOLD || mustRefresh) {
            trackPanel.refreshFrameCache(onlyRefreshFile)
            lastCacheRefreshScrollX = scrollX
        }
    }

    fun setScaleSize(scale: Double) {
        multiTrack.setScaleSize(scale)
        trackPanel.refreshFrameCache()
    }

    fun selectSlot(
        index: Int,
        forceSelect: Boolean = false,
        onlyRefreshUI: Boolean = false
    ) {
        multiTrack.selectSlot(index, forceSelect, onlyRefreshUI)
    }

    fun destroy() {
        coroutineContext[Job]?.cancel()
    }

    companion object {
        const val TAG = "VideoTrackHolder"
        private val CACHE_REFRESH_THRESHOLD = 5 * TrackConfig.THUMB_WIDTH
    }

    fun getMultiTrackLayout():MultiTrackLayout{
        return this.multiTrack
    }
}

