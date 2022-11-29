package com.ola.editor.kit.cutsame.customview;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.ola.editor.kit.cutsame.customview.StickySurfaceHolder;

public class StickyHolderSurfaceView extends SurfaceView {

    private com.ola.editor.kit.cutsame.customview.StickySurfaceHolder stickySurfaceHolder;

    public StickyHolderSurfaceView(Context context) {
        super(context);
        init();
    }

    public StickyHolderSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StickyHolderSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(21)
    public StickyHolderSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        stickySurfaceHolder = new com.ola.editor.kit.cutsame.customview.StickySurfaceHolder(super.getHolder());
    }

    @Override
    public SurfaceHolder getHolder() {
        return stickySurfaceHolder;
    }

}
