package com.ola.chat.picker.album.list

import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ola.chat.picker.album.adapter.GalleryMaterialListAdapter
import com.ola.chat.picker.album.model.MediaData
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel
import com.ola.chat.picker.R
import com.ola.chat.picker.utils.SizeUtil
import com.ola.chat.picker.utils.SpacesItemDecoration

abstract class BasePickerFragment(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val galleryPickerViewModel: GalleryPickerViewModel
) : Fragment() {
    var mContext: Context = context
    var itemClickListener: GalleryMaterialListAdapter.ItemClickListener? = null
    private lateinit var materialRecycleView: androidx.recyclerview.widget.RecyclerView
    private lateinit var materialListAdapter: GalleryMaterialListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_default_picker, container, false)
        materialRecycleView = view.findViewById(R.id.materialRecycleView)
        initData()
        return view
    }

    private fun initData() {
        materialListAdapter = GalleryMaterialListAdapter(
            mContext,
            lifecycleOwner,
            galleryPickerViewModel
        )
        materialListAdapter.setItemClickListener(itemClickListener)
        val layoutManager =
            androidx.recyclerview.widget.GridLayoutManager(context, 3)
        materialRecycleView.layoutManager = layoutManager
        materialRecycleView.addItemDecoration(
            SpacesItemDecoration(
                SizeUtil.dp2px(8f),
                SizeUtil.dp2px(8f),
                columnCountLimit = 3
            )
        )
        materialRecycleView.recycledViewPool.setMaxRecycledViews(0, 30)
        materialRecycleView.adapter = materialListAdapter

        getData {
            materialListAdapter.update(it)
        }
    }

    internal abstract fun getData(block: (List<MediaData>) -> Unit)
}