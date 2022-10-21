package com.vesdk.verecorder.record.demo.fragment;

import static com.ss.android.vesdk.VECameraSettings.CAMERA_FLASH_MODE.CAMERA_FLASH_OFF;
import static com.ss.android.vesdk.VECameraSettings.CAMERA_FLASH_MODE.CAMERA_FLASH_ON;
import static com.vesdk.vebase.demo.present.contract.StickerContract.TYPE_STICKER;
import static com.vesdk.verecorder.record.demo.fragment.PreviewBottomFragment.TAG_EFFECT;
import static com.vesdk.verecorder.record.demo.fragment.PreviewBottomFragment.TAG_FILTER;
import static com.vesdk.verecorder.record.demo.fragment.PreviewBottomFragment.TAG_STICKER;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bytedance.android.winnow.WinnowHolder;
import com.ss.android.vesdk.VEUtils;
import com.vesdk.vebase.Constant;
import com.vesdk.vebase.LiveDataBus;
import com.vesdk.vebase.LogUtils;
import com.vesdk.vebase.ToastUtils;
import com.vesdk.vebase.demo.base.BaseFragment;
import com.vesdk.vebase.demo.fragment.BaseFeatureFragment;
import com.vesdk.vebase.demo.model.ComposerNode;
import com.vesdk.vebase.demo.present.contract.PreviewContract;
import com.vesdk.vebase.old.util.PermissionUtil;
import com.vesdk.vebase.util.ThreadManager;
import com.vesdk.verecorder.R;
import com.vesdk.verecorder.record.demo.PreviewPresenter;
import com.vesdk.verecorder.record.demo.view.RecordTabView;
import com.vesdk.verecorder.record.preview.function.FeatureView;
import com.vesdk.verecorder.record.preview.model.CountDown;
import com.vesdk.verecorder.record.preview.model.LiveEventConstant;
import com.vesdk.verecorder.record.preview.model.PreviewConfig;
import com.vesdk.verecorder.record.preview.model.PreviewLifecycle;
import com.vesdk.verecorder.record.preview.model.ZoomConfig;
import com.vesdk.verecorder.record.preview.viewmodel.PreviewModel;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class PreviewFragment extends BaseFragment<PreviewContract.Presenter>
        implements PreviewContract.View, PreviewBottomFragment.OnItemClickListener {

    private SurfaceView surfaceView;
    private ImageView ivCapture;
    private static final String TAG = "Preview";
    private PreviewModel previewModel;
    private View rootView;
    private ViewGroup smallWindowContainer;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    public static final int ANIMATOR_DURATION = 400;

    private StickerFragment mStickerFragment; //道具 (贴纸)
    protected EffectFragment mEffectFragment; //美颜 (特效)
    protected EffectFragment mFilterFragment; //美颜 (滤镜)

    public static final int FEATURE_PIC = 0; //拍照
    public static final int FEATURE_VIDEO = 1; //视频
    public static final int FEATURE_DUET = 2; //合拍
    private int CURRENT_FEATURE = FEATURE_PIC;

    private TextView tvZoom, tv_record_time;
    private RecordTabView recordTab;
    private ImageView tabIndexLine;
    private View topFunction;

    boolean isDuet; //是否为合拍模式,默认false
    String duetVideoPath; //合拍的视频路径
    String duetAudioPath; //合拍的音频路径
    private boolean isGoingToEditorActivity = false;
    private View vRatio;
    private View vResolution;
    private String concatPath;

    public String getDuetVideoPath() {
        return duetVideoPath;
    }

    private PreviewLifecycle previewLifecycle;

    public static PreviewFragment getInstance(boolean isDuet, @Nullable String duetVideoPath, @Nullable String duetAudioPath) {
        PreviewFragment previewFragment = new PreviewFragment();
        Bundle arg = new Bundle();
        arg.putBoolean(Constant.isDuet, isDuet);
        if (duetVideoPath != null) {
            arg.putString(Constant.duetVideoPath, duetVideoPath);
        }
        if (duetVideoPath != null) {
            arg.putString(Constant.duetAudioPath, duetAudioPath);
        }
        previewFragment.setArguments(arg);
        return previewFragment;
    }

    public static PreviewFragment getInstance(@Nullable String effectType) {
        PreviewFragment previewFragment = new PreviewFragment();
        Bundle arg = new Bundle();
        if (effectType != null) {
            arg.putString(Constant.EFFECT_TYPE, effectType);
        }
        previewFragment.setArguments(arg);
        return previewFragment;
    }

    public static PreviewFragment getInstance(PreviewConfig config) {
        PreviewFragment previewFragment = new PreviewFragment();
        Bundle arg = new Bundle();
        arg.putBoolean(Constant.CUSTOM_CONFIG, true);
        arg.putParcelable(Constant.PREVIEW_CONFIG, config);
        previewFragment.setArguments(arg);
        return previewFragment;
    }


    @Override
    protected int getContentView() {
        return R.layout.recorder_fragment_preview;
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        onVisibleChange(!hidden);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        onVisibleChange(isVisibleToUser);
    }

    private void onVisibleChange(boolean isVisibleToUser) {
        if (previewLifecycle != null) {
            previewLifecycle.onCameraHiddenChanged(!isVisibleToUser);
        }
        if (mPresenter != null && mPresenter.getCapture() != null) {
            if (!isVisibleToUser) {
                mPresenter.getCapture().close();
            } else {
                mPresenter.getCapture().open();
            }
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = getArguments();
        PreviewConfig config = null;
        if (arguments != null) {
            isDuet = arguments.getBoolean(Constant.isDuet, false);
            duetVideoPath = arguments.getString(Constant.duetVideoPath);
            duetAudioPath = arguments.getString(Constant.duetAudioPath);
            config = arguments.getParcelable(Constant.PREVIEW_CONFIG);
        }

        previewModel = new ViewModelProvider(requireActivity(), ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication())).get(PreviewModel.class);
        previewModel.setPreviewConfig(config, isDuet);
        setPresenter(new PreviewPresenter(isDuet, duetVideoPath, duetAudioPath));
        initView(savedInstanceState);
        mPresenter.initRecorder(surfaceView);
        previewModel.inject(mPresenter.getCapture(), mPresenter.getRecorder());
        initConfig();
        initBottom();
        getLifecycle().addObserver(previewModel);

        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_FRAGMENT, Bitmap.class).observe(this, new Observer<Bitmap>() {
            @Override
            public void onChanged(@Nullable Bitmap bitmap) {
                LogUtils.d("onChanged----:" + bitmap);
                switch (previewModel.getUIStyle()) {
                    case PreviewConfig.Builder.UI_STYLE_CUT_SAME:
                        //nothing
                        break;
                    default:
                        ivCapture.setVisibility(View.VISIBLE);
                        ivCapture.setImageBitmap(bitmap);
                        hideFeature();
                        break;
                }
            }
        });

        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_SHOT_SUCCESS, String.class).observe(this, new Observer<String>() {
            @Override
            public void onChanged(String path) {
                LogUtils.d("onChanged----:" + path);
                if (previewLifecycle != null) {
                    previewLifecycle.onShotOrRecord(path, false, 0);
                }

            }
        });
        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_RECORD_STATE, Integer.class).observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer state) {
                LogUtils.d("EVENT_RECORD_STATE onChanged----:" + state);
                if (previewLifecycle != null) {
                    previewLifecycle.onRecordState(state);
                }

            }
        });
        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_GO_EDIT, Boolean.class).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                LogUtils.d("onChanged----:" + aBoolean);
                switch (previewModel.getUIStyle()) {
                    case PreviewConfig.Builder.UI_STYLE_CUT_SAME:
                        contactVideos();
                        break;
                    default:
                    case PreviewConfig.Builder.UI_STYLE_DUT:
                        if (isGoingToEditorActivity) return;
                        isGoingToEditorActivity = true;
                        ArrayList<String> videoPaths = new ArrayList<>();
                        long currentTime = System.currentTimeMillis();
                        String[] recordedVideoPaths = mPresenter.getRecorder().getRecordedVideoPaths();
                        //防止每次分段录制的视频名称一致1_frag_v 2_frag_v，导致到编辑页的轨道缓存问题，重命名解决
                        for (String recordedVideoPath : recordedVideoPaths) {
                            String newName = recordedVideoPath + "_" + currentTime;
                            LogUtils.d("片段路径newName:" + newName);
                            videoPaths.add(newName);
                            new File(recordedVideoPath).renameTo(new File(newName));
                        }

                        Intent intent = new Intent();
                        intent.setPackage(requireActivity().getPackageName());
                        intent.setAction("record_sdk_action_ve");
                        intent.putExtra("extra_key_from_type", 1);
                        intent.putExtra("extra_media_type", 3);
                        intent.putStringArrayListExtra("extra_video_paths", videoPaths);
                        startActivity(intent);
                        requireActivity().finish();
                        break;
                }

            }
        });

        //启用横屏拍摄
        previewModel.registerOrientation(getContext());

        generateFragment(TAG_EFFECT);
        mEffectFragment.init(); //初始化美颜效果

        //曝光
        ((SeekBar) findViewById(R.id.ec)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                previewModel.setExposureCompensation(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private void contactVideos() {
        if (previewLifecycle != null) {
            previewLifecycle.onContactVideo(false);
            ThreadManager.getTheadPool().execute(new Runnable() {
                @Override
                public void run() {
                    String[] paths = mPresenter.getRecorder().getRecordedVideoPaths();
                    if (previewModel.hasVideoChange() || concatPath == null) {
                        if (paths.length >= 2) {
                            concatPath = getContext().getExternalFilesDir("vesdk").getAbsolutePath() + File.separator + System.currentTimeMillis() + "-v.mp4";
                            int res = VEUtils.concatVideo(paths, concatPath);
                            LogUtils.d(res + " concatVideos" + paths);
                            if (res != 0) {
                                concatPath = null;
                            } else {
                                previewModel.setVideoPartChange(false);//避免重复合并视频
                            }
                        } else if (paths.length > 0) {
                            concatPath = paths[0];
                        } else {
                            LogUtils.e(" concatVideos none videos" + paths);
                            concatPath = null;
                            return;
                        }
                    }
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            long duration = mPresenter.getRecorder().getEndFrameTime();
                            previewLifecycle.onContactVideo(true);
                            if (previewLifecycle != null) {
                                previewLifecycle.onShotOrRecord(concatPath, true, duration);
                            }
                        }
                    });
                }
            });
        }
    }

    public PreviewModel getPreviewModel() {
        return previewModel;
    }


    private void initConfig() {
        if (getPreviewModel().autoChangeDisplay()) {
            findViewById(R.id.resolution).performClick();
        }

        if (previewLifecycle != null) {
            previewLifecycle.onCameraInit(mPresenter.getRecorder(), surfaceView, smallWindowContainer);
        }
    }

    private PreviewBottomFragment bottomFragment;
    private boolean flashOn = false;

    public boolean getFlashOn() {
        return flashOn;
    }

    private void initBottom() {

        bottomFragment = PreviewBottomFragment.newInstance();
        bottomFragment.setOnClickListener(this);
        bottomFragment.inject(mPresenter.getCapture(), mPresenter.getRecorder(), previewModel);
        getChildFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, bottomFragment)
                .commit();

    }

    /**
     * 如果是合拍页面 需要隐藏一些不需要显示的按钮
     */
    private void operateMenuUi() {
        switch (previewModel.getUIStyle()) {
            case PreviewConfig.Builder.UI_STYLE_DUT://合拍模式
                CURRENT_FEATURE = FEATURE_VIDEO;
                recordTab.setVisibility(View.INVISIBLE);
                tabIndexLine.setVisibility(View.INVISIBLE);
                img_duet_change.setVisibility(hasRecord ? View.GONE : View.VISIBLE);
                vRatio.setVisibility(View.GONE); //合拍模式下 不切换画幅
                break;
            case PreviewConfig.Builder.UI_STYLE_CUT_SAME://剪同款模式
                vRatio.setVisibility(View.GONE);
                vResolution.setVisibility(View.GONE);
                break;
            default:
                img_duet_change.setVisibility(View.GONE);
                break;
        }
    }

    private boolean isFrontCamera = false;
    private ImageView img_duet_change;

    @SuppressLint("ClickableViewAccessibility")
    private void initView(Bundle savedInstanceState) {

        FeatureView featureView = findViewById(R.id.feature);
        featureView.setOnZoomListener(new FeatureView.OnZoomListener() {
            @Override
            public void zoom(float zoom) {
                int temp = (int) zoom / 10;
                tvZoom.setText((temp == 0 ? 1 : temp) + ".0x");
            }
        });

        featureView.setOnFocusEnable(new FeatureView.OnFocusEnable() {
            @Override
            public boolean focusEnable() {
                Fragment showingFragment = showingFragment();
                if (showingFragment instanceof EffectFragment || showingFragment instanceof StickerFragment) {
                    closeFeature(true);
                    return false;
                } else {
                    return true;
                }
            }
        });

        img_duet_change = findViewById(R.id.img_duet_change);
        img_duet_change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPresenter.onSwitchDuet();
            }
        });
        tv_record_time = findViewById(R.id.tv_record_time);
        ivCapture = findViewById(R.id.iv_capture);
        surfaceView = findViewById(R.id.preview);
        smallWindowContainer = findViewById(R.id.fl_recorder_small_container);
        recordTab = findViewById(R.id.record_tab);
        topFunction = findViewById(R.id.top_function);
        tabIndexLine = findViewById(R.id.tab_index_line);
        recordTab.setDefaultSelectIndex(0);
        recordTab.setListener(new RecordTabView.OnSelectedListener() {
            @Override
            public void onSelected(WinnowHolder holder) {
                int index = holder.getAdapterPosition();
                switch (index) {
                    case 0: { //拍照
                        CURRENT_FEATURE = FEATURE_PIC;
                        bottomFragment.refreshFeature(CURRENT_FEATURE);
                        tv_record_time.setVisibility(View.GONE);
                    }
                    break;
                    case 1: { //摄像
                        CURRENT_FEATURE = FEATURE_VIDEO;
                        bottomFragment.refreshFeature(CURRENT_FEATURE);
                        if (previewModel.getUIStyle() == PreviewConfig.Builder.UI_STYLE_CUT_SAME) {
                            tv_record_time.setVisibility(View.GONE);
                        } else {
                            tv_record_time.setVisibility(View.VISIBLE);
                        }
                    }
                    break;
                }
            }
        });

        //返回
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //变焦
        tvZoom = findViewById(R.id.zoom);
        tvZoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZoomConfig config = previewModel.zoomConfigs.get(previewModel.zoomAuto.get());
                ((TextView) v).setText(config.zoom + "X");
                previewModel.zoom(config.zoom == 1.0f ? 0 : config.zoom * 10);
            }
        });
        //定时拍摄
        findViewById(R.id.delay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                CountDown countDown = previewModel.setCountDown();
//                ((ImageView) v).setImageDrawable(getDrawable(countDown.res)); // // getDrawable需要android21 5.0以上才有 5.0以下的手机上会崩溃
                ((ImageView) v).setImageResource(countDown.res);
            }
        });


        //闪光灯
        findViewById(R.id.flash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //具体模式参考 VECameraSettings.CAMERA_FLASH_MODE
                v.setSelected(!v.isSelected());
                if (v.isSelected()) {
                    flashOn = true;
                    mPresenter.getCapture().switchFlashMode(CAMERA_FLASH_OFF); // CAMERA_FLASH_TORCH
                } else {
                    flashOn = false;
                    mPresenter.getCapture().switchFlashMode(CAMERA_FLASH_OFF);
                }
            }
        });
        findViewById(R.id.flash).setVisibility(isFrontCamera ? View.GONE : View.VISIBLE); //前置时隐藏闪光灯按钮

        //多画幅
        vRatio = findViewById(R.id.ratio);
        vRatio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasRecord) return;
                int i = previewModel.picAuto.get();
                ((TextView) v).setText(previewModel.pics[i]);
                previewModel.changePic(i);
            }
        });


        //输出分辨率
        vResolution = findViewById(R.id.resolution);
        vResolution.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasRecord) return;
                //切换输出分辨率
                int i = previewModel.resolutionAuto.get();
                ((TextView) v).setText(previewModel.RESOLUTIONS_NAME[i]);
                previewModel.changeRatio(i);
            }
        });

        //切换前后摄像头
        findViewById(R.id.switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //切前后摄像头
                tvZoom.setText("1.0X");
                mPresenter.getCapture().switchCamera();
                //解决前置开启了闪光灯后 到后置拍照不打开闪光灯
                mPresenter.getCapture().switchFlashMode(flashOn ? CAMERA_FLASH_ON : CAMERA_FLASH_OFF);

                isFrontCamera = !isFrontCamera;
                findViewById(R.id.flash).setVisibility(isFrontCamera ? View.GONE : View.VISIBLE);
            }
        });

        rootView = findViewById(R.id.content);
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                LogUtils.d("onTouch----");
                closeFeature(true);
                return false;
            }
        });

        // 处理手势按下 抬起的操作(关闭 恢复美颜)
        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_PRESS, String.class).observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (s.equals("ACTION_DOWN")) {
                    mPresenter.onNormalDown();
                } else if (s.equals("ACTION_UP")) {
                    mPresenter.onNormalUp();
                }
            }
        });

        operateMenuUi();

        if (savedInstanceState == null) {
            if (previewModel.getUIStyle() == PreviewConfig.Builder.UI_STYLE_CUT_SAME) {
                previewModel.adjustViewMargin(tabIndexLine, 266);
                previewModel.adjustViewMargin(tvZoom, 234);
            }
        }

    }

    /**
     * 关闭所有的 feature 面板
     * close all feature panel
     * showBoard:代表是否从面板中点击录制后 显示一些控件  防止从道具等面板中点击录制后，会显示顶部工具栏等
     *
     * @return whether close panel successfully 是否成功关闭某个面板，即是否有面板正在开启中
     */
    public boolean closeFeature(boolean showBoard) {
        Fragment showingFragment = showingFragment();
        // 如果有正在展示的面板 并且不是bottom
        if (showingFragment != null && !(showingFragment instanceof PreviewBottomFragment)) {
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.board_enter, R.anim.board_exit);
            ft.hide(showingFragment).commitNow();

            if (bottomFragment != null) {
                ft.show(bottomFragment).commitNow();
            } else {
                ToastUtils.show("bottomFragment为空了");
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (previewLifecycle != null) {
                        previewLifecycle.onBottomPanelVisible(false, hasRecord);
                    }
                }
            }, ANIMATOR_DURATION);
            showOrHideBoard(showBoard);

        }

        return showingFragment != null;
    }

    @Override
    public void onStartTask() {

    }

    @Override
    public void changeDuetImage(final String imagePath) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(getContext()).load(imagePath).into(img_duet_change);
            }
        });

    }

    @Override
    public void onRecorderNativeInit(int ret, String msg) {
        if (previewLifecycle != null) {
            previewLifecycle.onRecorderNativeInit(ret, msg);
        }
    }

    public void runOnUiThread(Runnable action) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(action);
        }
    }


    /**
     * 流程 ⑧
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        //退出界面时若正在录制，需要停止录制，否则容易出现不可预知的crash
//        stopRecord();
        //退出Camera Capture
        if (mPresenter.getCapture() != null) {
            mPresenter.getCapture().destroy();
        }
        //退出Recorder
        if (mPresenter.getRecorder() != null) {
            mPresenter.getRecorder().onDestroy();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d("preview onResume....");

        tvZoom.setText("1.0X");
        requestRecordFunctionPermissions();
        mPresenter.getCapture().open();

    }

    /**
     * 解决 录制预览中，切换后台或锁屏，时长超过1分钟，返回后预览黑屏的问题。
     */
    @Override
    public void onStop() {
        super.onStop();
        LogUtils.d("preview onStop....");
        mPresenter.getCapture().close();
    }

    @Override
    public void onStart() {
        super.onStart();
        mPresenter.getCapture().open();
    }

    @Override
    public void onItemClick(String type) {

        LogUtils.d("onItemClick:" + type);

        showFeature(type);
        topFunction.setVisibility(View.VISIBLE);
    }

    /**
     * 展示某一个 feature 面板
     * Show a feature panel
     *
     * @param tag tag use to mark Fragment 用于标志 Fragment 的 tag
     */
    protected void showFeature(String tag) {
        if (surfaceView == null) return;
        Fragment showingFragment = showingFragment();
        if (showingFragment != null) {
            getChildFragmentManager().beginTransaction().hide(showingFragment).commitNow();
        }

        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setCustomAnimations(R.anim.board_enter, R.anim.board_exit);
        Fragment fragment = fm.findFragmentByTag(tag);

        if (fragment == null) {
            fragment = generateFragment(tag);
            if (fragment == null) {
                ToastUtils.show("fragment为空");
                return;
            }
            ft.add(R.id.fragment_container, fragment, tag).show(fragment).commitNow();
        } else {
            ft.show(fragment).commitNow();
        }
        ((BaseFeatureFragment) fragment).refreshIcon(CURRENT_FEATURE);
        showOrHideBoard(false);
        if (previewLifecycle != null) {
            previewLifecycle.onBottomPanelVisible(true, hasRecord);
        }
    }

    /**
     * 展示或关闭菜单面板
     * show board
     *
     * @param show 展示
     */
    private void showOrHideBoard(boolean show) {
        if (show) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (showingFragment() == null) {

                    }
                    if (hasRecord) {
                        recordTab.setVisibility(View.INVISIBLE);
                        tabIndexLine.setVisibility(View.INVISIBLE);
                    } else {
                        recordTab.setVisibility(View.VISIBLE);
                        tabIndexLine.setVisibility(View.VISIBLE);
                    }
                    topFunction.setVisibility(View.VISIBLE);
                    tvZoom.setVisibility(View.VISIBLE);

                    operateMenuUi();
                }
            }, ANIMATOR_DURATION);
        } else {

            recordTab.setVisibility(View.GONE);
            topFunction.setVisibility(View.INVISIBLE);
            tabIndexLine.setVisibility(View.GONE);
            tvZoom.setVisibility(View.GONE);
            img_duet_change.setVisibility(View.GONE);
        }
    }

    private Fragment showingFragment() {
        if (bottomFragment != null && !bottomFragment.isHidden()) {
            return bottomFragment;
        } else if (mFilterFragment != null && !mFilterFragment.isHidden()) {
            return mFilterFragment;
        } else if (mStickerFragment != null && !mStickerFragment.isHidden()) {
            return mStickerFragment;
        } else if (mEffectFragment != null && !mEffectFragment.isHidden()) {
            return mEffectFragment;
        }
        return null;
    }

    private ICheckAvailableCallback mCheckAvailableCallback = new ICheckAvailableCallback() {
        @Override
        public boolean checkAvailable(int id) {

//            LogUtils.d("checkAvailable-- id为" + id);
//            if (mSavedAnimojiPath != null && !mSavedAnimojiPath.equals("")) {
//                ToastUtils.show(getString(R.string.tip_close_animoji_first));
//                return false;
//            }
            return true;
        }
    };


    /**
     * 根据 TAG 创建对应的 Fragment
     * Create the corresponding Fragment based on TAG
     *
     * @param tag tag
     * @return Fragment
     */
    private Fragment generateFragment(String tag) {

        if (tag.equals(TAG_STICKER)) {  // 道具贴纸

            if (mStickerFragment != null) return mStickerFragment;

            StickerFragment stickerFragment = new TabStickerFragment()
                    .setCheckAvailableCallback(mCheckAvailableCallback)
                    .setType(TYPE_STICKER);
            stickerFragment.setCallback(new StickerFragment.IStickerCallback() {
                @Override
                public void onStickerSelected(final File file) {

                    LogUtils.d(file == null ? "所选贴纸file为null" : "file路径：" + file.getAbsolutePath());  // "/stickers/weilandongrizhuang"

                    mPresenter.setSticker(file);

                }
            });
            mStickerFragment = stickerFragment;
            return stickerFragment;

        } else if (tag.equals(TAG_EFFECT) || tag.equals(TAG_FILTER)) { // 美颜

            if (mEffectFragment != null && tag.equals(TAG_EFFECT)) {
                return mEffectFragment;
            } else if (mFilterFragment != null && tag.equals(TAG_FILTER)) {
                return mFilterFragment;
            }

            final EffectFragment effectFragment = generateEffectFragment(tag.equals(TAG_FILTER) ? true : false);
            effectFragment.setCheckAvailableCallback(mCheckAvailableCallback)
                    .setCallback(new EffectFragment.IEffectCallback() {

                        @Override
                        public void updateComposeNodes(final String[] nodes) {

                            StringBuilder sb = new StringBuilder();
                            for (String item : nodes) {
                                sb.append(item);
                                sb.append(" ");
                            }
                            LogUtils.d("updateComposeNodes：" + sb.toString()); // eyeshadow/wanxiahong hair/anlan lip/shaonvfen beauty_Android_camera

                            mPresenter.setComposerNodes(nodes);

                        }

                        @Override
                        public void updateComposeNodeIntensity(final ComposerNode node) {

                            LogUtils.d("updateComposeNodeIntensity： node=" + node.getNode() + " key=" + node.getKey() + " progress=" + node.getValue());

                            mPresenter.updateComposerNode(node, true);

                        }

                        // 选择滤镜后，会回调此方法和onFilterValueChanged
                        @Override
                        public void onFilterSelected(final File file) {  //   /Filter_01_38

                            LogUtils.d("onFilterSelected： file=" + (file != null ? file.getAbsolutePath() : "file为null"));

                            mPresenter.onFilterSelected(file);
                        }


                        @Override
                        public void onFilterValueChanged(final float cur) {
                            LogUtils.d("onFilterValueChanged： cur=" + cur);

                            mPresenter.onFilterValueChanged(cur);
                        }

                        @Override
                        public void setEffectOn(final boolean isOn) {
                            LogUtils.d("setEffectOn： isOn=" + isOn);
                        }

                        @Override
                        public void onDefaultClick() {
                            LogUtils.d("onDefaultClick....");
//                            onFragmentWorking(effectFragment);
                        }
                    });
            if (tag.equals(TAG_EFFECT)) {
                mEffectFragment = effectFragment;
            } else if (tag.equals(TAG_FILTER)) {
                mFilterFragment = effectFragment;
            }
            return effectFragment;

        }
        return null;
    }

    private EffectFragment generateEffectFragment(boolean filter) {
        EffectFragment effectFragment = new EffectFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("body", true);
        bundle.putBoolean("only_filter", filter);
        bundle.putSerializable("effect_type", getEffectType());
        effectFragment.setArguments(bundle);
        return effectFragment;
//        return null;
    }


    protected EffectType getEffectType() {
        Bundle arguments = getArguments();
        Serializable s = null;
        if (arguments != null) {
            s = arguments.getSerializable(Constant.EFFECT_TYPE);
        }
        if (!(s instanceof EffectType)) return EffectType.CAMERA;
        return (EffectType) s;
    }

    /**
     * 更新剪同款素材约束条件
     */
    public void updateCutSameConstraints(int width, int height, long duration, boolean changeRenderSize) {
        previewModel.updateCutSameConstraints(width, height, duration, changeRenderSize);
        if (bottomFragment != null) {
            bottomFragment.updateCutSameConstraints(width, height, duration);
        }
    }

    /**
     * 拍摄的视频/图片，确认保存时回调
     *
     * @param isVideo 是否视频
     */
    public void onConfirmPreview(boolean isVideo) {
        if (isVideo) {
            mPresenter.getRecorder().clearAllFrags();
            LiveDataBus.getInstance().with(LiveEventConstant.EVENT_RECORD_CLEAN, Boolean.class).postValue(true);
        }
    }


    /**
     * 刷新槽位是否已满
     *
     * @param isFull
     */
    public void updatePickFull(boolean isFull) {
        if (bottomFragment != null) {
            bottomFragment.updatePickFull(isFull);
        }
    }


    /**
     * 定义一个回调接口，用于当用户选择其中一个面板时，
     * 关闭其他面板的回调，此接口由各 Fragment 实现，
     * 在 onClose() 方法中要完成各 Fragment 中 UI 的初始化，
     * 即关闭用户已经开启的开关
     * <p>
     * Define a callback interface for when a user selects one of the panels，
     * close the callback of the other panel, which is implemented by each Fragment
     * In the onClose() method, initialize the UI of each Fragment:
     * turn off the switch that the user has already turned on
     */
    public interface OnCloseListener {
        void onClose();
    }


    public interface ICheckAvailableCallback {
        boolean checkAvailable(int id);
    }


    public enum EffectType {
        CAMERA,
        VIDEO
    }

    public void showTime(String time) {
        LogUtils.d("time:" + time);
        tv_record_time.setText(time);
    }

    //是否已经录制了一段，如果录制了，控制ui上没法点击切换画幅和720P
    private boolean hasRecord = false;

    //isFromTakeVideo是否从录制视频后的操作 如果是 hasRecord = true
    public void showFeature(boolean isFromTakeVideo) {
        if (isFromTakeVideo) hasRecord = true;
        if (isFromTakeVideo) {
            tabIndexLine.setVisibility(View.GONE);
            recordTab.setVisibility(View.GONE);
            tvZoom.setVisibility(View.GONE);
        } else {
            tabIndexLine.setVisibility(View.VISIBLE);
            recordTab.setVisibility(View.VISIBLE);
            tvZoom.setVisibility(View.VISIBLE);
        }
        topFunction.setVisibility(View.VISIBLE);
        ivCapture.setVisibility(View.GONE);

        operateMenuUi();
    }

    public void exitRecord() {
        hasRecord = false;
        if (isDuet) img_duet_change.setVisibility(View.VISIBLE);
        tv_record_time.setVisibility(View.GONE);
        showFeature(false);
    }

    public void hideFeature() {
        tabIndexLine.setVisibility(View.GONE);
        recordTab.setVisibility(View.GONE);
        topFunction.setVisibility(View.INVISIBLE);
        tvZoom.setVisibility(View.GONE);
        img_duet_change.setVisibility(View.GONE);
        if (previewModel.getUIStyle() == PreviewConfig.Builder.UI_STYLE_CUT_SAME) {
            tv_record_time.setVisibility(View.GONE);
        } else {
            tv_record_time.setVisibility(CURRENT_FEATURE == FEATURE_PIC ? View.GONE : View.VISIBLE);
        }
    }


    private int PERMISSION_REQUEST_CODE = 0;
    private String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    /**
     * 申请录制模块需要的权限
     */
    private void requestRecordFunctionPermissions() {
        if (!PermissionUtil.hasPermission(getContext(), permissions)) {
            LogUtils.d("无权限，申请权限----");
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * 所有权限都确认完后 会回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (!PermissionUtil.hasPermission(getContext(), permissions)) {
            Toast.makeText(getContext(), getString(R.string.ck_tips_permission_require), Toast.LENGTH_LONG).show();
        } else {
            ToastUtils.show(getString(R.string.ck_tips_permission_granted));
        }
        finish();
    }

    private void finish() {
        getActivity().finish();
    }

    public void onBackPressed() {
        Fragment showingFragment = showingFragment();
        if (showingFragment instanceof EffectFragment || showingFragment instanceof StickerFragment) {
            closeFeature(true);
        } else {
            if (previewLifecycle == null || !previewLifecycle.onHandleBack()) {
                finish();
            }
        }
    }

    public void setPreviewLifecycle(PreviewLifecycle mLifecycle) {
        this.previewLifecycle = mLifecycle;
    }

}
