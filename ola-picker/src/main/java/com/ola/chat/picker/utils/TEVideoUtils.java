package com.ola.chat.picker.utils;

import androidx.annotation.Keep;
import androidx.annotation.RestrictTo;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/17 19:50
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
@Keep
@RestrictTo({RestrictTo.Scope.LIBRARY})
public final class TEVideoUtils {
    private static long getFrameInterval;

    public TEVideoUtils() {
    }

    public static Object getVideoFileInfo(String strInVideo, int[] outInfo_) {
        return nativeGetFileInfo(strInVideo, outInfo_);
    }

    private static native Object nativeGetFileInfo(String var0, int[] var1);

}

