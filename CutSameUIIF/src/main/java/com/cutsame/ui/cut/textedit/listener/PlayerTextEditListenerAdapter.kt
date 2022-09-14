package com.cutsame.ui.cut.textedit.listener

import com.cutsame.ui.cut.textedit.PlayerTextBoxData
import com.cutsame.ui.cut.textedit.PlayerTextEditItemData

open class PlayerTextEditListenerAdapter : PlayerTextEditListener {

    override fun clickCancel() {

    }

    override fun clickSave() {

    }

    override fun selectTextItem(data: PlayerTextEditItemData?, pos: Int, seekDone: ((ret: Int) -> Unit)?) {

    }

    override fun clickEditTextItem(data: PlayerTextEditItemData?, pos: Int) {

    }

    override fun clickPlay(): Boolean {
        return false
    }

    override fun finishClickEditText(itemData: PlayerTextEditItemData?) {

    }

    override fun showOrHideKeyboardView(isShow: Boolean) {

    }

    override fun getCurItemTextBoxData(data: PlayerTextEditItemData?): PlayerTextBoxData? {
        return null
    }

    override fun getCanvasSize(): IntArray? {
        return null
    }

    override fun isPlaying(): Boolean {
        return false
    }

    override fun controlVideoPlaying(play: Boolean) {

    }

    override fun getCurPlayPosition(): Long {
        return 0
    }

    override fun getItemFrameBitmap(times: IntArray?, width: Int, height: Int, listener: PlayerTextItemThumbBitmapListener?) {

    }

    override fun inputTextChange(itemData: PlayerTextEditItemData?, text: String?) {

    }

    override fun controlTextAnimate(slatId: String?, enable: Boolean) {
    }
}
