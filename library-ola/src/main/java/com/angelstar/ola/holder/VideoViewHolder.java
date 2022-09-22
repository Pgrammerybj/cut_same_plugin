package com.angelstar.ola.holder;

import android.net.Uri;
import android.view.View;
import android.widget.VideoView;

import com.angelstar.ola.R;
import com.angelstar.ola.entity.CustomViewsInfo;
import com.angelstar.ybj.xbanner.holder.ViewHolder;

public class VideoViewHolder implements ViewHolder<CustomViewsInfo> {
    public VideoView videoView;

    private final String url_path = "http://lf3-ck.bytetos.com/obj/template-bucket/7117128998756794382/baiyueguangzhushazhi_7002448424021524488/baiyueguangzhushazhi_7002448424021524488.mp4";

    @Override
    public int getLayoutId() {
        return R.layout.layout_video_view;
    }

    @Override
    public void onBind(View itemView, CustomViewsInfo data, int position) {
        videoView = itemView.findViewById(R.id.player);
//        videoView.setUrl("http://vfx.mtime.cn/Video/2019/03/18/mp4/190318231014076505.mp4");
//        videoView.setUrl("http://lf3-ck.bytetos.com/obj/template-bucket/7117128998756794382/baiyueguangzhushazhi_7002448424021524488/baiyueguangzhushazhi_7002448424021524488.mp4");
        videoView.setVideoURI(Uri.parse(url_path));
    }
}
