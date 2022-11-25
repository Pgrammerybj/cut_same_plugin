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
    var startTime: Long = 0L,
    var endTime: Long = 0L,
    var isMoveTrack: Boolean = false,
    var isFromRecord: Boolean = false,
    var isAudioEffect: Boolean = false,
    var timeClipStart: Long = 0L,
    var timeClipEnd: Long = 0L,
    var extraId: String? = null,
    var notifySlotChanged: Boolean = true,
    var srtPath: String? = null
) : Parcelable