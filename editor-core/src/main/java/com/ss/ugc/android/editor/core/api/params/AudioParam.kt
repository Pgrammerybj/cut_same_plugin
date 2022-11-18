package com.ss.ugc.android.editor.core.api.params

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * @param audioName 音频名称
 * @param audioPath 音频文件本地路径
 */
@Parcelize
data class AudioParam(
    var audioName: String?,
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
): Parcelable