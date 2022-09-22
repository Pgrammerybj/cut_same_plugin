package com.angelstar.ola.holder;

import android.view.View;
import android.widget.ImageView;

import com.angelstar.ola.R;
import com.angelstar.ola.entity.CustomViewsInfo;
import com.angelstar.ybj.xbanner.holder.ViewHolder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

public class ImageViewHolder implements ViewHolder<CustomViewsInfo> {

    @Override
    public int getLayoutId() {
        return R.layout.layout_image_view;
    }

    @Override
    public void onBind(View itemView, CustomViewsInfo data, int position) {

        // 加载为四个都是圆角的图片 可以设置圆角幅度
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .bitmapTransform(new RoundedCorners(40));

        ImageView imageView = itemView.findViewById(R.id.iv);
        Glide.with(itemView.getContext()).load(data.getXBannerUrl()).apply(options).into(imageView);
    }
}
