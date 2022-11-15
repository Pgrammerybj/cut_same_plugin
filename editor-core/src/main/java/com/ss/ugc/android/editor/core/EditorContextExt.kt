package com.ss.ugc.android.editor.core

import androidx.lifecycle.MutableLiveData
import com.bytedance.ies.nle.editor_jni.NLEEditor
import com.bytedance.ies.nle.editor_jni.NLEModel
import com.bytedance.ies.nle.editor_jni.NLETrack
import com.bytedance.ies.nle.editor_jni.NLETrackType

fun <T : Any> IEditorContext.setVariable(key: String, value: T?) {
    envVariables.set(key, value)
}

fun <T : Any> IEditorContext.setVariable(key: String, value: T?, postOnMainThread: Boolean) {
    envVariables.set(key, value, postOnMainThread)
}

fun <T : Any> IEditorContext.getVariable(key: String): T? {
    return envVariables.get(key)
}

fun <T : Any> IEditorContext.getVariable(key: String, defaultVal: T): T {
    return envVariables.get(key, defaultVal)!!
}

fun <T : Any> IEditorContext.asLiveData(key: String): MutableLiveData<T> {
    return envVariables.asLiveData(key)
}

fun IEditorContext.getCurrentPosition(): Long {
    return player.curPosition()
}

fun IEditorContext.done() {
    editor.nleEditor.commitDone()
}

fun IEditorContext.commit() {
    editor.nleEditor.commit()
}

fun IEditorContext.doneWithMsg(msg: String?) {
    editor.nleEditor.commitDoneWithMsg(msg = msg)
}

fun IEditorContext.getNLEEditor(): NLEEditor {
    return editor.nleEditor
}

fun IEditorContext.getNLEModel(): NLEModel {
    return editor.getNleModel().apply {
        if ("true" != getExtra("DisableGlobalEffect")) {
            this.setExtra("DisableGlobalEffect", "true")
        }
    }
}

fun NLETrack?.nleTrackType(): NLETrackType {
    if (null == this) {
        return NLETrackType.NONE
    }
    val trackType = this.trackType
    val extTrackType = this.extraTrackType

    return if (trackType != NLETrackType.NONE) {
        trackType
    } else if (extTrackType != NLETrackType.NONE) {
        extTrackType
    } else {
        NLETrackType.NONE
    }
}

