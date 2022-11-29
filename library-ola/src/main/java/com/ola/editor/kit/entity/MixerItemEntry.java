package com.ola.editor.kit.entity;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/29 19:18
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 调音台条目
 */
public class MixerItemEntry {

    private String mixerImage;
    private String mixerTitle;
    private String mixerLabel;
    private Boolean isSelected;

    public MixerItemEntry(String mixerImage, String mixerTitle, String mixerLabel, Boolean isSelected) {
        this.mixerImage = mixerImage;
        this.mixerTitle = mixerTitle;
        this.mixerLabel = mixerLabel;
        this.isSelected = isSelected;
    }

    public String getMixerImage() {
        return mixerImage;
    }

    public void setMixerImage(String mixerImage) {
        this.mixerImage = mixerImage;
    }

    public String getMixerTitle() {
        return mixerTitle;
    }

    public void setMixerTitle(String mixerTitle) {
        this.mixerTitle = mixerTitle;
    }

    public String getMixerLabel() {
        return mixerLabel;
    }

    public void setMixerLabel(String mixerLabel) {
        this.mixerLabel = mixerLabel;
    }

    public Boolean getIsSelected() {
        return isSelected;
    }

    public void setIsSelected(Boolean isSelected) {
        this.isSelected = isSelected;
    }
}
