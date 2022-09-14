package com.vesdk.vebase.old.assist;

import  androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import com.bytedance.android.winnow.WinnowHolder;

import com.vesdk.vebase.old.model.DataContainer;
import com.vesdk.vebase.R;

/**
 * time : 2020/6/2
 *
 * description :
 * 缩略图holder
 */
public class ThumbnailHolder extends WinnowHolder<DataContainer.Thumbnail> {
    public ThumbnailHolder(View itemView) {
        super(itemView);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.holder_thumb;
    }

    @Override
    protected void onBindData(@NonNull DataContainer.Thumbnail thumb) {
        ((ImageView) itemView.findViewById(R.id.image)).setImageBitmap(thumb.thumb);
    }
}
