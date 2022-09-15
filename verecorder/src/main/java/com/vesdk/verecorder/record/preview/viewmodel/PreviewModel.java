package com.vesdk.verecorder.record.preview.viewmodel;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;

import com.ss.android.ttvecamera.TECameraSettings;
import com.ss.android.vesdk.VECameraCapture;
import com.ss.android.vesdk.VEDisplaySettings;
import com.ss.android.vesdk.VEFocusSettings;
import com.ss.android.vesdk.VEListener;
import com.ss.android.vesdk.VEPreviewRadio;
import com.ss.android.vesdk.VERecorder;
import com.ss.android.vesdk.VEResult;
import com.ss.android.vesdk.VESize;
import com.ss.android.vesdk.VEVideoEncodeSettings;
import com.vesdk.RecordInitHelper;
import com.vesdk.vebase.LiveDataBus;
import com.vesdk.vebase.LogUtils;
import com.vesdk.vebase.util.AutoPlusCircleInteger;
import com.vesdk.vebase.util.GsonUtil;
import com.vesdk.verecorder.R;
import com.vesdk.verecorder.record.ScreenUtils;
import com.vesdk.verecorder.record.preview.model.CountDown;
import com.vesdk.verecorder.record.preview.model.PreviewConfig;
import com.vesdk.verecorder.record.preview.model.Resolution;
import com.vesdk.verecorder.record.preview.model.ZoomConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * time : 2020/11/18
 * <p>
 * description :
 */
public class PreviewModel extends AndroidViewModel implements LifecycleObserver {
    private static final String TAG = "PreviewModel";
    private VECameraCapture capture;
    private VERecorder recorder;
    private static final int[] RESOLUTIONS = new int[]{540, 720, 1080};
    public final String[] RESOLUTIONS_NAME = new String[]{"540P", "720P", "1080P"};
    private MutableLiveData<Integer> resolution = new MutableLiveData<Integer>();
    public MutableLiveData<VEPreviewRadio> ratio = new MutableLiveData<VEPreviewRadio>();
    //    public ArrayList<Ratio> ratios = new ArrayList<>();
    public ArrayList<Resolution> resolutions = new ArrayList<>();
    public ArrayList<ZoomConfig> zoomConfigs = new ArrayList<>();

    public String[] pics = {"9:16", "3:4", "1:1", "4:3", "16:9"}; // , "18:9", "21:9"
    public String[] zooms = {"1.0x", "2.0x", "3.0x", "4.0x", "5.0x", "6.0x"};
    public float[] ratios = {16 * 1f / 9, 4 * 1f / 3, 1f, 3 * 1f / 4, 9 * 1f / 16}; //, 9 * 1f / 18, 9 * 1f / 21
    public int currentIndex = 0;
    public MutableLiveData<String> resolutionName = new MutableLiveData<String>();
    public MutableLiveData<String> picName = new MutableLiveData<String>();
    //    public MutableLiveData<CountDown> countDown = new MutableLiveData<>();
    public LiveDataBus.BusMutableLiveData<CountDown> countDown = new LiveDataBus.BusMutableLiveData<>();

    public MutableLiveData<CountDown> zoom = new MutableLiveData<>();
    // 0：开始录制
    public MutableLiveData<Integer> recordEvent = new MutableLiveData<>();
    public ArrayList<CountDown> countDowns = new ArrayList();
    public AutoPlusCircleInteger countAuto = new AutoPlusCircleInteger(0, 2);
    public AutoPlusCircleInteger picAuto = new AutoPlusCircleInteger(0, 4);
    public AutoPlusCircleInteger resolutionAuto = new AutoPlusCircleInteger(0, 2);
    public AutoPlusCircleInteger zoomAuto = new AutoPlusCircleInteger(0, 5);


    private CountDown currentCountDown;
    private TECameraSettings.ExposureCompensationInfo cameraECInfo;
    private OrientationEventListener orientationListener;
    private int orientation;
    public MutableLiveData<Resolution> curConfig = new MutableLiveData<Resolution>();
    @Nullable
    private PreviewConfig previewConfig = null;
    boolean isDuet; //是否为合拍模式,默认false
    private boolean hasVideoPartChange = true;
    private float cutSameRatio;

    public PreviewModel(@NonNull Application application) {
        super(application);
        resolution.setValue(RESOLUTIONS[1]);
        countDowns.add(new CountDown("无定时", 0, R.drawable.ic_no_delay));
        countDowns.add(new CountDown("3s", 3, R.drawable.ic_delay_3));
        countDowns.add(new CountDown("7s", 7, R.drawable.ic_delay_7));

        CountDown countDown = countDowns.get(0);
        this.currentCountDown = countDown;
        this.countDown.setValue(countDown);

        for (int i = 0; i < zooms.length; i++) {
            zoomConfigs.add(new ZoomConfig(zooms[i], (i + 1) * 1f));
        }

    }

