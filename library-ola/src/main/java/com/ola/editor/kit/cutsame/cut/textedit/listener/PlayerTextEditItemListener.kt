package com.ola.editor.kit.cutsame.cut.textedit.listener

import com.ola.editor.kit.cutsame.cut.textedit.PlayerTextEditItemData


/**
 * 文字列表Item操作回调
 */
interface PlayerTextEditItemListener {

    //用户选中item
    fun selectItem(data: PlayerTextEditItemData?, pos: Int)

    //点击编辑
    fun clickEditItem(data: PlayerTextEditItemData?, pos: Int)

}
