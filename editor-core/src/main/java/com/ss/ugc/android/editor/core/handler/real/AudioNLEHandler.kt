package com.ss.ugc.android.editor.core.handler.real

import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.core.*
import com.ss.ugc.android.editor.core.api.CommitLevel
import com.ss.ugc.android.editor.core.api.params.AudioParam
import com.ss.ugc.android.editor.core.handler.IAudioNLEHandler
import com.ss.ugc.android.editor.core.publicUtils.MediaUtil
import com.ss.ugc.android.editor.core.utils.DLog
import com.ss.ugc.android.editor.core.utils.FileUtil
import java.util.concurrent.TimeUnit

/**
 * @desc: 音频+歌词贴纸
 */
class AudioNLEHandler(editorContext: IEditorContext) : BaseNLEHandler(editorContext), IAudioNLEHandler {

    override fun deleteAudioTrack(slot: NLETrackSlot?, commitLevel: CommitLevel?): Pair<NLETrack?, NLETrackSlot?> {
        val slotDeleted: NLETrackSlot?
        val trackRelated: NLETrack?
        if (slot == null) {
            slotDeleted = selectedNleTrackSlot
            trackRelated = selectedNleTrack
        } else {
            slotDeleted = slot
            trackRelated = editorContext.getNLEModel().getTrackBySlot(slot)
        }
        if (trackRelated?.nleTrackType() != NLETrackType.AUDIO) {
            DLog.w("selectedNleTrack is not audio track")
            return Pair(null, null)
        }
        trackRelated.let {
            it.removeSlot(slotDeleted)
            if (it.getExtra("AudioTrackType") == TrackType.BGM) {
                it.setExtra("AudioTrackType", TrackType.EDITOR_MUSIC)
            }
            if (!nleModel.hasExtra(Constants.MODEL_EXTRA_IS_AUDIO_DELETED)) { //埋点使用
                nleModel.setExtra(Constants.MODEL_EXTRA_IS_AUDIO_DELETED, true.toString())
            }

            // 删除音频片段的同时删除对应的歌词贴纸
            slotDeleted?.uuid?.let { uuid ->
                nleModel.tracks?.forEach { track ->
                    track.slots.forEach { slot ->
                        NLESegmentSubtitleSticker.dynamicCast(slot.mainSegment)?.let { it ->
                            if (TextUtils.equals(it.connectedAudioSlotUUID, uuid)) {
                                track.removeSlot(slot)
                                return@forEach
                            }
                        }
                    }
                }
            }
            executeCommit(commitLevel)
            return Pair(trackRelated, slotDeleted)
        }
        return Pair(null, null)
    }

    override fun addAudioTrack(
        param: AudioParam,
        autoSeekFlag: Int,
        commitLevel: CommitLevel?,
        slotCallBack: ((NLETrackSlot) -> Unit)?
    ): Boolean {
        val audioFileInfo = MediaUtil.getAudioFileInfo(param.audioPath)
        if (audioFileInfo == null) {
            DLog.w("Invalid audio file path")
            return false
        }
        var audioSlotUUID : String?
        val duration = audioFileInfo.duration
        DLog.d("record trace add audio record $param   $duration")
        val addTargetTrackLayer = nleModel.getAddTrackAudioLayer(param.startTime)
        nleModel.tracks.firstOrNull() {
            !it.mainTrack && it.nleTrackType() == NLETrackType.AUDIO && it.layer == addTargetTrackLayer && it.getExtra(
                "AudioTrackType"
            ) != TrackType.ORIGIN
        }?.let { track ->
            //创建一个slot
            val slot = createTrackSlot(param, duration, slotCallBack)
            audioSlotUUID = slot.uuid
            track.addSlot(slot)
            editorContext.getMainTrack().timeSort()

            if (!param.srtPath.isNullOrEmpty() && audioSlotUUID != null) {
                addSubtitleSticker(audioSlotUUID!!, param, duration)
            }
            executeCommit(commitLevel)
        } ?: kotlin.run {
            NLETrack().apply {
                mainTrack = false
                layer = addTargetTrackLayer
                Log.d("audio-record", "addAudioTrack $layer")
                setVETrackType(Constants.TRACK_AUDIO)
                extraTrackType = NLETrackType.AUDIO
                setExtra(Constants.MUSIC_NAME, param.audioName)
                //创建一个slot
                val slot = createTrackSlot(param, duration, slotCallBack)
                audioSlotUUID = slot.uuid
                this.addSlot(slot)
                nleModel.addTrack(this)
                editorContext.getMainTrack().timeSort()

                if (!param.srtPath.isNullOrEmpty() && audioSlotUUID != null) {
                    addSubtitleSticker(audioSlotUUID!!, param, duration)
                }
                executeCommit(commitLevel)
            }
        }

        DLog.d("record trace seek time  ${param.startTime + duration * 1000}")
        //添加音轨后，处于prepare状态，需要seek
        player.seekToPosition(
            TimeUnit.MICROSECONDS.toMillis(if (autoSeekFlag == 0) param.startTime else param.startTime + duration * 1000),
            ifMoveTrack = param.isMoveTrack
        )
        return true
    }

