package com.angelstar.ybj.xbanner.transformers;

import android.annotation.SuppressLint;
import android.view.View;

/**
 * author: yangbaojiang.
 * time: 2018/10/9
 * mail:pgrammer.ybj@outlook.com
 * github:https://github.com/Pgrammerybj
 * describe: 适用于一屏显示多个模式
 */
public class ScalePageTransformer extends BasePageTransformer {

    private static final float MIN_SCALE = 0.82F;
    private static final float MIN_ALPHA = 0.90f;

    @Override
    public void handleInvisiblePage(View view, float position) {
        view.setScaleY(MIN_SCALE);
        view.setAlpha(MIN_ALPHA);
    }

    @SuppressLint("LongLogTag")
    @Override
    public void handleLeftPage(View view, float position) {
        float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
        float scale = Math.max(MIN_SCALE, 1 + 0.3f * position);
        view.setScaleY(scale);
        view.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
//        Log.i("JackYang_handleLeftPage", "handleLeftPage: " + scale);
    }

    @SuppressLint("LongLogTag")
    @Override
    public void handleRightPage(View view, float position) {
        float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
        float scale = Math.max(MIN_SCALE, 1 - 0.3f * position);
        view.setScaleY(scale);
        view.setAlpha(MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
//        Log.i("JackYang_handleRightPage", "handleRightPage: " + scale);
    }
}