    private void queryZoomAbility() {
        capture.setZoomListener(new VERecorder.VECameraZoomListener() {
            @Override
            public boolean enableSmooth() {
                return false;
            }

            @Override
            public void onZoomSupport(int cameraType, boolean supportZoom, boolean supportSmooth, float maxZoom, List<Integer> ratios) {
                Log.i(TAG, "onZoomSupport: cameraType: " + cameraType + " supportZoom: " + supportZoom + " supportSmooth:" + supportSmooth + " maxZoom:" + maxZoom + " ratios:" + ratios.toString());
            }

            @Override
            public void onChange(int cameraType, float zoomValue, boolean stopped) {
                Log.i(TAG, "onChange:  + cameraType:" + cameraType + " zoomValue:" + zoomValue + " stopped:" + stopped);
            }
        });
        capture.setShaderZoomListener(new VERecorder.VEShaderZoomListener() {
            @Override
            public void getShaderStep(float step) {
                Log.i(TAG, "getShaderStep: step:" + step);
            }
        });
        capture.queryShaderZoomAbility();
        capture.queryZoomAbility();
    }

    public void inject(VECameraCapture capture, VERecorder recorder) {
        this.capture = capture;
        this.recorder = recorder;
        queryZoomAbility();
        initCameraECInfo();
    }

    private void initCameraECInfo() {
        capture.setCameraStateListener(new VEListener.VECameraStateExtListener() {

            @Override
            public void onError(int ret, String msg) {
                LogUtils.d("onError: ret:" + ret + " msg:" + msg);
            }

            @Override
            public void onInfo(int infoType, int ext, String msg) {
//                LogUtils.d("onInfo: " + infoType + " ext:" + ext + " msg:" + msg);
            }

            @Override
            public void cameraOpenSuccess() {
                cameraECInfo = capture.getCameraECInfo();
                LogUtils.d("initCameraECInfo: " + GsonUtil.toJson(cameraECInfo));
            }

            @Override
            public void cameraOpenFailed(int cameraType) {

            }
        });
    }


    /**
     * 调整曝光
     *
     * @param value
     */
    public void setExposureCompensation(int value) {
        try {
            float ratio = value * 1f / 100f;
            float realValue = ratio * (cameraECInfo.max - cameraECInfo.min) + cameraECInfo.min;
            LogUtils.d("曝光值：" + realValue + "  " + (int) realValue);
            capture.setExposureCompensation((int) realValue);
        } catch (Exception e) {
            LogUtils.d("调整曝光Exception：" + e.getMessage());
        }
    }

    public void zoom(float zoom) {
        capture.startZoom(zoom);
    }

    // 多画幅
    public int changePic(int index) {
        if (index != -1) {
            this.currentIndex = index;
        }
        changeResolution(pics[currentIndex], resolution.getValue(), ratios[currentIndex], false);

        if (currentIndex > 2) {
            enableLandscape(true);
        } else {
            enableLandscape(false);
        }

        return 0;
    }

    private Resolution config;

    public Resolution getResolution() {
        return config;
    }

    /**
     * 输出分辨率
     *
     * @param index
     * @return
     */
    public int changeRatio(int index) {
        resolution.setValue(RESOLUTIONS[index]);
        config = new Resolution(pics[currentIndex], resolution.getValue(), ratios[currentIndex]);
        LogUtils.d("changeRatio: config: " + config.toString());
        recorder.changeVideoEncodeSettings(
                new VEVideoEncodeSettings
                        .Builder(VEVideoEncodeSettings.USAGE_RECORD, recorder.getVideoEncodeSettings())
                        .setVideoRes(config.width, config.height)
                        .build());
//        recorder.changeVideoOutputSize(config.width, config.height);
        return 0;
    }


    public void delayRecord() {
        this.countDown.setValue(currentCountDown);
    }


    public CountDown getCountDown() {
        return currentCountDown;
    }

