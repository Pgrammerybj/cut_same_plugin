package com.ss.ugc.android.editor.core.handler.real

import com.bytedance.ies.nle.editor_jni.*
import com.bytedance.ies.nleeditor.getMainSegment
import com.ss.android.vesdk.VEUtils
import com.ss.ugc.android.editor.core.*
import com.ss.ugc.android.editor.core.api.CommitLevel
import com.ss.ugc.android.editor.core.api.VariableKeys
import com.ss.ugc.android.editor.core.api.params.*
import com.ss.ugc.android.editor.core.handler.ITrackNLEHandler
import com.ss.ugc.android.editor.core.settings.KVSettingsManager
import com.ss.ugc.android.editor.core.settings.SettingsKey.FIXED_CANVAS_RATIO
import com.ss.ugc.android.editor.core.settings.SettingsKey.PICTURE_TRACK_TIME
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.EditorCoreUtil
import com.ss.ugc.android.editor.core.utils.GsonUtil
import java.util.concurrent.TimeUnit

/**
 * @desc 轨道操作
 */
class TrackNLEHandler(editorContext: IEditorContext) : BaseNLEHandler(editorContext),
    ITrackNLEHandler {

    override fun initMainTrack(selectedMedias: MutableList<EditMedia>) {
        val nleMainTrack: NLETrack = NLETrack().apply { mainTrack = true }
        nleMainTrack.setVETrackType(Constants.TRACK_VIDEO)
        nleMainTrack.extraTrackType = NLETrackType.VIDEO
        selectedMedias.apply {
            var canvasRatio: Float = 9F / 16F
            var hasSetCanvasRatio = false //标记位，设置默认视频比例取第一个视频
            var currentSlotStartTime = 0L
            forEachIndexed { index, media ->
                val videoInfo = fileInfo(media.path)
                val hasOriginAudio = VEUtils.getAudioFileInfoForAllTracks(media.path).isNotEmpty()
                nleMainTrack.addSlot(NLETrackSlot().also { slot ->
                    // 设置视频片段到槽里头
                    slot.mainSegment = NLESegmentVideo().also { segment ->
                        segment.avFile = NLEResourceAV().also { resource ->
                            resource.hasAudio = hasOriginAudio
                            resource.resourceType =
                                if (media.isVideo) NLEResType.VIDEO else NLEResType.IMAGE
                            resource.duration =
                                if (media.isVideo) TimeUnit.MILLISECONDS.toMicros(videoInfo.duration.toLong()) else TimeUnit.MILLISECONDS.toMicros(
                                    KVSettingsManager.get(PICTURE_TRACK_TIME, 4000L)
                                )
                            if (videoInfo.rotation == 90 || videoInfo.rotation == 270) {
                                resource.height = videoInfo.width.toLong()
                                resource.width = videoInfo.height.toLong()
                            } else {
                                resource.height = videoInfo.height.toLong()
                                resource.width = videoInfo.width.toLong()
                            }
                            if (resource.height != 0L && !hasSetCanvasRatio) {
                                canvasRatio = resource.width.toFloat() / resource.height
                                hasSetCanvasRatio = true
                            }
                            resource.resourceFile = media.path
                        }
                        segment.timeClipStart = 0
                        segment.timeClipEnd =
                            if (media.isVideo) TimeUnit.MILLISECONDS.toMicros(videoInfo.duration.toLong()) else TimeUnit.MILLISECONDS.toMicros(
                                KVSettingsManager.get(PICTURE_TRACK_TIME, 4000L)
                            )
                        segment.keepTone = true
                        EditorCoreUtil.setDefaultCanvasColor(segment)
                    }
                    slot.startTime = currentSlotStartTime
                    // currentSlotStartTime += slot.measuredEndTime
                    currentSlotStartTime += slot.duration
                    // 添加index Mapping,主轨都是0
                })
            }
            editorContext.nleModel.clearTrack() // TODO: 先全部清空，仅保持只有一条轨道
            // 添加轨道到model
            editorContext.nleModel.addTrack(nleMainTrack)
//            nleModel.mainTrack.setExtra(Constants.MAIN_TRACK_MUTE_ENABLE, "true") // 设置轨道区原声按钮enable
            val fixedRatio: CanvasRatio = KVSettingsManager.get(FIXED_CANVAS_RATIO, ORIGINAL)
            editorContext.nleModel.canvasRatio = when (fixedRatio) {
                is RATIO_9_16 -> 9F / 16F
                is RATIO_3_4 -> 3F / 4F
                is RATIO_1_1 -> 1F / 1F
                is RATIO_4_3 -> 4F / 3F
                is RATIO_16_9 -> 16F / 9F
                is ORIGINAL -> canvasRatio
            }
            val ratioVal = when (fixedRatio) {
                is RATIO_9_16 -> 9F / 16F
                is RATIO_3_4 -> 3F / 4F
                is RATIO_1_1 -> 1F / 1F
                is RATIO_4_3 -> 4F / 3F
                is RATIO_16_9 -> 16F / 9F
                is ORIGINAL -> canvasRatio
            }
            editorContext.setVariable(VariableKeys.CHANGE_RATIO_EVENT, ratioVal)
            val imageCover = NLEResourceNode().apply {
                resourceFile = ""
                resourceType = NLEResType.IMAGE
            }
            editorContext.nleModel.cover = NLEVideoFrameModel().apply {
                coverMaterial = NLEStyCanvas().apply {
                    type = NLECanvasType.VIDEO_FRAME
                    image = imageCover
                }
                videoFrameTime = 0L
                canvasRatio = 9F / 16F
                enable = false
            }
            nleEditor.commitDone()
        }
    }

    override fun closeOriVolume(commitLevel: CommitLevel?) {
        editorContext.getMainTrack().sortedSlots?.forEach { slot ->
            val segment = slot.getMainSegment<NLESegmentVideo>()
            val originVolume = segment?.volume ?: 0f
            slot.setExtra(Constants.SLOT_MUTE_VOLUME_KEY, originVolume.toString())
            segment?.volume = 0F
            slot.keyframes.forEach { keyframe ->
                val keyframeOriVolume = keyframe.getMainSegment<NLESegmentVideo>()?.volume ?: 0f
                keyframe.setExtra(Constants.SLOT_MUTE_VOLUME_KEY, keyframeOriVolume.toString())
                DLog.d("muteVolume open save keyframe volume $keyframeOriVolume slot ${keyframe.uuid}")
                keyframe.getMainSegment<NLESegmentVideo>()?.volume = 0f
            }
        }
        executeCommit(commitLevel)
    }
}