package com.cutsame.ui.gallery.data

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cutsame.ui.gallery.viewmodel.GalleryPickerViewModel

class TabFragmentPagerAdapter(
    private val tabList: List<TabType>,
    activity: FragmentActivity,
    val galleryPickerViewModel: GalleryPickerViewModel
) : FragmentStateAdapter(activity) {


    override fun getItemCount(): Int {
        return tabList.size
    }

    override fun createFragment(position: Int): Fragment {
        val tab = tabList[position]
        return galleryPickerViewModel.getFragmentByType(tab)
    }

}