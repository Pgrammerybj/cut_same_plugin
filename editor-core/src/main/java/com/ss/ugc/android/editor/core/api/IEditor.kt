package com.ss.ugc.android.editor.core.api

import com.bytedance.ies.nle.editor_jni.NLEEditor
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.ss.ugc.android.editor.core.api.CommitLevel.DONE
import com.ss.ugc.android.editor.core.handler.IAudioNLEHandler
import com.ss.ugc.android.editor.core.handler.ITrackNLEHandler

interface IEditor :
    ITrackNLEHandler,
    IAudioNLEHandler
{

    val nleEditor: NLEEditor

    fun setNLEEditor(editor: NLEEditor)

    fun getNleModel(): NLEModel {
        return if (nleEditor.model == null) {
            NLEModel().apply {
                nleEditor.model = this
            }
        } else {
            nleEditor.model
        }
    }

    fun compose(commitLevel: CommitLevel? = DONE, block: IEditor.() -> Unit)
}