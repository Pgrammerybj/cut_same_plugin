package com.cutsame.ui.template.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.cutsame.solution.template.model.TemplateCategory

class TemplatePagerAdapter(fm: androidx.fragment.app.FragmentManager) :
    FragmentPagerAdapter(fm) {

    companion object {
        const val TAG = "TemplatePagerAdapter"
    }

    private var categoryList: List<TemplateCategory> = ArrayList()

    fun updateItems(items: List<TemplateCategory>) {
        categoryList = items
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
        return TemplatePageFragment.newInstance(categoryList[position])
    }

    override fun getCount(): Int {
        return categoryList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return categoryList[position].name
    }

}