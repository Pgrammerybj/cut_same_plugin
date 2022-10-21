package com.vesdk.verecorder.record;

import android.app.Application;
import android.util.Log;

import com.vesdk.vebase.app.IModuleInit;


/**
 *  on 2018/6/21 0021.
 */

public class RecorderModuleInit implements IModuleInit {

    public static final String TAG = "------";

    @Override
    public boolean onInitAhead(Application application) {
        Log.e(TAG, "拍摄模块初始化 RecorderModuleInit onInitAhead.....");
        return false;
    }

    @Override
    public boolean onInitLow(Application application) {
        Log.e(TAG, "拍摄模块初始化 RecorderModuleInit onInitLow.....");
        return false;
    }
}
