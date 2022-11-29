package com.ola.editor.kit.banner.utils;

import android.content.Context;
import android.util.TypedValue;

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/8 12:04
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public class SizeUtils {

    /**
     * dp转px
     *
     * @param dpVal dp value
     * @return px value
     */
    public static int dp2px(float dpVal, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpVal,
                context.getResources().getDisplayMetrics());
    }
}
