package com.angelstar.ola;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.angelstar.ola.adapter.MixerEqualizerAdapter;
import com.angelstar.ola.adapter.MixerRecyclerViewAdapter;
import com.angelstar.ola.adapter.MixerReverbAdapter;
import com.angelstar.ola.adapter.SlideAdapter;
import com.angelstar.ola.effectcore.BbEffectConstants;
import com.angelstar.ola.effectcore.BbEffectCoreEventHandler;
import com.angelstar.ola.effectcore.BbEffectCoreImpl;
import com.angelstar.ola.entity.AudioMixingEntry;
import com.angelstar.ola.entity.OlaLyricsConvertSrtFile;
import com.angelstar.ola.entity.OlaTemplateResponse;
import com.angelstar.ola.interfaces.ITemplateVideoStateListener;
import com.angelstar.ola.interfaces.SimpleSeekBarListener;
import com.angelstar.ola.player.IPlayerActivityDelegate;
import com.angelstar.ola.player.TemplateActivityDelegate;
import com.angelstar.ola.utils.SizeUtil;
import com.angelstar.ola.view.FloatSliderView;
import com.angelstar.ola.view.ScaleSlideBar;
import com.angelstar.ybj.xbanner.OlaBannerView;
import com.angelstar.ybj.xbanner.VideoItemView;
import com.angelstar.ybj.xbanner.indicator.RectangleIndicator;
import com.cutsame.solution.template.model.TemplateItem;
import com.cutsame.ui.CutSameUiIF;
import com.cutsame.ui.template.play.PlayCacheServer;
import com.cutsame.ui.utils.JsonHelper;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ola.chat.picker.utils.MTUtils;
import com.ola.chat.picker.utils.SpaceItemDecoration;
import com.ola.download.RxNetDownload;
import com.ola.download.callback.DownloadCallback;
import com.ola.download.utils.CommonUtils;
import com.ss.ugc.android.editor.core.NLEEditorContext;
import com.ss.ugc.android.editor.core.api.params.AudioParam;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

public class OlaTemplateFeedActivity extends AppCompatActivity implements OlaBannerView.ScrollPageListener {

    private static final String TAG = "ola-jackyang";
    NLEEditorContext nleEditorContext;
    private IPlayerActivityDelegate editorActivityDelegate;
    private SurfaceView mSurfaceView;

    private FloatSliderView mFloatSliderView;
    private TextView mTvCurrentPlayTime;
    private TextView mTvVideoTotalTime;
    //当前获得焦点的View
    private VideoItemView mVideoItemView;
    private RecyclerView mMixerRecyclerView;
    private ScaleSlideBar mScaleSlideBar;
    private RecyclerView mRcMenuReverb, mRcMenuEqualizer;
    private HttpProxyCacheServer httpProxyCacheServer;
    //当前真实的选中的条目位置
    private int currentRealPosition = 0;
    private AudioMixingEntry mAudioMixingEntry;
    private MixerRecyclerViewAdapter adapter;
    private AudioParam audioParam;

    private final ITemplateVideoStateListener videoStateListener = new ITemplateVideoStateListener() {

        @Override
        public void onPlayViewActivate(Boolean activate) {
            mVideoItemView.setActivated(activate);
        }

        @Override
        public void onPlayTimeChanged(String curPlayerTime, String totalPlayerTime, boolean isPause) {
            mTvCurrentPlayTime.setText(String.format("%s", curPlayerTime));
            mTvVideoTotalTime.setText(String.format("%s", totalPlayerTime));
            if (nleEditorContext != null && nleEditorContext.getPlayer().isPlaying() && nleEditorContext.getPlayer().totalDuration() != 0) {
                //视频播放时动态更新全屏状态下的进度条
                float position = 100 * nleEditorContext.getPlayer().curPosition() / nleEditorContext.getPlayer().totalDuration();
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "curPlayerTime:" + 100 * nleEditorContext.getPlayer().curPosition() + " | totalPlayerTime:" + nleEditorContext.getPlayer().totalDuration());
                }
                mFloatSliderView.setCurrPosition(position);
            }

