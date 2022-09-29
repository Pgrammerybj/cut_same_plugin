package com.angelstar.ola;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/29 14:30
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public interface ITemplateVideoStateListener {
    void onPlayViewActivate(Boolean activate);

    void onPlayTimeChanged(String curPlayerTime, String totalPlayerTime, boolean isPause);
}
