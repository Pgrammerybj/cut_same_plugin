package com.ss.ugc.android.editor.core.api.impl

import com.bytedance.ies.nle.editor_jni.*
import com.ss.ugc.android.editor.core.IEditorContext
import com.ss.ugc.android.editor.core.api.CommitLevel
import com.ss.ugc.android.editor.core.api.CommitLevel.*
import com.ss.ugc.android.editor.core.api.IEditor
import com.ss.ugc.android.editor.core.api.params.EditMedia
import com.ss.ugc.android.editor.core.commit
import com.ss.ugc.android.editor.core.done
import com.ss.ugc.android.editor.core.handler.ITrackNLEHandler
import com.ss.ugc.android.editor.core.handler.TrackNLEHandler
import com.ss.ugc.android.editor.core.proxy.dynamicProxy

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2021/8/23
 */
class DefaultEditor(private val editorContext: IEditorContext) : IEditor {

    companion object {
        const val useDynamicProxy = false
    }

    private val trackHandlerProxy: ITrackNLEHandler by lazy {
        if (useDynamicProxy) {
            (TrackNLEHandler(editorContext) as ITrackNLEHandler).dynamicProxy()
        } else {
            TrackNLEHandler(editorContext)
        }
    }

    /**
     * 用于保存数据，做 undo、redo、草稿恢复
     */

    override var nleEditor: NLEEditor = NLEEditor()

    override fun setNLEEditor(editor: NLEEditor) {
        nleEditor = editor
    }

    override fun compose(commitLevel: CommitLevel?, block: IEditor.() -> Unit) {
        this.block()
        commitLevel?.apply {
            when (this) {
                COMMIT -> {
                    editorContext.commit()
                }
                DONE -> {
                    editorContext.done()
                }
                NONE -> {
                }
            }
        }
    }

    override fun initMainTrack(selectedMedias: MutableList<EditMedia>) {
        trackHandlerProxy.initMainTrack(selectedMedias)
    }
   }