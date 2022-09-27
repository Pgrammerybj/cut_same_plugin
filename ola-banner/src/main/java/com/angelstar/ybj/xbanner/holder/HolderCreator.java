package com.angelstar.ybj.xbanner.holder;



public interface HolderCreator<VH extends ViewHolder> {
    /**
     * 创建ViewHolder
     */
    VH createViewHolder(int viewType);

    /**
     * ViewType
     * @param position
     * @return
     */
    int getViewType(int position);
}
