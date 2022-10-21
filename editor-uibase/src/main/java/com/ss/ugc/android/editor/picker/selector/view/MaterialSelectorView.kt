package com.ss.ugc.android.editor.picker.selector.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ss.ugc.android.editor.base.R
import com.ss.ugc.android.editor.base.utils.SizeUtil
import com.ss.ugc.android.editor.picker.album.view.adapter.ImageSelectedRVAdapter
import com.ss.ugc.android.editor.picker.data.model.MediaItem
import com.ss.ugc.android.editor.picker.data.model.MediaType
import com.ss.ugc.android.editor.picker.selector.config.SelectorViewConfig
import com.ss.ugc.android.editor.picker.selector.config.SpaceItemDecoration
import com.ss.ugc.android.editor.picker.selector.viewmodel.MaterialSelectModel

/**
 * 选择View
 */
class MaterialSelectorView(
    private val root: ViewGroup,
    private val lifecycleOwner: LifecycleOwner,
    private val materialSelectModel: MaterialSelectModel,
    private val selectorViewConfig: SelectorViewConfig? = null,
    private val confirmClickListener: (() -> Unit),
    private val maxSelectedCount: Int
) {
    private lateinit var contentView: ViewGroup
    private lateinit var confirmTV: TextView
    private lateinit var rvPickerSelected: RecyclerView
    private lateinit var imageSelectedRVAdapter: ImageSelectedRVAdapter

    private var viewType: SelectViewType = SelectViewType.CONFIRM //默认为提交功能类型

    fun init() {
        initContentView(root)
        initObserver()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initObserver() {
        materialSelectModel.apply {
            selectCount.observe(lifecycleOwner, { count ->
                count?.let {
                    //改变确认按钮的计数
                    confirmTV.text = String.format(
                        root.context.getString(R.string.ck_confirm_picker),
                        count,
                        maxSelectedCount
                    )
                    //通知Adapter更新列表
                    imageSelectedRVAdapter.updateSelectedList(materialSelectModel.selectedList)

                    //控制相册的「确定选择」按钮的样式
                    if (count != maxSelectedCount) {
                        confirmTV.isEnabled = false
                        confirmTV.setTextColor(Color.parseColor("#858585"))
                        confirmTV.background =
                            root.resources.getDrawable(R.drawable.bg_finish_select_disable_btn, null)
                    } else {
                        confirmTV.setTextColor(Color.WHITE)
                        confirmTV.isEnabled = true
                        confirmTV.background =
                            root.resources.getDrawable(R.drawable.bg_finish_select_enable_btn, null)
                    }
                }
            })
        }
    }

    private fun initContentView(root: ViewGroup) {
        contentView = LayoutInflater.from(root.context)
            .inflate(R.layout.layout_material_selector, root, true) as ViewGroup
        confirmTV = contentView.findViewById(R.id.confirmTV)
        rvPickerSelected = contentView.findViewById(R.id.recyclerviewPickerSelected)


        val linearLayoutManager = LinearLayoutManager(root.context)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        rvPickerSelected.layoutManager = linearLayoutManager
        rvPickerSelected.addItemDecoration(SpaceItemDecoration(0, 0, 0, SizeUtil.dp2px(8F)))

        selectorViewConfig?.finishBtnText?.apply {
            confirmTV.text = this
        }
        confirmTV.setOnClickListener {
            confirmClickListener()
        }

        imageSelectedRVAdapter = ImageSelectedRVAdapter(root.context,maxSelectedCount,materialSelectModel)
        rvPickerSelected.adapter = imageSelectedRVAdapter
    }

    fun changeViewType(type: SelectViewType) {
        if (viewType == type) {
            return
        }

        viewType = type
        when (type) {
            SelectViewType.CONFIRM -> {
                confirmTV.visibility = View.VISIBLE
            }
            SelectViewType.ADD -> {
                confirmTV.visibility = View.GONE
            }
        }
    }
}