package com.cutsame.ui.template.view

import androidx.fragment.app.FragmentStatePagerAdapter
import com.cutsame.solution.template.model.TemplateItem

class TemplatePreviewPagerAdapter(
    fm: androidx.fragment.app.FragmentManager,
    items: List<TemplateItem>
) : FragmentStatePagerAdapter(fm) {

     var items = items
        set(value) {
            if (value == items) {
                return
            }
            field = value
            notifyDataSetChanged()
        }

    override fun getItem(position: Int): androidx.fragment.app.Fragment {
        return TemplatePreviewFragment.newInstance(items[position])
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getItemPosition(`object`: Any): Int {
        return androidx.viewpager.widget.PagerAdapter.POSITION_NONE
    }

    fun getItemAt(position: Int): TemplateItem {
        return items[position]
    }
}