package com.angelstar.ola.adapter;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;

import androidx.annotation.DrawableRes;

import com.angelstar.ola.interfaces.IScaleSlideBarAdapter;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/7 18:08
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 刻度选中后的回调
 */
public class SlideAdapter implements IScaleSlideBarAdapter {


    protected StateListDrawable[] mItems;
    protected String[] content;
    protected int[] textColor;

    public SlideAdapter(Resources resources, String[] slideContent, @DrawableRes int slideDrawable) {
        int size = slideContent.length;
        content = slideContent;
        mItems = new StateListDrawable[size];
        Drawable drawable;
        for (int i = 0; i < size; i++) {
            drawable = resources.getDrawable(slideDrawable);
            if (drawable instanceof StateListDrawable) {
                mItems[i] = (StateListDrawable) drawable;
            } else {
                mItems[i] = new StateListDrawable();
                mItems[i].addState(new int[] {}, drawable);
            }
        }
    }

    @Override
    public int getCount() {
        return mItems.length;
    }

    @Override
    public String getText(int position) {
        return content[position];
    }

    @Override
    public StateListDrawable getItem(int position) {
        return mItems[position];
    }

    @Override
    public int getTextColor(int position) {
        return textColor[position];
    }

    public void setTextColor(int[] color){
        textColor = color;
    }
}
