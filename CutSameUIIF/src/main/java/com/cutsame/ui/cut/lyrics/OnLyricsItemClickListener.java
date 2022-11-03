package com.cutsame.ui.cut.lyrics;

import android.view.View;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/30 10:40
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public interface OnLyricsItemClickListener {
    /**
     * @param view     当前条目View
     * @param position 条目index
     */
    void onItemClick(View view, int position);
}
