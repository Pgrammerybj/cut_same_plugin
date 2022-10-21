package com.vesdk.vebase.task;

import android.content.Context;
import android.os.AsyncTask;


import com.vesdk.RecordInitHelper;
import com.vesdk.vebase.LogUtils;
import com.vesdk.vebase.resource.ResourceHelper;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 *  on 2019-07-20 13:05
 */
public class UnzipTask extends AsyncTask<String, Void, Boolean> {
    public static final String DIR = "resource";
    public static final String LOCAL_DIR = "LocalResource";

    public interface IUnzipViewCallback {
        Context getContext();
        void onStartTask();
        void onEndTask(boolean result);
    }

    private WeakReference<IUnzipViewCallback> mCallback;

    public UnzipTask(IUnzipViewCallback callback) {
        mCallback = new WeakReference<>(callback);
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        IUnzipViewCallback callback = mCallback.get();
        if (callback == null) return false;
        boolean isSucRes = copyResource(callback, strings[0]);
        boolean isSucLocalRes = copyResource(callback, strings[1]);

        if (isSucRes && isSucLocalRes) {
            return true;
        }

        LogUtils.d("isSucRes:" + isSucRes + "  isSucLocalRes:" + isSucLocalRes);
        return false;

//        return FileUtils.unzipAssetFile(mCallback.get().getContext(), zipPath, dstFile);
    }

    private boolean copyResource(IUnzipViewCallback callback, String path) {
        File dstFile = callback.getContext().getExternalFilesDir("assets");
//        FileUtils.clearDir(new File(dstFile, path));

        LogUtils.d("path:" + path + "  dstFile:" + dstFile.getAbsolutePath() ); //dstFile:/storage/emulated/0/Android/data/com.ss.android.vesdk.vedemo/files/assets

        try {
            FileUtils.copyAssets(callback.getContext().getAssets(), path, dstFile.getAbsolutePath());
//            FileUtils.copyAssets(callback.getContext().getAssets(), path, ROOT_TEST );

            initResource(RecordInitHelper.getApplicationContext(), path);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPreExecute() {
        IUnzipViewCallback callback = mCallback.get();
        if (callback == null) return;
        callback.onStartTask();
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        IUnzipViewCallback callback = mCallback.get();
        if (callback == null) return;
        callback.onEndTask(result);
    }

    private void initResource(Context context, String child) {
        String dstFile = ResourceHelper.getInstance().getResourceRootPath();

//        FileUtils.clearDir(new File(dstFile, child));

//        try {
//            FileUtil.unzip(application.getAssets().open("EditorResource.zip"), dstFile );
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        try {
            FileUtils.copyAssets(context.getAssets(), child, dstFile );
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
