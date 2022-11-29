package com.ola.editor.kit.player

import android.content.Intent
import com.ola.editor.kit.interfaces.ITemplateVideoStateListener
import com.ss.ugc.android.editor.core.NLEEditorContext


interface IPlayerActivityDelegate {

    var nleEditorContext: NLEEditorContext?

    var viewStateListener: ITemplateVideoStateListener?

    fun onCreate()

    fun onResume()

    fun onPause()

    fun play()

    fun pause()

    fun onDestroy()

    /**
     * 插入-替换本地视频素材
     */
    fun importVideoMedia(filePathList: List<String>) {}

    fun restoreDraftContent(draftModelPath: String) {}
}