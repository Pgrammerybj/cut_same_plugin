package com.angelstar.ola.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * @Author：yangbaojiang
 * @Date: 2022/10/26 14:57
 * @E-Mail: pgrammer.ybj@outlook.com
 * @Description: android
 */
public class ThreadUtils {
    public static final Handler uiHandler= new Handler(Looper.getMainLooper());
}

