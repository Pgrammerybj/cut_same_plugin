package com.cutsame.ui.gallery.album.list

import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.cutsame.ui.R
import com.cutsame.ui.customview.SpacesItemDecoration
import com.cutsame.ui.gallery.album.adapter.GalleryMaterialListAdapter
import com.cutsame.ui.gallery.album.model.MediaData
import com.cutsame.ui.gallery.viewmodel.GalleryPickerViewModel
import com.cutsame.ui.utils.SizeUtil

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