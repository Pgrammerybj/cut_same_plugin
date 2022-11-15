package com.ss.ugc.android.editor.core.api

import com.bytedance.ies.nle.editor_jni.NLETrackSlot

/**
 * @author: Jeff Beyond  <ouyanghaibing@bytedance.com>
 * @date: 2022/3/8
 * @desc: the channel to connect editor-core with client
 */
interface IEditorClientChannel {

    fun calculatePipPreviewScale(slot: NLETrackSlot): Float
}