package com.vesdk.vebase.demo.present;

import android.content.Context;
import android.content.pm.PackageManager;

import com.ss.android.vesdk.VESDK;
import com.ss.android.vesdk.VEVersionUtil;
import com.vesdk.RecordInitHelper;
import com.vesdk.vebase.LogUtils;
import com.vesdk.vebase.UserData;
import com.vesdk.vebase.demo.present.contract.WelcomeContract;
import com.vesdk.vebase.task.UnzipTask;


/**
 *  on 2019-07-20 17:30
 */
public class WelcomePresenter extends WelcomeContract.Presenter implements UnzipTask.IUnzipViewCallback {

    private UserData mUserData;
    public WelcomePresenter() {
        mUserData = UserData.getInstance(RecordInitHelper.getApplicationContext());
    }

    @Override
    public void startTask() {
        LogUtils.d("WelcomePresenter startTask---------");
        UnzipTask mTask = new UnzipTask(this);
        mTask.execute(UnzipTask.DIR, UnzipTask.LOCAL_DIR);
    }

    @Override
    public int getVersionCode() {
        Context context = RecordInitHelper.getApplicationContext();
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Override
    public String getVersionName() {
//        SDKInfoUtil.getInfoText();
        return "vesdk:" + VEVersionUtil.getVESDKVersion() +"     effect:"+ VESDK.getEffectSDKVer();
//        Context context = BaseContextHelper.getApplicationContext();
//        try {
//            return "v " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//            return "";
//        }
    }

    @Override
    public boolean resourceReady() {
        return mUserData.isResourceReady() ; // && mUserData.getVersion() == getVersionCode()
    }


    @Override
    public Context getContext() {
        return RecordInitHelper.getApplicationContext();
    }

    @Override
    public void onStartTask() {
        if (isAvailable()) {
            getView().onStartTask();
        }
    }

    @Override
    public void onEndTask(boolean result) {
        LogUtils.d("WelcomePresenter onEndTask---------" + result);
        if (getView() == null) return;
        if (result) {
            mUserData.setResourceReady(true);
//            mUserData.setVersion(getVersionCode());
        }
        if (isAvailable()) {
            getView().onEndTask(result);
        }
    }
}
