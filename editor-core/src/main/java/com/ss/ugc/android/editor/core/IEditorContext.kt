package com.ss.ugc.android.editor.core

import android.view.SurfaceView
import androidx.fragment.app.FragmentActivity
import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.core.api.IEditor
import com.ss.ugc.android.editor.core.api.IEditorClientChannel
import com.ss.ugc.android.editor.core.api.IEnvVariables
import com.ss.ugc.android.editor.core.api.IPlayer

interface IEditorContext {

    /**
     * 初始化
     */
    fun init(workSpace: String?, surfaceView: SurfaceView)

    /**
     * 是否初始化
     */
    var hasInitialized: Boolean

    var nleMediaConfig: NLEMediaConfig?

    val activity: FragmentActivity

    val nleSession: INLEMediaSession

    fun getMainTrack(): NLETrack

    fun getSelectedTrack(): NLETrack?

    fun getSelectedTrackSlot(): NLETrackSlot?

    /**
     * 环境变量接口
     */
    var envVariables: IEnvVariables

    var editorClientChannel: IEditorClientChannel?
    /**
     * 编辑接口
     */
    var editor: IEditor
    val nleModel: NLEModel
    var nleTemplateModel: NLETemplateModel

    /**
     * 播放器接口
     */
    var player: IPlayer

    /**
     * 视频导出接口
     */
//    var exporter: IVideoCompile

    fun getString(resId: Int): String
}
