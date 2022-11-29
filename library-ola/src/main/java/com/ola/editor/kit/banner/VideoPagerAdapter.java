package com.ola.editor.kit.banner;

import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;

import java.util.ArrayList;

import static com.ola.editor.kit.banner.OlaBannerView.MAX_VALUE;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/27 11:08
 * @E-Mail: pgrammer.ybj@outlook.com
 * TODO:内部Adapter，包装setAdapter传进来的adapter，设置getCount返回MAX_VALUE
 */
public class VideoPagerAdapter extends PagerAdapter {

    /**
     * 轮播图地址集合
     */
    private ArrayList<VideoItemView> mBannerUrlList;

    public VideoPagerAdapter(ArrayList<VideoItemView> bannerUrlList) {
        this.mBannerUrlList = bannerUrlList;
    }

    public void updateBannerUrlLis(ArrayList<VideoItemView> bannerUrlList){
        this.mBannerUrlList = bannerUrlList;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return MAX_VALUE;
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
            return container;
        }
        VideoItemView videoItemView = mBannerUrlList.get(getRealPosition(position));
        //为了保证安全先移除
        ViewGroup viewGroup = (ViewGroup) videoItemView.getParent();
        if (null != viewGroup) {
            viewGroup.removeView(videoItemView);
        }
        container.addView(videoItemView);
        return videoItemView;
    }

    public int getRealCount() {
        return mBannerUrlList == null ? 0 : mBannerUrlList.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
//        container.removeView((View) object);
    }
}
