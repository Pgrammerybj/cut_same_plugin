package com.angelstar.ola.interfaces;

import android.view.View;

import com.angelstar.ola.entity.MixerItemEntry;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/30 10:40
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public interface OnMixerItemClickListener {
    /**
     * @param view     当前条目View
     * @param data     条目所对应的数据
     * @param position 条目index
     */
    void onItemClick(View view, MixerItemEntry data, int position);
}
