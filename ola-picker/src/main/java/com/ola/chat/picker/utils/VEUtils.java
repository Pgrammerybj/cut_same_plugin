package com.ola.chat.picker.utils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/17 19:48
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public class VEUtils {

    public static int getVideoFileInfo(@NonNull String strInVideo, @NonNull int[] outInfo) {
//        MonitorUtils.monitorStatistics("iesve_veutils_get_video_info", 1, (VEKeyValue)null);
        VEUtils.VEVideoFileInfo info = (VEUtils.VEVideoFileInfo)TEVideoUtils.getVideoFileInfo(strInVideo, (int[])null);
        if (info == null) {
            return -1;
        } else {
            int length = Math.min(outInfo.length, 12);
            switch(length) {
                case 12:
                    outInfo[11] = info.bitDepth;
                case 11:
                    outInfo[10] = info.maxDuration;
                case 10:
                    outInfo[9] = info.keyFrameCount;
                case 9:
                    outInfo[8] = info.codec;
                case 8:
                    outInfo[7] = info.fps;
                case 7:
                    outInfo[6] = info.bitrate;
                case 4:
                case 5:
                case 6:
                    outInfo[3] = info.duration;
                case 3:
                    outInfo[2] = info.rotation;
                case 2:
                    outInfo[1] = info.height;
                case 1:
                    outInfo[0] = info.width;
                default:
                    return 0;
            }
        }
    }


    public static String getVideoEncodeTypeByID(@NonNull int codecID) {
        String codecType = "unknown";
        switch(codecID) {
            case 2:
                codecType = "mpeg2";
                break;
            case 5:
                codecType = "h263";
                break;
            case 13:
                codecType = "mpeg4";
                break;
            case 28:
                codecType = "h264";
                break;
            case 140:
                codecType = "vp8";
                break;
            case 168:
                codecType = "vp9";
                break;
            case 174:
                codecType = "bytevc1";
        }

        return codecType;
    }

    @Keep
    public static class VEVideoFileInfo {
        public int width;
        public int height;
        public int rotation;
        public int duration;
        public int bitrate;
        public int fps;
        public int codec;
        public int keyFrameCount;
        public int maxDuration;
        public String formatName;
        public int bitDepth;
        public boolean bHDR = false;
        public int isSupportImport = -1;

        public VEVideoFileInfo() {
        }

        public String toString() {
            return "width = " + this.width + ", height = " + this.height + ", rotation = " + this.rotation + ", duration = " + this.duration + ", bitrate = " + this.bitrate + ", fps = " + this.fps + ", codec = " + this.codec + ", keyFrameCount = " + this.keyFrameCount + ", maxDuration = " + this.maxDuration + ", formatName = " + this.formatName;
        }
    }
}
