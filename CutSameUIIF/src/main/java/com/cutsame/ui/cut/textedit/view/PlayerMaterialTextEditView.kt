package com.cutsame.ui.cut.textedit.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.cut.textedit.PlayerTextEditAdapter
import com.cutsame.ui.cut.textedit.PlayerTextEditItemData
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditItemListener
import com.cutsame.ui.cut.textedit.listener.PlayerTextEditListener
import com.cutsame.ui.utils.SizeUtil
import kotlinx.android.synthetic.main.layout_textedit_view.view.*
import java.util.*

class PlayerMaterialTextEditView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private lateinit var contentRootView: View
    private lateinit var textEditViewAdapter: PlayerTextEditAdapter
    private var editListener: PlayerTextEditListener? = null

    val curSelectItemData: PlayerTextEditItemData?
        get() = textEditViewAdapter.curSelectItemData

    init {
        initView(context)
    }

    private fun initView(context: Context) {
        contentRootView = LayoutInflater.from(context).inflate(R.layout.layout_textedit_view, this, true)
        textRecyclerView.layoutManager = object : androidx.recyclerview.widget.LinearLayoutManager(context, HORIZONTAL, false) {
            override fun smoothScrollToPosition(
                recyclerView: RecyclerView?,
                state: RecyclerView.State?,
                position: Int
            ) {
                val linearSmoothScroller = object :
                    androidx.recyclerview.widget.LinearSmoothScroller(recyclerView!!.context) {
                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                        return super.calculateSpeedPerPixel(displayMetrics) * 2
                    }

                    override fun calculateDxToMakeVisible(
                        view: View?,
                        snapPreference: Int
                    ): Int {
                        val layoutManager = this.layoutManager
                        return if (layoutManager != null && layoutManager.canScrollHorizontally()) {
                            val params =
                                view!!.layoutParams as RecyclerView.LayoutParams
                            val left = layoutManager.getDecoratedLeft(view) - params.leftMargin
                            val right =
                                layoutManager.getDecoratedRight(view) + params.rightMargin
                            val start = layoutManager.paddingLeft
                            val end = layoutManager.width - layoutManager.paddingRight
                            return start + (end - start) / 2 - (right - left) / 2 - left
                        } else {
                            0
                        }
                    }
                }
                linearSmoothScroller.targetPosition = position
                startSmoothScroll(linearSmoothScroller)
            }
        }
        textRecyclerView.setHasFixedSize(true)
        textRecyclerView.addItemDecoration(SpacesItemDecoration(0, SizeUtil.dp2px(16f), rowCountLimit = 1))
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
