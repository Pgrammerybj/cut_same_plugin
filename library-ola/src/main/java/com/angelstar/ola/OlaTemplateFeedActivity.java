package com.angelstar.ola;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.angelstar.ola.adapter.MixerRecyclerViewAdapter;
import com.angelstar.ola.adapter.SlideAdapter;
import com.angelstar.ola.entity.MixerItemEntry;
import com.angelstar.ola.interfaces.ITemplateVideoStateListener;
import com.angelstar.ola.interfaces.OnMixerItemClickListener;
import com.angelstar.ola.interfaces.ScaleSlideBarListener;
import com.angelstar.ola.interfaces.SimpleSeekBarListener;
import com.angelstar.ola.player.IPlayerActivityDelegate;
import com.angelstar.ola.player.TemplateActivityDelegate;
import com.angelstar.ola.view.FloatSliderView;
import com.angelstar.ola.view.ScaleSlideBar;
import com.angelstar.ola.viewmodel.TemplateNetPageModel;
import com.angelstar.ola.viewmodel.TemplateNetPageViewModelFactory;
import com.angelstar.ybj.xbanner.OlaBannerView;
import com.angelstar.ybj.xbanner.VideoItemView;
import com.angelstar.ybj.xbanner.indicator.RectangleIndicator;
import com.cutsame.solution.template.model.TemplateCategory;
import com.cutsame.solution.template.model.TemplateItem;
import com.cutsame.ui.CutSameUiIF;
import com.cutsame.ui.template.play.PlayCacheServer;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ss.ugc.android.editor.core.NLEEditorContext;
import com.ss.ugc.android.editor.core.utils.DLog;
import com.ss.ugc.android.editor.main.template.SpaceItemDecoration;
import com.ss.ugc.android.editor.picker.mediapicker.PickType;
import com.ss.ugc.android.editor.picker.mediapicker.PickerActivity;
import com.ss.ugc.android.editor.picker.mediapicker.PickerConfig;

import java.util.ArrayList;
import java.util.List;

public class OlaTemplateFeedActivity extends AppCompatActivity implements OlaBannerView.ScrollPageListener {

    private static final String TAG = "ola-jackyang";
    NLEEditorContext nleEditorContext;
    private IPlayerActivityDelegate editorActivityDelegate;
    private SurfaceView mSurfaceView;
    private TemplateNetPageModel templateNetPageModel;


