package com.ss.ugc.android.editor.core

import android.os.Environment
import android.view.SurfaceView
import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import com.bytedance.ies.nle.editor_jni.*
import com.bytedance.ies.nle.mediapublic.nlesession.NLEMediaSessionPublic
import com.bytedance.ies.nleeditor.NLE
import com.ss.ugc.android.editor.core.Constants.Companion.SELECTED_NLE_TRACK
import com.ss.ugc.android.editor.core.api.*
import com.ss.ugc.android.editor.core.api.impl.DefaultEditor
import com.ss.ugc.android.editor.core.api.impl.DefaultPlayer
import com.ss.ugc.android.editor.core.vm.BaseViewModel

@Keep
class NLEEditorContext(activity: FragmentActivity) : BaseViewModel(activity), IEditorContext {

    // 这个一定要在init里添加
    init {
        // 加载 草稿/undo/redo 功能模块
        NLE.loadNLELibrary()
    }

    override val nleModel: NLEModel
        get() = getNLEModel()

    override lateinit var nleTemplateModel: NLETemplateModel

    override var envVariables: IEnvVariables = LiveDataEnvVariables()
    override var editorClientChannel: IEditorClientChannel? = null
    override var player: IPlayer = DefaultPlayer(this)
    override var editor: IEditor = DefaultEditor(this)
    override var hasInitialized: Boolean = false
    override var nleMediaConfig: NLEMediaConfig? = null

    override fun getMainTrack(): NLETrack {
        return if (nleModel.mainTrack == null) {
            NLETrack().apply {
                mainTrack = true
                nleModel.addTrack(this)
            }
        } else {
            nleModel.mainTrack
        }
    }

    override fun getSelectedTrack(): NLETrack? {
        return getVariable(SELECTED_NLE_TRACK)
    }

    override lateinit var nleSession: INLEMediaSession

    override fun init(workSpace: String?, surfaceView: SurfaceView) {
        if (!hasInitialized) {
            val realWorkSpace = workSpace ?: Environment.getExternalStorageDirectory().absolutePath
            this.nleMediaConfig = NLEMediaConfig().also {
                it.nleMediaAbConfig.enableRebuildModelWhenMainTrackChange = false
                it.workSpace = realWorkSpace
                it.nleMediaAbConfig.forCanvasMode = true
                nleSession = NLEMediaSessionPublic.create(it, surfaceView, getNLEEditor())
                nleSession.mediaSettingApi.setLoopPlay(true)
            }
            player.init(nleSession.playerApi)
            hasInitialized = true
        }
    }

    override fun onResume() {
        player.resume()
    }

    override fun onPause() {
        player.pause()
    }

    override fun onDestroy() {
        player.destroy()
        hasInitialized = false
    }

    override fun getString(resId: Int): String = activity.getString(resId)


    fun looperPlay() {
        nleSession.playerApi?.apply {
            seekTime(0, NLESeekFlag.EDITOR_SEEK_FLAG_LastSeek) {
                refreshCurrentFrame()
                play()
            }
        }
    }

}