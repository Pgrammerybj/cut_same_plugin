package com.ss.ugc.android.editor.core.handler

import com.bytedance.ies.nle.editor_jni.NLETrack
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.core.api.CommitLevel
import com.ss.ugc.android.editor.core.api.CommitLevel.COMMIT
import com.ss.ugc.android.editor.core.api.CommitLevel.DONE
import com.ss.ugc.android.editor.core.api.params.AudioParam

interface IAudioNLEHandler {
    /**
     * 添加音频轨道
     *[autoSeekFlag] 音轨添加后，视频整体进度会回到0，需要seek
     * flag 为0 时 seek 到添加音频开始的位置
     * flag 为1 时 seek 到添加音频结束的位置
     *
     */
    fun addAudioTrack(param: AudioParam, autoSeekFlag: Int = 0, commitLevel: CommitLevel? = DONE, slotCallBack: ((NLETrackSlot) -> Unit)? = null): Boolean

    /**
     * 删除音频轨道
     */
    fun deleteAudioTrack(slot: NLETrackSlot? = null, commitLevel: CommitLevel? = COMMIT): Pair<NLETrack?, NLETrackSlot?>
}