// Copyright (C) 2018 Beijing Bytedance Network Technology Co., Ltd.
package com.vesdk.vebase;

import android.util.Log;


public class LogUtils {

    private static final String TAG = "VESDK_DEMO----";

    public static void v(String msg) {
        Log.v(TAG, msg);
    }

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    public static void i(String msg) {
        Log.i(TAG, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void d(String msg) {
        Log.d(TAG, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
    }

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }
}
