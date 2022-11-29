package com.ola.editor.kit.interfaces;

import android.view.View;

import com.ola.editor.kit.entity.MixerItemEntry;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/30 10:40
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public interface OnMixerItemClickListener {
    /**
     * @param view     当前条目View
     * @param index     条目所对应的index，传递给SDK
     * @param position 条目index
     */
    void onItemClick(View view,int index, int position);
}
