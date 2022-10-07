package com.angelstar.ola.interfaces;

import android.graphics.drawable.StateListDrawable;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/7 18:06
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public interface IScaleSlideBarAdapter {
    //条目数
    int getCount();
    //刻度下方文案
    String getText(int position);
    //刻度高亮素材
    StateListDrawable getItem(int position);
    //刻度下方文案颜色
    int getTextColor(int position);
}
