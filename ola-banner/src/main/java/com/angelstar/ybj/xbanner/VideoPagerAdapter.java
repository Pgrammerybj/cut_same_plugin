package com.angelstar.ybj.xbanner;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/27 11:08
 * @E-Mail: pgrammer.ybj@outlook.com
 * TODO:内部Adapter，包装setAdapter传进来的adapter，设置getCount返回Integer.MAX_VALUE
 */
public class VideoPagerAdapter extends PagerAdapter {

    private final Context mContext;
    /**
     * 轮播图地址集合
     */
    private final ArrayList<VideoItemView> mBannerUrlList;

    public VideoPagerAdapter(Context context, ArrayList<VideoItemView> bannerUrlList) {
        this.mBannerUrlList = bannerUrlList;
        this.mContext = context;
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    public int getRealPosition(int position) {
        return position % mBannerUrlList.size();
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        if (getRealCount() == 0) {
            return null;
        }
        VideoItemView videoItemView = mBannerUrlList.get(getRealPosition(position));
        videoItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "点击了：" + getRealPosition(position), Toast.LENGTH_SHORT).show();
            }
        });
        container.addView(videoItemView);
        return videoItemView;
    }

    public int getRealCount() {
        return mBannerUrlList == null ? 0 : mBannerUrlList.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
