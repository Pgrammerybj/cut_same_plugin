package com.ss.ugc.android.editor.core.api.params

/**
 * @param audioName 音频名称
 * @param audioPath 音频文件本地路径
 * @param playTime 播放时长
 */
data class AudioParam(
    var audioName: String,
    var audioPath: String,
    var audioId: String? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val isMoveTrack: Boolean = false,
    val isFromRecord: Boolean = false,
    val isAudioEffect: Boolean = false,
    val timeClipStart: Long = 0L,
    val timeClipEnd: Long = 0L,
    val extraId: String? = null,
    val notifySlotChanged: Boolean = true,
    val srtPath: String? = null
)