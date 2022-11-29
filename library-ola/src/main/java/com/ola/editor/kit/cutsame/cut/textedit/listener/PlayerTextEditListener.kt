package com.ola.editor.kit.cutsame.cut.textedit.listener

import com.ola.editor.kit.cutsame.cut.textedit.PlayerTextBoxData
import com.ola.editor.kit.cutsame.cut.textedit.PlayerTextEditItemData

/**
 * 文字功能编辑回调
 */
interface PlayerTextEditListener {

    //获取视频画布原始大小
    fun getCanvasSize(): IntArray?

    //视频是否在播放
    fun isPlaying(): Boolean

    //获取当前播放位置
    fun getCurPlayPosition(): Long

    //点击取消
    fun clickCancel()

    //点击保存
    fun clickSave()

    //选择一个文字item
    fun selectTextItem(data: PlayerTextEditItemData?, pos: Int, seekDone: ((ret: Int) -> Unit)? = null)

    //点击编辑一个文字item
    fun clickEditTextItem(data: PlayerTextEditItemData?, pos: Int)

    //点击播放按钮
    fun clickPlay(): Boolean

    //点击完成文字修改
    fun finishClickEditText(data: PlayerTextEditItemData?)

    //键盘是否展示
    fun showOrHideKeyboardView(isShow: Boolean)

    //获取文字item对应的画布中的位置
    fun getCurItemTextBoxData(data: PlayerTextEditItemData?): PlayerTextBoxData?

    //控制视频播放
    fun controlVideoPlaying(play: Boolean)

    //抽帧封面图
    fun getItemFrameBitmap(times: IntArray?, width: Int, height: Int, listener: PlayerTextItemThumbBitmapListener?)

    //文本框输入内容监听变化
    fun inputTextChange(itemData: PlayerTextEditItemData?, text: String?)

    //控制文字动画功能
    fun controlTextAnimate(slatId: String?, enable: Boolean)

}
