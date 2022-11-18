package com.ss.ugc.android.editor.core.handler

import com.ss.ugc.android.editor.core.api.CommitLevel
import com.ss.ugc.android.editor.core.api.params.EditMedia

interface ITrackNLEHandler {

    /**
     * 用所选的视频/图片初始化主轨
     */
    fun initMainTrack(selectedMedias: MutableList<EditMedia>)

    /**
     * 点+，插入素材片段（视频/图片）
     */
//    fun addMediaToMainTrack(selectedMedias: MutableList<EditMedia>, param: ImportParam)

    /**
     * 替换素材
     */
//    fun replaceMediaForSlot(selectedMedias: MutableList<EditMedia>): Boolean

    /**
     * 点击关闭/开启原声按钮
     */
    fun closeOriVolume(commitLevel: CommitLevel? = CommitLevel.COMMIT)

    /**
     * 删除视频
     */
//    fun deleteVideo(slot: NLETrackSlot? = null, commitLevel: CommitLevel? = CommitLevel.COMMIT): Pair<NLETrack?, NLETrackSlot?>

    /**
     * 修改音量
     */
//    fun changeVolume(volume: Float, commitLevel: CommitLevel? = CommitLevel.COMMIT)

}