package com.ola.download.utils;

import android.util.Log;

import com.ola.download.RxNetDownload;

public class LogUtils {
    private static final String TAG = "ola-jackyang-net";

    public static void d(String msg) {
        if (RxNetDownload.enableLog) {
            Log.d(TAG, msg);
        }
    }

    public static void i(String msg) {
        if (RxNetDownload.enableLog) {
            Log.i(TAG, msg);
        }
    }

    public static void e(String msg) {
        if (RxNetDownload.enableLog) {
            Log.e(TAG, msg);
        }
    }

}
