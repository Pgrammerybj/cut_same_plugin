package com.ola.chat.picker.album.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.ola.chat.picker.entry.ImagePickConfig
import com.ola.chat.picker.utils.PickerConstant
import com.ola.chat.picker.viewmodel.GalleryPickerViewModel

class TabFragmentPagerAdapter(
    private val tabList: List<PickerConstant.TabType>,
    activity: FragmentActivity,
    val galleryPickerViewModel: GalleryPickerViewModel,
    private val isCutSameScene: Boolean,
    private val imagePickConfig: ImagePickConfig?
) : FragmentStateAdapter(activity) {


    override fun getItemCount(): Int {
        return tabList.size
    }

    override fun createFragment(position: Int): Fragment {
        val tab = tabList[position]
        return galleryPickerViewModel.getFragmentByType(tab,isCutSameScene,imagePickConfig)
    }

}