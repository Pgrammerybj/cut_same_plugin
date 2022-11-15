package com.ss.ugc.android.editor.core

import android.text.TextUtils
import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.core.publicUtils.MediaUtil
import com.ss.ugc.android.editor.core.publicUtils.VideoMetaDataInfo
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.math.max


fun NLETrack.getVETrackType(): String {
    return getExtra(Constants.TRACK_TYPE)
}

fun NLETrack.setVETrackType(trackType: String) {
    setExtra(Constants.TRACK_TYPE, trackType)
}

/**
 * 记录保存的track的数量
 */
fun NLEModel.setTrackLayer(count: Int) {
    setExtra(Constants.TRACK_LAYER, count.toString())
}

fun NLEModel.trackMaxLayer(): Int {
    return if (TextUtils.isEmpty(getExtra(Constants.TRACK_LAYER))) {
        0
    } else {
        getExtra(Constants.TRACK_LAYER).toInt()
    }
}

fun NLEEditorContext.setExtra(extraKey: String, extraVal: String) {
    getNLEModel().setExtra(extraKey, extraVal)
}


fun NLEModel.getAddTrackLayer(trackType: String): Int {
    if (cover?.enable == true) {
        return getMaxLayer(trackType) + 1
    }
    var trackLayer = getMaxLayer(trackType) + 1
    getShouldAddTrack(trackType)?.apply {
        removeTrack(this) // 如果是空的track 先remove掉 再新添同layer的track
        trackLayer = this.layer
    }
    return trackLayer
}

fun NLEModel.getAddTrackAudioLayer(startTime: Long): Int {
    val layerMap = mutableMapOf<Int, MutableList<NLETrackSlot>>()
    tracks.filter {
        !it.mainTrack && it.getVETrackType() == Constants.TRACK_AUDIO
    }.forEach { track ->
        if (layerMap[track.layer] == null) {
            layerMap[track.layer] = mutableListOf()
        }
        layerMap[track.layer]?.addAll(track.sortedSlots)
    }

    var targetLayer = 0

    kotlin.run outside@{
        //排个顺序
        layerMap.toSortedMap { o1, o2 -> (o1 - o2) }.forEach track@{
            var find = true
            it.value.forEach slot@{ slot ->
                if (slot.startTime >= startTime || slot.endTime > startTime) {
                    // 这个 layer 不行
                    find = false
                }
            }
            if (find) {
                return@outside
            }
            targetLayer++
        }
    }

    return max(0, targetLayer)
}

fun NLEModel.getAddFilterTrack(seqIn: Long, seqOut: Long, filterType: String): NLETrack? {

    for (index in 0..100) {
        val tracks = tracks.filter {
            !it.mainTrack && it.getVETrackType() == filterType && it.layer == index
        }
        if (tracks.isEmpty()) { //说明没有此layer的track 直接新建并return
            return NLETrack().apply {
                layer = index
            }
        }
        val slots = mutableListOf<NLETrackSlot>()
        tracks.forEach { track ->
            track.slots.filter { slot ->
                (slot.startTime <= seqIn && seqIn <= slot.endTime) ||
                        slot.startTime <= seqOut && seqOut <= slot.endTime ||
                        seqIn <= slot.startTime && seqOut >= slot.endTime
            }.forEach {
                slots.add(it)
            }
        }
        if (slots.isEmpty()) { //说明track可以放下要添加的slot
            return NLETrack().apply {
                layer = index
            }
        }
    }
    return null
}

fun NLEModel.getRecordSegmentCount(key: String): Int {
    val list = mutableListOf<NLETrackSlot>()
    var number = 0
    tracks.filter {
        !it.mainTrack && it.getVETrackType() == Constants.TRACK_AUDIO
    }.forEach { track ->
        track.sortedSlots.forEach {
            if (it.mainSegment.resource.resourceType == NLEResType.RECORD) {
                list.add(it)
                number = max(number, it.mainSegment.resource.resourceName.removePrefix(key).toInt())
            }
        }
    }
    return number
}

fun NLESegment.getVolume(): Float {
    return NLESegmentVideo.dynamicCast(this)?.volume
        ?: NLESegmentAudio.dynamicCast(this)?.volume ?: 0.0f
}

fun NLETrackSlot.isMute(): Boolean {
    return mainSegment.getVolume() == 0.0f
}

fun NLESegment.setVolume(volume: Float) {
    NLESegmentVideo.dynamicCast(this)?.volume = volume
    NLESegmentAudio.dynamicCast(this)?.volume = volume
}

fun NLESegment.setAlpha(alpha: Float) {
    NLESegmentVideo.dynamicCast(this)?.alpha = alpha
}

/**
 * 设置轨道和slot上的滤镜 保存是哪一个
 */
fun NLEModel.setFilterPosition(filterType: String, position: Int) {
    setExtra(filterType, position.toString())
}

fun NLETrackSlot.setFilterPosition(filterType: String, position: Int) {
    setExtra(filterType, position.toString())
}


// 获取目前已添加了多少个全局调节的slot
fun NLEModel.getAdjustSegmentCount(): Int {
    mutableListOf<NLETrackSlot>()
    var number = 0
    tracks.filter {
        !it.mainTrack && it.getVETrackType() == Constants.TRACK_FILTER_ADJUST
    }.forEach { track ->
        track.sortedSlots.forEach {
            number++
        }
    }
    return number
}

fun NLETrackSlot.isTextSticker(): Boolean {
    return NLESegmentTextSticker.dynamicCast(this.mainSegment) != null
}

fun NLETrackSlot.isInfoSticker(): Boolean {
    return NLESegmentInfoSticker.dynamicCast(this.mainSegment) != null
}

fun Long.toMilli(): Long {
    return TimeUnit.MICROSECONDS.toMillis(this)
}

fun Long.toMicro(): Long {
    return TimeUnit.MILLISECONDS.toMicros(this)
}

/**
 * 获取当前track类型中的 layer的最大值
 *  trackType = TRACK_VIDEO / TRACK_AUDIO / TRACK_STICKER
 */
fun NLEModel.getMaxLayer(trackType: String): Int {
    var maxLayer = -1
    val realTracks = if (cover?.enable == true) {
        cover.tracks
    } else {
        tracks
    }
    return realTracks.filter {
        !it.mainTrack && it.getVETrackType() == trackType
    }.forEach {
        maxLayer = if (it.layer > maxLayer) it.layer else maxLayer
    }.let {
        maxLayer
    }
}

fun NLEModel.getShouldAddTrack(trackType: String): NLETrack? {
    return tracks.filter {
        !it.mainTrack && it.getVETrackType() == trackType
    }.sortedBy {
        it.layer
    }.forEach {
        if (it.slots.size == 0) {
            return it
        }
    }.let {
        null
    }
}

fun NLETrackSlot.inMainTrack(nleModel: NLEModel) = nleModel.getTrackBySlot(this)?.mainTrack ?: false

fun NLETrackSlot.track(nleModel: NLEModel): NLETrack? = nleModel.getTrackBySlot(this)

fun NLETrack?.isTextTrack(): Boolean {
    this?.apply {
        return resourceType == NLEResType.TEXT_STICKER
    }
    return false
}


fun fileInfo(path: String, nleType: NLEResType): VideoMetaDataInfo {
    val mediaType = when (nleType) {
        NLEResType.VIDEO, NLEResType.IMAGE -> NLEResType.VIDEO.swigValue()
        else -> 0
    }
    return fileInfo(path)
}

fun fileInfo(path: String): VideoMetaDataInfo {
    return MediaUtil.getRealVideoMetaDataInfo(path)
}