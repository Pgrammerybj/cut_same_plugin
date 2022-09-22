package com.vesdk.vebase.app;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import com.ss.android.vesdk.VEAuth;
import com.ss.android.vesdk.VEConfigCenter;
import com.ss.android.vesdk.VEConfigKeys;
import com.ss.android.vesdk.VELogProtocol;
import com.ss.android.vesdk.VESDK;
import com.vesdk.vebase.LogUtils;
import com.vesdk.vebase.ToastUtils;
import com.vesdk.vebase.old.util.FileUtil;

import java.io.File;

import static com.ss.android.vesdk.VELogUtil.BEF_LOG_LEVEL_DEBUG;
import static com.ss.android.vesdk.VELogUtil.LOG_LEVEL_D;


/**
 * on 2018/6/21 0021.
 * 基础库自身初始化操作
 */
public class BaseModuleInit implements IModuleInit {
    public static final String TAG = "---JackYang---";

    @Override
    public boolean onInitAhead(Application application) {
        Log.e(TAG, "基础层初始化 BaseModuleInit onInitAhead.....");
        initVESDK(application);
        return false;
    }

    private static final String LICENSE_PATH = "resource/LicenseBag.bundle";
    private static final String LICENSE_NAME = "labcv_test_20220608_20221231_com.bytedance.solution.ck_4.0.2.5.licbag";

    private void initVESDK(Application application) {
        VESDK.setAssetManagerEnable(true);
        // 初始化vesdk 环境
        VESDK.init(application, Environment.getExternalStorageDirectory().getAbsolutePath());

        File licenseDir = application.getExternalFilesDir("license");
        File licenseTarget = new File(licenseDir, LICENSE_NAME); // 0/Android/data/com.ss.android.vesdk.vedemo/files/license/ve_tob.licbag
        boolean copy = FileUtil.copyAssetFile(application, LICENSE_PATH + "/" + LICENSE_NAME, licenseTarget.getAbsolutePath());
        if (!copy) {
            ToastUtils.show("license文件未复制成功");
            LogUtils.e("license文件未复制成功...");
        }

        LogUtils.d("License file exist: " + licenseTarget.exists() + ",path:" + licenseTarget.getAbsolutePath()
                + " ,file size:" + licenseTarget.length());

        //进行鉴权
        int result = VEAuth.init(licenseTarget.getAbsolutePath());
        // 如果result!=0 代表鉴权文件过期或者是鉴权不通过，需控制后续sdk相关业务逻辑，防止使用sdk崩溃
        if (result != 0) {
            ToastUtils.show("鉴权出问题啦~");
        }
        AuthUtil.getInstance().init(result);

        //设置log输出等级
        VESDK.setLogLevel(LOG_LEVEL_D);
        VESDK.setEffectLogLevel(BEF_LOG_LEVEL_DEBUG);
        VESDK.registerLogger(new VELogProtocol() {
            @Override
            public void logToLocal(int i, String s) {
                LogUtils.d("logToLocal: " + s);
            }
        });

        //开启sticker新引擎
        VESDK.setEnableStickerAmazing(true);

        // 解决导入16：9的视频或图片出现黑边的问题
        VEConfigCenter.getInstance()
                .updateValue(VEConfigKeys.KEY_ENABLE_RENDER_ENCODE_RESOLUTION_ALIGN4, true);
    }

    @Override
    public boolean onInitLow(Application application) {
        Log.e(TAG, "基础层初始化 BaseModuleInit onInitLow.....");
        return false;
    }
}