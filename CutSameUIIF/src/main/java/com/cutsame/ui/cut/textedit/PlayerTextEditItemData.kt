package com.cutsame.ui.cut.textedit

import android.text.TextUtils
import com.ss.android.ugc.cut_ui.TextItem

class PlayerTextEditItemData(private val textItem: TextItem?) {
    var saltId: String? = null
        private set
    private var editText: String? = null

    //用于点击x还原文字功能
    private var restoreEditText: String? = null

    private var textAngle: Float = 0.0f

    private var startTime: Long = 0

    private var duration: Long = 0

    val isValid: Boolean
        get() = !TextUtils.isEmpty(this.editText) && !TextUtils.isEmpty(saltId)

    init {
        if (textItem != null) {
            saltId = textItem.materialId
            editText = textItem.text
            duration = textItem.duration
            //文字可能出现叠加效果，加100ms
            startTime = textItem.targetStartTime + 100
            textAngle = textItem.rotation.toFloat()
            //原始文字如果没有赋值过就赋值，赋值过了不用了，防止从编辑页回来又赋值一次，把改动后的值赋值了
            if (restoreEditText == null) {
                restoreEditText = editText
            }
        }
    }

    fun getTextAngle(): Float {
        return textAngle
    }

    fun getFrameTime(): Long {
        return startTime
    }

    fun getDuration(): Long {
        return duration
    }

    fun getEditText(): String? {
        return editText
    }

    fun setEditText(editText: String?) {
        if (editText == null) {
            return
        }
        this.editText = editText
        if (textItem != null) {
            textItem.text = editText
        }
    }

    fun isContainerTimeRange(pos: Long): Boolean {
        if (pos < startTime) {
            return false
        }
        return pos <= startTime + duration
    }

    fun getOriginText(): String? {
        return restoreEditText
    }

    fun restoreData() {
        editText = restoreEditText
        if (textItem != null) {
            textItem.text = editText?:""
        }
    }

    fun isChangeText(): Boolean {
        return !TextUtils.equals(editText, restoreEditText)
    }


}
