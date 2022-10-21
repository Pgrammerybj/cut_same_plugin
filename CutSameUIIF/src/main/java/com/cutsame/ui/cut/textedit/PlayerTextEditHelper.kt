package com.cutsame.ui.cut.textedit

import com.ss.android.ugc.cut_ui.TextItem

object PlayerTextEditHelper {

    /**
     * 对sdk的TextSegment包装一下PlayerEditItemData
     */
    fun covertItemTextData(textItems: List<TextItem>?): MutableList<PlayerTextEditItemData>? {
        if (textItems == null || textItems.isEmpty()) {
            return null
        }
        val dataList = mutableListOf<PlayerTextEditItemData>()
        textItems!!.forEach {
            dataList.add(PlayerTextEditItemData(it))
        }
        return dataList
    }

}
