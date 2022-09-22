package com.angelstar.ola.holder;

import com.angelstar.ybj.xbanner.holder.HolderCreator;
import com.angelstar.ybj.xbanner.holder.ViewHolder;

public class BannerHolderCreator implements HolderCreator<ViewHolder> {
    public VideoViewHolder videoViewHolder;

    public BannerHolderCreator() {
        this.videoViewHolder = new VideoViewHolder();
    }

    @Override
    public ViewHolder createViewHolder(int viewType) {
        if (viewType==0){
            return videoViewHolder;
        }
        return new ImageViewHolder();
    }

    @Override
    public int getViewType(int position) {
        return position;
    }
}
