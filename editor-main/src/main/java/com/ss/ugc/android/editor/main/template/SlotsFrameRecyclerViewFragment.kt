package com.ss.ugc.android.editor.main.template

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ss.ugc.android.editor.base.fragment.BaseFragment
import com.ss.ugc.android.editor.main.R

@SuppressLint("ValidFragment")
class SlotsFrameRecyclerViewFragment @JvmOverloads constructor(var frameMessageList:MutableList<SlotDataITem>) :BaseFragment(){
    var recyclerViewAdapter:SlotsFrameRecyclerViewAdapter? = null
    override fun getContentView(): Int {
        return R.layout.fragment_slots_frame_recyclerview
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    fun initRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.slots_frame_recyclerview) as RecyclerView
        val decoration = SpaceItemDecoration(top = 12,bottom = 50,left = 12,right = 12)
        recyclerView.addItemDecoration(decoration)  // 给 recyclerView 的每个 item 设置间隔距离
        val layoutManager = LinearLayoutManager(this.activity, RecyclerView.HORIZONTAL,false)
        recyclerView.layoutManager = layoutManager // 适配 layoutManager
        recyclerViewAdapter = SlotsFrameRecyclerViewAdapter(this,frameMessageList)
        recyclerViewAdapter!!.setHasStableIds(true)
        recyclerView.adapter = recyclerViewAdapter // 适配 adapter
    }
}

/**
 *  该类用于给 recyclerView 的每个 item 设置间隔距离
 */
class SpaceItemDecoration (var top: Int,var bottom: Int,var left:Int,var right: Int): RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = top
        outRect.bottom = bottom
        outRect.left = left
        outRect.right = right
    }
}