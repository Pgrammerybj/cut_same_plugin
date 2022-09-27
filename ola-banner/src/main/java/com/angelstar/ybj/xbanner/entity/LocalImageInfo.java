package com.angelstar.ybj.xbanner.entity;

import androidx.annotation.DrawableRes;

/**
 * author: yangbaojiang.
 * time: 2018/12/3
 * mail:pgrammer.ybj@outlook.com
 * github:https://github.com/Pgrammerybj
 * describe: 本地资源图片
 */
public class LocalImageInfo extends SimpleBannerInfo {

    @DrawableRes
    private int bannerRes;

    public LocalImageInfo(int bannerRes) {
        this.bannerRes = bannerRes;
    }

    @Override
    public Integer getXBannerUrl() {
        return bannerRes;
    }
}
