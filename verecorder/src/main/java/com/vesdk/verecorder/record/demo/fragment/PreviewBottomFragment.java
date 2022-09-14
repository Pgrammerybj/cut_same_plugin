package com.vesdk.verecorder.record.demo.fragment;

import static com.ss.android.vesdk.VECameraSettings.CAMERA_FLASH_MODE.CAMERA_FLASH_OFF;
import static com.ss.android.vesdk.VECameraSettings.CAMERA_FLASH_MODE.CAMERA_FLASH_ON;
import static com.ss.android.vesdk.VECameraSettings.CAMERA_FLASH_MODE.CAMERA_FLASH_TORCH;
import static com.vesdk.verecorder.record.demo.fragment.PreviewFragment.FEATURE_PIC;
import static com.vesdk.verecorder.record.demo.fragment.PreviewFragment.FEATURE_VIDEO;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dmcbig.mediapicker.utils.ScreenUtils;
import com.ss.android.ttve.model.VEFrame;
import com.ss.android.vesdk.VECameraCapture;
import com.ss.android.vesdk.VECameraSettings;
import com.ss.android.vesdk.VECommonCallback;
import com.ss.android.vesdk.VEFrameAvailableListener;
import com.ss.android.vesdk.VEGetFrameSettings;
import com.ss.android.vesdk.VEImageUtils;
import com.ss.android.vesdk.VEInfo;
import com.ss.android.vesdk.VEListener;
import com.ss.android.vesdk.VERecorder;
import com.ss.android.vesdk.VESize;
import com.ss.android.vesdk.VEUtils;
import com.ss.android.vesdk.model.VEPrePlayParams;
import com.vesdk.RecordInitHelper;
import com.vesdk.vebase.CommonUtils;
import com.vesdk.vebase.LiveDataBus;
import com.vesdk.vebase.LogUtils;
import com.vesdk.vebase.ToastUtils;
import com.vesdk.vebase.fragment.BaseFragment;
import com.vesdk.vebase.old.util.FileUtil;
import com.vesdk.verecorder.R;
import com.vesdk.verecorder.record.demo.adapter.VideoAdapter;
import com.vesdk.verecorder.record.demo.view.CircularProgressView;
import com.vesdk.verecorder.record.demo.view.CountDownDialog;
import com.vesdk.verecorder.record.demo.view.CustomLinearLayout;
import com.vesdk.verecorder.record.preview.model.CountDown;
import com.vesdk.verecorder.record.preview.model.LiveEventConstant;
import com.vesdk.verecorder.record.preview.model.PreviewConfig;
import com.vesdk.verecorder.record.preview.model.PreviewLifecycle;
import com.vesdk.verecorder.record.preview.viewmodel.PreviewModel;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PreviewBottomFragment extends BaseFragment implements CountDownDialog.Callback {
    private static final String TAG = "PreviewBottom";
    private static final int MSG_UPDATE_TIME = 0x01;
    public static final int TIME_DELAY = 0;
    private VECameraCapture capture;
    private VERecorder recorder;
    private TextView tv_pic_back, tv_pic_save, tv_editor;
    private LinearLayout llBeauty, llLeft;


    public static final String TAG_EFFECT = "effect";
    public static final String TAG_STICKER = "sticker";
    public static final String TAG_FILTER = "filter";
    public static final String TAG_ALGORITHM = "algorithm";
    public static final String TAG_ANIMOJI = "animoji";
    public static final String TAG_ARSCAN = "arscan";
    private View ib_go_editor;
    private RecyclerView recyclerView;


    public static PreviewBottomFragment newInstance() {
        Bundle args = new Bundle();
        PreviewBottomFragment fragment = new PreviewBottomFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getContentView() {
        return R.layout.recorder_fragment_bottom;
    }

    private CircularProgressView btStart;
    private RelativeLayout rl_recyclerview;
    private TextView tv_camera_time_limit;
    private LinearLayout llBottom1;

    private final Handler mHandler = new MyHandle(Looper.getMainLooper(), this);

    @Override
    protected void init(Bundle savedInstanceState) {
        rl_recyclerview = (RelativeLayout) findViewById(R.id.rl_recycleview);
        rl_recyclerview.setVisibility(View.GONE);

        ib_go_editor = findViewById(R.id.ib_go_editor);
        ib_go_editor.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goEditorActivity();
            }
        });

        llBottom1 = (LinearLayout) findViewById(R.id.ll_bottom1);
        btStart = (CircularProgressView) findViewById(R.id.progress);
        btStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CommonUtils.isFastClick(1500)) {
                    return;
                }
                clickCircleStart();
            }
        });
        tv_editor = (TextView) findViewById(R.id.tv_editor); //拍照后 导入剪辑的字体
        tv_pic_back = (TextView) findViewById(R.id.tv_pic_back); //拍照后的返回按钮
        tv_pic_back.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_pic_back.setVisibility(View.GONE);
                tv_pic_save.setVisibility(View.GONE);
                tv_editor.setVisibility(View.GONE);

                llBeauty.setVisibility(View.VISIBLE);
                llLeft.setVisibility(View.VISIBLE);

                ((PreviewFragment) getParentFragment()).showFeature(false);
                btStart.setSelected(!btStart.isSelected());
                capture.startPreview();
            }
        });

        tv_pic_save = (TextView) findViewById(R.id.tv_pic_save); //拍照后的保存按钮
        tv_pic_save.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.d("点击保存");
                ToastUtils.show(getString(R.string.ck_save_success) + picPath);
            }
        });

        tv_pic_back.setVisibility(View.GONE);
        tv_pic_save.setVisibility(View.GONE);
        tv_editor.setVisibility(View.GONE);

        LinearLayout ll_sticker = (LinearLayout) findViewById(R.id.ll_sticker);
        ll_sticker.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onItemClick(TAG_STICKER);
            }
        });
        LinearLayout ll_filter = (LinearLayout) findViewById(R.id.ll_filter);
        ll_filter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onItemClick(TAG_FILTER);
            }
        });

        llLeft = (LinearLayout) findViewById(R.id.ll_left);
        llBeauty = (LinearLayout) findViewById(R.id.ll_beauty);
        llBeauty.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener.onItemClick(TAG_EFFECT);
            }
        });
        tv_camera_time_limit = findViewById(R.id.tv_camera_time_limit);
        customLayout = (CustomLinearLayout) findViewById(R.id.customLayout);
        customLayout.setVisibility(View.INVISIBLE);
        customLayout.setonClick(new CustomLinearLayout.OnCustomClickItem() {
            @Override
            public void onClick(int duration, float speed, CustomLinearLayout.TimeStatus mSelectedTime, CustomLinearLayout.SpeedStatus mSelectedSpeed) {
                mCurrentTime = mSelectedTime;
                maxDuration = duration * 1000;
                currentSpeed = speed;
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.bottom_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(RecordInitHelper.getApplicationContext());  //LinearLayoutManager中定制了可扩展的布局排列接口，子类按照接口中的规范来实现就可以定制出不同排雷方式的布局了
        //配置布局，默认为vertical（垂直布局），下边这句将布局改为水平布局
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        mVideoItemList = new ArrayList<>();
        adapter = new VideoAdapter(mVideoItemList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new VideoAdapter.OnItemClickListener() {
            @Override
            public void onDeleteIconClick(View view, final int position) {
                checkPrePlay(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                recorder.deleteLastFrag();
                                btStart.removeSegment(position);
                                mPreviewModel.setVideoPartChange(true);
                                if (mCurrentTime != CustomLinearLayout.TimeStatus.TIME_free) {
                                    updateTimeLimit(recorder.getEndFrameTime());
                                }
                                updateNextEnable();
                                if (mVideoItemList.size() == 0) {
                                    exitRecord();
                                }
                            }
                        });
                    }
                });
            }

            @Override
            public void onItemClick(View view, int position) {
                //点击预览拍摄的视频
                String recordedVideoPath = recorder.getRecordedVideoPaths()[position];
                previewExportVideo(recordedVideoPath);
            }
        });

        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_START, String.class).observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                clickCircleStart();
            }
        });
        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_RECORD_CLEAN, Boolean.class).observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean s) {
                mVideoItemList.clear();
                updateVideoParts();
                btStart.cleanSegment();
                exitRecord();
            }
        });
        recorder.setOnInfoListener(new VECommonCallback() {
            @Override
            public void onCallback(int rc, int intValue, float fltValue, String msg) {
                if (rc == VEInfo.TE_RECORD_INFO_RECORDING_CLIP_TIMESTAMP) {
//                    Message message = Message.obtain(mHandler);
//                    message.what = MSG_UPDATE_TIME;
//                    message.arg1 = (int) (fltValue / 1000f + 0.5f);
                    mHandler.sendEmptyMessage(MSG_UPDATE_TIME);//录制时长刷新
                }
            }
        });
        updateNextEnable();
        switch (mPreviewModel.getUIStyle()) {
            case PreviewConfig.Builder.UI_STYLE_DUT:
                // 如果是合拍模式 布局需要有些更改
                refreshFeature(FEATURE_VIDEO);
                String duetPath = ((PreviewFragment) getParentFragment()).getDuetVideoPath();
                maxDuration = VEUtils.getVideoFileInfo(duetPath).duration; // 单位 ms
                LogUtils.d("maxDuration:" + maxDuration);
                customLayout.setDurationHide(true); // 合拍模式下隐藏录制时长选择
                mCurrentTime = CustomLinearLayout.TimeStatus.TIME_15;
                break;
            case PreviewConfig.Builder.UI_STYLE_CUT_SAME:
                ll_filter.setVisibility(View.GONE);
                llBottom1.setVisibility(View.GONE);
                ViewGroup.MarginLayoutParams stickerParams = (ViewGroup.MarginLayoutParams) ll_sticker.getLayoutParams();
                stickerParams.leftMargin = ScreenUtils.dp2px(getContext(), 63);
                ViewGroup.MarginLayoutParams beautyParams = (ViewGroup.MarginLayoutParams) llBeauty.getLayoutParams();
                beautyParams.rightMargin = ScreenUtils.dp2px(getContext(), 63);

                if (savedInstanceState == null) {//底部间距UI配置
                    View fragmentContainer = findViewById(R.id.rl_bottom_menu_container);
                    mPreviewModel.adjustViewMargin(fragmentContainer, 100f);
                }
                break;
        }
    }

    public void runOnUiThread(Runnable action) {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(action);
        }
    }

    /**
     * 更新剪同款素材约束条件
     */
    public void updateCutSameConstraints(int width, int height, long duration) {
        mCurrentTime = CustomLinearLayout.TimeStatus.TIME_CUSTOM_LIMIT;
        maxDuration = (int) duration;
        updateNextEnable();
    }


    public void updatePickFull(boolean isFull) {
        if (btStart == null) {
            return;
        }
        btStart.setEnabled(!isFull);
    }

    private void updateNextEnable() {
        if (ib_go_editor == null) {
            return;
        }
        if (mCurrentTime == CustomLinearLayout.TimeStatus.TIME_CUSTOM_LIMIT) {
            ib_go_editor.setEnabled(recorder.getEndFrameTime() >= maxDuration);
        } else {
            ib_go_editor.setEnabled(true);
        }
    }


    private void exitRecord() {
        checkStopPlay();
        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_RECORD_STATE, Integer.class).postValue(
                PreviewLifecycle.RECORD_CANCEL);//录制完成
        if (mPreviewModel.getUIStyle() == PreviewConfig.Builder.UI_STYLE_CUT_SAME) {
            llBottom1.setVisibility(View.GONE);
            tv_camera_time_limit.setVisibility(View.GONE);
        } else {
            llBottom1.setVisibility(View.VISIBLE);
        }
        rl_recyclerview.setVisibility(View.GONE);
        btStart.setBackgroundResource(R.drawable.bt_video_selector); // R.drawable.bg_take_pic_selector
        ((PreviewFragment) getParentFragment()).exitRecord();
        mPreviewModel.setVideoPartChange(true);
    }

    private void previewExportVideo(String exportFilePath) {
        File file = new File(exportFilePath);
        Uri uri;
        String packageName = requireActivity().getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(requireContext(), packageName + ".FileProvider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }


    public void inject(VECameraCapture capture, VERecorder recorder, PreviewModel previewModel) {
        this.capture = capture;
        this.recorder = recorder;
        this.mPreviewModel = previewModel;

        if (countDownObserver == null) {
            countDownObserver = new Observer<CountDown>() {
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onChanged(@Nullable CountDown countDown) {
                    LogUtils.d("countDown----" + countDown.name + "  " + countDown.delay);
                    startDialogCountDown(countDown.delay);
                }
            };
            previewModel.countDown.observe(this, countDownObserver);
        }

    }


    /**
     * 点击圆形按钮 开始录制或拍照
     */
    private void clickCircleStart() {
        if (mCurrent_feature == FEATURE_PIC) { // 拍照
            if (mPreviewModel.getCountDown().delay == 0 || btStart.isSelected()) { // //直接开始拍照
                startTakePic();
            } else {
                mPreviewModel.delayRecord(); // 定时录制
            }
        } else if (mCurrent_feature == FEATURE_VIDEO) { // 录像

            if (mPreviewModel.getCountDown().delay == 0 || mCurrentStatus == CameraStatus.Recording) { //直接开始录制 || !isFirst
                takeVideo();
            } else {
                mPreviewModel.delayRecord(); // 定时录制
            }
        }

    }

    private List<Bitmap> mVideoItemList;
    private CustomLinearLayout customLayout;
    private VideoAdapter adapter;

    private void goEditorActivity() {
        LiveDataBus.getInstance().with(LiveEventConstant.EVENT_GO_EDIT, Boolean.class).postValue(true);
    }

    private String picPath;

    /**
     * 开始拍照
     */
    private void startTakePic() {

        if (btStart.isSelected()) {
            Intent intent = new Intent();
            intent.setPackage(requireActivity().getPackageName());
            intent.setAction("record_sdk_action_ve");
            intent.putExtra("extra_key_from_type", 1);
            intent.putExtra("extra_media_type", 1);
            ArrayList<String> videoPaths = new ArrayList<>();
            videoPaths.add(picPath);
            intent.putStringArrayListExtra("extra_video_paths", videoPaths);
            startActivity(intent);
            requireActivity().finish();
            return;
        }

        int width = 720;
        int height = 1280;
        if (mPreviewModel.getResolution() != null) {
            width = mPreviewModel.getResolution().width;
            height = mPreviewModel.getResolution().height;
        }

        final boolean flashOn = ((PreviewFragment) getParentFragment()).getFlashOn();
        capture.switchFlashMode(flashOn ? CAMERA_FLASH_TORCH : CAMERA_FLASH_OFF);
        picPath = mPreviewModel.getSavePath(getContext());

        if (mPreviewModel.getUIStyle() == PreviewConfig.Builder.UI_STYLE_CUT_SAME) {
            takePhoto(picPath, width, height, 0, false);

        } else {
            shotScreen(picPath, width, height, flashOn);
        }

    }

    private void shotScreen(final String picPath, int width, int height, final boolean flashOn) {
        recorder.shotScreen(width, height, false, true, new VERecorder.IBitmapShotScreenCallback() {
            @Override
            public void onShotScreen(Bitmap bitmap, int ret) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        onShotSuccess();
                    }
                });

                capture.switchFlashMode(flashOn ? CAMERA_FLASH_OFF : CAMERA_FLASH_OFF);
                LiveDataBus.getInstance().with(LiveEventConstant.EVENT_FRAGMENT, Bitmap.class).postValue(bitmap);
                LogUtils.d(TAG, "onImage: ----" + bitmap + "  " + Thread.currentThread().getName());
                VEImageUtils.compressToJPEG(bitmap, 80, picPath);
                LiveDataBus.getInstance().with(LiveEventConstant.EVENT_SHOT_SUCCESS, String.class).postValue(picPath);
                //通知相册刷新
                sendNotice();
            }
        }, false);
    }

    private void takePhoto(
            final String imagePath,
            final int realWidth,
            final int realHeight,
            final int rotation,
            boolean isOpenShotTimeOptimize
    ) {
        //TODO 不开闪光灯用抽帧，开闪光灯用拍照
        if (isOpenShotTimeOptimize) {
            VEGetFrameSettings settings = new VEGetFrameSettings.Builder()
                    .setGetFrameType(VEGetFrameSettings.VEGetFrameType.HD_GET_FRAME_MODE)
                    .setEffectType(VEGetFrameSettings.VEGetFrameEffectType.SOME_EFFECT)
                    // 这里和高清拍照的线上参数保持一致即可
                    .setFitMode(VEGetFrameSettings.VEGetFrameFitMode.CENTER_CROP)
                    .setTargetResolution(new VESize(realWidth, realHeight))
                    // 这里都默认以1080p来算,即使用户选的是720p分辨率
                    .setRotation(rotation) // 这里和线上高清拍照的rotation保持一致
                    .setGetFrameCallback(new VEGetFrameSettings.IGetFrameCallback() {

                        @Override
                        public void onResult(int[] data, int width, int height) {
                            LogUtils.d("onResult--", "onResult");
                            Bitmap createBitmap = Bitmap.createBitmap(data, width, height, Bitmap.Config.ARGB_8888);
                            VEImageUtils.compressToJPEG(createBitmap, 100, imagePath);
                            LiveDataBus.getInstance().with(LiveEventConstant.EVENT_SHOT_SUCCESS, String.class).postValue(picPath);
                            //通知相册刷新
                            sendNotice();
                        }
                    }).build();
            LogUtils.d("onResult--", "getPreviewFrame  start ");


            int rect = recorder.getPreviewFrame(settings);
            LogUtils.d("onResult--", "rect = " + rect);


        } else {
            capture.takePicture(new VECameraSettings.PictureCallback() {
                @Override
                public void onPictureTaken(VEFrame veFrame) {
                    VEGetFrameSettings getFrameSettings = new VEGetFrameSettings.Builder()
                            .setGetFrameType(VEGetFrameSettings.VEGetFrameType.RENDER_PICTURE_MODE)
                            .setEffectType(VEGetFrameSettings.VEGetFrameEffectType.SOME_EFFECT)
                            .setFitMode(VEGetFrameSettings.VEGetFrameFitMode.CENTER_CROP)
                            .setTargetResolution(new VESize(realWidth, realHeight))
                            .setRotation(rotation)
                            .setGetFrameCallback(new VEGetFrameSettings.IGetFrameCallback() {

                                @Override
                                public void onResult(int[] data, int width, int height) {

                                    Bitmap createBitmap = Bitmap.createBitmap(data, width, height, Bitmap.Config.ARGB_8888);
                                    VEImageUtils.compressToJPEG(createBitmap, 100, imagePath);
                                    LogUtils.d("onResult--", "takePicture imagePath = " + imagePath);
                                    LiveDataBus.getInstance().with(LiveEventConstant.EVENT_SHOT_SUCCESS, String.class).postValue(picPath);
                                    //通知相册刷新
                                    sendNotice();
                                }
                            }).build();

                    recorder.renderFrame(veFrame, getFrameSettings);
                }

                @Override
                public void onTakenFail(Exception e) {
                    LogUtils.i(TAG, "onTakenFail: " + e);
                }
            });
        }

    }


    private void onShotSuccess() {
        switch (mPreviewModel.getUIStyle()) {
            case PreviewConfig.Builder.UI_STYLE_CUT_SAME:
                //nothing
                break;
            default:
                btStart.setSelected(!btStart.isSelected());
                tv_pic_back.setVisibility(View.VISIBLE);
                tv_pic_save.setVisibility(View.VISIBLE);
                tv_editor.setVisibility(View.VISIBLE);
                llBeauty.setVisibility(View.GONE);
                llLeft.setVisibility(View.GONE);
                break;
        }
    }

    private void sendNotice() {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(picPath)));
        requireActivity().sendBroadcast(intent);
    }

    private CameraStatus mCurrentStatus = CameraStatus.STOP;

    /**
     * 定时结束
     */
    @Override
    public void onFinish() {
        if (mCurrent_feature == FEATURE_PIC) { // 拍照
            startTakePic();
        } else if (mCurrent_feature == FEATURE_VIDEO) { // 录像
            takeVideo();
        }
    }

    public enum CameraStatus {
        STOP,
        Recording
    }


    public boolean isFeatureVideo() {
        return mCurrent_feature == FEATURE_VIDEO;
    }

    /**
     * 开始录制
     */
    public void takeVideo() {
        if (!isFeatureVideo()) {
            ToastUtils.show(getString(R.string.ck_tips_please_click_video_btn));
            return;
        }

        boolean flashOn = ((PreviewFragment) getParentFragment()).getFlashOn();
        if (mCurrentStatus == CameraStatus.STOP) {
            if (mCurrentTime != CustomLinearLayout.TimeStatus.TIME_free && recorder.getEndFrameTime() >= maxDuration) {
                LogUtils.d("run到达录制时长1111--------" + recorder.getEndFrameTime());
                ToastUtils.show(getString(R.string.ck_tips_reached_recording_time));
                return;
            }
            LiveDataBus.getInstance().with(LiveEventConstant.EVENT_RECORD_STATE, Integer.class).postValue(
                    PreviewLifecycle.RECORD_START//录制开始
            );
            capture.switchFlashMode(flashOn ? CAMERA_FLASH_TORCH : CAMERA_FLASH_OFF);
            customLayout.showBottom();
            hideVideoFeature(true); //开始录制时隐藏面板

            btStart.setSelected(true);
            btStart.setBackgroundResource(R.drawable.bt_video_selector_restart); // R.drawable.bg_take_pic_selector

            checkPrePlay(new Runnable() {
                @Override
                public void run() {
                    if (mPreviewModel.enableAudioRecorder()) {
                        recorder.enableAudioRecorder(true);//open before start record
                    }
                    recorder.startRecord(currentSpeed);
                    mCurrentStatus = CameraStatus.Recording;
                }
            });
        } else {
            LiveDataBus.getInstance().with(LiveEventConstant.EVENT_RECORD_STATE, Integer.class).postValue(
                    PreviewLifecycle.RECORD_PAUSE//录制暂停
            );
            capture.switchFlashMode(flashOn ? CAMERA_FLASH_ON : CAMERA_FLASH_OFF);
            mHandler.removeMessages(MSG_UPDATE_TIME);
            LogUtils.d("totalDuration:" + recorder.getEndFrameTime());
            btStart.setSelected(false);
            if (mPreviewModel.enableAudioRecorder()) {
                recorder.enableAudioRecorder(false);//close audio when stop
            }
            if (mPreviewModel.isRefactorRecorder()) {//新版为异步接口
                recorder.stopRecord(new VEListener.VECallListener() {
                    @Override
                    public void onDone(int i) {
                        onRecordSuccess();
                    }
                });
            } else {//旧版录制为同步接口
                recorder.stopRecord();
                onRecordSuccess();
            }
            mCurrentStatus = CameraStatus.STOP;

            hideVideoFeature(false); //停止录制时展现面板


        }
    }

    private void checkPrePlay(final Runnable action) {
        PreviewConfig previewConfig = mPreviewModel.getPreviewConfig();
        if (previewConfig != null && previewConfig.getStopPrePlay()) {
            if (0 != recorder.stopPrePlay(new VEListener.VECallListener() {
                @Override
                public void onDone(int i) {
                    action.run();
                }
            })) {
                action.run();
            }
        } else {
            action.run();
        }
    }

    private void checkStopPlay() {
        PreviewConfig previewConfig = mPreviewModel.getPreviewConfig();
        if (previewConfig != null && previewConfig.getStopPrePlay()) {
            recorder.startPrePlay(
                    new VEPrePlayParams(-1, true,
                            VEPrePlayParams.StopStrategy.ONLY_SHOT_WINDOW)
            );
        }
    }

    private void onRecordSuccess() {
        //视频缩略图
        VEUtils.getVideoFrames2(recorder.getRecordedVideoPaths()[recorder.getRecordedVideoPaths().length - 1], new int[]{0}, 0, 0, false, new VEFrameAvailableListener() {
            @Override
            public boolean processFrame(ByteBuffer frame, int width, int height, int ptsMs) {
                Bitmap stitchBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                stitchBmp.copyPixelsFromBuffer(frame.position(0));
                mVideoItemList.add(stitchBmp);
                updateVideoParts();
                return true;
            }
        });
        if (mCurrentTime != CustomLinearLayout.TimeStatus.TIME_free && recorder.getEndFrameTime() >= maxDuration) {
            //尝试进入编辑页
            goEditorActivity();
        }
        btStart.addSegment(recorder.getEndFrameTime());
        mPreviewModel.setVideoPartChange(true);
    }

    private void updateVideoParts() {
        adapter.setList(mVideoItemList);
        adapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(mVideoItemList.size() - 1);
        updateNextEnable();
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtils.e("bottomFragment onPause....");
        // 退到后台 停止录制
        if (mCurrent_feature == FEATURE_VIDEO && mCurrentStatus == CameraStatus.Recording) {
            LogUtils.e("bottomFragment onPause1111111....");
            takeVideo();
        }
    }

    // 点击录制后，是否隐藏面板
    private void hideVideoFeature(boolean hide) {
        rl_recyclerview.setVisibility(hide ? View.GONE : View.VISIBLE);
        llLeft.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);
        llBeauty.setVisibility(hide ? View.INVISIBLE : View.VISIBLE);

        if (hide) {
            llBottom1.setVisibility(View.GONE);
            ((PreviewFragment) getParentFragment()).hideFeature();
        } else {
            ((PreviewFragment) getParentFragment()).showFeature(true);
        }
    }

    private CustomLinearLayout.TimeStatus mCurrentTime = CustomLinearLayout.TimeStatus.TIME_free;
    private int maxDuration = -1 * 1000;
    private float currentSpeed = 1.0f;
    private Observer<CountDown> countDownObserver;

    private static class MyHandle extends Handler {
        private WeakReference<PreviewBottomFragment> reference;

        public MyHandle(@NonNull Looper looper, PreviewBottomFragment fragment) {
            super(looper);
            reference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            PreviewBottomFragment fragment = reference.get();
            if (msg.what == MSG_UPDATE_TIME) {
                if (fragment != null) {
                    fragment.updateRecordTime();
                }
            }
        }
    }

    /**
     * 更新录制时长
     */
    private void updateRecordTime() {
        long recordTime = recorder.getEndFrameTime();
        LogUtils.d("updateRecordTime-------time=" + recordTime);
        String time = FileUtil.stringForTime((int) recordTime);

        if (mCurrentTime == CustomLinearLayout.TimeStatus.TIME_CUSTOM_LIMIT) {
            tv_camera_time_limit.setVisibility(View.VISIBLE);
            updateTimeLimit(recordTime);
        } else {
            ((PreviewFragment) getParentFragment()).showTime(time);
        }
        if (mCurrentTime != CustomLinearLayout.TimeStatus.TIME_free) {
            btStart.setMaxDuration(maxDuration);
            btStart.setProgress((int) recordTime);
            if (recordTime >= maxDuration && mCurrentStatus == CameraStatus.Recording) {
                LogUtils.d("run到达录制时长--------" + recordTime);
                takeVideo();
            }
        }
    }

    private void updateTimeLimit(final long endFrameTime) {
        float max = maxDuration / 1000f;
        float endTime = Math.min(endFrameTime / 1000f, max);
        tv_camera_time_limit.setText(String.format(getString(R.string.format_time_limit), endTime, max));

    }

    private PreviewModel mPreviewModel;

    public void startDialogCountDown(int time) {
        CountDownDialog countDownDialog = new CountDownDialog(requireContext());
        countDownDialog.show();
        countDownDialog.start(time);
        countDownDialog.setCallBack(this);
    }

    /**
     * 定义RecyclerView选项单击事件的回调接口
     */
    public interface OnItemClickListener {

        void onItemClick(String type);
    }

    private OnItemClickListener onClickListener;

    //提供setter方法
    public void setOnClickListener(OnItemClickListener onItemClickListener) {
        this.onClickListener = onItemClickListener;
    }

    private int mCurrent_feature = FEATURE_PIC;

    public void refreshFeature(int current_feature) {
        mCurrent_feature = current_feature;
        if (current_feature == FEATURE_VIDEO) {
            btStart.setBackgroundResource(R.drawable.bt_video_selector); // R.drawable.bg_take_pic_selector
            customLayout.setVisibility(View.VISIBLE);
        } else if (current_feature == FEATURE_PIC) {
            btStart.setBackgroundResource(R.drawable.bt_pic_selector);
            customLayout.setVisibility(View.INVISIBLE);
        }
    }


}