    private ITemplateVideoStateListener videoStateListener = new ITemplateVideoStateListener() {

        @Override
        public void onPlayViewActivate(Boolean activate) {
            mVideoItemView.setActivated(activate);
        }

        @Override
        public void onPlayTimeChanged(String curPlayerTime, String totalPlayerTime, boolean isPause) {
            mTvCurrentPlayTime.setText(String.format("%s", curPlayerTime));
            mTvVideoTotalTime.setText(String.format("%s", totalPlayerTime));
            DLog.d("update seek progress/" + curPlayerTime + "//" + totalPlayerTime);
            if (nleEditorContext != null && !nleEditorContext.getVideoPlayer().isPlaying()
                    && nleEditorContext.getVideoPlayer().totalDuration() != 0) {
                //视频播放时动态更新全屏状态下的进度条
                float position = 100 * nleEditorContext.getVideoPlayer().curPosition() / nleEditorContext.getVideoPlayer().totalDuration();
                mFloatSliderView.setCurrPosition(position);
            }
            if (isPause) {
                mVideoItemView.getVideStateView().setImageResource(R.mipmap.icon_video_play);
            }
        }
    };
    private FloatSliderView mFloatSliderView;
    private TextView mTvCurrentPlayTime;
    private TextView mTvVideoTotalTime;
    //当前获得焦点的View
    private VideoItemView mVideoItemView;
    private RecyclerView mMixerRecyclerView;
    private List<MixerItemEntry> mixerList = new ArrayList<>();
    private ScaleSlideBar mScaleSlideBar;
    private RecyclerView mRcMenuMulti;
    private HttpProxyCacheServer httpProxyCacheServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_ola_template_homepage);


        mSurfaceView = new SurfaceView(this);

        editorActivityDelegate = new TemplateActivityDelegate(this, mSurfaceView);
        editorActivityDelegate.setViewStateListener(videoStateListener);
        editorActivityDelegate.onCreate(savedInstanceState);
        nleEditorContext = editorActivityDelegate.getNleEditorContext();

        TemplateCategory templateCategory = new TemplateCategory(0, "Vlog");
        templateNetPageModel = ViewModelProviders.of(this, new TemplateNetPageViewModelFactory(templateCategory)).get(TemplateNetPageModel.class);
        templateNetPageModel.loadFeedList(true);
        templateNetPageModel.getTemplateItems().observe(this, new Observer<List<TemplateItem>>() {
            @Override
            public void onChanged(List<TemplateItem> templateItems) {
                TemplateItem templateItem = templateItems.get(0);
                Log.i(TAG, "onChanged: 模版数据已经回来" + templateItems.size() + " | title;" + templateItem.getTitle());
                httpProxyCacheServer = PlayCacheServer.INSTANCE.getProxy(getApplicationContext());
            }
        });
        initView(mSurfaceView);
        initPlayerView();
    }

    private void initView(SurfaceView mSurfaceView) {
        OlaBannerView mBannerView = findViewById(R.id.banner_view);
        mBannerView.setIndicator(new RectangleIndicator(this));
        mBannerView.setScrollPageListener(this);
        List<String> bannerData = new ArrayList<>();
        bannerData.add("https://img2.baidu.com/it/u=3871466532,4184504555&fm=253&fmt=auto&app=138&f=JPEG?w=420&h=672");
        bannerData.add("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fpic1.win4000.com%2Fmobile%2F2018-11-02%2F5bdbfaa772b74.jpg%3Fdown&refer=http%3A%2F%2Fpic1.win4000.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=auto?sec=1666854321&t=328f07614130a0c57da081fe71971f35");
        bannerData.add("https://pics0.baidu.com/feed/77c6a7efce1b9d16ea9a0f497a8ffa878c546416.jpeg?token=fa7b61af9c9600dacdd91d593c08f4e7");
        bannerData.add("https://photo.tuchong.com/250829/f/31548923.jpg");
        bannerData.add("https://photo.tuchong.com/392724/f/16858773.jpg");
        bannerData.add("https://photo.tuchong.com/408963/f/18401047.jpg");
        bannerData.add("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fpic1.win4000.com%2Fmobile%2F2020-04-14%2F5e9563a01a89c.jpg&refer=http%3A%2F%2Fpic1.win4000.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=auto?sec=1666854399&t=69a35ee9f901a27fbad75e23e8911893");

        mBannerView.setBannerData(createVideoItem(bannerData), mSurfaceView);
    }

    private ArrayList<VideoItemView> createVideoItem(List<String> bannerData) {
        ArrayList<VideoItemView> itemList = new ArrayList<>();
        for (int i = 0; i < bannerData.size(); i++) {
            VideoItemView videoItemView = new VideoItemView(this);
            videoItemView.bindData(bannerData.get(i));
            videoItemView.setOnClickPlayListener(new VideoItemView.OnClickPlayStateListener() {
                @Override
                public void onVideoClick(View view) {
                    //点击视频播放/暂停按钮
                    if (mVideoItemView.getVideStateView().isActivated()) {
                        nleEditorContext.getVideoPlayer().pause();
                    } else {
                        startPlay(videoItemView.getVideStateView());
                    }
                }

                @Override
                public void onEditVideoClick(View view) {
                    //点击视频编辑按钮
                    List<TemplateItem> templateItems = templateNetPageModel.getTemplateItems().getValue();
                    TemplateItem templateItem = templateItems.get(0);
                    String videoCache = httpProxyCacheServer.getProxyUrl(templateItem.getVideoInfo().getUrl());
                    Intent cutSameIntent = CutSameUiIF.INSTANCE.createCutUIIntent(OlaTemplateFeedActivity.this, templateItem, videoCache);
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

    private void initPlayerView() {
        mFloatSliderView = findViewById(R.id.temp_video_player_seekbar);
        mTvCurrentPlayTime = findViewById(R.id.tv_current_play_time);
        mTvVideoTotalTime = findViewById(R.id.tv_total_video_time);
        mMixerRecyclerView = findViewById(R.id.recyclerview_video_mixer);
        initRecyclerView();
    }

    private void initRecyclerView() {
        initMockData();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mMixerRecyclerView.setLayoutManager(linearLayoutManager);
        MixerRecyclerViewAdapter adapter = new MixerRecyclerViewAdapter(this, mixerList);
        mMixerRecyclerView.addItemDecoration(new SpaceItemDecoration(0, 0, 0, 30));
        mMixerRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnMixerItemClickListener() {
            @Override
            public void onItemClick(View view, MixerItemEntry data, int position) {
                //弹出调音面板
                if (position == 0) {
                    showMixerMenu();
                }
                Toast.makeText(OlaTemplateFeedActivity.this, position == 0 ? "弹出调音面板" : data.getMixerTitle() + "+" + position, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initMixerMenuView() {

        //初始化带刻度的AISlideBar
        Resources resources = getResources();
        String[] slideContent = new String[]{"关", "低度", "中度", "高度"};
        SlideAdapter mAdapter = new SlideAdapter(resources, slideContent, R.drawable.selector_slide_bg);

        mScaleSlideBar.setAdapter(mAdapter);

        mScaleSlideBar.setPosition(2);

        mScaleSlideBar.setOnGbSlideBarListener(new ScaleSlideBarListener() {
            @Override
            public void onPositionSelected(int position) {
                Toast.makeText(OlaTemplateFeedActivity.this, "selected " + position, Toast.LENGTH_SHORT).show();
                Log.d("edanelx", "selected " + position);
            }
        });


        //调音面板内部混响条目
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRcMenuMulti.setLayoutManager(linearLayoutManager);
        MixerRecyclerViewAdapter adapter = new MixerRecyclerViewAdapter(this, mixerList);
        mRcMenuMulti.addItemDecoration(new SpaceItemDecoration(0, 0, 0, 30));
        mRcMenuMulti.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnMixerItemClickListener() {
            @Override
            public void onItemClick(View view, MixerItemEntry data, int position) {
                Toast.makeText(OlaTemplateFeedActivity.this, "选择混响：" + data.getMixerTitle(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMixerMenu() {
        BottomSheetDialog answerSheetDialog = new BottomSheetDialog(this, R.style.MixerBottomSheetDialogTheme);
        View inflate = LayoutInflater.from(this).inflate(R.layout.layout_mixer_menu, null, false);
        answerSheetDialog.setContentView(inflate);
        answerSheetDialog.setCanceledOnTouchOutside(true);
        answerSheetDialog.setCancelable(true);
        answerSheetDialog.show();
        //设置透明背景
        answerSheetDialog.getWindow().findViewById(R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);

        mScaleSlideBar = inflate.findViewById(R.id.scale_slide_bar);
        mRcMenuMulti = inflate.findViewById(R.id.recyclerview_mixer_menu_multi);
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
        peopleSeekBar.setProgress(68);
        tvPProgress.setText(String.valueOf(peopleSeekBar.getProgress()));
        accompanySeekBar.setProgress(47);
        tvAProgress.setText(String.valueOf(accompanySeekBar.getProgress()));
        tvPProgressT.setText("人声音量");
        tvAProgressT.setText("伴奏音量");
        peopleSeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvPProgress.setText(String.valueOf(progress));
            }
        });
        accompanySeekBar.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvAProgress.setText(String.valueOf(progress));
            }
        });
    }

    //初始化调音台模拟数据
    private void initMockData() {
        MixerItemEntry defaultItem = new MixerItemEntry("", "调音", "label", false);
        mixerList.add(defaultItem);
        MixerItemEntry xiangCun = new MixerItemEntry("imag", "乡村", "label", true);
        mixerList.add(xiangCun);
        MixerItemEntry jiedao = new MixerItemEntry("imag", "街道", "label", false);
        mixerList.add(jiedao);
        MixerItemEntry tianYuan = new MixerItemEntry("imag", "田园", "label", false);
        mixerList.add(tianYuan);
        MixerItemEntry dianZi = new MixerItemEntry("imag", "电子", "label", false);
        mixerList.add(dianZi);
        MixerItemEntry jueShi = new MixerItemEntry("imag", "爵士", "label", false);
        mixerList.add(jueShi);
        MixerItemEntry shuoChang = new MixerItemEntry("imag", "说唱", "label", false);
        mixerList.add(shuoChang);
        MixerItemEntry yaoGun = new MixerItemEntry("imag", "摇滚", "label", false);
        mixerList.add(yaoGun);
        MixerItemEntry guoFeng = new MixerItemEntry("imag", "国风", "label", false);
        mixerList.add(guoFeng);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (editorActivityDelegate != null) {
            editorActivityDelegate.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (editorActivityDelegate != null) {
            editorActivityDelegate.onPause();
        }
    }

    private void startPlay(ImageView videStateView) {
        if (null != nleEditorContext) {
            nleEditorContext.getVideoPlayer().play();
            if (null != videStateView) {
                Log.i("JackYang-Ola", "onPageSelected");
                videStateView.setImageResource(R.mipmap.icon_video_stop);
            }
        }
    }

    @Override
    public void onPageSelected(int position, VideoItemView videoItemView) {
        this.mVideoItemView = videoItemView;
        //切换ViewPager前先恢复播放器参数
        nleEditorContext.getVideoPlayer().resume();
        startPlay(videoItemView.getVideStateView());
    }
}