    private fun createTrackSlot(param: AudioParam, duration: Int, slotCallBack: ((NLETrackSlot) -> Unit)?): NLETrackSlot {
        val slot = NLETrackSlot().also { slot ->
            // 设置音频片段到槽里头
            slot.mainSegment = NLESegmentAudio().also { segment ->
                segment.avFile = NLEResourceAV().also { resource ->
                    resource.resourceName = param.audioName  //音乐名称
                    resource.resourceType = when {
                        param.isFromRecord -> {
                            NLEResType.RECORD
                        }
                        param.isAudioEffect -> {
                            NLEResType.SOUND
                        }
                        else -> {
                            NLEResType.AUDIO
                        }
                    }
                    resource.duration = TimeUnit.MILLISECONDS.toMicros(duration.toLong())
                    resource.resourceFile = param.audioPath
                }
                segment.timeClipStart = param.timeClipStart
                segment.timeClipEnd =
                    if (param.timeClipEnd == 0L) TimeUnit.MILLISECONDS.toMicros(duration.toLong()) else param.timeClipEnd
                segment.keepTone = true
            }
            param.extraId?.let {
                slot.setExtra(Constants.VOICE_HAS_TEXT_STICKER, it)
            }
            slot.setExtra(Constants.MUSIC_NAME, param.audioName)
            slot.startTime = param.startTime
            if (param.endTime != 0L) {
                slot.endTime = param.endTime
            }
            //为了添加音轨后 执行trackPanel.selectSlot
            if (!param.isFromRecord && param.notifySlotChanged) {
                editorContext.setVariable(
                    Constants.SELECTED_AUDIO_TRACK_SLOT,
                    slot,
                    true
                )
            }
            slotCallBack?.invoke(slot)
        }
        return slot
    }

    private fun addSubtitleSticker(audioSlotUUID: String, param: AudioParam, duration: Int) {
        val slot = NLETrackSlot().apply {
            layer = nleModel.layerMax + 1
            startTime = param.startTime
            endTime = if (param.endTime != 0L) {
                param.endTime
            } else {
                startTime + TimeUnit.MILLISECONDS.toMicros(duration.toLong())
            }

            mainSegment = NLESegmentSubtitleSticker().apply {
                timeClipStart = param.timeClipStart
//                timeClipEnd = endTime
                timeClipEnd = if (param.timeClipEnd == 0L) TimeUnit.MILLISECONDS.toMicros(duration.toLong()) else param.timeClipEnd
                // 歌词内容，注意resourceFile 字段传入的是字符串
                srtFile = NLEResourceNode().apply {
                    resourceFile = FileUtil.readJsonFile(param.srtPath)
                    resourceType = NLEResType.SRT
                }
                // 歌词样式，resourceFile 传入的是歌词样式在SD卡上的路径
                effectSDKFile = NLEResourceNode().apply {
                    resourceFile = LocalSubtitleStickerPath.DEFAULT_SUBTITLE_PATH
                    resourceType = NLEResType.SUBTITLE_STICKER
                }
                // 文字样式，可以通过修改NLEStyleText 修改文字颜色&字体
                style = NLEStyText().apply {
                    // 文字颜色
                    textColor = Color.parseColor("#FFFFFFFF").toLong()
                    // 字体，resourceFile 传入的是字体目录在SD卡上的路径
                    font = NLEResourceNode().apply {
                        resourceFile = LocalSubtitleStickerPath.DEFAULT_FONT_PATH
                        resourceType = NLEResType.FONT
                    }
                }
                connectedAudioSlotUUID = audioSlotUUID
            }

            transformX = 0.03990507125854492f
            transformY = -0.1528027057647705f
            rotation = 0f
            mirror_X = false
            mirror_Y = false
        }
        nleModel.addTrack(NLETrack().apply {
            setVETrackType(Constants.TRACK_SUBTITLE_STICKER)
            layer = nleModel.getAddTrackLayer(Constants.TRACK_SUBTITLE_STICKER)
            extraTrackType = NLETrackType.STICKER
            addSlot(slot)
        })
    }
}