            if (isPause) {
                mVideoItemView.getVideStateView().setImageResource(R.mipmap.icon_video_play);
            }
        }
    };

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 10001) {
                switch (msg.arg1) {
                    case BbEffectConstants.stateAudioEffectFinish:
                        Toast.makeText(OlaTemplateFeedActivity.this, "我收到播放完成的消息啦", Toast.LENGTH_SHORT).show();
                        //播放完成后，重置到开头继续播放
                        BbEffectCoreImpl.INSTANCE.setAudioMixingPosition(0);
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MTUtils.makeStatusBarTransparent(this);
        setContentView(R.layout.activity_ola_template_homepage);
        //视频缓存框架
        httpProxyCacheServer = PlayCacheServer.INSTANCE.getProxy(getApplicationContext());
        mSurfaceView = new SurfaceView(this);

        mAudioMixingEntry = JsonHelper.fromJson(MockJson.getJson(this, "AudioMixingJson.json"), AudioMixingEntry.class);

        audioParam = OlaLyricsConvertSrtFile.INSTANCE.startConvert(mAudioMixingEntry, this);
        //todo:拉取我们自己的服务端模版

//        TemplateCategory templateCategory = new TemplateCategory(0, "Vlog");
//        templateNetPageModel = ViewModelProviders.of(this, new TemplateNetPageViewModelFactory(templateCategory)).get(TemplateNetPageModel.class);
//        templateNetPageModel.loadFeedList(true);
//        templateNetPageModel.getTemplateItems().observe(this, templateItems -> {
//            TemplateItem templateItem = templateItems.get(0);
//            Log.i(TAG, "onChanged: 模版数据已经回来" + templateItems.size() + " | title;" + templateItem.getTitle());
//        });
        //初始化调音台
        intiAudioMixing();
        initActivityDelegate();
        initView();
        initPlayerView();
    }

    /**
     * 调音台相关音效
     */
    private void intiAudioMixing() {
        String mixingFilePath = getExternalCacheDir().getAbsolutePath() + "/accompaniment_bea2c80d430f89100b643c8422193120.mp3";
        String voiceFilePath = getExternalCacheDir().getAbsolutePath() + "/record_287_2022-11-09-11-27-04.pcm";

        BbEffectCoreImpl.INSTANCE.createEffectCore(getApplicationContext(), new BbEffectCoreEventHandler(mHandler));
        BbEffectCoreImpl.INSTANCE.initialize(2);
        BbEffectCoreImpl.INSTANCE.setAudioEffectDataSource(mAudioMixingEntry.getEffectJson());
        BbEffectCoreImpl.INSTANCE.setAudioMixingFilePath(mixingFilePath, 0);
        BbEffectCoreImpl.INSTANCE.setAudioRecordFilePath(voiceFilePath, false);

        AudioMixingEntry.TunerModel tunerModel = mAudioMixingEntry.getTunerModel();
        //设置AI级别
        int aiLevel = tunerModel == null ? -1 : tunerModel.getAiIndex();
        BbEffectCoreImpl.INSTANCE.setParameters("{\"che.audio.ainoise.level\": " + aiLevel + "}");
        if (tunerModel != null) {
            BbEffectCoreImpl.INSTANCE.setAudioEffectPreset(mAudioMixingEntry.getTunerModel().getEffectIndex());
            BbEffectCoreImpl.INSTANCE.setCurrentEqualizerIndex(tunerModel.getEqualizerIndex());
            BbEffectCoreImpl.INSTANCE.setCurrentReverbIndex(tunerModel.getReverbIndex());
            if (mAudioMixingEntry.getTunerModel().isEnableInEarMonitoring()) {
                BbEffectCoreImpl.INSTANCE.enableInEarMonitoring(true);
            }

            if (tunerModel.getMixingPitch() != 0) {
                BbEffectCoreImpl.INSTANCE.setAudioMixingPitch(tunerModel.getMixingPitch());
            }
            BbEffectCoreImpl.INSTANCE.adjustRecordingSignalVolume(tunerModel.getRecordSignalVolume());
            BbEffectCoreImpl.INSTANCE.adjustAudioMixingVolume(tunerModel.getAudioMixingVolume());

            BbEffectCoreImpl.INSTANCE.adjustPlaybackSignalVolume(tunerModel.getPlaybackSignalVolume());
        }

        BbEffectCoreImpl.INSTANCE.setAudioProfile(mAudioMixingEntry.getAudioProfile());
        //先暂时这样吧，后续还需要优化结构和时机
        BbEffectCoreImpl.INSTANCE.start(); //需要和视频同步播放
    }

    private void initActivityDelegate() {
        editorActivityDelegate = new TemplateActivityDelegate(this, mSurfaceView, audioParam);
        editorActivityDelegate.setViewStateListener(videoStateListener);
        editorActivityDelegate.onCreate();
        nleEditorContext = editorActivityDelegate.getNleEditorContext();
    }

    private void initView() {
        OlaBannerView mBannerView = findViewById(R.id.banner_view);
        mBannerView.setIndicator(new RectangleIndicator(this));
        mBannerView.setScrollPageListener(this);

        // TODO: 2022/11/7 mock 模版网络数据 
        OlaTemplateResponse olaTemplateResponse = JsonHelper.fromJson(MockJson.TEMPLATE_ITEM_JSON, OlaTemplateResponse.class);

        if (null == olaTemplateResponse || olaTemplateResponse.getList().size() == 0) {
            return;
        }
        List<TemplateItem> templateItemList = olaTemplateResponse.getList();
        mBannerView.setBannerData(createVideoItem(templateItemList), mSurfaceView);
    }

    private ArrayList<VideoItemView> createVideoItem(List<TemplateItem> bannerData) {
        if (bannerData == null || bannerData.size() == 0) {
            return null;
        }
        ArrayList<VideoItemView> itemList = new ArrayList<>();

        for (int i = 0; i < bannerData.size(); i++) {
            TemplateItem templateItem = bannerData.get(i);
            if (templateItem == null || templateItem.getCover() == null || TextUtils.isEmpty(templateItem.getExtra())) {
                break;
            }

            //后台视频下载，下载策略和时机后续再优化
            String videoFilePath = downloadVideo(templateItem);
            VideoItemView videoItemView = new VideoItemView(this);
            videoItemView.bindData(templateItem.getCover().getUrl(), videoFilePath);
            videoItemView.setOnClickPlayListener(new VideoItemView.OnClickPlayStateListener() {
                @Override
                public void onVideoClick(View view) {
                    //点击视频播放/暂停按钮
                    if (mVideoItemView.getVideStateView().isActivated()) {
                        nleEditorContext.getPlayer().pause();
                        BbEffectCoreImpl.INSTANCE.pause();
                    } else {
                        startPlay(videoItemView.getVideStateView());
                        BbEffectCoreImpl.INSTANCE.resume();
                    }
                }

                @Override
                public void onEditVideoClick(View view) {
                    //点击视频编辑按钮
                    TemplateItem templateItem = bannerData.get(currentRealPosition);
                    if (null == templateItem
                            || templateItem.getFragmentCount() == 0
                            || templateItem.getVideoInfo() == null
                            || TextUtils.isEmpty(templateItem.getExtra())) {
                        //后端下发的模版数据有误，上报异常监控
                        Toast.makeText(OlaTemplateFeedActivity.this, "后端下发的模版数据有误，上报异常监控", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String videoCache = httpProxyCacheServer.getProxyUrl(templateItem.getVideoInfo().getUrl());
                    Intent cutSameIntent = CutSameUiIF.INSTANCE.createCutUIIntent(OlaTemplateFeedActivity.this, templateItem, audioParam, videoCache);
                    if (cutSameIntent != null) {
                        cutSameIntent.setPackage(getPackageName());
                        startActivity(cutSameIntent);
                    }
                }
            });
            itemList.add(videoItemView);
        }
        return itemList;
    }

    private String downloadVideo(TemplateItem bannerData) {
        if (bannerData.getVideoInfo() == null) {
            String errorMessage = "TemplateItem.VideoInfo不应该为null";
            Log.e(TAG, "downloadVideo: " + errorMessage);
            return errorMessage;
        }
        String videoUrl = bannerData.getVideoInfo().getUrl();
        String cacheVideoDir = CommonUtils.getCacheVideoDir(this);
        String videoName = videoUrl.substring(videoUrl.lastIndexOf("/"));
        RxNetDownload.execute(videoUrl, cacheVideoDir, videoName, new DownloadCallback() {
            @Override
            public void onStart(Disposable d) {
                Log.i(TAG, "开始下载: " + videoName);
            }

            @Override
            public void onProgress(long totalByte, long currentByte, int progress) {
            }

            @Override
            public void onFinish(File file) {
                Log.i(TAG, "下载完成: " + file.getAbsolutePath());
            }

            @Override
            public void onError(String msg) {
                Log.i(TAG, "onError: " + msg);
            }
        });
        return cacheVideoDir.concat(videoName);
    }

    private void initPlayerView() {
        mFloatSliderView = findViewById(R.id.temp_video_player_seekbar);
        mTvCurrentPlayTime = findViewById(R.id.tv_current_play_time);
        mTvVideoTotalTime = findViewById(R.id.tv_total_video_time);
        mMixerRecyclerView = findViewById(R.id.recyclerview_video_mixer);
        initRecyclerView();
    }

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mMixerRecyclerView.setLayoutManager(linearLayoutManager);
        adapter = new MixerRecyclerViewAdapter(this, mAudioMixingEntry.getBoardEffects(), -1);
        mMixerRecyclerView.addItemDecoration(new SpaceItemDecoration(0, 0, 0, 30));
        mMixerRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener((view, index, position) -> {
            //弹出调音面板
            if (position == 0) {
                showMixerMenu();
            } else {
                BbEffectCoreImpl.INSTANCE.setAudioEffectPreset(index);
                BbEffectCoreImpl.INSTANCE.changeReverbAndEqualizer(index, mAudioMixingEntry.getTunerModel());
            }
        });
    }

    private void initMixerMenuView() {
        //初始化带刻度的AISlideBar
        Resources resources = getResources();
        String[] slideContent = new String[]{"关", "低度", "中度", "高度"};
        SlideAdapter mAdapter = new SlideAdapter(resources, slideContent, R.drawable.selector_slide_bg);

        mScaleSlideBar.setAdapter(mAdapter);

        mScaleSlideBar.setPosition(mAudioMixingEntry.getTunerModel().getAiIndex());

        mScaleSlideBar.setOnGbSlideBarListener(position -> {
            BbEffectCoreImpl.INSTANCE.setParameters("{\"che.audio.ainoise.level\":" + (position - 1) + "}");
        });

        SpaceItemDecoration spaceItemDecoration = new SpaceItemDecoration(0, 0, 0, SizeUtil.INSTANCE.dp2px(7));

        //调音面板内部混响条目
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRcMenuReverb.setLayoutManager(linearLayoutManager);
        MixerReverbAdapter mixerReverbAdapter = new MixerReverbAdapter(this, mAudioMixingEntry.getReverbList(), mAudioMixingEntry.getTunerModel().getReverbIndex());
        mRcMenuReverb.addItemDecoration(spaceItemDecoration);
        mRcMenuReverb.setAdapter(mixerReverbAdapter);
        mixerReverbAdapter.setOnItemClickListener((view, data, position) -> {
            int boardEffectIndex = BbEffectCoreImpl.INSTANCE.changeAudioAndBoardEffect(0, data);
            adapter.setActivePosition(boardEffectIndex);
            Toast.makeText(OlaTemplateFeedActivity.this, "选择混响：" + data, Toast.LENGTH_SHORT).show();
        });

        //调音面板内部均衡器条目
        LinearLayoutManager linearLayoutManager2 = new LinearLayoutManager(this);
        linearLayoutManager2.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRcMenuEqualizer.setLayoutManager(linearLayoutManager2);
        MixerEqualizerAdapter mixerEqualizerAdapter = new MixerEqualizerAdapter(this, mAudioMixingEntry.getEqualizerEffects(), mAudioMixingEntry.getTunerModel().getEqualizerIndex());
        mRcMenuEqualizer.addItemDecoration(spaceItemDecoration);
        mRcMenuEqualizer.setAdapter(mixerEqualizerAdapter);
        mixerEqualizerAdapter.setOnItemClickListener((view, data, position) -> {
            int boardEffectIndex = BbEffectCoreImpl.INSTANCE.changeAudioAndBoardEffect(1, data);
            adapter.setActivePosition(boardEffectIndex);
            Toast.makeText(OlaTemplateFeedActivity.this, "选择均衡器：" + data, Toast.LENGTH_SHORT).show();
        });
    }

    private void showMixerMenu() {
        BottomSheetDialog answerSheetDialog = new BottomSheetDialog(this, R.style.MixerBottomSheetDialogTheme);
        @SuppressLint("InflateParams") View inflate = LayoutInflater.from(this).inflate(R.layout.layout_mixer_menu, null, false);
        answerSheetDialog.setContentView(inflate);
        answerSheetDialog.setCanceledOnTouchOutside(true);
        answerSheetDialog.setCancelable(true);
        answerSheetDialog.show();
        //设置透明背景
        answerSheetDialog.getWindow().findViewById(R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);

        mScaleSlideBar = inflate.findViewById(R.id.scale_slide_bar);
        mRcMenuReverb = inflate.findViewById(R.id.recyclerview_mixer_reverb);
        mRcMenuEqualizer = inflate.findViewById(R.id.recyclerview_mixer_equalizer);
        FrameLayout mSeekBarMixerPeople = inflate.findViewById(R.id.seekbar_mixer_people);
        FrameLayout mSeekBarMixerAccompany = inflate.findViewById(R.id.seekbar_mixer_accompany);
        initMixerSeekBar(mSeekBarMixerPeople, mSeekBarMixerAccompany);
        initMixerMenuView();
    }

    private void initMixerSeekBar(FrameLayout mSeekBarMixerPeople, FrameLayout mSeekBarMixerAccompany) {
        SeekBar peopleSeekBar = mSeekBarMixerPeople.findViewById(R.id.seekbar_mixer_people_voice);
        SeekBar accompanySeekBar = mSeekBarMixerAccompany.findViewById(R.id.seekbar_mixer_people_voice);
        TextView tvPProgress = mSeekBarMixerPeople.findViewById(R.id.tv_progress);
        TextView tvPProgressT = mSeekBarMixerPeople.findViewById(R.id.tv_progress_title);
        TextView tvAProgress = mSeekBarMixerAccompany.findViewById(R.id.tv_progress);
        TextView tvAProgressT = mSeekBarMixerAccompany.findViewById(R.id.tv_progress_title);
        peopleSeekBar.setProgress(mAudioMixingEntry.getTunerModel().getRecordSignalVolume());
        tvPProgress.setText(String.valueOf(peopleSeekBar.getProgress()));
        accompanySeekBar.setProgress(mAudioMixingEntry.getTunerModel().getAudioMixingVolume());
        tvAProgress.setText(String.valueOf(accompanySeekBar.getProgress()));
        tvPProgressT.setText("人声音量");
        tvAProgressT.setText("伴奏音量");
        peopleSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvPProgress.setText(String.valueOf(progress));
                BbEffectCoreImpl.INSTANCE.adjustRecordingSignalVolume(progress);
            }
        });
        accompanySeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvAProgress.setText(String.valueOf(progress));
                BbEffectCoreImpl.INSTANCE.adjustAudioMixingVolume(progress);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        BbEffectCoreImpl.INSTANCE.resume();
        if (editorActivityDelegate != null) {
            editorActivityDelegate.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        BbEffectCoreImpl.INSTANCE.pause();
        if (editorActivityDelegate != null) {
            editorActivityDelegate.onPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        BbEffectCoreImpl.INSTANCE.stop();
    }

    private void startPlay(ImageView videStateView) {
        if (null != nleEditorContext) {
            nleEditorContext.getPlayer().play();
            if (null != videStateView) {
                videStateView.setImageResource(R.mipmap.icon_video_stop);
            }
        }
    }

    @Override
    public void onPageSelected(int position, VideoItemView videoItemView) {
        this.mVideoItemView = videoItemView;
        currentRealPosition = position;
        //切换ViewPager前先恢复播放器参数
        if (null != nleEditorContext && null != editorActivityDelegate) {
            //切换视频后，应该从有歌词的位置开始播放，这个seekTo(0)目前不准确
            BbEffectCoreImpl.INSTANCE.setAudioMixingPosition(0);
            List<String> filePathList = new ArrayList<>();
            filePathList.add(mVideoItemView.videoFilePath);
            editorActivityDelegate.importVideoMedia(filePathList);
            nleEditorContext.getPlayer().resume();
            startPlay(videoItemView.getVideStateView());
        }
    }
}