package com.cutsame.ui.cut.textedit.view

import android.content.Context
import android.graphics.Bitmap
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.customview.setGlobalDebounceOnClickListener
import com.cutsame.ui.cut.textedit.PlayerTextEditAdapter
import com.cutsame.ui.cut.textedit.PlayerTextEditItemData
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditItemListener
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListener
import com.cutsame.ui.utils.SizeUtil.dp2px
import java.util.*

class PlayerTextEditView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private lateinit var contentRootView: View
    private lateinit var textRecyleView: RecyclerView
    private lateinit var textEditViewAdapter: PlayerTextEditAdapter
    private var editListener: PlayerTextEditListener? = null

    val curSelectItemData: PlayerTextEditItemData?
        get() = textEditViewAdapter.curSelectItemData

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        contentRootView = LayoutInflater.from(context).inflate(R.layout.layout_textedit_view, this, true)
        textRecyleView = contentRootView.findViewById(R.id.text_recyleview)
        val layoutManager = GridLayoutManager(context, 4)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        textRecyleView.layoutManager = layoutManager
        textRecyleView.addItemDecoration(
            SpacesItemDecoration(
                dp2px(15f),
                dp2px(15f),
                columnCountLimit = 4
            )
        )
        contentRootView.findViewById<View>(R.id.save_btn).setGlobalDebounceOnClickListener {
            editListener?.clickSave()
        }
        contentRootView.findViewById<View>(R.id.cancel_btn).setGlobalDebounceOnClickListener {
            editListener?.clickCancel()
        }
    }

    fun initData() {
        textEditViewAdapter = PlayerTextEditAdapter(context, object : PlayerTextEditItemListener {
            override fun selectItem(data: PlayerTextEditItemData?, pos: Int) {
                updateCurEditItemStatus(pos)
                editListener?.selectTextItem(data, pos)
            }

            override fun clickEditItem(data: PlayerTextEditItemData?, pos: Int) {
                editListener?.clickEditTextItem(data, pos)
            }
        })
        textRecyleView.adapter = textEditViewAdapter
    }

    fun getCurSelectPos(): Int {
        return textEditViewAdapter.curPos
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

    fun scrollToPos(pos: Int) {
        textRecyleView.scrollToPosition(pos)
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
