package com.cutsame.ui.cut.lyrics;

import androidx.annotation.Keep;

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/2 11:49
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 编辑歌词样式字体
 */
@Keep
public class FontItemEntry {

    public FontItemEntry() {
    }

    public FontItemEntry(String fontCover, String fontName, boolean isSelect, boolean isDownload) {
        this.fontCover = fontCover;
        this.fontName = fontName;
        this.isSelect = isSelect;
        this.isDownload = isDownload;
    }

    //字体封面
    String fontCover;
    //字体名称
    String fontName;
    //当前是否选中
    boolean isSelect = false;
    //当前字体是否已经下载
    boolean isDownload = false;

    public String getFontCover() {
        return fontCover;
    }

    public void setFontCover(String fontCover) {
        this.fontCover = fontCover;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public boolean isSelect() {
        return isSelect;
    }

    public void setSelect(boolean select) {
        isSelect = select;
    }

    public boolean isDownload() {
        return isDownload;
    }

    public void setDownload(boolean download) {
        isDownload = download;
    }
}
