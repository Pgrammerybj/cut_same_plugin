package com.angelstar.ola.holder;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.angelstar.ola.R;
import com.angelstar.ola.entity.CustomViewsInfo;
import com.angelstar.ybj.xbanner.holder.ViewHolder;

public class VideoViewHolder implements ViewHolder<CustomViewsInfo> {
    private final SurfaceView mSurfaceView;
    public FrameLayout surfaceViewContainer;

    public VideoViewHolder(SurfaceView mSurfaceView) {
        this.mSurfaceView = mSurfaceView;
    }

    @Override
    public int getLayoutId() {
        return R.layout.layout_video_view;
    }

    @Override
    public void onBind(View itemView, CustomViewsInfo data, int position) {
        Log.i("jackyang_onBind: ", "VideoViewHolder->" + position);
        surfaceViewContainer = itemView.findViewById(R.id.fl_player);
        TextView pagePosition = itemView.findViewById(R.id.tv_page);
        pagePosition.setText(String.valueOf(position));
        FrameLayout parent = (FrameLayout) mSurfaceView.getParent();
        if (null == parent || parent.getChildCount() == 1) {
            surfaceViewContainer.addView(mSurfaceView);
        } else {
            if (position == 0) {
                parent.removeView(mSurfaceView);
                surfaceViewContainer.addView(mSurfaceView);
            }
        }
    }
}
