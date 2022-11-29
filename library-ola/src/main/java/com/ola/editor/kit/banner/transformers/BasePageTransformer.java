package com.ola.editor.kit.banner.transformers;

import android.util.Log;
import android.view.View;

import androidx.viewpager.widget.ViewPager;


/**
 * Created by Thomas on 2016/10/18.
 * <p>

 * github: https://github.com/Pgrammerybj
 * description：
 */
public abstract class BasePageTransformer implements ViewPager.PageTransformer {

    @Override
    public void transformPage(View view, float position) {
        ViewPager viewPager;
        if (view.getParent() instanceof ViewPager) {
            viewPager = (ViewPager) view.getParent();
        } else {
            return;
        }
        position = getRealPosition(viewPager, view);
        Log.i("JackYang", "transformPage_position:" + position);

        if (position < -1 || position > 1) {
            handleInvisiblePage(view, position);
        } else {
            //不透明->半透明
            if (position <= 0) {//[0,-1]
                handleLeftPage(view, position);
            } else {//[1,0] position <= 1.0f
                //半透明->不透明
                handleRightPage(view, position);
            }
        }
    }

    /**
     * 重新计算position
     */
    private float getRealPosition(ViewPager viewPager, View page) {
        int width = viewPager.getMeasuredWidth() - viewPager.getPaddingLeft() - viewPager.getPaddingRight();
        return (float) (page.getLeft() - viewPager.getScrollX() - viewPager.getPaddingLeft()) / width;
    }

    public abstract void handleInvisiblePage(View view, float position);

    public abstract void handleLeftPage(View view, float position);

    public abstract void handleRightPage(View view, float position);
}