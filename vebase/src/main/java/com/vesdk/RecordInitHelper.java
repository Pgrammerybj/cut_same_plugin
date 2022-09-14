package com.vesdk;

import android.content.Context;

import com.vesdk.vebase.ToastUtils;
import com.vesdk.vebase.UserData;
import com.vesdk.vebase.task.UnzipTask;

import java.lang.ref.WeakReference;

public class RecordInitHelper {
    private static Context applicationContext;

    public static void setApplicationContext(Context applicationContext) {
        RecordInitHelper.applicationContext = applicationContext;
        ToastUtils.init(applicationContext);
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }

    private WeakReference<UnzipTask.IUnzipViewCallback> mCallback;

    /**
     * 初始化资源等信息
     *
     * @param callback
     */
    public static void initResource(final UnzipTask.IUnzipViewCallback callback) {
        if (!UserData.getInstance(getApplicationContext()).isResourceReady()) {
            UnzipTask mTask = new UnzipTask(new UnzipTask.IUnzipViewCallback() {
                @Override
                public Context getContext() {
                    return getApplicationContext();
                }

                @Override
                public void onStartTask() {
                    if (callback != null) {
                        callback.onStartTask();
                    }
                }

                @Override
                public void onEndTask(boolean result) {
                    if (result) {
                        UserData.getInstance(getApplicationContext()).setResourceReady(true);
                    }
                    if (callback != null) {
                        callback.onEndTask(result);
                    }
                }
            });
            mTask.execute(UnzipTask.DIR, UnzipTask.LOCAL_DIR);
        } else {
            if (callback != null) {
                callback.onEndTask(true);
            }
        }
    }

}
