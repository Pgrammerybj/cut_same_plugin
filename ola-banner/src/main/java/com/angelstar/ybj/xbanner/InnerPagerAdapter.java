package com.angelstar.ybj.xbanner;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.viewpager.widget.PagerAdapter;

import com.bumptech.glide.Glide;

import java.util.List;

/**
 * @Author：yangbaojiang
 * @Date: 2022/9/27 11:08
 * @E-Mail: pgrammer.ybj@outlook.com
 * TODO:内部Adapter，包装setAdapter传进来的adapter，设置getCount返回Integer.MAX_VALUE
 */
public class InnerPagerAdapter extends PagerAdapter {

    private final Context mContext;
    private final float mBannerRadius;
    /**
     * 轮播图地址集合
     */
    private final List<String> mBannerUrlList;

    public InnerPagerAdapter(Context context, float bannerRadius, List<String> bannerUrlList) {
        this.mBannerUrlList = bannerUrlList;
        this.mContext = context;
        this.mBannerRadius = bannerRadius;
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {
        return arg0 == arg1;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        CardView cardView = new CardView(mContext);
        cardView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cardView.setCardElevation(5);
        cardView.setRadius(mBannerRadius);

        ImageView bannerIv = new ImageView(mContext);
        bannerIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bannerIv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        int size = mBannerUrlList.size();
        if (size == 0) {
            return bannerIv;
        }
        position %= size;
        if (position < 0) {
            position = size + position;
        }
        Glide.with(mContext).load(mBannerUrlList.get(position)).into(bannerIv);


        //文字
        TextView textView = new TextView(mContext);
        textView.setText(String.valueOf(position));
        textView.setTextColor(mContext.getResources().getColor(android.R.color.holo_blue_light));
        textView.setTextSize(30);

        cardView.addView(bannerIv);
        cardView.addView(textView);
        container.addView(cardView);
        return cardView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
