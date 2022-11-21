package com.cutsame.ui.cut.textedit.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.cut.textedit.PlayerTextEditAdapter
import com.cutsame.ui.cut.textedit.PlayerTextEditItemData
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditItemListener
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListener
import com.cutsame.ui.utils.SizeUtil
import com.cutsame.ui.utils.UniversalHorizontalLayoutManager
import kotlinx.android.synthetic.main.layout_textedit_view.view.*
import java.util.*

class PlayerMaterialTextEditView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var contentRootView: View
    private lateinit var textEditViewAdapter: PlayerTextEditAdapter
    private var editListener: PlayerTextEditListener? = null

    val curSelectItemData: PlayerTextEditItemData?
        get() = textEditViewAdapter.curSelectItemData

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        contentRootView =
            LayoutInflater.from(context).inflate(R.layout.layout_textedit_view, this, true)
        textRecyclerView.layoutManager = UniversalHorizontalLayoutManager(context)
        textRecyclerView.setHasFixedSize(true)
        textRecyclerView.addItemDecoration(
            SpacesItemDecoration(
                0,
                SizeUtil.dp2px(16f),
                rowCountLimit = 1
            )
        )
    }

    fun initData() {
        textEditViewAdapter = PlayerTextEditAdapter(object : PlayerTextEditItemListener {
            override fun selectItem(data: PlayerTextEditItemData?, pos: Int) {
                updateCurEditItemStatus(pos)
                editListener?.selectTextItem(data, pos)
            }

            override fun clickEditItem(data: PlayerTextEditItemData?, pos: Int) {
                editListener?.clickEditTextItem(data, pos)
            }
        })
        textRecyclerView.adapter = textEditViewAdapter
    }

    fun updateTextData(dataList: List<PlayerTextEditItemData>?) {
        if (dataList == null || dataList.isEmpty()) {
            return
        }
        textEditViewAdapter.setDataList(dataList)
    }

    fun updateCurEditItemText(text: String?) {
        textEditViewAdapter.updateCurEditItemText(text)
    }

    fun updateCurEditItemStatus(pos: Int) {
        textEditViewAdapter.updateCurSelectStatusView(pos)
    }

    fun setEditListener(editListener: PlayerTextEditListener?) {
        this.editListener = editListener
    }

    fun hasData(): Boolean {
        return textEditViewAdapter.itemCount > 0
    }

    fun addThumbBitmap(thumbMap: HashMap<String, Bitmap>?) {
        textEditViewAdapter.addThumbBitmap(thumbMap)
    }

    fun refreshAll() {
        textEditViewAdapter.notifyDataSetChanged()
    }

}
