package com.ola.editor.kit.interfaces;

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/22 18:52
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 音频播放进度回调
 */
public interface ITemplateAudioPlayListener {
    void onPlayTimeChanged(float progress);
}
