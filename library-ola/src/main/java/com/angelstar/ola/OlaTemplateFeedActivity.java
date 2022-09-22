package com.angelstar.ola;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.viewpager.widget.ViewPager;

import com.angelstar.ola.entity.CustomViewsInfo;
import com.angelstar.ola.holder.BannerHolderCreator;
import com.angelstar.ybj.xbanner.XBanner;
import com.angelstar.ybj.xbanner.transformers.Transformer;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class OlaTemplateFeedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ola_template_homepage);
        initView();
    }


    private void initView() {
        XBanner banner = findViewById(R.id.banner);
        List<CustomViewsInfo> data = new ArrayList<>();
        data.add(new CustomViewsInfo("https://photo.tuchong.com/250829/f/31548923.jpg"));
        data.add(new CustomViewsInfo("http://lf3-ck.bytetos.com/obj/template-bucket/7117128998756794382/baiyueguangzhushazhi_7002448424021524488/cover.png"));
        data.add(new CustomViewsInfo("https://photo.tuchong.com/392724/f/16858773.jpg"));
        data.add(new CustomViewsInfo("https://photo.tuchong.com/408963/f/18401047.jpg"));
        final BannerHolderCreator holderCreator = new BannerHolderCreator();
        banner.setBannerData(data, holderCreator);
        //设置轮播缩放效果
        banner.setPageTransformer(Transformer.Scale);
        banner.setOnItemClickListener(new XBanner.OnItemClickListener() {
            @Override
            public void onItemClick(XBanner banner, Object model, View view, int position) {
                Toast.makeText(OlaTemplateFeedActivity.this, "点击了" + position, Toast.LENGTH_SHORT).show();
            }
        });
        banner.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                Log.i("onPageScrolled=", i + "");
            }

            @Override
            public void onPageSelected(int i) {
                Log.i("onPageSelected=", i + "");
                if (i == 0) {
                    holderCreator.videoViewHolder.videoView.start();
                } else {
                    holderCreator.videoViewHolder.videoView.pause();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                Log.i("ScrollStateChanged=", i + "");
            }
        });
    }
}