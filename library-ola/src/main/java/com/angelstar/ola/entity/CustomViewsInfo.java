package com.angelstar.ola.entity;

import com.angelstar.ybj.xbanner.entity.BaseBannerInfo;

/**
 * author: yangbaojiang.
 * time: 2018/12/3
 * mail:pgrammer.ybj@outlook.com
 * github:https://github.com/Pgrammerybj
 * describe: CustomViewsInfo 继承 SimpleBannerInfo 根据个人情况重载两个方法
 */
public class CustomViewsInfo implements BaseBannerInfo {

    private String info;

    public CustomViewsInfo(String info) {
        this.info = info;
    }

    @Override
    public String getXBannerUrl() {
        return info;
    }

    @Override
    public String getXBannerTitle() {
        return null;
    }

}
