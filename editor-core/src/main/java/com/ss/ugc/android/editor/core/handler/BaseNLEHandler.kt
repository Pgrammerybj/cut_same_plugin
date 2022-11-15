package com.ss.ugc.android.editor.core.handler

import android.os.Handler
import android.os.Looper
import com.bytedance.ies.nle.editor_jni.INLEMediaSession
import com.bytedance.ies.nle.editor_jni.NLEEditor
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.bytedance.ies.nle.editor_jni.NLETrack
import com.ss.ugc.android.editor.core.*
import com.ss.ugc.android.editor.core.api.CommitLevel
import com.ss.ugc.android.editor.core.api.CommitLevel.*
import com.ss.ugc.android.editor.core.api.IPlayer

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/17
 */
open class BaseNLEHandler(protected val editorContext: IEditorContext) {

    protected val nleModel: NLEModel by lazy {
        editorContext.getNLEModel().apply {
            if ("true" != getExtra("DisableGlobalEffect")) {
                this.setExtra("DisableGlobalEffect", "true")
            }
        }
    }
    protected val nleEditor: NLEEditor by lazy {
        editorContext.getNLEEditor()
    }

    protected val nleSession: INLEMediaSession by lazy {
        editorContext.nleSession
    }

    protected val selectedNleTrack: NLETrack?
        get() {
            return editorContext.getSelectedTrack()
        }

    protected val mainHandler: Handler = Handler(Looper.getMainLooper())

    protected val player: IPlayer = editorContext.player

    protected fun executeCommit(commitLevel: CommitLevel?, msg: String? = null) {
        commitLevel?.apply {
            when (this) {
                COMMIT -> {
                    editorContext.commit()
                }
                DONE -> {
                    editorContext.doneWithMsg(msg)
                }
                NONE -> {
                }
            }
        }
    }
}