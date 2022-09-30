package com.angelstar.ola;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.angelstar.ola.adapter.MixerRecyclerViewAdapter;
import com.angelstar.ola.entity.MixerItemEntry;
import com.angelstar.ola.interfaces.ITemplateVideoStateListener;
import com.angelstar.ola.interfaces.OnMixerItemClickListener;
import com.angelstar.ola.player.IPlayerActivityDelegate;
import com.angelstar.ola.player.TemplateActivityDelegate;
import com.angelstar.ola.view.FloatSliderView;
import com.angelstar.ybj.xbanner.OlaBannerView;
import com.angelstar.ybj.xbanner.VideoItemView;
import com.angelstar.ybj.xbanner.indicator.RectangleIndicator;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.ss.ugc.android.editor.core.NLEEditorContext;
import com.ss.ugc.android.editor.core.utils.DLog;
import com.ss.ugc.android.editor.main.template.SpaceItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class OlaTemplateFeedActivity extends AppCompatActivity implements OlaBannerView.ScrollPageListener {

    NLEEditorContext nleEditorContext;
    private IPlayerActivityDelegate editorActivityDelegate;
    private SurfaceView mSurfaceView;

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
                    if (mVideoItemView.getVideStateView().isActivated()) {
                        nleEditorContext.getVideoPlayer().pause();
                    } else {
                        startPlay(videoItemView.getVideStateView());
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
        initAnimals();
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

    private void showMixerMenu() {
        BottomSheetDialog answerSheetDialog = new BottomSheetDialog(this,R.style.MixerBottomSheetDialogTheme);
        View inflate = LayoutInflater.from(this).inflate(R.layout.layout_mixer_menu, null, false);
        answerSheetDialog.setContentView(inflate);
        answerSheetDialog.setCanceledOnTouchOutside(true);
        answerSheetDialog.setCancelable(true);
        answerSheetDialog.show();
        //设置透明背景
        answerSheetDialog.getWindow().findViewById(R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
    }

    //初始化调音台模拟数据
    private void initAnimals() {
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