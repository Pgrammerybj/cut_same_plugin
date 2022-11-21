package com.cutsame.ui.cut.lyrics;

import androidx.annotation.Keep;

import java.util.List;

/**
 * @Author：yangbaojiang
 * @Date: 2022/11/2 11:49
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: 编辑歌词样式字体
 */
@Keep
public class FontItemEntry {

    private String type;
    private FontResource resource;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FontResource getResource() {
        return resource;
    }

    public void setResource(FontResource resource) {
        this.resource = resource;
    }

    static class FontResource{
        private List<FontItem> list;

        public List<FontItem> getList() {
            return list;
        }

        public void setList(List<FontItem> list) {
            this.list = list;
        }

        static class FontItem{
            //字体封面
            String icon;
            //字体名称
            String name;
            //字体在本地的路径
            String path;
            //当前字体是否已经下载
            boolean isDownload = false;

            public String getIcon() {
                return icon;
            }

            public void setIcon(String icon) {
                this.icon = icon;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getPath() {
                return path;
            }

            public void setPath(String path) {
                this.path = path;
            }

            public boolean isDownload() {
                return isDownload;
            }

            public void setDownload(boolean download) {
                isDownload = download;
            }
        }
    }
}
