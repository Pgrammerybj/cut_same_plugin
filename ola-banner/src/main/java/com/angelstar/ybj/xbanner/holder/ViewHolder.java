package com.angelstar.ybj.xbanner.holder;

import android.view.View;

import androidx.annotation.LayoutRes;


public interface ViewHolder<T> {

    /**
     * @return 轮播条目布局文件
     */
    @LayoutRes
    int getLayoutId();

    /**
     * 绑定数据
     */
    void onBind(View itemView, T data, int position);
}
