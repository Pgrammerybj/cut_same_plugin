package com.angelstar.ola;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.angelstar.ola.entity.CustomViewsInfo;
import com.angelstar.ola.holder.BannerHolderCreator;
import com.angelstar.ola.player.IPlayerActivityDelegate;
import com.angelstar.ola.player.TemplateActivityDelegate;
import com.angelstar.ybj.xbanner.XBanner;
import com.angelstar.ybj.xbanner.transformers.Transformer;
import com.ss.ugc.android.editor.base.theme.ThemeStore;
import com.ss.ugc.android.editor.base.utils.CommonUtils;
import com.ss.ugc.android.editor.core.NLEEditorContext;

import java.util.ArrayList;
import java.util.List;

public class OlaTemplateFeedActivity extends AppCompatActivity implements View.OnClickListener {

    NLEEditorContext nleEditorContext;
    private IPlayerActivityDelegate editorActivityDelegate;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ola_template_homepage);


        mSurfaceView = new SurfaceView(this);
        editorActivityDelegate = new TemplateActivityDelegate(this, mSurfaceView);
        editorActivityDelegate.onCreate(savedInstanceState);
        nleEditorContext = editorActivityDelegate.getNleEditorContext();

        initView(mSurfaceView);
        initPlayerView();
    }

    private void initView(SurfaceView mSurfaceView) {
        XBanner banner = findViewById(R.id.banner);
        List<CustomViewsInfo> data = new ArrayList<>();
        data.add(new CustomViewsInfo("https://photo.tuchong.com/250829/f/31548923.jpg"));
        data.add(new CustomViewsInfo("http://lf3-ck.bytetos.com/obj/template-bucket/7117128998756794382/baiyueguangzhushazhi_7002448424021524488/cover.png"));
        data.add(new CustomViewsInfo("https://photo.tuchong.com/392724/f/16858773.jpg"));
        data.add(new CustomViewsInfo("https://photo.tuchong.com/408963/f/18401047.jpg"));
        final BannerHolderCreator holderCreator = new BannerHolderCreator(mSurfaceView);
        banner.setBannerData(data, holderCreator);
        //设置轮播缩放效果
        banner.setPageTransformer(Transformer.Scale);
        banner.setOnItemClickListener(new XBanner.OnItemClickListener() {
            @Override
            public void onItemClick(XBanner banner, Object model, View view, int position) {
                Toast.makeText(OlaTemplateFeedActivity.this, "点击了" + position, Toast.LENGTH_SHORT).show();
            }
        });


        banner.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            int lastPagePosition = 0;

            @SuppressLint("LongLogTag")
            @Override
            public void onPageSelected(int currentPosition) {
                super.onPageSelected(currentPosition);
                Log.i("jackyang_onPageSelected 前一个页面lastPagePosition=" + lastPagePosition, " | 当前页面是=" + currentPosition);
//                if (holderCreator.videoViewHolder.surfaceViewContainer.getChildCount() == 0) {
////                        holderCreator.videoViewHolder.surfaceViewContainer.addView(mSurfaceView);
//                }
//                banner.getViewPager().getAdapter();
                lastPagePosition = currentPosition;
            }
        });

//        banner.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//
//            int lastPagePosition = 0;
//
//            @SuppressLint("LongLogTag")
//            @Override
//            public void onPageScrolled(int lastPosition, float v, int i1) {
//                Log.i("jackyang_onPageScrolled=", lastPosition + "");
////                lastPagePosition = lastPosition;
//            }
//
//            @SuppressLint("LongLogTag")
//            @Override
//            public void onPageSelected(int currentPosition) {
//                Log.i("jackyang_onPageSelected 前一个页面lastPagePosition=" + lastPagePosition, " | 当前页面是=" + currentPosition);
//                if (currentPosition == 0) {
//                    if (holderCreator.videoViewHolder.surfaceViewContainer.getChildCount() == 0) {
////                        holderCreator.videoViewHolder.surfaceViewContainer.addView(mSurfaceView);
//                    }
//                } else {
////                    holderCreator.videoViewHolder.videoView.pause();
//                }
//
//                lastPagePosition = currentPosition;
//            }
//
//            @SuppressLint("LongLogTag")
//            @Override
//            public void onPageScrollStateChanged(int i) {
//                Log.i("jackyang_ScrollStateChanged=", i + "");
//            }
//        });
    }

    private void initPlayerView() {
        ImageView iv_play = findViewById(com.ss.ugc.android.editor.main.R.id.iv_play);
        if (ThemeStore.INSTANCE.getPlayIconRes() != null && ThemeStore.INSTANCE.getPlayIconRes() > 0) {
            iv_play.setImageResource(ThemeStore.INSTANCE.getPlayIconRes());
        }
        iv_play.setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        if (CommonUtils.isFastClick()) {
            return;
        }
        if (v.getId() == com.ss.ugc.android.editor.main.R.id.iv_play) {
            if (v.isActivated()) {
                nleEditorContext.getVideoPlayer().setPlayingInFullScreen(false);
                nleEditorContext.getVideoPlayer().pause();
            } else {
                nleEditorContext.getVideoPlayer().setPlayingInFullScreen(true);
                nleEditorContext.getVideoPlayer().play();
            }
        }
    }
}