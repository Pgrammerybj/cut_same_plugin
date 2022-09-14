package com.ss.ugc.android.editor.main.template

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.bytedance.ies.nle.editor_jni.NLEResourceNode
import com.bytedance.ies.nle.editor_jni.NLESegmentVideo
import com.bytedance.ies.nle.editor_jni.NLETrackSlot
import com.ss.ugc.android.editor.base.viewmodel.BaseEditorViewModel

class TemplateInfoViewModel(activity: FragmentActivity) : BaseEditorViewModel(activity) {

    companion object {
        const val DEFAULT_KEEP_AUDIO: Boolean = false
        const val DEFAULT_ALIGN_CANVAS: String = "align_canvas"
        const val ALT_ALIGN_VIDEO: String = "align_video"

        const val TITLE_INPUT_TAG: String = "TITLE_INPUT"
        const val DESC_INPUT_TAG: String = "DESC_INPUT"
        const val KEYWORD_INPUT_TAG: String = "KEYWORD_INPUT"
    }

    private val nleModel = nleEditorContext.nleModel
    private var originalKeepAudioValue : Boolean? = null

//    val coverResChangedEvent = MutableLiveData<String>()
    var coverResChangedEvent:  MutableLiveData<String>? = null
    val keepAudio = MutableLiveData<Boolean>()
    val alignmentMode = MutableLiveData<Boolean>()


    init {
        if (nleModel.alignMode.isNullOrEmpty()) {
            alignmentMode.value = false
            nleModel.alignMode = DEFAULT_ALIGN_CANVAS
        } else {
            alignmentMode.value = (nleModel.alignMode != DEFAULT_ALIGN_CANVAS)
        }
        if (nleEditorContext.templateInfo?.coverRes == null) {
            nleEditorContext.templateInfo?.coverRes = NLEResourceNode()
        }
    }

    fun getTitle(): String = nleEditorContext.templateInfo!!.title

    fun getDesc(): String = nleEditorContext.templateInfo!!.desc

    fun getKeyword(): String = nleEditorContext.templateInfo!!.tag

    fun getTemplateCover(): String? = nleEditorContext.templateInfo?.coverRes?.resourceFile

    fun isExportClickable(): Boolean = !nleEditorContext.templateInfo!!.title.isNullOrEmpty()

    fun checkIfEnableAudio(allMutableSlots: ArrayList<NLETrackSlot>) {
        var isKeepAudio = false
        for (item in allMutableSlots) {
            if ((NLESegmentVideo.dynamicCast(item.mainSegment)).enableAudio) {
                isKeepAudio = true
            }
        }
        keepAudio.value = isKeepAudio
        originalKeepAudioValue = isKeepAudio
    }

    fun setTemplateInfoText(tag: String, input: String) {
        when (tag) {
            TITLE_INPUT_TAG -> nleEditorContext.templateInfo!!.title = input
            DESC_INPUT_TAG -> nleEditorContext.templateInfo!!.desc = input
            KEYWORD_INPUT_TAG -> nleEditorContext.templateInfo!!.tag = input
        }
    }

    fun setKeepAudio() {
        keepAudio.value = !keepAudio.value!!
    }

    fun setAlignmentMode() {
        alignmentMode.value = !alignmentMode.value!!
    }

    fun resetAllValues(resetConfig: Boolean) {
        nleEditorContext.nleTemplateModel.setExtra("cover_path","") // 不保存已经设置了的模版封面
        nleEditorContext.templateInfo?.apply {
            title = ""
            desc = ""
            tag = ""
            if (resetConfig) {
                keepAudio.value = originalKeepAudioValue
                alignmentMode.value = (nleModel.alignMode != DEFAULT_ALIGN_CANVAS)
            }
        }
    }

    fun commitAlignmentAndKeepAudio(allMutableSlots: ArrayList<NLETrackSlot>) {
        for (item in allMutableSlots) {
            (NLESegmentVideo.dynamicCast(item.mainSegment)).enableAudio = keepAudio.value!!
        }
        nleModel.alignMode = if (alignmentMode.value!!) ALT_ALIGN_VIDEO else DEFAULT_ALIGN_CANVAS
        nleEditorContext.done()
    }
}