    public CountDown setCountDown() {
        CountDown countDown = countDowns.get(countAuto.get());
        this.currentCountDown = countDown;
        return countDown;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void onResume() {
//        changePic(currentIndex);
    }

    /**
     * 设置对焦
     *
     * @param settings
     * @return
     */
    public boolean focusAtPoint(VEFocusSettings settings) {
        return capture.focusAtPoint(settings) == VEResult.TER_OK;
    }

    public void registerOrientation(Context context) {
        orientationListener = new OrientationEventListener(context) {

            @Override
            public void onOrientationChanged(int orientation) {
                Log.i(TAG, "onOrientationChanged: " + orientation);
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;  //手机平放时，检测不到有效的角度
                }
                //这种方式只是演示api,平放无角度这种需要业务自行适配

                //只检测是否有四个角度的改变
                if (orientation > 350 || orientation < 10) { //0度
                    orientation = 0;
                } else if (orientation > 80 && orientation < 100) { //90度
                    orientation = 90;
                } else if (orientation > 170 && orientation < 190) { //180度
                    orientation = 180;
                } else if (orientation > 260 && orientation < 280) { //270度
                    orientation = 270;
                } else {
                    return;
                }
                if (PreviewModel.this.orientation == orientation) {
                    return;
                }
                PreviewModel.this.orientation = orientation;

                Log.i(TAG, "onOrientationChanged set display orientation: " + orientation);
                config = new Resolution(pics[currentIndex], resolution.getValue(), ratios[currentIndex]);
                VEDisplaySettings settings = new VEDisplaySettings.Builder()
                        .setDisplayRatio(ratios[currentIndex])
                        .setRotation(orientation)
                        .build();
                recorder.setDisplaySettings(settings);
            }
        };
    }

    public void enableLandscape(boolean enable) {
        if (orientationListener != null) {
            if (enable) {
                if (orientationListener.canDetectOrientation()) {
                    orientationListener.enable();
                }
            } else {
                orientationListener.disable();
            }
        }
    }

    public void setPreviewConfig(@Nullable PreviewConfig previewConfig, boolean isDuet) {
        this.previewConfig = previewConfig;
        this.isDuet = isDuet;
    }

    public int getUIStyle() {
        if (isDuet) {
            return PreviewConfig.Builder.UI_STYLE_DUT;//兼容原合拍模式
        }
        if (previewConfig == null) {
            return PreviewConfig.Builder.UI_STYLE_DEFAULT;
        }
        return previewConfig.getUiStyle();
    }

    /**
     * 是否使用重构后的recorder,重构后的录制接口为异步接口，需要区分开
     */
    public boolean isRefactorRecorder() {
        if (previewConfig != null) {
            return previewConfig.getEnableRefactorRecorder();
        }
        return false;
    }
    /**
     * 是否录制原声
     */
    public boolean enableAudioRecorder() {
        if (previewConfig != null) {
            return previewConfig.getEnableAudioRecorder();
        }
        return false;
    }

    @Nullable
    public PreviewConfig getPreviewConfig() {
        return previewConfig;
    }

    public void adjustViewMargin(View view, float marginDp) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.bottomMargin = ScreenUtils.dp2px(RecordInitHelper.getApplicationContext(), marginDp);
    }

    public void setVideoPartChange(boolean hasVideoPartChange) {
        this.hasVideoPartChange = hasVideoPartChange;
    }

    public boolean hasVideoChange() {
        return hasVideoPartChange;
    }

    /**
     * 更新剪同款素材约束条件
     */
    public void updateCutSameConstraints(int width, int height, long duration, boolean changeRenderSize) {
        float ratio = height * 1f / width;
        if (cutSameRatio != ratio) {
            this.cutSameRatio = ratio;
            changeResolution("custom", resolution.getValue(), this.cutSameRatio, changeRenderSize);
        }
    }

    private void changeResolution(String name, int resolution, float ratio, boolean changeRenderSize) {
        config = new Resolution(name, resolution, ratio);
        curConfig.postValue(config);
        VEDisplaySettings.Builder builder = new VEDisplaySettings.Builder()
                // 设置显示比例，比例=视频高度/视频宽度。
                .setDisplayRatio(ratio);
        if (changeRenderSize) {
            builder.setRenderSize(new VESize(config.width, config.height));
        }
        //切换预览
        recorder.setDisplaySettings(builder.build());
        //同时设置输出视频
        recorder.changeVideoEncodeSettings(
                new VEVideoEncodeSettings
                        .Builder(VEVideoEncodeSettings.USAGE_RECORD, recorder.getVideoEncodeSettings())
                        .setVideoRes(config.width, config.height)
                        .build());
//        recorder.changeVideoOutputSize(config.width, config.height);
        LogUtils.d("changeResolution: config: " + config.toString() + "   currentIndex:" + currentIndex);

    }

    public boolean autoChangeDisplay() {
        PreviewConfig previewConfig = getPreviewConfig();
        if (previewConfig != null) {
            return previewConfig.getAutoChangeDisplay();
        }
        return true;
    }

    public String getSavePath(Context context) {
        PreviewConfig previewConfig = getPreviewConfig();
        String path;
        if (previewConfig != null && !previewConfig.getSaveAlbum()) {
            File vePath = context.getExternalFilesDir("temp_pic");
            try {
                if (!vePath.exists() || !vePath.isDirectory()) {
                    if (vePath.mkdirs()) {
                        new File(vePath, ".nomedia").createNewFile();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            path = vePath.getAbsolutePath();
        } else {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
        }
        return path + File.separator + "picture_" + System.currentTimeMillis() + ".png";
    }
}
