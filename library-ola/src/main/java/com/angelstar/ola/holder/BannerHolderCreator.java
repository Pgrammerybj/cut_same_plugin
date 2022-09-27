package com.angelstar.ola.holder;

import android.view.SurfaceView;

import com.angelstar.ybj.xbanner.holder.HolderCreator;
import com.angelstar.ybj.xbanner.holder.ViewHolder;

import java.util.HashMap;

public class BannerHolderCreator implements HolderCreator<ViewHolder<VideoViewHolder>> {
    private final SurfaceView mSurfaceView;
    private HashMap<Integer, VideoViewHolder> videoViewHolders = new HashMap();

    public BannerHolderCreator(SurfaceView mSurfaceView) {
        this.mSurfaceView = mSurfaceView;
    }

    @Override
    public ViewHolder createViewHolder(int position) {

        if (videoViewHolders.get(position) != null) {
            return videoViewHolders.get(position);
        } else {
            VideoViewHolder videoViewHolder = new VideoViewHolder(mSurfaceView);
            videoViewHolders.put(position, videoViewHolder);
            return videoViewHolder;
        }
    }

    @Override
    public int getViewType(int position) {
        return position;
    }
}
