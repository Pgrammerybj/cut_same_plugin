package com.angelstar.ola;

import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.angelstar.ola.player.IPlayerActivityDelegate;
import com.angelstar.ola.player.TemplateActivityDelegate;
import com.angelstar.ybj.xbanner.OlaBannerView;
import com.angelstar.ybj.xbanner.VideoItemView;
import com.angelstar.ybj.xbanner.indicator.RectangleIndicator;
import com.ss.ugc.android.editor.base.theme.ThemeStore;
import com.ss.ugc.android.editor.base.utils.CommonUtils;
import com.ss.ugc.android.editor.core.NLEEditorContext;

import java.util.ArrayList;
import java.util.List;

public class OlaTemplateFeedActivity extends AppCompatActivity implements View.OnClickListener, OlaBannerView.ScrollPageListener {

    NLEEditorContext nleEditorContext;
    private IPlayerActivityDelegate editorActivityDelegate;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_ola_template_homepage);


        mSurfaceView = new SurfaceView(this);
        editorActivityDelegate = new TemplateActivityDelegate(this, mSurfaceView);
        editorActivityDelegate.onCreate(savedInstanceState);
        nleEditorContext = editorActivityDelegate.getNleEditorContext();

        initView(mSurfaceView);
//        initPlayerView();
    }

    private void initView(SurfaceView mSurfaceView) {
        OlaBannerView mBannerView = findViewById(R.id.banner_view);
        mBannerView.setIndicator(new RectangleIndicator(this));

        List<String> bannerData = new ArrayList<>();
        bannerData.add("https://img2.baidu.com/it/u=3871466532,4184504555&fm=253&fmt=auto&app=138&f=JPEG?w=420&h=672");
        bannerData.add("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fpic1.win4000.com%2Fmobile%2F2018-11-02%2F5bdbfaa772b74.jpg%3Fdown&refer=http%3A%2F%2Fpic1.win4000.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=auto?sec=1666854321&t=328f07614130a0c57da081fe71971f35");
        bannerData.add("https://pics0.baidu.com/feed/77c6a7efce1b9d16ea9a0f497a8ffa878c546416.jpeg?token=fa7b61af9c9600dacdd91d593c08f4e7");
        bannerData.add("https://photo.tuchong.com/250829/f/31548923.jpg");
        bannerData.add("https://photo.tuchong.com/392724/f/16858773.jpg");
        bannerData.add("https://photo.tuchong.com/408963/f/18401047.jpg");
        bannerData.add("https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fpic1.win4000.com%2Fmobile%2F2020-04-14%2F5e9563a01a89c.jpg&refer=http%3A%2F%2Fpic1.win4000.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=auto?sec=1666854399&t=69a35ee9f901a27fbad75e23e8911893");

        mBannerView.setBannerData(createVideoItem(bannerData), mSurfaceView);
        mBannerView.setScrollPageListener(this);
    }

    private ArrayList<VideoItemView> createVideoItem(List<String> bannerData) {
        ArrayList<VideoItemView> itemList = new ArrayList<>();
        for (int i = 0; i < bannerData.size(); i++) {
            itemList.add(new VideoItemView(this).bindData(bannerData.get(i)));
        }
        return itemList;
    }

//    private void initPlayerView() {
//        ImageView iv_play = findViewById(com.ss.ugc.android.editor.main.R.id.iv_play);
//        if (ThemeStore.INSTANCE.getPlayIconRes() != null && ThemeStore.INSTANCE.getPlayIconRes() > 0) {
//            iv_play.setImageResource(ThemeStore.INSTANCE.getPlayIconRes());
//        }
//        iv_play.setOnClickListener(this);
//    }

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

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            return;
        }
//        if (v.getId() == R.id.iv_play) {
//            if (v.isActivated()) {
//                nleEditorContext.getVideoPlayer().setPlayingInFullScreen(false);
//                nleEditorContext.getVideoPlayer().pause();
//            } else {
//                startPlay();
//            }
//        }
    }

    private void startPlay() {
        if (null != nleEditorContext && null != nleEditorContext.getVideoPlayer()) {
            nleEditorContext.getVideoPlayer().setPlayingInFullScreen(true);
            nleEditorContext.getVideoPlayer().play();
        }
    }

    @Override
    public void onPageSelected(int position) {
        startPlay();
    }
}